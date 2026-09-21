/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.io.IOException
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.channels.{ ClosedChannelException, SelectionKey, Selector, SocketChannel }
import java.nio.ByteBuffer
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentLinkedQueue }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicLong, AtomicReference }

import scala.util.control.NonFatal

/** What a selected key should do: whose socket it belongs to, and the continuation to run. */
private[net] final class Registration(val socket: NioRawSocket, val cb: () => Unit)

/**
 * JVM [[RawIoEngine]]: one daemon selector thread drives non-blocking NIO channels, invoking one-shot
 * callbacks on readiness. The effect-free counterpart of the former `Fx` NIO selector engine that the generic
 * `ldbc.net.IoEngine[F]` wraps. Interest registration is marshalled onto the selector thread.
 *
 * Every connection in the process shares this one thread, so its failure modes are handled explicitly
 * rather than swallowed. Recoverable errors are reported and rate-limited so the loop survives without
 * burning a core; fatal ones end the thread but hand over to a replacement, which resumes the same
 * selector with every registration still intact.
 */
private[net] final class NioRawEngine private (initial: Selector, baseName: String)
  extends RawIoEngine,
          PollerDiagnostics:

  @volatile private var selector: Selector = initial

  private val pending         = new ConcurrentLinkedQueue[() => Unit]()
  private val liveSockets     = ConcurrentHashMap.newKeySet[NioRawSocket]()
  private val dispatching     = new AtomicReference[NioRawSocket](null)
  private val loopErrors      = new AtomicLong(0)
  private val failedHandovers = new AtomicInteger(0)
  private val generation      = new AtomicInteger(0)
  private val revivals        = new AtomicInteger(0)
  private val terminated      = new AtomicBoolean(false)
  private val threadName      = new AtomicReference[String](baseName)

  override def loopErrorCount: Long = loopErrors.get()

  override def revivalCount: Int = revivals.get()

  override def isTerminated: Boolean = terminated.get()

  override def liveSocketCount: Int = liveSockets.size()

  override def pollerThreadName: String = threadName.get()

  @volatile private var faultInjection: () => Unit = null

  @volatile private var dispatchFault: () => Unit = null

  override def injectFault(fault: () => Unit): Unit =
    faultInjection = fault
    selector.wakeup()
    ()

  override def injectDispatchFault(fault: () => Unit): Unit =
    dispatchFault = fault
    selector.wakeup()
    ()

  private def drain(): Unit =
    var r = pending.poll(); while r != null do { r(); r = pending.poll() }

  /**
   * Runs the continuation of every ready key, disabling its interest first.
   *
   * The interest is cleared before the callback because registration is one-shot: a callback that
   * wants more readiness re-registers itself, and re-arming after it would immediately undo that.
   * The socket being dispatched is published so that a thread dying between those two steps can be
   * accounted for — its key is left armed with no interest, so nothing will ever select it again.
   *
   * Each key is isolated. An exception from one continuation must not end the sweep: the keys not
   * reached yet have already had their events reported, and the one that threw has already been
   * disarmed.
   *
   * `dispatching` is cleared when the work finishes and when a recoverable error is handled, but
   * deliberately **not** in a `finally`. A fatal error has to leave it set: the handover reads it to
   * find the one socket whose event was consumed but whose continuation never ran (§ [[failInFlight]]).
   */
  private def processSelectedKeys(): Unit =
    val it = selector.selectedKeys().iterator()
    while it.hasNext do
      val key = it.next(); it.remove()
      if key.isValid then
        val reg = key.attachment().asInstanceOf[Registration]
        if reg == null then key.interestOps(0)
        else
          dispatching.set(reg.socket)
          try
            key.interestOps(0)
            val fault = dispatchFault
            if fault != null then fault()
            if reg.cb != null then reg.cb()
            dispatching.set(null)
          catch
            case NonFatal(e) =>
              dispatching.set(null)
              reportLoopError(e)

  /**
   * The poller loop.
   *
   * The interrupt flag is tested explicitly because the blocking call does not raise on it: an
   * interrupted `select()` returns immediately with the flag still set, which would spin rather than
   * stop. Reading it with `Thread.interrupted()` clears it and turns it into the exception the
   * handover path expects.
   *
   * Recoverable errors are reported and slept off so the loop keeps serving the other connections.
   * Fatal ones are deliberately not caught: they leave through `finally`, which hands over to a
   * replacement thread, and then reach the default uncaught-exception handler so the stack trace is
   * not lost. `completed` records whether this thread ever finished a whole iteration, which is what
   * distinguishes a thread that ran for a while from one that died on startup.
   */
  private def runLoop(): Unit =
    var completed = false
    try
      while true do
        try
          if Thread.interrupted() then throw new InterruptedException("ldbc-net poller interrupted")
          val fault = faultInjection
          if fault != null then fault()
          drain()
          selector.select()
          drain()
          processSelectedKeys()
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

  /**
   * Pauses after a recoverable error so a persistent failure cannot spin the thread at full speed.
   *
   * `InterruptedException` is intentionally not caught, for the same reason the loop tests the
   * interrupt flag itself: an interrupted poller must end rather than carry the flag forward. A
   * `Selector.select()` called with the flag set returns immediately and never clears it, so a loop
   * that kept going would become a silent busy-wait — the very failure this back-off exists to
   * prevent. Ending lets a replacement start with a clean flag.
   */
  private def backoff(): Unit = Thread.sleep(NioRawEngine.BackoffMillis)

  /**
   * Settles the callbacks of the socket whose key was being dispatched when the thread died.
   *
   * Its interest has already been cleared, so a replacement thread driving the same selector would
   * never see it again. Everything else on the selector survives the handover untouched.
   */
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
  private def failAllPending(cause: Throwable): Unit =
    liveSockets.forEach(s => settle(s, cause))

  private def settle(socket: NioRawSocket, cause: Throwable): Unit =
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
      if n >= NioRawEngine.MaxFailedHandovers then giveUp(s"gave up after $n failed handovers")
      else
        try startThread()
        catch case _: Throwable => giveUp("could not be replaced")
    catch case _: Throwable => ()

  private def giveUp(reason: String): Unit =
    terminated.set(true)
    pending.clear()
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
   * Brings a terminated engine back up with a fresh selector and thread.
   *
   * Reaching the terminated state must not be the end of the process's database access: this engine
   * is a singleton, so a permanent stop would mean no connection could ever be made again. Nothing
   * needs to be carried over — every socket was already failed by [[giveUp]] — so a clean selector is
   * the right starting point.
   *
   * @return the reason a revival could not happen, or `None` when the engine is usable
   */
  private def revive(): Option[Throwable] =
    synchronized {
      if !terminated.get() then None
      else
        try
          selector = Selector.open()
          failedHandovers.set(0)
          revivals.incrementAndGet()
          terminated.set(false)
          startThread()
          None
        catch case NonFatal(e) => Some(e)
    }

  /**
   * Queues an interest registration onto the selector thread, which is the only thread allowed to
   * touch the selector.
   *
   * `onFailure` is required rather than optional. `ch.register` throws `ClosedChannelException`
   * when the channel was closed between the read attempt and this task running, and a task that
   * simply died here would leave the caller waiting for a readiness notification that can no longer
   * arrive — the registration is queued, so there is no call stack left to propagate to. The same
   * applies once the engine has given up: with no thread to drive it, a registration would wait
   * forever, so it is refused immediately instead.
   */
  private[net] def register(
    socket:    NioRawSocket,
    ch:        SocketChannel,
    ops:       Int,
    cb:        () => Unit,
    onFailure: Throwable => Unit
  ): Unit =
    if terminated.get() then onFailure(new IOException("ldbc-net poller terminated"))
    else
      pending.add { () =>
        try { ch.register(selector, ops, new Registration(socket, cb)); () }
        catch case NonFatal(e) => onFailure(e)
      }
      selector.wakeup()
      ()

  private def newSocket(ch: SocketChannel): NioRawSocket =
    val socket = new NioRawSocket(ch, this)
    liveSockets.add(socket)
    socket

  private[net] def forget(socket: NioRawSocket): Unit =
    liveSockets.remove(socket)
    ()

  override def connect(
    host:    String,
    port:    Int,
    options: SocketOptions,
    cb:      Either[Throwable, RawSocket] => Unit
  ): Canceler =
    revive() match
      case Some(error) =>
        cb(Left(new IOException("ldbc-net poller could not be restarted", error)))
        Canceler.noop
      case None =>
        val ch     = SocketChannel.open()
        val socket = newSocket(ch)
        try
          ch.configureBlocking(false)
          NioRawEngine.withOptions(ch, options)
          def completed(): Unit =
            try { ch.finishConnect(); cb(Right(socket)) }
            catch
              case e: Throwable =>
                forget(socket)
                cb(Left(e))
          def failed(e: Throwable): Unit =
            forget(socket)
            cb(Left(e))
          if ch.connect(new InetSocketAddress(host, port)) then cb(Right(socket))
          else register(socket, ch, SelectionKey.OP_CONNECT, () => completed(), failed)
        catch
          case e: Throwable =>
            forget(socket)
            cb(Left(e))
        new Canceler:
          override def cancel(): Unit = socket.close()

private[net] object NioRawEngine:

  /** Consecutive replacements that never completed an iteration before the engine gives up. */
  private[net] val MaxFailedHandovers = 3

  /** Pause after a recoverable loop error, matching the interval Netty's event loop uses. */
  private[net] val BackoffMillis = 1000L

  /**
   * Applies [[SocketOptions]] to a channel before it is connected, the JVM counterpart of the Native
   * `CInterop.applyOptions`. The buffer sizes are only set when given, so leaving them unset keeps the
   * platform defaults rather than pinning them to a value of ours. The kernel is free to round what it
   * is asked for, so the resulting socket may report a different size than the one requested.
   */
  private[net] def withOptions(ch: SocketChannel, options: SocketOptions): Unit =
    ch.setOption(StandardSocketOptions.TCP_NODELAY, java.lang.Boolean.valueOf(options.noDelay))
    ch.setOption(StandardSocketOptions.SO_KEEPALIVE, java.lang.Boolean.valueOf(options.keepAlive))
    options.sendBufferSize.foreach(size => ch.setOption(StandardSocketOptions.SO_SNDBUF, Integer.valueOf(size)))
    options.receiveBufferSize.foreach(size => ch.setOption(StandardSocketOptions.SO_RCVBUF, Integer.valueOf(size)))

  /**
   * Starts an engine with its own selector and thread.
   *
   * `name` becomes the poller thread's name, with a generation suffix appended on each handover.
   * Tests pass a distinct one so they can find their own thread and stay off [[global]].
   */
  private[net] def start(name: String = "ldbc-net-nio-raw"): NioRawEngine =
    val engine = new NioRawEngine(Selector.open(), name)
    engine.startThread()
    engine

  lazy val global: NioRawEngine = start()

/**
 * [[RawSocket]] over a non-blocking NIO channel driven by [[NioRawEngine]].
 *
 * The socket holds the callback of whichever read and write are currently outstanding. Keeping them
 * here is what makes the completion contract in [[RawSocket]] enforceable: closing the channel,
 * failing to place an interest registration, or losing the poller can settle them instead of leaving
 * the caller waiting for a notification that will never arrive. Every settlement goes through
 * `getAndSet(null)`, so a callback cannot be invoked twice however the races fall out.
 */
private[net] final class NioRawSocket(ch: SocketChannel, engine: NioRawEngine) extends RawSocket:

  private val pendingRead  = new AtomicReference[Either[Throwable, Option[Array[Byte]]] => Unit](null)
  private val pendingWrite = new AtomicReference[Either[Throwable, Unit] => Unit](null)

  /**
   * Settles whatever read and write are outstanding with `cause`, if any still are.
   *
   * Called when the socket is closed, when a registration cannot be placed, and when the engine
   * gives up its poller.
   */
  private[net] def failPending(cause: Throwable): Unit =
    val r = pendingRead.getAndSet(null)
    val w = pendingWrite.getAndSet(null)
    try if r != null then r(Left(cause))
    finally if w != null then w(Left(cause))

  /**
   * Reads up to `n` bytes. A negative result from the channel is end of stream, zero means readable
   * but nothing buffered yet — in which case readiness is registered and the attempt repeated.
   *
   * Cancelling stops the channel from being touched again, because consuming bytes is the one
   * irreversible side effect here: a `cb` discarded after cancellation would take the bytes with it.
   * The pending slot is cleared at the same time, so a later `close` does not resurrect a callback
   * the caller has already walked away from. This is best effort — a cancel arriving while the
   * selector thread is already inside `ch.read` cannot un-consume — which is why the contract tells
   * callers to discard the session rather than resume it.
   */
  override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): Canceler =
    if n <= 0 then { cb(Right(Some(Array.emptyByteArray))); Canceler.noop }
    else if !ch.isOpen then { cb(Left(new ClosedChannelException)); Canceler.noop }
    else
      val buf       = ByteBuffer.allocate(n)
      val cancelled = new AtomicBoolean(false)
      pendingRead.set(cb)
      def finish(result: Either[Throwable, Option[Array[Byte]]]): Unit =
        val waiting = pendingRead.getAndSet(null)
        if waiting != null then waiting(result)
      val finishFailure: Throwable => Unit = e => finish(Left(e))
      def attempt():     Unit              =
        if cancelled.get() then ()
        else
          try
            val got = ch.read(buf)
            if got < 0 then finish(Right(None))
            else if got == 0 then engine.register(this, ch, SelectionKey.OP_READ, () => attempt(), finishFailure)
            else
              buf.flip()
              val bytes = new Array[Byte](buf.remaining())
              buf.get(bytes)
              finish(Right(Some(bytes)))
          catch case e: Throwable => finish(Left(e))
      attempt()
      new Canceler:
        override def cancel(): Unit =
          cancelled.set(true)
          pendingRead.getAndSet(null)
          ()

  /**
   * Writes `bytes` in full, registering for writability whenever the channel accepts only part of
   * them.
   *
   * The returned [[Canceler]] is intentionally a no-op: `write` is not cancelable (see
   * [[RawSocket]]). Stopping midway would leave a partial frame on the peer, which is harder to
   * recover from than simply finishing the transfer. Closing the socket is a different matter and
   * does settle the callback, since the remaining bytes can no longer be delivered at all.
   */
  override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit): Canceler =
    val buf = ByteBuffer.wrap(bytes)
    pendingWrite.set(cb)
    def finish(result: Either[Throwable, Unit]): Unit =
      val waiting = pendingWrite.getAndSet(null)
      if waiting != null then waiting(result)
    val finishFailure: Throwable => Unit = e => finish(Left(e))
    def attempt():     Unit              =
      try
        ch.write(buf)
        if !buf.hasRemaining then finish(Right(()))
        else engine.register(this, ch, SelectionKey.OP_WRITE, () => attempt(), finishFailure)
      catch case e: Throwable => finish(Left(e))
    attempt()
    new Canceler:
      override def cancel(): Unit = ()

  /**
   * Closes the channel, then settles whatever was still waiting on it.
   *
   * The order matters: closing first means no attempt can resume and read bytes after the callback
   * has already been told the socket is gone.
   */
  override def close(): Unit =
    engine.forget(this)
    try ch.close()
    catch case _: Throwable => ()
    failPending(new ClosedChannelException)
