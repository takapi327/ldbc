/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import cats.*
import cats.syntax.all.*

import cats.effect.*

import fs2.Chunk
import fs2.io.net.*
import fs2.io.net.tls.*

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.net.packet.request.SSLRequestPacket

object SSLNegotiation:

  /** Parameters for `negotiateSSL`. */
  case class Options[F[_]](
    tlsContext:    TLSContext[F],
    tlsParameters: TLSParameters,
    fallbackOk:    Boolean,
    logger:        Option[String => F[Unit]]
  )

  def negotiateSSL[F[_]](
    socket:          Socket[F],
    capabilityFlags: Seq[CapabilitiesFlags],
    sslOptions:      SSLNegotiation.Options[F],
    sequenceIdRef:   Ref[F, Byte]
  ): Resource[F, Socket[F]] =
    for
      sequenceId <- Resource.eval(sequenceIdRef.get)
      _ <- Resource.eval(socket.write(Chunk.byteVector(SSLRequestPacket(sequenceId, capabilityFlags).encode.bytes)))
      socket$ <-
        sslOptions.tlsContext
          .clientBuilder(socket)
          .withParameters(sslOptions.tlsParameters)
          .withLogger(
            sslOptions.logger.fold[TLSLogger[F]](TLSLogger.Disabled)(logger => TLSLogger.Enabled(x => logger(x)))
          )
          .build
      _ <- Resource.eval(sequenceIdRef.update(sequenceId => ((sequenceId + 1) % 256).toByte))
    yield socket$
