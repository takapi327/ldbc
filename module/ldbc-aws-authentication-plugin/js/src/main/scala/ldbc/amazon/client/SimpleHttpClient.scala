/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

import scala.concurrent.duration.*

import com.comcast.ip4s.*

import cats.effect.*

import fs2.io.net.*
import fs2.io.net.tls.*

/**
 * Secure HTTP client that supports both HTTP and HTTPS protocols.
 * 
 * Security Features:
 * - Validates URI schemes and rejects unsupported protocols
 * - Uses TLS for HTTPS connections with proper certificate validation
 * - Defaults to secure ports (443 for HTTPS, 80 for HTTP)
 * - Prevents credentials from being sent over cleartext connections
 * 
 * This addresses the security vulnerability where AWS credentials
 * could be sent over unencrypted HTTP connections.
 */
final case class SimpleHttpClient[F[_]: Network: Async](
  connectTimeout: Duration,
  readTimeout:    Duration
) extends BasedHttpClient[F]:

  override def createSocket(address: SocketAddress[Host], isSecure: Boolean, host: String): Resource[F, Socket[F]] =
    if isSecure then
      for
        socket     <- Network[F].client(address)
        tlsContext <- Network[F].tlsContext.systemResource
        tlsSocket  <- tlsContext
                       .clientBuilder(socket)
                       .withParameters(TLSParameters(servername = Some(host)))
                       .build
      yield tlsSocket
    else Network[F].client(address)
