/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net.effect

import ldbc.effect.Async

import ldbc.net.SSL

/**
 * Upgrades a connected plaintext [[Socket]] to TLS, generic over the effect `F`. Backed by the per-platform
 * generic [[PlatformTls]] engine (JVM = JSSE `SSLEngine`, JS = node `tls`, Native = s2n-tls), so any effect
 * with an `Async` instance (`IO` / `Task` / `Fx`) completes the handshake natively with no bridging.
 */
trait TlsUpgrade[F[_]]:
  def client(socket: Socket[F], host: String, port: Int, ssl: SSL): F[Socket[F]]

object TlsUpgrade:

  /**
   * The default TLS upgrade for any `F : Async`. [[SSL.None]] is a no-op (the plaintext socket is returned
   * unchanged); every other policy is completed by the platform TLS engine, which applies trust and — per the
   * policy — hostname verification during the handshake.
   */
  given tlsUpgrade[F[_]](using F: Async[F]): TlsUpgrade[F] = new TlsUpgrade[F]:
    override def client(socket: Socket[F], host: String, port: Int, ssl: SSL): F[Socket[F]] =
      ssl match
        case SSL.None => F.pure(socket)
        case _        => PlatformTls.client[F](socket, host, port, ssl)
