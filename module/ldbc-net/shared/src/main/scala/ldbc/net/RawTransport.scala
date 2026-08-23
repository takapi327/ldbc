/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

/**
 * The effect-free, callback-based transport layer (design §4.3). The platform engines expose this; the
 * generic `ldbc.net.effect.Socket[F]` / `IoEngine[F]` wrap it once with `F.async`, so every effect
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
 */
trait RawSocket:
  /** Reads up to `n` bytes. `Some(bytes)` on data (empty for `n <= 0`), `None` at end of stream. */
  def read(n:      Int, cb:         Either[Throwable, Option[Array[Byte]]] => Unit): Canceler
  def write(bytes: Array[Byte], cb: Either[Throwable, Unit] => Unit):                Canceler
  def close():                                                                       Unit

/** Opens [[RawSocket]]s. One implementation per platform. Connect timeout is applied at the `F` layer. */
trait RawIoEngine:
  def connect(host: String, port: Int, options: SocketOptions, cb: Either[Throwable, RawSocket] => Unit): Canceler
