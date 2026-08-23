/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.channels.{ ClosedChannelException, SelectionKey, Selector, SocketChannel }
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * JVM [[RawIoEngine]]: one daemon selector thread drives non-blocking NIO channels, invoking one-shot
 * callbacks on readiness. The effect-free counterpart of the former `Fx` NIO selector engine that the generic
 * `ldbc.net.effect.IoEngine[F]` wraps. Interest registration is marshalled onto the selector thread.
 */
private[net] final class NioRawEngine private (selector: Selector) extends RawIoEngine:

  private val pending = new ConcurrentLinkedQueue[() => Unit]()

  private def drain(): Unit =
    var r = pending.poll(); while r != null do { r(); r = pending.poll() }

  private def loop(): Unit =
    while true do
      try
        drain()
        selector.select()
        drain()
        val it = selector.selectedKeys().iterator()
        while it.hasNext do
          val key = it.next(); it.remove()
          if key.isValid then
            val cb = key.attachment().asInstanceOf[() => Unit]
            key.interestOps(0) // one-shot; the callback re-registers if it needs more
            if cb != null then cb()
      catch case _: Throwable => ()

  private[net] def register(ch: SocketChannel, ops: Int, cb: () => Unit): Unit =
    pending.add(() => { ch.register(selector, ops, (cb: Object)); () })
    selector.wakeup()
    ()

  override def connect(
    host:    String,
    port:    Int,
    options: SocketOptions,
    cb:      Either[Throwable, RawSocket] => Unit
  ): Canceler =
    val ch = SocketChannel.open()
    try
      ch.configureBlocking(false)
      ch.setOption(StandardSocketOptions.TCP_NODELAY, java.lang.Boolean.valueOf(options.noDelay))
      ch.setOption(StandardSocketOptions.SO_KEEPALIVE, java.lang.Boolean.valueOf(options.keepAlive))
      def completed(): Unit =
        try { ch.finishConnect(); cb(Right(new NioRawSocket(ch, this))) }
        catch case e: Throwable => cb(Left(e))
      if ch.connect(new InetSocketAddress(host, port)) then cb(Right(new NioRawSocket(ch, this)))
      else register(ch, SelectionKey.OP_CONNECT, () => completed())
    catch case e: Throwable => cb(Left(e))
    new Canceler:
      override def cancel(): Unit =
        try ch.close()
        catch case _: Throwable => ()

private[net] object NioRawEngine:
  lazy val global: NioRawEngine =
    val engine = new NioRawEngine(Selector.open())
    val t      = new Thread(() => engine.loop(), "ldbc-net-nio-raw")
    t.setDaemon(true); t.start()
    engine

/** [[RawSocket]] over a non-blocking NIO channel driven by [[NioRawEngine]]. */
private[net] final class NioRawSocket(ch: SocketChannel, engine: NioRawEngine) extends RawSocket:

  override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): Canceler =
    if n <= 0 then { cb(Right(Some(Array.emptyByteArray))); Canceler.noop }
    else if !ch.isOpen then { cb(Left(new ClosedChannelException)); Canceler.noop }
    else
      val buf = ByteBuffer.allocate(n)
      def attempt(): Unit =
        try
          val got = ch.read(buf)
          if got < 0 then cb(Right(None))                                                  // end of stream
          else if got == 0 then engine.register(ch, SelectionKey.OP_READ, () => attempt()) // readable but no bytes yet
          else { buf.flip(); val a = new Array[Byte](buf.remaining()); buf.get(a); cb(Right(Some(a))) } // up to n bytes
        catch case e: Throwable => cb(Left(e))
      attempt()
      Canceler.noop

  override def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit): Canceler =
    val buf = ByteBuffer.wrap(bytes)
    def attempt(): Unit =
      try
        ch.write(buf)
        if !buf.hasRemaining then cb(Right(()))
        else engine.register(ch, SelectionKey.OP_WRITE, () => attempt())
      catch case e: Throwable => cb(Left(e))
    attempt()
    new Canceler:
      override def cancel(): Unit = ()

  override def close(): Unit =
    try ch.close()
    catch case _: Throwable => ()
