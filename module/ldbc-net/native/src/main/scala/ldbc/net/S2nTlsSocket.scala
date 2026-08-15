/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.util.concurrent.atomic.AtomicBoolean

import scala.scalanative.runtime.Intrinsics
import scala.scalanative.unsafe.*

import ldbc.fx.Fx

/**
 * A [[Socket]] that encrypts through an s2n connection, driven fully non-blocking (design Phase 3 /
 * P1). Each s2n call runs on the run loop; when s2n reports `S2N_BLOCKED_ON_READ`/`WRITE`, the fd's
 * readiness is awaited on the engine's poller (no thread is parked) and the call is retried. Reads
 * never return more than `n` bytes; `close()` sends close_notify best-effort, frees the s2n
 * resources exactly once, and closes the underlying socket.
 *
 * `close()` MUST be called (use via `Fx.bracket` or a pool): without it the native s2n
 * connection/config memory and the io-callback registry entry (which pins the underlying socket)
 * are never released — there is no finalizer.
 */
private[net] final class S2nTlsSocket(
  conn:   Ptr[Byte],
  config: Ptr[Byte],
  raw:    FdSocket,
  ioId:   Long,
  hostId: Long
) extends Socket:

  private val closed = new AtomicBoolean(false)
  private val engine = raw.ioEngine
  private val fd     = raw.fileDescriptor
  private val st     = raw.channelState

  /** `s2n_blocked_status`: 0 = not blocked, 1 = blocked on read, 2 = blocked on write. */
  private inline val BlockedRead  = 1
  private inline val BlockedWrite = 2

  /** Converts a JVM `Long` to a native `Size` (ssize_t). */
  private def toSize(value: Long): Size = Size.valueOf(Intrinsics.castLongToRawSize(value))

  /** Suspends until the fd is readable, then resumes on the poller thread. */
  private def awaitReadable: Fx[Unit] = Fx.async { cb =>
    engine.armRead(fd, st, () => cb(Right(())))
    new Fx.Canceler { override def cancel(): Unit = st.readReady = null }
  }

  /** Suspends until the fd is writable, then resumes on the poller thread. */
  private def awaitWritable: Fx[Unit] = Fx.async { cb =>
    engine.armWrite(fd, st, () => cb(Right(())))
    new Fx.Canceler { override def cancel(): Unit = st.writeReady = null }
  }

  /** Awaits readiness for the direction s2n blocked on, or fails if it was a genuine error. */
  private def awaitBlocked(blocked: Int, what: String): Fx[Unit] =
    blocked match
      case BlockedRead  => awaitReadable
      case BlockedWrite => awaitWritable
      case _            => Fx.raiseError(new RuntimeException(s"TLS $what failed"))

  /** Runs the handshake to completion, awaiting readiness whenever s2n blocks. */
  private[net] def handshake: Fx[Unit] =
    Fx.delay {
      val blocked = stackalloc[CInt]()
      val rc      = S2n.s2n_negotiate(conn, blocked)
      (rc, !blocked)
    }.flatMap { (rc, blocked) =>
      if rc >= 0 then Fx.unit
      else awaitBlocked(blocked, "handshake").flatMap(_ => handshake)
    }

  /** Frees the expected-host registry entry once the handshake has completed. */
  private[net] def releaseHost(): Unit = S2nBridge.unregister(-1L, hostId)

  override def read(n: Int): Fx[Option[Array[Byte]]] =
    if n <= 0 then Fx.pure(Some(Array.emptyByteArray))
    else
      val arr = new Array[Byte](n)
      def attempt: Fx[Option[Array[Byte]]] =
        Fx.delay {
          val blocked = stackalloc[CInt]()
          val got     = S2n.s2n_recv(conn, arr.at(0), toSize(n.toLong), blocked).toLong
          (got, !blocked)
        }.flatMap { (got, blocked) =>
          if got > 0 then Fx.pure(Some(java.util.Arrays.copyOf(arr, got.toInt)))
          else if got == 0 then Fx.pure(None)
          else awaitBlocked(blocked, "read").flatMap(_ => attempt)
        }
      attempt

  override def write(bytes: Array[Byte]): Fx[Unit] =
    def attempt(offset: Int): Fx[Unit] =
      if offset >= bytes.length then Fx.unit
      else
        Fx.delay {
          val blocked = stackalloc[CInt]()
          val sent    = S2n.s2n_send(conn, bytes.at(offset), toSize((bytes.length - offset).toLong), blocked).toLong
          (sent, !blocked)
        }.flatMap { (sent, blocked) =>
          if sent > 0 then attempt(offset + sent.toInt)
          else awaitBlocked(blocked, "write").flatMap(_ => attempt(offset))
        }
    attempt(0)

  override def close(): Fx[Unit] =
    /**
     * A single best-effort `close_notify`. It is deliberately NOT retried on
     * `S2N_BLOCKED_ON_WRITE`: a peer that has already gone away keeps the send blocked forever, so an
     * await/retry loop would hang `close()` (and thus a failed-handshake cleanup) indefinitely. When
     * the peer is alive the 7-byte alert flushes into the empty socket buffer in this one call; when
     * it cannot, the following `raw.close()` still tears the connection down.
     */
    val shutdown: Fx[Unit] =
      Fx.delay {
        val blocked = stackalloc[CInt]()
        S2n.s2n_shutdown_send(conn, blocked)
      }.map(_ => ())
    shutdown
      .handleErrorWith(_ => Fx.unit)
      .flatMap(_ => Fx.delay(release()))
      .flatMap(_ => raw.close())

  /** Frees the s2n connection/config and the io registry entry exactly once. */
  private def release(): Unit =
    if closed.compareAndSet(false, true) then
      S2n.s2n_connection_free(conn)
      S2n.s2n_config_free(config)
      S2nBridge.unregister(ioId, -1L)
      ()
