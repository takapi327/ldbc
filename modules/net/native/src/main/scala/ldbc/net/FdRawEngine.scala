/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.IOException
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference }
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.scalanative.libc.errno.errno
import scala.scalanative.meta.LinktimeInfo
import scala.scalanative.posix.errno.{ EAGAIN, EINPROGRESS, EWOULDBLOCK }
import scala.util.control.NonFatal

/**
 * Scala Native [[RawIoEngine]]: a single daemon poller thread drives non-blocking sockets through
 * `epoll`/`kqueue`, invoking one-shot callbacks on readiness. The effect-free counterpart of the `Fx`
 * `NativeIoEngine` that the generic `ldbc.net.IoEngine[F]` wraps once with `F.async`.
 *
 * The connect timeout is applied at the `F` layer ([[ldbc.net.IoEngine.fromRaw]]); blocking DNS
 * (`getaddrinfo`) runs on a transient daemon thread so the poller thread is never stalled, and the TCP
 * handshake plus all reads/writes are non-blocking (design `NATIVE_EPOLL_IOENGINE_DESIGN.md`).
 *
 * Every connection in the process shares this one thread, so its failure modes are handled explicitly
 * rather than swallowed. Recoverable errors are reported and rate-limited so the loop survives without
 * burning a core; fatal ones end the thread but hand over to a replacement, which resumes the same
 * poller with every registration still intact.
 */
private[net] final class FdRawEngine(initial: Poller, baseName: String = "ldbc-net-fd-raw")
  extends RawIoEngine,
          PollerDiagnostics:

  @volatile private var poller: Poller = initial

  private val registry        = new ConcurrentHashMap[Int, ChannelState]()
  private val sockets         = new ConcurrentHashMap[Int, FdRawSocket]()
  private val pendingConnects = ConcurrentHashMap.newKeySet[Throwable => Unit]()
  private val tasks           = new ConcurrentLinkedQueue[() => Unit]()
  private val dispatching     = new AtomicReference[FdRawSocket](null)
  private val loopErrors      = new AtomicLong(0)
  private val failedHandovers = new AtomicInteger(0)
  private val generation      = new AtomicInteger(0)
  private val revivals        = new AtomicInteger(0)
  private val released        = new AtomicInteger(0)
  private val terminated      = new AtomicBoolean(false)
  private val threadName      = new AtomicReference[String](baseName)

  @volatile private var faultInjection: () => Unit = null

  @volatile private var dispatchFault: () => Unit = null

  override def loopErrorCount: Long = loopErrors.get()

  override def revivalCount: Int = revivals.get()

  override def releasedMultiplexers: Int = released.get()

  override def isTerminated: Boolean = terminated.get()

  override def liveSocketCount: Int = sockets.size()

  override def pollerThreadName: String = threadName.get()

  override def injectFault(fault: () => Unit): Unit =
    faultInjection = fault
    poller.wakeup()

  override def injectDispatchFault(fault: () => Unit): Unit =
    dispatchFault = fault
    poller.wakeup()

  private def enqueue(task: () => Unit): Unit =
    tasks.add(task)
    poller.wakeup()

  private def drainTasks(): Unit =
    var t = tasks.poll()
    while t != null do
      try t()
      catch case NonFatal(e) => reportLoopError(e)
      t = tasks.poll()

  /**
   * Runs the continuations armed for one ready fd.
   *
   * The socket being dispatched is published so that a thread dying part-way through can be accounted
   * for: the continuation has already been taken out of [[ChannelState]] by then, so a replacement
   * thread has no way to find it again.
   *
   * The whole body is isolated per fd. One event is one fd's worth of work, and the poller hands the
   * whole batch over in a single call: letting an exception out of here would abandon the events the
   * multiplexer already reported but has not delivered yet. Those fds are armed one-shot, so they are
   * disarmed the moment the event was produced — nothing would ever report them again, and every
   * socket in the rest of the batch would wait forever.
   *
   * `dispatching` is cleared when the work finishes and when a recoverable error is handled, but
   * deliberately **not** in a `finally`. A fatal error has to leave it set: the handover reads it to
   * find the one socket whose event was consumed but whose continuation never ran (§ [[failInFlight]]).
   */
  private def dispatch(fd: Int, readable: Boolean, writable: Boolean, error: Boolean): Unit =
    val st = registry.get(fd)
    if st != null then
      dispatching.set(sockets.get(fd))
      try
        val fault = dispatchFault
        if fault != null then fault()
        if error then fireAll(st)
        else
          if writable then
            val cb = st.writeReady
            st.writeReady  = null
            st.writeFailed = null
            val cc = st.connectReady
            st.connectReady = null
            if cc != null then cc()
            if cb != null then cb()
          if readable then
            val cb = st.readReady
            st.readReady  = null
            st.readFailed = null
            if cb != null then cb()
        dispatching.set(null)
      catch
        case NonFatal(e) =>
          dispatching.set(null)
          reportLoopError(e)

  /**
   * The poller loop.
   *
   * Recoverable errors are reported and slept off so the loop keeps serving the other connections.
   * Fatal ones are deliberately not caught: they leave through `finally`, which hands over to a
   * replacement thread, and then reach the default uncaught-exception handler so the stack trace is
   * not lost. `completed` records whether this thread ever finished a whole iteration, which is what
   * distinguishes a thread that ran for a while from one that died on startup.
   *
   * Unlike the JVM engine, a failing multiplexer here does not throw: `epoll_wait` and `kevent`
   * return `-1`, so the outcome has to be inspected rather than waited for as an exception.
   */
  private def runLoop(): Unit =
    var completed = false
    try
      while true do
        try
          if Thread.interrupted() then throw new InterruptedException("ldbc-net poller interrupted")
          val fault = faultInjection
          if fault != null then fault()
          poller.poll(dispatch) match
            case PollOutcome.Events(_)    => ()
            case PollOutcome.Retry        => ()
            case PollOutcome.Failed(code) =>
              reportLoopError(new IOException(s"poll failed (errno=$code)"))
              backoff()
          drainTasks()
          completed = true
        catch
          case NonFatal(e) =>
            reportLoopError(e)
            backoff()
    finally handover(completed)

  /**
   * Reports a swallowed error.
   *
   * `ldbc-net` has no runtime dependencies, so there is no logger to route this through; `System.err`
   * is the one sink available on all three platforms. Without it a poller that fails repeatedly is
   * indistinguishable from one that is idle.
   */
  private def reportLoopError(t: Throwable): Unit =
    loopErrors.incrementAndGet()
    System.err.println(s"[ldbc-net] poller loop error (${ Thread.currentThread().getName }): $t")
    t.printStackTrace()

  /** Pauses after a recoverable error so a persistent failure cannot spin the thread at full speed. */
  private def backoff(): Unit = Thread.sleep(FdRawEngine.BackoffMillis)

  /** Settles the callbacks of the socket whose continuations were being run when the thread died. */
  private def failInFlight(cause: Throwable): Unit =
    val victim = dispatching.getAndSet(null)
    if victim != null then settle(victim, cause)

  /**
   * Settles every socket the engine still tracks.
   *
   * Each one is guarded separately: these callbacks belong to user code, and one that throws must not
   * take the remaining sockets down with it — they would be left waiting on a poller that is already
   * gone, which is the failure this whole path exists to prevent.
   */
  /**
   * Failure handlers for connects that have not resolved yet.
   *
   * A connect in flight is not reachable through [[liveSockets]]: its callback expects a socket, not
   * a read or write result, so [[failAllPending]] cannot settle it. Its interest registration also
   * sits in the queue that [[giveUp]] discards. Without a handle of its own it would simply be
   * forgotten, which is the one outcome the completion contract rules out.
   */
  /**
   * The collections are snapshotted before anything is settled. Removing entries as they are visited
   * is not enough: a callback invoked here can reach back into the engine — a pool told that its
   * connection died will go and open another, and that one revives the engine — and a
   * `ConcurrentHashMap` iterator is free to hand back an entry added after the sweep began. Taking a
   * copy first makes the set of victims exactly the set that existed when the engine gave up.
   */
  private def failAllPending(cause: Throwable): Unit =
    new java.util.ArrayList(pendingConnects).forEach { fail =>
      pendingConnects.remove(fail)
      guarded(fail, cause)
    }
    failArmed(cause)
    new java.util.ArrayList(sockets.keySet()).forEach { fd =>
      val socket = sockets.remove(fd)
      registry.remove(fd)
      if socket != null then settle(socket, cause)
    }

  /**
   * Reports the failure to every continuation armed on the poller right now.
   *
   * These are not reachable through [[sockets]]: a continuation belongs to whoever armed it, and the
   * TLS layer arms its own while driving a handshake. Refusing new arms only covers callers that
   * arrive after the engine gave up — the ones already parked need telling.
   */
  private def failArmed(cause: Throwable): Unit =
    new java.util.ArrayList(registry.values()).forEach { st =>
      val r = st.readFailed
      st.readFailed = null
      st.readReady  = null
      if r != null then guarded(r, cause)
      val w = st.writeFailed
      st.writeFailed = null
      st.writeReady  = null
      if w != null then guarded(w, cause)
    }

  /**
   * Releases a multiplexer that is being replaced.
   *
   * Built before the old one is closed, so a failure to create the replacement leaves the engine with
   * a working poller rather than none at all.
   */
  private def closeQuietly(old: Poller): Unit =
    try
      old.close()
      released.incrementAndGet()
      ()
    catch case NonFatal(e) => reportLoopError(e)

  private def guarded(fail: Throwable => Unit, cause: Throwable): Unit =
    try fail(cause)
    catch case NonFatal(e) => reportLoopError(e)

  private def settle(socket: FdRawSocket, cause: Throwable): Unit =
    try socket.failPending(cause)
    catch case NonFatal(e) => reportLoopError(e)

  /**
   * Replaces the dying poller thread, or gives up.
   *
   * The whole body is guarded because this runs inside `finally`: an exception escaping here would
   * replace the fatal error on its way out, and that error is the only record of what went wrong.
   */
  private def handover(completed: Boolean): Unit =
    try
      failInFlight(new IOException("ldbc-net poller died while dispatching"))
      val n =
        if completed then { failedHandovers.set(0); 0 }
        else failedHandovers.incrementAndGet()
      if n >= FdRawEngine.MaxFailedHandovers then giveUp(s"gave up after $n failed handovers")
      else
        try startThread()
        catch case _: Throwable => giveUp("could not be replaced")
    catch case _: Throwable => ()

  /**
   * Stops trying to keep a poller alive and settles everything that was waiting on it.
   *
   * The sockets that were just failed stop being tracked, one by one as [[failAllPending]] settles
   * them. They are dead by definition — every operation on them has been settled and this engine
   * will never serve them again — so counting them afterwards would misreport what the engine is
   * responsible for. Their `close` still works; it simply has nothing left to deregister.
   *
   * Dropping the per-fd state here also keeps a later revival safe: the operating system reuses file
   * descriptors, so a stale entry left behind could be picked up by an unrelated socket that happens
   * to be given the same number.
   */
  private def giveUp(reason: String): Unit =
    terminated.set(true)
    tasks.clear()
    System.err.println(s"[ldbc-net] poller $reason")
    failAllPending(new IOException(s"ldbc-net poller $reason"))

  private[net] def startThread(): Unit =
    val n    = generation.incrementAndGet()
    val name = if n == 1 then baseName else s"$baseName-$n"
    threadName.set(name)
    val t = new Thread(() => runLoop(), name)
    t.setDaemon(true)
    t.start()

  /**
   * Brings a terminated engine back up with a fresh multiplexer and thread.
   *
   * Reaching the terminated state must not be the end of the process's database access: this engine
   * is a singleton, so a permanent stop would mean no connection could ever be made again. Nothing
   * needs to be carried over — every socket was already failed by [[giveUp]] — so a clean poller is
   * the right starting point.
   *
   * @return the reason a revival could not happen, or `None` when the engine is usable
   */
  private def revive(): Option[Throwable] =
    synchronized {
      if !terminated.get() then None
      else
        try
          val replacement = FdRawEngine.newPoller()
          closeQuietly(poller)
          poller = replacement
          failedHandovers.set(0)
          revivals.incrementAndGet()
          terminated.set(false)
          startThread()
          None
        catch case NonFatal(e) => Some(e)
    }

  private def fireAll(st: ChannelState): Unit =
    st.readFailed  = null
    st.writeFailed = null
    val c = st.connectReady; st.connectReady = null; if c != null then c()
    val r = st.readReady; st.readReady       = null; if r != null then r()
    val w = st.writeReady; st.writeReady     = null; if w != null then w()

  /**
   * Arms one-shot read readiness for `fd`.
   *
   * Refused once the engine has given up: with no thread left to drive the multiplexer, arming would
   * park the caller on a notification that can never arrive. The JVM engine refuses the equivalent
   * registration for the same reason.
   *
   * `onFailure` is required rather than optional, and is the only way the refusal can reach the
   * caller. Not every caller is a [[FdRawSocket]] — the Native TLS layer arms readiness with a
   * continuation of its own while driving the handshake — so settling the socket's pending read or
   * write is not enough to cover everyone waiting on this fd.
   */
  private[net] def armRead(fd: Int, st: ChannelState, ready: () => Unit, onFailure: Throwable => Unit): Unit =
    if terminated.get() then onFailure(terminatedError)
    else
      st.readReady  = ready
      st.readFailed = onFailure
      enqueue(() => poller.arm(fd, read = true, write = false))

  /** Arms one-shot write readiness for `fd`, refusing once the engine has given up (see [[armRead]]). */
  private[net] def armWrite(fd: Int, st: ChannelState, ready: () => Unit, onFailure: Throwable => Unit): Unit =
    if terminated.get() then onFailure(terminatedError)
    else
      st.writeReady  = ready
      st.writeFailed = onFailure
      enqueue(() => poller.arm(fd, read = false, write = true))

  private def terminatedError: IOException = new IOException("ldbc-net poller terminated")

  private[net] def deregisterAndClose(fd: Int): Unit =
    sockets.remove(fd)
    enqueue { () =>
      registry.remove(fd)
      poller.remove(fd)
      CInterop.closeFd(fd)
    }

  private[net] def start(): FdRawEngine =
    startThread()
    this

  override def connect(
    host:    String,
    port:    Int,
    options: SocketOptions,
    cb:      Either[Throwable, RawSocket] => Unit
  ): Canceler =
    revive() match
      case Some(error) =>
        cb(Left(new IOException("ldbc-net poller could not be restarted", error)))
        return Canceler.noop
      case None => ()

    val done  = new AtomicBoolean(false)
    val fdRef = new java.util.concurrent.atomic.AtomicInteger(-1)

    lazy val failConnect: Throwable => Unit = error =>
      if done.compareAndSet(false, true) then
        pendingConnects.remove(failConnect)
        val fd = fdRef.get()
        if fd >= 0 then deregisterAndClose(fd)
        cb(Left(error))

    pendingConnects.add(failConnect)

    val connect: Runnable = () =>
      try
        val resolved = CInterop.resolve(host, port)
        val fd       = resolved.fd
        fdRef.set(fd)
        CInterop.applyOptions(fd, options)
        val st = new ChannelState
        registry.put(fd, st)

        def finishConnect(): Unit =
          if done.compareAndSet(false, true) then
            pendingConnects.remove(failConnect)
            val soError = CInterop.socketError(fd)
            if soError == 0 then
              val socket = new FdRawSocket(fd, st, this)
              sockets.put(fd, socket)
              cb(Right(socket))
            else
              deregisterAndClose(fd)
              cb(Left(new java.io.IOException(s"connect to $host:$port failed (errno=$soError)")))

        st.connectReady = () => finishConnect()
        val err = CInterop.beginConnect(fd, resolved)
        if err == 0 then finishConnect()
        else if err == EINPROGRESS then enqueue(() => { poller.add(fd); poller.arm(fd, read = false, write = true) })
        else failConnect(new java.io.IOException(s"connect to $host:$port failed (errno=$err)"))
      catch case e: Throwable => failConnect(e)

    val worker = new Thread(connect, "ldbc-net-fd-connect")
    worker.setDaemon(true)
    worker.start()

    new Canceler:
      override def cancel(): Unit =
        if done.compareAndSet(false, true) then
          pendingConnects.remove(failConnect)
          val fd = fdRef.get()
          if fd >= 0 then deregisterAndClose(fd)

private[net] object FdRawEngine:
  private lazy val ignoreSigpipe: Unit = CInterop.ignoreSigpipe()

  /** Consecutive replacements that never completed an iteration before the engine gives up. */
  private[net] val MaxFailedHandovers = 3

  /** Pause after a recoverable loop error, matching the interval Netty's event loop uses. */
  private[net] val BackoffMillis = 1000L

  private[net] def newPoller(): Poller =
    if LinktimeInfo.isLinux then new EpollPoller()
    else if LinktimeInfo.isMac then new KqueuePoller()
    else throw new UnsupportedOperationException("ldbc-net: only Linux (epoll) and macOS (kqueue) are supported")

  /**
   * Starts an engine with its own multiplexer and thread.
   *
   * `name` becomes the poller thread's name, with a generation suffix appended on each handover.
   * Tests pass a distinct one so they can find their own thread and stay off [[global]].
   */
  private[net] def start(name: String = "ldbc-net-fd-raw"): FdRawEngine =
    ignoreSigpipe
    new FdRawEngine(newPoller(), name).start()

  lazy val global: FdRawEngine = start()

/**
 * [[RawSocket]] over a raw non-blocking fd, driven by [[FdRawEngine]]'s poller.
 *
 * The socket holds the callback of whichever read and write are currently outstanding. Keeping them
 * here is what makes the completion contract in [[RawSocket]] enforceable: closing the fd can settle
 * them instead of leaving the caller waiting for a readiness notification that will never arrive,
 * since `deregisterAndClose` drops the per-fd state the poller would have fired. Every settlement
 * goes through `getAndSet(null)`, so a callback cannot be invoked twice however the races fall out.
 */
private[net] final class FdRawSocket(fd: Int, st: ChannelState, engine: FdRawEngine) extends RawSocket:

  private val closed       = new AtomicBoolean(false)
  private val pendingRead  = new AtomicReference[Either[Throwable, Option[Array[Byte]]] => Unit](null)
  private val pendingWrite = new AtomicReference[Either[Throwable, Unit] => Unit](null)

  /**
   * Claims the read slot, refusing to displace an operation that is already parked there.
   *
   * A byte stream has no way to share itself between two concurrent readers — whoever won the race
   * would take bytes the other was waiting for — so overlapping reads are a misuse. Silently
   * overwriting the slot would break the completion contract in [[RawSocket]]: the displaced
   * callback would never be invoked at all. Refusing the newcomer keeps the parked operation intact
   * and makes the mistake visible at the point it happens.
   */
  private def claimRead(cb: Either[Throwable, Option[Array[Byte]]] => Unit): Boolean =
    pendingRead.compareAndSet(null, cb)

  /** Claims the write slot. See [[claimRead]]; the same reasoning applies to a half-sent frame. */
  private def claimWrite(cb: Either[Throwable, Unit] => Unit): Boolean =
    pendingWrite.compareAndSet(null, cb)

  /** Settles whatever read and write are outstanding with `cause`, if any still are. */
  private[net] def failPending(cause: Throwable): Unit =
    val r = pendingRead.getAndSet(null)
    val w = pendingWrite.getAndSet(null)
    try if r != null then r(Left(cause))
    finally if w != null then w(Left(cause))

  /** The raw fd, used by the Native TLS layer to drive s2n directly. */
  private[net] def fileDescriptor: Int = fd

  /** The per-fd interest/continuation state, used by the Native TLS layer to await readiness. */
  private[net] def channelState: ChannelState = st

  /** The owning engine, used by the Native TLS layer to arm read/write readiness on the poller. */
  private[net] def ioEngine: FdRawEngine = engine

  /**
   * Reads up to `n` bytes, arming read readiness whenever `recv` reports it would block.
   *
   * Cancelling clears the armed continuation so no further `recv` is issued, and clears the pending
   * slot so a later `close` does not resurrect a callback the caller has already walked away from.
   */
  override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): Canceler =
    if n <= 0 then { cb(Right(Some(Array.emptyByteArray))); Canceler.noop }
    else if closed.get() then { cb(Left(new java.io.IOException("socket closed"))); Canceler.noop }
    else
      if !claimRead(cb) then
        cb(Left(new IllegalStateException("another read is already in progress on this socket")))
        return Canceler.noop
      def finish(result: Either[Throwable, Option[Array[Byte]]]): Unit =
        val waiting = pendingRead.getAndSet(null)
        if waiting != null then waiting(result)
      def attempt(): Unit =
        val buf = new Array[Byte](n)
        val r   = CInterop.recvInto(fd, buf, n)
        if r > 0 then finish(Right(Some(java.util.Arrays.copyOf(buf, r))))
        else if r == 0 then finish(Right(None))
        else if errno == EAGAIN || errno == EWOULDBLOCK then
          engine.armRead(fd, st, () => attempt(), e => finish(Left(e)))
        else finish(Left(new java.io.IOException(s"read failed (errno=$errno)")))
      attempt()
      new Canceler:
        override def cancel(): Unit =
          st.readReady = null
          pendingRead.getAndSet(null)
          ()

  /**
   * Writes `bytes` in full, arming write readiness whenever `send` reports it would block.
   *
   * The returned [[Canceler]] is intentionally a no-op: `write` is not cancelable (see
   * [[RawSocket]]). Clearing `writeReady` here would strand the unsent remainder of a partially
   * written frame, leaving the peer waiting for bytes that never arrive. Closing the socket is a
   * different matter and does settle the callback, since the remaining bytes can no longer be
   * delivered at all.
   */
  override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit): Canceler =
    if closed.get() then { cb(Left(new java.io.IOException("socket closed"))); Canceler.noop }
    else
      val off = new java.util.concurrent.atomic.AtomicInteger(0)
      if !claimWrite(cb) then
        cb(Left(new IllegalStateException("another write is already in progress on this socket")))
        return Canceler.noop
      def finish(result: Either[Throwable, Unit]): Unit =
        val waiting = pendingWrite.getAndSet(null)
        if waiting != null then waiting(result)
      def attempt(): Unit =
        var blocked = false
        var failed  = false
        while off.get() < bytes.length && !blocked && !failed do
          val w = CInterop.sendFrom(fd, bytes, off.get(), bytes.length - off.get())
          if w > 0 then off.addAndGet(w)
          else if w < 0 && (errno == EAGAIN || errno == EWOULDBLOCK) then
            engine.armWrite(fd, st, () => attempt(), e => finish(Left(e))); blocked = true
          else { finish(Left(new java.io.IOException(s"write failed (errno=$errno)"))); failed = true }
        if !blocked && !failed && off.get() >= bytes.length then finish(Right(()))
      attempt()
      new Canceler:
        override def cancel(): Unit = ()

  /**
   * Marks the socket closed, hands the fd to the poller thread for deregistration, and settles
   * whatever was still waiting on it.
   *
   * Unlike the JVM engine, closing here cannot precede the notification: `deregisterAndClose` only
   * queues the work, so the fd may still be open when the callbacks are settled. What keeps a
   * settled read from resuming is the `closed` flag, which [[read]] checks on entry.
   */
  override def close(): Unit =
    if closed.compareAndSet(false, true) then
      engine.deregisterAndClose(fd)
      failPending(new java.io.IOException("socket closed"))
