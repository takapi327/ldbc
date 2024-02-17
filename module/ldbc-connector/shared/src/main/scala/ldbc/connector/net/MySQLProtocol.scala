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

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.exception.MySQLException
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.protocol.*

trait MySQLProtocol[F[_]]:

  def initialPacket: InitialPacket

  def authenticate(user: String, password: String): F[Unit]

  def resetSequenceId: F[Unit]

  def close(): F[Unit]

object MySQLProtocol:

  def apply[F[_]: Temporal: Console: Tracer](
    sockets:     Resource[F, Socket[F]],
    debug:       Boolean,
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, MySQLProtocol[F]] =
    for
      sequenceIdRef    <- Resource.eval(Ref[F].of[Byte](0x01))
      initialPacketRef <- Resource.eval(Ref[F].of[Option[InitialPacket]](None))
      ps               <- PacketSocket[F](debug, sockets, sslOptions, sequenceIdRef, initialPacketRef, readTimeout)
      protocol         <- Resource.make(fromPacketSocket(ps, sequenceIdRef, initialPacketRef))(_.close())
    yield protocol

  def fromPacketSocket[F[_]: Temporal: Console: Tracer](
    packetSocket:     PacketSocket[F],
    sequenceIdRef:    Ref[F, Byte],
    initialPacketRef: Ref[F, Option[InitialPacket]]
  ): F[MySQLProtocol[F]] =
    for
      given Exchange[F] <- Exchange[F]
      initialPacketOpt  <- initialPacketRef.get
    yield initialPacketOpt match
      case Some(initial) =>
        new MySQLProtocol[F]:

          override def initialPacket: InitialPacket = initial

          override def authenticate(user: String, password: String): F[Unit] =
            Authentication[F](packetSocket, initialPacket).apply(user, password, None)

          override def resetSequenceId: F[Unit] =
            sequenceIdRef.update(_ => 0.toByte)

          override def close(): F[Unit] = resetSequenceId *> packetSocket.send(ComQuitPacket())
      case None => throw new MySQLException("Initial packet is not set")
