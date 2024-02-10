/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.concurrent.duration.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.*

import fs2.io.net.Socket

import ldbc.connector.authenticator.*
import ldbc.connector.exception.MySQLException
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
      initialPacketRef <- Resource.eval(Ref[F].of[Option[InitialPacket]](None))
      ps <- PacketSocket[F](debug, sockets, sslOptions, sequenceIdRef, initialPacketRef, readTimeout)
      protocol <- Resource.make(fromPacketSocket(ps, sequenceIdRef, initialPacketRef))(_.close())
    yield protocol

  def fromPacketSocket[F[_]: Temporal: Console](
    packetSocket:  PacketSocket[F],
    sequenceIdRef: Ref[F, Byte],
    initialPacketRef: Ref[F, Option[InitialPacket]]
  ): F[MySQLProtocol[F]] =
    for
      given Exchange[F] <- Exchange[F]
      initialPacketOpt <- initialPacketRef.get
    yield initialPacketOpt match
      case Some(initial) =>
        new MySQLProtocol[F]:
  
          override def initialPacket: InitialPacket = initial
  
          private def readUntilOk(): F[Unit] =
            packetSocket.receive(AuthenticationPacket.decoder(initialPacket.capabilityFlags)).flatMap {
              case _: AuthMoreDataPacket => readUntilOk()
              case _: OKPacket           => Concurrent[F].unit
              case error: ERRPacket =>
                Concurrent[F].raiseError(error.toException("Connection error"))
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
      case None => throw new MySQLException(None, "Initial packet is not set")
