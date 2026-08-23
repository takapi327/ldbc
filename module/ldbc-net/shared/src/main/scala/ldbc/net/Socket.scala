/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import scala.concurrent.duration.FiniteDuration

import ldbc.effect.{ Async, Concurrent }

import ldbc.net.{ ConnectTimeoutException, PlatformRawEngine, RawIoEngine, RawSocket, SocketOptions }

/**
 * Non-blocking byte transport generic over the effect `F` (design §4.3). This replaces the `Fx`-returning
 * `ldbc.net.Socket`: it wraps the effect-free [[ldbc.net.RawSocket]] with `F.async`, so any effect with an
 * `Async` instance (`IO` / `Task` / `Fx`) runs the same raw I/O natively.
 */
trait Socket[F[_]]:
  def read(n: Int): F[Option[Array[Byte]]]
  def write(bytes: Array[Byte]): F[Unit]
  def close(): F[Unit]

/**
 * A [[Socket]] backed by a known [[ldbc.net.RawSocket]], exposing it so the platform TLS layer can reach the
 * concrete raw socket (`FdRawSocket` / `NodeRawSocket`) it must drive directly.
 */
trait RawBackedSocket:
  def underlying: RawSocket

object Socket:

  /** Wraps a raw callback socket as a `Socket[F]`. Each op is one `F.async` over the raw callback. */
  def fromRaw[F[_]](raw: RawSocket)(using F: Async[F]): Socket[F] = new FromRawSocket[F](raw)

  /** The [[fromRaw]] wrapper, named so it exposes its [[underlying]] raw socket to the TLS layer. */
  private[net] final class FromRawSocket[F[_]](raw: RawSocket)(using F: Async[F]) extends Socket[F], RawBackedSocket:
    override def underlying: RawSocket = raw
    override def read(n: Int): F[Option[Array[Byte]]] =
      F.async(cb => F.delay { val c = raw.read(n, cb); Some(F.delay(c.cancel())): Option[F[Unit]] })
    override def write(bytes: Array[Byte]): F[Unit] =
      F.async(cb => F.delay { val c = raw.write(bytes, cb); Some(F.delay(c.cancel())): Option[F[Unit]] })
    override def close(): F[Unit] = F.delay(raw.close())

/** Opens [[Socket]]s over `F`. The connect timeout is applied via `Concurrent.timeout`. */
trait IoEngine[F[_]]:
  def connect(
    host:    String,
    port:    Int,
    timeout: FiniteDuration,
    options: SocketOptions = SocketOptions.default
  ): F[Socket[F]]

object IoEngine:

  /**
   * The platform-default engine for any `F : Concurrent`, wrapping the effect-free platform raw engine
   * (JVM = NIO selector, JS = node `net`, Native = epoll/kqueue) once with `F.async`. Kept in the companion
   * so it is found in the implicit scope of `IoEngine[F]` without an extra import, and so `IO` / `Task` /
   * `Fx` all drive the same native raw engine with no bridging.
   */
  given platformGlobal[F[_]](using F: Concurrent[F]): IoEngine[F] = fromRaw(PlatformRawEngine.global)

  /** Wraps a raw callback engine as an `IoEngine[F]`. */
  def fromRaw[F[_]](raw: RawIoEngine)(using F: Concurrent[F]): IoEngine[F] = new IoEngine[F]:
    override def connect(host: String, port: Int, timeout: FiniteDuration, options: SocketOptions): F[Socket[F]] =
      val connecting: F[Socket[F]] =
        F.async(cb =>
          F.delay {
            val c = raw.connect(host, port, options, r => cb(r.map(Socket.fromRaw[F])))
            Some(F.delay(c.cancel())): Option[F[Unit]]
          }
        )
      F.timeout(connecting, timeout)(
        new ConnectTimeoutException(s"connect to $host:$port timed out after $timeout")
      )
