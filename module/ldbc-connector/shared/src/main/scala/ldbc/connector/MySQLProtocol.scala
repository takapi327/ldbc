/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.*

import fs2.io.net.Socket

import ldbc.connector.authenticator.*
import ldbc.connector.net.*
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.protocol.Exchange

trait MySQLProtocol[F[_]]:

  def initialPacket: InitialPacket

  def authenticate(user: String, password: String): F[Unit]

  def resetSequenceId: F[Unit]

  def close(): F[Unit]

object MySQLProtocol:

  def apply[F[_]: Temporal: Console](
    sockets:     Resource[F, Socket[F]],
    debug:       Boolean,
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, MySQLProtocol[F]] =
    for
      sequenceIdRef  <- Resource.eval(Ref[F].of[Byte](0x01))
      socket         <- sockets
      ps             <- Resource.eval(PacketSocket[F](debug, socket, sequenceIdRef, readTimeout))
      initialPacket$ <- Resource.eval(ps.receive(InitialPacket.decoder))
      socket$ <- sslOptions.fold(Resource.pure(ps))(option =>
                   SSLNegotiation
                     .negotiateSSL(socket, initialPacket$.capabilityFlags, option, sequenceIdRef)
                     .flatMap(ssl => Resource.eval(PacketSocket[F](debug, ssl, sequenceIdRef, readTimeout)))
                 )
      protocol <- Resource.make(fromPacketSocket(socket$, initialPacket$, sequenceIdRef))(_.close())
    yield protocol

  def fromPacketSocket[F[_]: Temporal: Console](
    packetSocket:    PacketSocket[F],
    initialPacket$ : InitialPacket,
    sequenceIdRef:   Ref[F, Byte]
  ): F[MySQLProtocol[F]] =
    Exchange[F].map { implicit ex =>
      new MySQLProtocol[F]:

        override def initialPacket: InitialPacket = initialPacket$

        private def readUntilOk(): F[Unit] =
          packetSocket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
            case _: AuthMoreDataPacket => readUntilOk()
            case _: OKPacket           => Concurrent[F].unit
            case error: ERRPacket =>
              Concurrent[F].raiseError(new Exception(s"Connection error: ${ error.errorMessage }"))
          }

        override def authenticate(user: String, password: String): F[Unit] =
          val plugin = initialPacket.authPlugin match
            case "mysql_native_password" => new MysqlNativePasswordPlugin
            case "caching_sha2_password" => CachingSha2PasswordPlugin(Some(password), None)
            case _                       => throw new Exception(s"Unknown plugin: ${ initialPacket.authPlugin }")

          val hashedPassword = plugin.hashPassword(password, initialPacket.scrambleBuff)

          val handshakeResponse = HandshakeResponsePacket(
            initialPacket.capabilityFlags,
            user,
            Array(hashedPassword.length.toByte) ++ hashedPassword,
            plugin.name
          )

          packetSocket.send(handshakeResponse) <* readUntilOk()

        override def resetSequenceId: F[Unit] =
          sequenceIdRef.update(_ => 0.toByte)

        override def close(): F[Unit] = resetSequenceId *> packetSocket.send(ComQuitPacket())
    }
