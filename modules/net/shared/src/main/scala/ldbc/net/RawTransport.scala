/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * The effect-free, callback-based transport layer (design §4.3). The platform engines expose this; the
 * generic `ldbc.net.Socket[F]` / `IoEngine[F]` wrap it once with `F.async`, so every effect
 * (`IO` / `Task` / `Fx`) wraps the *same* raw callbacks natively — no per-effect engine, no bridging.
 * This is the sole transport layer; the earlier per-effect `Fx`-returning socket stack has been removed.
 */

/** Effect-free cancellation handle for a pending raw operation. */
trait Canceler:
  def cancel(): Unit

object Canceler:
  val noop: Canceler = new Canceler:
    override def cancel(): Unit = ()

/**
 * Non-blocking byte transport as raw callbacks. `read`/`write` register interest and invoke `cb` once
 * the operation completes, returning a [[Canceler]] that deregisters it.
 *
 * Completion contract — implementations must honour this:
 *   - The `cb` handed to `read` / `write` is invoked **exactly once**, whether the operation succeeds
 *     or fails. Never invoking it is not a quiet no-op: the `F.async` wrapping this callback would
 *     never complete, stranding the caller with no way back short of a timeout it may not have set.
 *   - At most one `read` and one `write` may be outstanding on a socket at a time. Issuing a second
 *     one while the first is still parked fails immediately with an `IllegalStateException` rather
 *     than displacing it — a byte stream cannot be split between two readers, and dropping the
 *     first callback to make room would break the guarantee above.
 *   - `close` settles the pending `read` / `write` callbacks with a failure before returning. This
 *     does not contradict the cancellation contract below: `write` being uncancelable means
 *     *cancellation does not stop the transfer*, whereas closing gives up the socket itself, so the
 *     remaining bytes can no longer be delivered and success cannot honestly be reported.
 *   - If the engine loses its multiplexer and can no longer drive callbacks, the pending ones are
 *     settled with a failure too, for the same reason.
 *
 * Cancellation contract — implementations must honour this, and callers must not expect more:
 *   - `read`: cancelling MUST NOT consume bytes from the transport, so a later `read` still sees them.
 *     This is best effort only: a cancel that races an in-flight platform read may still consume, and
 *     nothing can prevent that. Callers must therefore treat a cancelled read as having left the byte
 *     stream in an unknown position, and discard the session rather than resume it.
 *   - `write`: NOT cancelable. The [[Canceler]] is a no-op on every platform, so the bytes already
 *     handed to `write` are always transferred in full. Abandoning a partial write would leave a
 *     framed protocol unrecoverable on the peer side, which is worse than finishing. Note that the
 *     surrounding effect's fiber is still cancelled immediately; only the byte transfer runs on.
 *   - `connect`: cancelling closes the channel.
 */
trait RawSocket:
  /** Reads up to `n` bytes. `Some(bytes)` on data (empty for `n <= 0`), `None` at end of stream. */
  def read(n:      Int, cb:         Either[Throwable, Option[Array[Byte]]] => Unit): Canceler
  def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit):                Canceler
  def close():                                                                       Unit

/** Opens [[RawSocket]]s. One implementation per platform. Connect timeout is applied at the `F` layer. */
trait RawIoEngine:
  def connect(host: String, port: Int, options: SocketOptions, cb: Either[Throwable, RawSocket] => Unit): Canceler

/** Raised when a connect does not establish a connection within its timeout (applied at the `F` layer). */
final class ConnectTimeoutException(message: String) extends RuntimeException(message)
