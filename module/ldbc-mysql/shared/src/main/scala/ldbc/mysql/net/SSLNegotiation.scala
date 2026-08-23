/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net

import ldbc.effect.{ Concurrent, Ref, Resource }
import ldbc.mysql.data.CapabilitiesFlags
import ldbc.mysql.net.packet.request.SSLRequestPacket
import ldbc.net.SSL
import ldbc.net.effect.{ Socket, TlsUpgrade }

/**
 * MySQL STARTTLS negotiation: the SSL request packet is written in the clear, then the plaintext socket
 * is upgraded to TLS via [[ldbc.net.effect.TlsUpgrade]] (the transport supports upgrading an
 * already-connected socket, which is exactly what MySQL requires).
 */
object SSLNegotiation:

  /**
   * Parameters for [[negotiateSSL]].
   *
   * @param tlsConfig  the TLS policy (trust/verification) to apply
   * @param host       the server hostname (for SNI and hostname verification)
   * @param port       the server port (session-cache hint)
   * @param fallbackOk whether a failed TLS negotiation may fall back to plaintext
   * @param logger     an optional TLS logger
   */
  case class Options[F[_]](
    tlsConfig:  SSL,
    host:       String,
    port:       Int,
    fallbackOk: Boolean,
    logger:     Option[String => F[Unit]]
  )

  /**
   * Sends the SSL request and upgrades `socket` to TLS.
   *
   * @param socket          the connected plaintext socket
   * @param capabilityFlags the negotiated capability flags
   * @param sslOptions      the TLS options
   * @param sequenceIdRef   the MySQL packet sequence id
   * @return a resource producing the encrypted socket
   */
  def negotiateSSL[F[_]](
    socket:          Socket[F],
    capabilityFlags: Set[CapabilitiesFlags],
    sslOptions:      Options[F],
    sequenceIdRef:   Ref[F, Byte]
  )(using F: Concurrent[F], tls: TlsUpgrade[F]): Resource[F, Socket[F]] =
    for
      sequenceId <- Resource.eval(sequenceIdRef.get)
      _          <- Resource.eval(
             socket.write(SSLRequestPacket(sequenceId, capabilityFlags).encode.bytes.toArray)
           )
      encrypted <- Resource.make(
                     tls.client(socket, sslOptions.host, sslOptions.port, sslOptions.tlsConfig)
                   )(_.close())
      _ <- Resource.eval(sequenceIdRef.update(id => ((id + 1) % 256).toByte))
    yield encrypted
