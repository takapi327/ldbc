/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.channels.{ ClosedChannelException, SelectionKey, Selector, SocketChannel }
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * JVM [[RawIoEngine]]: one daemon selector thread drives non-blocking NIO channels, invoking one-shot
 * callbacks on readiness. The effect-free counterpart of the former `Fx` NIO selector engine that the generic
 * `ldbc.net.IoEngine[F]` wraps. Interest registration is marshalled onto the selector thread.
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
      NioRawEngine.withOptions(ch, options)
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

  lazy val global: NioRawEngine =
    val engine = new NioRawEngine(Selector.open())
    val t      = new Thread(() => engine.loop(), "ldbc-net-nio-raw")
    t.setDaemon(true); t.start()
    engine

/** [[RawSocket]] over a non-blocking NIO channel driven by [[NioRawEngine]]. */
private[net] final class NioRawSocket(ch: SocketChannel, engine: NioRawEngine) extends RawSocket:

  /**
   * Reads up to `n` bytes. A negative result from the channel is end of stream, zero means readable
   * but nothing buffered yet — in which case readiness is registered and the attempt repeated.
   *
   * Cancelling stops the channel from being touched again, because consuming bytes is the one
   * irreversible side effect here: a `cb` discarded after cancellation would take the bytes with it.
   * Invoking `cb` late is harmless by comparison, since the effect layer ignores it, so only the
   * read is suppressed. This is best effort — a cancel arriving while the selector thread is
   * already inside `ch.read` cannot un-consume — which is why the contract tells callers to discard
   * the session rather than resume it.
   */
  override def read(n: Int, cb: Either[Throwable, Option[Array[Byte]]] => Unit): Canceler =
    if n <= 0 then { cb(Right(Some(Array.emptyByteArray))); Canceler.noop }
    else if !ch.isOpen then { cb(Left(new ClosedChannelException)); Canceler.noop }
    else
      val buf       = ByteBuffer.allocate(n)
      val cancelled = new AtomicBoolean(false)
      def attempt(): Unit =
        if cancelled.get() then ()
        else
          try
            val got = ch.read(buf)
            if got < 0 then cb(Right(None))
            else if got == 0 then engine.register(ch, SelectionKey.OP_READ, () => attempt())
            else
              buf.flip()
              val bytes = new Array[Byte](buf.remaining())
              buf.get(bytes)
              cb(Right(Some(bytes)))
          catch case e: Throwable => cb(Left(e))
      attempt()
      new Canceler:
        override def cancel(): Unit = cancelled.set(true)

  /**
   * Writes `bytes` in full, registering for writability whenever the channel accepts only part of
   * them.
   *
   * The returned [[Canceler]] is intentionally a no-op: `write` is not cancelable (see
   * [[RawSocket]]). Stopping midway would leave a partial frame on the peer, which is harder to
   * recover from than simply finishing the transfer.
   */
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
