/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import ldbc.fx.Fx

/**
 * TLS as a `Socket => Socket` decorator: wraps an already-connected plaintext [[Socket]] and returns
 * an encrypted one, which also expresses STARTTLS upgrades (MySQL-style) naturally. Platform
 * engines: JVM = JSSE `SSLEngine` driven by an Fx loop; JS and Native are later phases.
 */
object Tls:

  /**
   * Wraps the plaintext `socket` in a TLS client session, completing the handshake — including
   * certificate and (per `ssl`) hostname verification — before the returned [[Socket]] is
   * produced. The plaintext socket is closed if the handshake fails.
   *
   * @param socket the connected plaintext socket to upgrade
   * @param host   the server hostname or IP the client intended to reach; used for SNI (DNS names
   *               only) and hostname verification
   * @param port   the server port, used as the TLS session-cache hint
   * @param ssl    the TLS policy to apply
   * @return an encrypted [[Socket]] whose `read`/`write` carry plaintext
   */
  def client(socket: Socket, host: String, port: Int, ssl: SSL): Fx[Socket] =
    ssl match
      case SSL.None => Fx.pure(socket)
      case _        =>
        PlatformTls.client(SerializedSocket.unwrap(socket), host, port, ssl).flatMap(SerializedSocket.apply)
