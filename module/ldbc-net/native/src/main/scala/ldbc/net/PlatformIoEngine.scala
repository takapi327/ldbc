/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.duration.FiniteDuration
import scala.scalanative.meta.LinktimeInfo
import scala.scalanative.posix.errno.EINPROGRESS

import ldbc.fx.Fx

/**
 * Mutable per-fd interest/continuation state, mirroring the JVM `SelectorEngine.ChannelState`. Each
 * callback is one-shot: the poller nulls it after firing (the interest is registered with
 * ONESHOT semantics so it is auto-disabled after one event).
 */
private[net] final class ChannelState:
  @volatile var connectReady: () => Unit = null
  @volatile var readReady:    () => Unit = null
  @volatile var writeReady:   () => Unit = null

/**
 * Scala Native transport: a single daemon poller thread drives non-blocking sockets through
 * `epoll`/`kqueue`, completing `Fx` callbacks. DNS resolution (blocking `getaddrinfo`) runs on the
 * `Fx.blocking` pool; the TCP handshake and all reads/writes are non-blocking (design
 * `NATIVE_EPOLL_IOENGINE_DESIGN.md`).
 */
private[net] final class NativeIoEngine(poller: Poller) extends IoEngine:

  private val registry = new ConcurrentHashMap[Int, ChannelState]()
  private val tasks    = new ConcurrentLinkedQueue[() => Unit]()

  private def enqueue(task: () => Unit): Unit =
    tasks.add(task)
    poller.wakeup()

  /** Drains queued interest changes; called by the poller thread each iteration. */
  private def drainTasks(): Unit =
    var t = tasks.poll()
    while t != null do
      try t()
      catch { case _: Throwable => () }
      t = tasks.poll()

  /** The poller thread's run loop. Each event and task runs inside its own guard. */
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

  /** Fires every pending callback (used on error/EOF so a stuck operation cannot hang). */
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

  private[net] def deregister(fd: Int): Unit =
    enqueue { () =>
      registry.remove(fd)
      poller.remove(fd)
    }

  private[net] def start(): NativeIoEngine =
    val thread = new Thread(() => loop(), "fx-net-poller")
    thread.setDaemon(true)
    thread.start()
    this

  override def connect(host: String, port: Int, timeout: FiniteDuration, options: SocketOptions): Fx[Socket] =
    Fx.blocking(CInterop.resolve(host, port)).flatMap { resolved =>
      Fx.async[Socket] { cb =>
        val fd = resolved.fd
        CInterop.applyOptions(fd, options)
        val st = new ChannelState
        registry.put(fd, st)
        val done  = new AtomicBoolean(false)
        val timer = new AtomicReference[Fx.Canceler](Fx.Canceler.noop)

        def finishConnect(): Unit =
          if done.compareAndSet(false, true) then
            timer.get.cancel()
            val soError = CInterop.socketError(fd)
            if soError == 0 then cb(Right(new FdSocket(fd, st, this)))
            else
              deregister(fd); CInterop.closeFd(fd)
              cb(Left(new java.io.IOException(s"connect to $host:$port failed (errno=$soError)")))

        st.connectReady = () => finishConnect()
        val err = CInterop.beginConnect(fd, resolved)
        if err == 0 then finishConnect()
        else if err == EINPROGRESS then
          enqueue(() => { poller.add(fd); poller.arm(fd, read = false, write = true) })
          timer.set(Fx.sleep(timeout).unsafeRun { _ =>
            if done.compareAndSet(false, true) then
              deregister(fd); CInterop.closeFd(fd)
              cb(Left(new ConnectTimeoutException(s"connect to $host:$port timed out after $timeout")))
          })
        else
          done.set(true)
          registry.remove(fd); CInterop.closeFd(fd)
          cb(Left(new java.io.IOException(s"connect to $host:$port failed (errno=$err)")))

        new Fx.Canceler:
          override def cancel(): Unit =
            if done.compareAndSet(false, true) then
              timer.get.cancel(); deregister(fd); CInterop.closeFd(fd)
      }.flatMap(SerializedSocket.apply)
    }

private[net] object PlatformIoEngine:
  private lazy val ignoreSigpipe: Unit = CInterop.ignoreSigpipe()

  lazy val global: IoEngine =
    ignoreSigpipe
    val poller =
      if LinktimeInfo.isLinux then new EpollPoller()
      else if LinktimeInfo.isMac then new KqueuePoller()
      else throw new UnsupportedOperationException("ldbc-net: only Linux (epoll) and macOS (kqueue) are supported")
    new NativeIoEngine(poller).start()
