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

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.exception.MySQLException
import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.protocol.*

/**
 * MySQLProtocol is a protocol to communicate with MySQL server.
 * It provides a way to authenticate, reset sequence id, and close the connection.
 * 
 * @tparam F
 *   the effect type
 */
trait MySQLProtocol[F[_]]:

  /**
   * Returns the initial packet.
   * 
   * @return
   *   the initial packet
   */
  def initialPacket: InitialPacket

  /**
   * Authenticates the user with the given password.
   * 
   * @param user
   *   the user name
   * @param password
   *   the password
   * @param database
   *   Database used for login
   * @param useSSL
   *   whether to use SSL
   * @param allowPublicKeyRetrieval
   *   whether to allow public key retrieval
   * @param capabilitiesFlags
   *   Values for the capabilities flag bitmask used by the MySQL protocol.
   */
  def authenticate(
    user:                    String,
    password:                String,
    database:                Option[String],
    useSSL:                  Boolean,
    allowPublicKeyRetrieval: Boolean,
    capabilitiesFlags:       List[CapabilitiesFlags]
  ): F[Unit]

  /**
   * Creates a statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def statement(sql: String): Statement[F]

  /**
   * Resets the sequence id.
   */
  def resetSequenceId: F[Unit]

  /**
   * Closes the connection.
   */
  def close(): F[Unit]

object MySQLProtocol:

  def apply[F[_]: Temporal: Console: Tracer](
    sockets:           Resource[F, Socket[F]],
    debug:             Boolean,
    sslOptions:        Option[SSLNegotiation.Options[F]],
    readTimeout:       Duration,
    capabilitiesFlags: List[CapabilitiesFlags]
  ): Resource[F, MySQLProtocol[F]] =
    for
      sequenceIdRef    <- Resource.eval(Ref[F].of[Byte](0x01))
      initialPacketRef <- Resource.eval(Ref[F].of[Option[InitialPacket]](None))
      ps <- PacketSocket[F](debug, sockets, sslOptions, sequenceIdRef, initialPacketRef, readTimeout, capabilitiesFlags)
      protocol <- Resource.make(fromPacketSocket(ps, sequenceIdRef, initialPacketRef))(_.close())
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

          override def authenticate(
            user:                    String,
            password:                String,
            database:                Option[String],
            useSSL:                  Boolean,
            allowPublicKeyRetrieval: Boolean,
            capabilitiesFlags:       List[CapabilitiesFlags]
          ): F[Unit] =
            Authentication[F](
              packetSocket,
              initialPacket,
              user,
              password,
              database,
              useSSL,
              allowPublicKeyRetrieval,
              capabilitiesFlags
            )
              .start()

          override def statement(sql: String): Statement[F] =
            Statement[F](packetSocket, initialPacket, sql, resetSequenceId)

          override def resetSequenceId: F[Unit] =
            sequenceIdRef.update(_ => 0.toByte)

          override def close(): F[Unit] = resetSequenceId *> packetSocket.send(ComQuitPacket())
      case None => throw new MySQLException("Initial packet is not set")
