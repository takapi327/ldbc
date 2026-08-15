/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.InetSocketAddress
import java.nio.channels.{ SelectionKey, Selector, SocketChannel }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }
import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

import ldbc.fx.Fx

/** JVM engine: one daemon selector thread drives non-blocking NIO channels, completing Fx callbacks. */
private[net] final class SelectorEngine(selector: Selector) extends IoEngine:
  import SelectorEngine.ChannelState

  private val tasks = new ConcurrentLinkedQueue[() => Unit]()

  private[net] def enqueue(task: () => Unit): Unit =
    tasks.add(task)
    selector.wakeup()
    ()

  /**
   * The selector thread's run loop. Every task and every key is processed inside its own
   * `NonFatal` guard, with a loop-level backstop, so an exception from one task/callback/key (e.g. a
   * `CancelledKeyException` racing `key.isValid`) can never kill the thread and hang all connections.
   */
  private[net] def loop(): Unit =
    while true do
      try
        selector.select()
        var t = tasks.poll()
        while t != null do
          try t()
          catch { case NonFatal(_) => () }
          t = tasks.poll()
        val it = selector.selectedKeys().iterator()
        while it.hasNext do
          val key = it.next()
          it.remove()
          try
            if key.isValid then
              val st = key.attachment().asInstanceOf[ChannelState]
              if key.isConnectable then
                key.interestOps(0)
                val cb = st.connectReady
                st.connectReady = null
                if cb != null then cb(key)
              if key.isValid && key.isReadable then
                key.interestOps(key.interestOps & ~SelectionKey.OP_READ)
                val cb = st.readReady
                st.readReady = null
                if cb != null then cb(key)
              if key.isValid && key.isWritable then
                key.interestOps(key.interestOps & ~SelectionKey.OP_WRITE)
                val cb = st.writeReady
                st.writeReady = null
                if cb != null then cb(key)
          catch { case NonFatal(_) => () }
      catch { case NonFatal(_) => () }

  private[net] def enableInterest(key: SelectionKey, op: Int): Unit =
    enqueue(() => if key.isValid then key.interestOps(key.interestOps | op))
  private[net] def disableInterest(key: SelectionKey, op: Int): Unit =
    enqueue(() => if key.isValid then key.interestOps(key.interestOps & ~op))

  override def connect(host: String, port: Int, timeout: FiniteDuration, options: SocketOptions): Fx[Socket] = Fx
    .async[Socket] { cb =>
      val ch = SocketChannel.open()
      ch.configureBlocking(false)
      ch.setOption(java.net.StandardSocketOptions.TCP_NODELAY, java.lang.Boolean.valueOf(options.noDelay))
      ch.setOption(java.net.StandardSocketOptions.SO_KEEPALIVE, java.lang.Boolean.valueOf(options.keepAlive))
      options.sendBufferSize
        .foreach(size => ch.setOption(java.net.StandardSocketOptions.SO_SNDBUF, Integer.valueOf(size)))
      options.receiveBufferSize
        .foreach(size => ch.setOption(java.net.StandardSocketOptions.SO_RCVBUF, Integer.valueOf(size)))
      val st    = new ChannelState
      val done  = new AtomicBoolean(false)
      val timer = new AtomicReference[Fx.Canceler](Fx.Canceler.noop)
      st.connectReady = key =>
        if done.compareAndSet(false, true) then
          timer.get.cancel()
          try
            ch.finishConnect()
            key.interestOps(0)
            cb(Right(new NioSocket(ch, key, st, this)))
          catch { case e: Throwable => cb(Left(e)) }
      ch.connect(new InetSocketAddress(host, port))
      enqueue(() => { ch.register(selector, SelectionKey.OP_CONNECT, st); () })
      timer.set(Fx.sleep(timeout).unsafeRun { _ =>
        if done.compareAndSet(false, true) then
          try ch.close()
          catch { case _: Throwable => () }
          cb(Left(new ConnectTimeoutException(s"connect to $host:$port timed out after $timeout")))
      })
      new Fx.Canceler {
        override def cancel(): Unit = {
          timer.get.cancel();
          try ch.close()
          catch { case _: Throwable => () }
        }
      }
    }
    .flatMap(SerializedSocket.apply)

private[net] object SelectorEngine:
  final class ChannelState:
    @volatile var connectReady: SelectionKey => Unit = null
    @volatile var readReady:    SelectionKey => Unit = null
    @volatile var writeReady:   SelectionKey => Unit = null
