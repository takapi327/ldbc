/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.ByteBuffer
import java.nio.channels.{ SelectionKey, SocketChannel }

import ldbc.fx.Fx

/** JVM [[Socket]] over a non-blocking NIO channel; `read`/`write` register interest and suspend an
 *  [[Fx]] that the [[SelectorEngine]] completes when the channel becomes ready. */
private[net] final class NioSocket(
  ch:     SocketChannel,
  key:    SelectionKey,
  st:     SelectorEngine.ChannelState,
  engine: SelectorEngine
) extends Socket:

  override def read(n: Int): Fx[Option[Array[Byte]]] =
    if n <= 0 then Fx.pure(Some(Array.emptyByteArray))
    else
      Fx.async { cb =>
        lazy val onReadable: SelectionKey => Unit = _ =>
          try
            val buf = ByteBuffer.allocate(n)
            NioSocket.interpret(buf, ch.read(buf)) match
              case NioSocket.Eof     => cb(Right(None))
              case NioSocket.Data(a) => cb(Right(Some(a)))
              case NioSocket.More =>
                st.readReady = onReadable
                engine.enableInterest(key, SelectionKey.OP_READ)
          catch { case e: Throwable => cb(Left(e)) }
        st.readReady = onReadable
        engine.enableInterest(key, SelectionKey.OP_READ)
        new Fx.Canceler {
          override def cancel(): Unit = { st.readReady = null; engine.disableInterest(key, SelectionKey.OP_READ) }
        }
      }

  override def write(bytes: Array[Byte]): Fx[Unit] = Fx.async { cb =>
    val buf = ByteBuffer.wrap(bytes)
    def attempt(): Unit =
      try
        ch.write(buf)
        if !buf.hasRemaining then cb(Right(()))
        else
          st.writeReady = _ => attempt()
          engine.enableInterest(key, SelectionKey.OP_WRITE)
      catch { case e: Throwable => cb(Left(e)) }
    attempt()
    new Fx.Canceler {
      override def cancel(): Unit = { st.writeReady = null; engine.disableInterest(key, SelectionKey.OP_WRITE) }
    }
  }

  override def close(): Fx[Unit] = Fx.delay { try ch.close() catch { case _: Throwable => () } }

private[net] object NioSocket:

  /** The outcome of a single non-blocking read of up to `n` bytes. */
  private[net] sealed trait ReadOutcome

  /** The peer closed the connection (`SocketChannel.read` returned `-1`). */
  private[net] case object Eof extends ReadOutcome

  /** The channel was readable but yielded no bytes yet (`read` returned `0`); the caller keeps waiting. */
  private[net] case object More extends ReadOutcome

  /** `read` returned `> 0` bytes. */
  private[net] final case class Data(bytes: Array[Byte]) extends ReadOutcome

  /**
   * Classifies a `SocketChannel.read` result. Critically, a `0` result (readable but no bytes,
   * e.g. a spurious wakeup) is [[More]] — distinct from [[Eof]] — so it is never misreported as an
   * end-of-stream empty array.
   *
   * @param buf the buffer that was read into (flipped and drained on [[Data]])
   * @param r   the value returned by `SocketChannel.read`
   */
  private[net] def interpret(buf: ByteBuffer, r: Int): ReadOutcome =
    if r < 0 then Eof
    else if r == 0 then More
    else
      buf.flip()
      val arr = new Array[Byte](buf.remaining())
      buf.get(arr)
      Data(arr)
