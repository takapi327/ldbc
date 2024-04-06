/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.concurrent.duration.*
import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.*

import fs2.io.net.Socket

import org.typelevel.otel4s.trace.Tracer

import scodec.Decoder

import ldbc.connector.data.*
import ldbc.connector.exception.MySQLException
import ldbc.connector.net.packet.ResponsePacket
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
   */
  def authenticate(user: String, password: String): F[Unit]

  /**
   * Creates a Statement object for sending SQL statements to the database.
   * SQL statements without parameters are normally executed using Statement objects.
   * If the same SQL statement is executed many times, it may be more efficient to use a PreparedStatement object.
   */
  def statement(): F[Statement[F]]

  /**
   * Creates a client prepared statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]]

  /**
   * Creates a server prepared statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]]

  /**
   * Resets the sequence id.
   */
  def resetSequenceId: F[Unit]

  /**
   * Closes the connection.
   */
  def close(): F[Unit]

  /**
   * Sets the schema name that will be used for subsequent queries.
   *
   * @param schema
   *   the name of a schema in which to work
   */
  def setSchema(schema: String): F[Unit]

  /**
   * Returns the statistics of the connection.
   */
  def getStatistics: F[StatisticsPacket]

  /**
   * Returns true if the connection has not been closed and is still valid.
   */
  def isValid: F[Boolean]

  /**
   * Resets the connection.
   */
  def resetConnection: F[Unit]

  /**
   * Controls whether or not multiple SQL statements are allowed to be executed at once.
   *
   * @param optionOperation
   *   [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_ON]] or [[EnumMySQLSetOption.MYSQL_OPTION_MULTI_STATEMENTS_OFF]]
   */
  def setOption(optionOperation: EnumMySQLSetOption): F[Unit]

  /**
   * Changes the user of the connection.
   *
   * @param user
   *   the user name
   * @param password
   *   the password
   */
  def changeUser(user: String, password: String): F[Unit]

object MySQLProtocol:

  case class MySQLProtocolImpl[F[_]: Temporal: Console: Tracer](
    initialPacket:           InitialPacket,
    packetSocket:            PacketSocket[F],
    database:                Option[String],
    useSSL:                  Boolean,
    allowPublicKeyRetrieval: Boolean,
    capabilitiesFlags:       List[CapabilitiesFlags],
    sequenceIdRef:           Ref[F, Byte],
    initialPacketRef:        Ref[F, Option[InitialPacket]]
  )(using ev: MonadError[F, Throwable], ex: Exchange[F])
    extends MySQLProtocol[F]:

    private val authenticate =
      Authentication[F](packetSocket, initialPacket, database, useSSL, allowPublicKeyRetrieval, capabilitiesFlags)
    private val utilityCommands = UtilityCommands[F](packetSocket, initialPacket)

    override def authenticate(user: String, password: String): F[Unit] = authenticate.start(user, password)

    override def statement(): F[Statement[F]] =
      Ref[F]
        .of(Vector.empty[String])
        .map(batchedArgs => Statement[F](packetSocket, initialPacket, batchedArgs, resetSequenceId))

    override def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]] =
      for
        params      <- Ref[F].of(ListMap.empty[Int, Parameter])
        batchedArgs <- Ref[F].of(Vector.empty[String])
      yield PreparedStatement.Client[F](packetSocket, initialPacket, sql, params, batchedArgs, resetSequenceId)

    private def repeatProcess[P <: ResponsePacket](times: Int, decoder: Decoder[P]): F[List[P]] =

      def read(remaining: Int, acc: List[P]): F[List[P]] =
        if remaining <= 0 then ev.pure(acc)
        else packetSocket.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

      read(times, List.empty[P])

    override def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]] =
      for
        result <- resetSequenceId *> packetSocket.send(ComStmtPreparePacket(sql)) *>
                    packetSocket.receive(ComStmtPrepareOkPacket.decoder(initialPacket.capabilityFlags)).flatMap {
                      case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                      case ok: ComStmtPrepareOkPacket => ev.pure(ok)
                    }
        _           <- repeatProcess(result.numParams, ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags))
        _           <- repeatProcess(result.numColumns, ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags))
        params      <- Ref[F].of(ListMap.empty[Int, Parameter])
        batchedArgs <- Ref[F].of(Vector.empty[String])
      yield PreparedStatement
        .Server[F](packetSocket, initialPacket, result.statementId, sql, params, batchedArgs, resetSequenceId)

    override def resetSequenceId: F[Unit] =
      sequenceIdRef.update(_ => 0.toByte)

    override def close(): F[Unit] = resetSequenceId *> utilityCommands.comQuit()

    override def setSchema(schema: String): F[Unit] = resetSequenceId *> utilityCommands.comInitDB(schema)

    override def getStatistics: F[StatisticsPacket] = resetSequenceId *> utilityCommands.comStatistics()

    override def isValid: F[Boolean] = resetSequenceId *> utilityCommands.comPing()

    override def resetConnection: F[Unit] = resetSequenceId *> utilityCommands.comResetConnection()

    override def setOption(optionOperation: EnumMySQLSetOption): F[Unit] =
      resetSequenceId *> utilityCommands.comSetOption(optionOperation)

    override def changeUser(user: String, password: String): F[Unit] =
      resetSequenceId *> authenticate.changeUser(user, password)

  def apply[F[_]: Temporal: Console: Tracer](
    sockets:                 Resource[F, Socket[F]],
    database:                Option[String],
    debug:                   Boolean,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration,
    allowPublicKeyRetrieval: Boolean,
    capabilitiesFlags:       List[CapabilitiesFlags]
  ): Resource[F, MySQLProtocol[F]] =
    for
      sequenceIdRef    <- Resource.eval(Ref[F].of[Byte](0x01))
      initialPacketRef <- Resource.eval(Ref[F].of[Option[InitialPacket]](None))
      ps <- PacketSocket[F](debug, sockets, sslOptions, sequenceIdRef, initialPacketRef, readTimeout, capabilitiesFlags)
      protocol <- Resource.make(
                    fromPacketSocket(
                      ps,
                      database,
                      sslOptions,
                      allowPublicKeyRetrieval,
                      capabilitiesFlags,
                      sequenceIdRef,
                      initialPacketRef
                    )
                  )(_.close())
    yield protocol

  def fromPacketSocket[F[_]: Temporal: Console: Tracer](
    packetSocket:            PacketSocket[F],
    database:                Option[String],
    sslOptions:              Option[SSLNegotiation.Options[F]],
    allowPublicKeyRetrieval: Boolean,
    capabilitiesFlags:       List[CapabilitiesFlags],
    sequenceIdRef:           Ref[F, Byte],
    initialPacketRef:        Ref[F, Option[InitialPacket]]
  )(using ev: MonadError[F, Throwable]): F[MySQLProtocol[F]] =
    for
      given Exchange[F] <- Exchange[F]
      initialPacketOpt  <- initialPacketRef.get
    yield initialPacketOpt match
      case Some(initial) =>
        MySQLProtocolImpl(
          initial,
          packetSocket,
          database,
          sslOptions.isDefined,
          allowPublicKeyRetrieval,
          capabilitiesFlags,
          sequenceIdRef,
          initialPacketRef
        )
      case None => throw new MySQLException("Initial packet is not set")
