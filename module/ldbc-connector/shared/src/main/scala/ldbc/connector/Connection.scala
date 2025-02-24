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
import cats.effect.std.UUIDGen

import fs2.hashing.Hashing
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.{ Connection, DatabaseMetaData }
import ldbc.sql.logging.{LogEvent, LogHandler}

import ldbc.connector.data.*
import ldbc.connector.exception.*
import ldbc.connector.net.*
import ldbc.connector.net.protocol.*

type Connection[F[_]] = ldbc.sql.Connection[F]
object Connection:

  private val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  private val defaultCapabilityFlags: Set[CapabilitiesFlags] = Set(
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

  private def consoleLogger[F[_] : Console : Sync]: LogHandler[F] =
    case LogEvent.Success(sql, args) =>
      Console[F].println(
        s"""Successful Statement Execution:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      )
    case LogEvent.ProcessingFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed ResultSet Processing:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
    case LogEvent.ExecFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed Statement Execution:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)

  private def unitBefore[F[_]: Async]: Connection[F] => F[Unit]         = _ => Async[F].unit
  private def unitAfter[F[_]: Async]:  (Unit, Connection[F]) => F[Unit] = (_, _) => Async[F].unit

  def apply[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host: String,
    port: Int,
    user: String
  ): Tracer[F] ?=> Resource[F, LdbcConnection[F]] =
    this.default[F, Unit](host, port, user, before = unitBefore, after = unitAfter)

  def apply[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    logHandler: Option[LogHandler[F]] = None
  ): Tracer[F] ?=> Resource[F, LdbcConnection[F]] = this.default[F, Unit](
    host,
    port,
    user,
    password,
    database,
    debug,
    ssl,
    socketOptions,
    readTimeout,
    allowPublicKeyRetrieval,
    databaseTerm,
    logHandler,
    unitBefore,
    unitAfter
  )

  def withBeforeAfter[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    host:                    String,
    port:                    Int,
    user:                    String,
    before:                  Connection[F] => F[A],
    after:                   (A, Connection[F]) => F[Unit],
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    logHandler: Option[LogHandler[F]] = None
  ): Tracer[F] ?=> Resource[F, LdbcConnection[F]] = this.default(
    host,
    port,
    user,
    password,
    database,
    debug,
    ssl,
    socketOptions,
    readTimeout,
    allowPublicKeyRetrieval,
    databaseTerm,
    logHandler,
    before,
    after
  )

  def default[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    ssl:                     SSL = SSL.None,
    socketOptions:           List[SocketOption] = defaultSocketOptions,
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    logHandler: Option[LogHandler[F]] = None,
    before:                  Connection[F] => F[A],
    after:                   (A, Connection[F]) => F[Unit]
  ): Tracer[F] ?=> Resource[F, LdbcConnection[F]] =

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
                      allowPublicKeyRetrieval,
                      databaseTerm,
        logHandler.getOrElse(consoleLogger),
                      before,
                      after
                    )
    yield connection

  def fromSockets[F[_]: Async: Tracer: Console: Hashing: UUIDGen, A](
    sockets:                 Resource[F, Socket[F]],
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String] = None,
    database:                Option[String] = None,
    debug:                   Boolean = false,
    sslOptions:              Option[SSLNegotiation.Options[F]],
    readTimeout:             Duration = Duration.Inf,
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = None,
    logHandler: LogHandler[F],
    acquire:                 Connection[F] => F[A],
    release:                 (A, Connection[F]) => F[Unit]
  ): Resource[F, LdbcConnection[F]] =
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then Set(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else Set.empty) ++
      (if sslOptions.isDefined then Set(CapabilitiesFlags.CLIENT_SSL) else Set.empty)
    val hostInfo = HostInfo(host, port, user, password, database)
    for
      given Exchange[F] <- Resource.eval(Exchange[F])
      protocol <-
        Protocol[F](sockets, hostInfo, debug, sslOptions, allowPublicKeyRetrieval, readTimeout, capabilityFlags)
      _                <- Resource.eval(protocol.startAuthentication(user, password.getOrElse("")))
      serverVariables  <- Resource.eval(protocol.serverVariables())
      readOnly         <- Resource.eval(Ref[F].of[Boolean](false))
      autoCommit       <- Resource.eval(Ref[F].of[Boolean](true))
      connectionClosed <- Resource.eval(Ref[F].of[Boolean](false))
      connection <-
        Resource.make(
          Temporal[F].pure(
            ConnectionImpl[F](
              protocol,
              serverVariables,
              database,
              readOnly,
              autoCommit,
              connectionClosed,
              databaseTerm.getOrElse(DatabaseMetaData.DatabaseTerm.CATALOG),
              logHandler
            )
          )
        )(_.close())
      _ <- Resource.make(acquire(connection))(v => release(v, connection))
    yield connection

  def fromSocketGroup[F[_]: Tracer: Console: Hashing: UUIDGen, A](
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
    allowPublicKeyRetrieval: Boolean = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = None,
    logHandler: LogHandler[F],
    acquire:                 Connection[F] => F[A],
    release:                 (A, Connection[F]) => F[Unit]
  )(using ev: Async[F]): Resource[F, LdbcConnection[F]] =

    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new SQLClientInfoException(msg)))

    def sockets: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(
      sockets,
      host,
      port,
      user,
      password,
      database,
      debug,
      sslOptions,
      readTimeout,
      allowPublicKeyRetrieval,
      databaseTerm,
      logHandler,
      acquire,
      release
    )
