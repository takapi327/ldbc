/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import com.comcast.ip4s.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.data.CapabilitiesFlags
import ldbc.connector.net.*
import ldbc.connector.net.protocol.*
import ldbc.connector.exception.MySQLException

trait Connection[F[_]]:

  /**
   * Puts this connection in read-only mode as a hint to the driver to enable
   * database optimizations.
   *
   * @param isReadOnly
   *   true enables read-only mode; false disables it
   */
  def setReadOnly(isReadOnly: Boolean): F[Unit]

  /**
   * Sets this connection's auto-commit mode to the given state.
   * If a connection is in auto-commit mode, then all its SQL statements will be executed and committed as individual transactions.
   * Otherwise, its SQL statements are grouped into transactions that are terminated by a call to either the method commit or the method rollback.
   * By default, new connections are in auto-commit mode.
   *
   * @param isAutoCommit
   *   true to enable auto-commit mode; false to disable it
   */
  def setAutoCommit(isAutoCommit: Boolean): F[Unit]

  /**
   * Retrieves the current auto-commit mode for this Connection object.
   *
   * @return
   *   the current state of this Connection object's auto-commit mode
   */
  def getAutoCommit: F[Boolean]

  /**
   * Retrieves whether this Connection object is in read-only mode.
   *
   * @return
   *   true if this Connection object is read-only; false otherwise
   */
  def isReadOnly: F[Boolean]

  /**
   * Creates a statement with the given SQL.
   *
   * @param sql
   *   SQL queries based on text protocols
   */
  def statement(sql: String): Statement[F]

  /**
   * Creates a client-side prepared statement with the given SQL.
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
   * Releases this Connection object's database and LDBC resources immediately instead of waiting for them to be automatically released.
   *
   * Calling the method close on a Connection object that is already closed is a no-op.
   *
   * It is strongly recommended that an application explicitly commits or rolls back an active transaction prior to calling the close method.
   * If the close method is called and there is an active transaction, the results are implementation-defined.
   */
  def close(): F[Unit]

object Connection:

  private val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  private val defaultCapabilityFlags: List[CapabilitiesFlags] = List(
    CapabilitiesFlags.CLIENT_LONG_PASSWORD,
    CapabilitiesFlags.CLIENT_FOUND_ROWS,
    CapabilitiesFlags.CLIENT_LONG_FLAG,
    CapabilitiesFlags.CLIENT_PROTOCOL_41,
    CapabilitiesFlags.CLIENT_TRANSACTIONS,
    CapabilitiesFlags.CLIENT_RESERVED2,
    CapabilitiesFlags.CLIENT_MULTI_RESULTS,
    CapabilitiesFlags.CLIENT_PS_MULTI_RESULTS,
    CapabilitiesFlags.CLIENT_PLUGIN_AUTH,
    CapabilitiesFlags.CLIENT_CONNECT_ATTRS,
    CapabilitiesFlags.CLIENT_PLUGIN_AUTH_LENENC_CLIENT_DATA,
    CapabilitiesFlags.CLIENT_DEPRECATE_EOF,
    CapabilitiesFlags.CLIENT_QUERY_ATTRIBUTES,
    CapabilitiesFlags.MULTI_FACTOR_AUTHENTICATION
  )

  case class ConnectionImpl[F[_]: Temporal: Tracer: Console](
    protocol:   MySQLProtocol[F],
    readOnly:   Ref[F, Boolean],
    autoCommit: Ref[F, Boolean]
  ) extends Connection[F]:
    override def setReadOnly(isReadOnly: Boolean): F[Unit] =
      readOnly.update(_ => isReadOnly) *>
        protocol
          .statement("SET SESSION TRANSACTION READ " + (if isReadOnly then "ONLY" else "WRITE"))
          .executeQuery()
          .void

    override def isReadOnly: F[Boolean] = readOnly.get

    override def setAutoCommit(isAutoCommit: Boolean): F[Unit] =
      autoCommit.update(_ => isAutoCommit) *>
        protocol
          .statement("SET autocommit=" + (if isAutoCommit then "1" else "0"))
          .executeQuery()
          .void

    override def getAutoCommit: F[Boolean] = autoCommit.get

    override def statement(sql: String): Statement[F] = protocol.statement(sql)

    override def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]] =
      protocol.clientPreparedStatement(sql)

    override def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]] =
      protocol.serverPreparedStatement(sql)

    override def close(): F[Unit] = getAutoCommit.flatMap { autoCommit =>
      if !autoCommit then protocol.statement("ROLLBACK").executeQuery().void
      else Applicative[F].unit
    }

  def apply[F[_]: Temporal: Network: Console](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = Connection.defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  ): Tracer[F] ?=> Resource[F, Connection[F]] =

    val logger: String => F[Unit] = s => Console[F].println(s"TLS: $s")

    for
      sslOp <- ssl.toSSLNegotiationOptions(if debug then logger.some else none)
      connection <- fromSocketGroup(
                      Network[F],
                      host,
                      port,
                      user,
                      password,
                      database,
                      debug,
                      socketOptions,
                      sslOp,
                      readTimeout,
                      allowPublicKeyRetrieval
                    )
    yield connection

  def fromSockets[F[_]: Temporal: Tracer: Console](
    sockets:                 Resource[F, Socket[F]],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  ): Resource[F, Connection[F]] =
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then List(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else List.empty) ++
      (if sslOptions.isDefined then List(CapabilitiesFlags.CLIENT_SSL) else List.empty)
    for
      protocol <- MySQLProtocol[F](sockets, debug, sslOptions, readTimeout, capabilityFlags)
      _ <- Resource.eval(
             protocol.authenticate(
               user,
               password.getOrElse(""),
               database,
               sslOptions.isDefined,
               allowPublicKeyRetrieval,
               capabilityFlags
             )
           )
      readOnly   <- Resource.eval(Ref[F].of[Boolean](false))
      autoCommit <- Resource.eval(Ref[F].of[Boolean](true))
      connection <- Resource.make(Temporal[F].pure(ConnectionImpl[F](protocol, readOnly, autoCommit)))(_.close())
    yield connection

  def fromSocketGroup[F[_]: Tracer: Console](
    socketGroup:             SocketGroup[F],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    socketOptions:           List[SocketOption],
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false
  )(using ev: Temporal[F]): Resource[F, Connection[F]] =

    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new MySQLException(sql = None, message = msg)))

    def sockets: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(sockets, host, port, user, password, database, debug, sslOptions, readTimeout, allowPublicKeyRetrieval)
