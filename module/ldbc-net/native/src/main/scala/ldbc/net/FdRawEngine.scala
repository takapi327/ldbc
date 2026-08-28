/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.scalanative.libc.errno.errno
import scala.scalanative.meta.LinktimeInfo
import scala.scalanative.posix.errno.{ EAGAIN, EINPROGRESS, EWOULDBLOCK }

/**
 * Scala Native [[RawIoEngine]]: a single daemon poller thread drives non-blocking sockets through
 * `epoll`/`kqueue`, invoking one-shot callbacks on readiness. The effect-free counterpart of the `Fx`
 * `NativeIoEngine` that the generic `ldbc.net.IoEngine[F]` wraps once with `F.async`.
 *
 * The connect timeout is applied at the `F` layer ([[ldbc.net.IoEngine.fromRaw]]); blocking DNS
 * (`getaddrinfo`) runs on a transient daemon thread so the poller thread is never stalled, and the TCP
 * handshake plus all reads/writes are non-blocking (design `NATIVE_EPOLL_IOENGINE_DESIGN.md`).
 */
private[net] final class FdRawEngine(poller: Poller) extends RawIoEngine:

  private val registry = new ConcurrentHashMap[Int, ChannelState]()
  private val tasks    = new ConcurrentLinkedQueue[() => Unit]()

  private def enqueue(task: () => Unit): Unit =
    tasks.add(task)
    poller.wakeup()

  private def drainTasks(): Unit =
    var t = tasks.poll()
    while t != null do
      try t()
      catch { case _: Throwable => () }
      t = tasks.poll()

  private def loop(): Unit =
    while true do
      try
        poller.poll { (fd, readable, writable, error) =>
          try
            val st = registry.get(fd)
            if st != null then
              if error then fireAll(st)
              else
                if writable then
                  val cb = st.writeReady
                  st.writeReady = null
                  val cc = st.connectReady
                  st.connectReady = null
                  if cc != null then cc()
                  if cb != null then cb()
                if readable then
                  val cb = st.readReady
                  st.readReady = null
                  if cb != null then cb()
          catch { case _: Throwable => () }
        }
        drainTasks()
      catch { case _: Throwable => () }

  private def fireAll(st: ChannelState): Unit =
    val c = st.connectReady; st.connectReady = null; if c != null then c()
    val r = st.readReady; st.readReady       = null; if r != null then r()
    val w = st.writeReady; st.writeReady     = null; if w != null then w()

  private[net] def armRead(fd: Int, st: ChannelState, ready: () => Unit): Unit =
    st.readReady = ready
    enqueue(() => poller.arm(fd, read = true, write = false))

  private[net] def armWrite(fd: Int, st: ChannelState, ready: () => Unit): Unit =
    st.writeReady = ready
    enqueue(() => poller.arm(fd, read = false, write = true))

  private[net] def deregisterAndClose(fd: Int): Unit =
    enqueue { () =>
      registry.remove(fd)
      poller.remove(fd)
      CInterop.closeFd(fd)
    }

  private[net] def start(): FdRawEngine =
    val thread = new Thread(() => loop(), "ldbc-net-fd-raw")
    thread.setDaemon(true)
    thread.start()
    this

  override def connect(
    host:    String,
    port:    Int,
    options: SocketOptions,
    cb:      Either[Throwable, RawSocket] => Unit
  ): Canceler =
    val done  = new AtomicBoolean(false)
    val fdRef = new java.util.concurrent.atomic.AtomicInteger(-1)

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
            val soError = CInterop.socketError(fd)
            if soError == 0 then cb(Right(new FdRawSocket(fd, st, this)))
            else
              deregisterAndClose(fd)
              cb(Left(new java.io.IOException(s"connect to $host:$port failed (errno=$soError)")))

        st.connectReady = () => finishConnect()
        val err = CInterop.beginConnect(fd, resolved)
        if err == 0 then finishConnect()
        else if err == EINPROGRESS then enqueue(() => { poller.add(fd); poller.arm(fd, read = false, write = true) })
        else
          done.set(true)
          registry.remove(fd); CInterop.closeFd(fd)
          cb(Left(new java.io.IOException(s"connect to $host:$port failed (errno=$err)")))
      catch case e: Throwable => if done.compareAndSet(false, true) then cb(Left(e))

    val worker = new Thread(connect, "ldbc-net-fd-connect")
    worker.setDaemon(true)
    worker.start()

    new Canceler:
      override def cancel(): Unit =
        if done.compareAndSet(false, true) then
          val fd = fdRef.get()
          if fd >= 0 then deregisterAndClose(fd)

private[net] object FdRawEngine:
  private lazy val ignoreSigpipe: Unit = CInterop.ignoreSigpipe()

  lazy val global: FdRawEngine =
    ignoreSigpipe
    val poller =
      if LinktimeInfo.isLinux then new EpollPoller()
      else if LinktimeInfo.isMac then new KqueuePoller()
      else throw new UnsupportedOperationException("ldbc-net: only Linux (epoll) and macOS (kqueue) are supported")
    new FdRawEngine(poller).start()

/** [[RawSocket]] over a raw non-blocking fd, driven by [[FdRawEngine]]'s poller. */
private[net] final class FdRawSocket(fd: Int, st: ChannelState, engine: FdRawEngine) extends RawSocket:

  private val closed = new AtomicBoolean(false)

  /** The raw fd, used by the Native TLS layer to drive s2n directly. */
  private[net] def fileDescriptor: Int = fd

  /** The per-fd interest/continuation state, used by the Native TLS layer to await readiness. */
  private[net] def channelState: ChannelState = st

  /** The owning engine, used by the Native TLS layer to arm read/write readiness on the poller. */
  private[net] def ioEngine: FdRawEngine = engine

  override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): Canceler =
    if n <= 0 then { cb(Right(Some(Array.emptyByteArray))); Canceler.noop }
    else if closed.get() then { cb(Left(new java.io.IOException("socket closed"))); Canceler.noop }
    else
      def attempt(): Unit =
        val buf = new Array[Byte](n)
        val r   = CInterop.recvInto(fd, buf, n)
        if r > 0 then cb(Right(Some(java.util.Arrays.copyOf(buf, r))))
        else if r == 0 then cb(Right(None))
        else if errno == EAGAIN || errno == EWOULDBLOCK then engine.armRead(fd, st, () => attempt())
        else cb(Left(new java.io.IOException(s"read failed (errno=$errno)")))
      attempt()
      new Canceler:
        override def cancel(): Unit = st.readReady = null

  override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit): Canceler =
    if closed.get() then { cb(Left(new java.io.IOException("socket closed"))); Canceler.noop }
    else
      val off = new java.util.concurrent.atomic.AtomicInteger(0)
      def attempt(): Unit =
        var blocked = false
        var failed  = false
        while off.get() < bytes.length && !blocked && !failed do
          val w = CInterop.sendFrom(fd, bytes, off.get(), bytes.length - off.get())
          if w > 0 then off.addAndGet(w)
          else if w < 0 && (errno == EAGAIN || errno == EWOULDBLOCK) then
            engine.armWrite(fd, st, () => attempt()); blocked = true
          else { cb(Left(new java.io.IOException(s"write failed (errno=$errno)"))); failed = true }
        if !blocked && !failed && off.get() >= bytes.length then cb(Right(()))
      attempt()
      new Canceler:
        override def cancel(): Unit = st.writeReady = null

  override def close(): Unit =
    if closed.compareAndSet(false, true) then engine.deregisterAndClose(fd)
