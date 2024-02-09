/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.*

import cats.effect.*
import cats.effect.std.*

import fs2.io.net.Socket

import scodec.Decoder

import ldbc.connector.net.*
import ldbc.connector.net.packet.*
import ldbc.connector.net.packet.response.InitialPacket

trait MySQLProtocol[F[_]]:

  def initialPacket: InitialPacket

  def changeCommandPhase: F[Unit]

object MySQLProtocol:

  def apply[F[_]: Temporal: Console](
    sockets:     Resource[F, Socket[F]],
    debug:       Boolean,
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, MySQLProtocol[F]] =
    for
      sequenceIdRef <- Resource.eval(Ref[F].of[Byte](0x01))
      socket        <- sockets
      ps            <- Resource.eval(PacketSocket[F](debug, socket, sequenceIdRef, readTimeout))
      initialPacket$ <- Resource.eval(ps.receive(InitialPacket.decoder))
      socket$ <- sslOptions.fold(Resource.pure(ps))(option =>
                   SSLNegotiation
                     .negotiateSSL(socket, initialPacket$.capabilityFlags, option, sequenceIdRef)
                     .flatMap(ssl => Resource.eval(PacketSocket[F](debug, ssl, sequenceIdRef, readTimeout)))
                 )
    yield new MySQLSocket[F]:
      override def initialPacket: InitialPacket = initialPacket$
      override def changeCommandPhase: F[Unit] =
        sequenceIdRef.update(_ => 0.toByte)
