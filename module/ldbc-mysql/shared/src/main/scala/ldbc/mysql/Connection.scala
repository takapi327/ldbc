/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.sql.SQLClientInfoException
import scala.concurrent.duration.*

import ldbc.fx.{ Fx, Ref, Resource }

import ldbc.net.{ IoEngine, SSL, Socket, SocketOptions }

import ldbc.mysql.telemetry.Tracer

import ldbc.sql.DatabaseMetaData

import ldbc.mysql.data.*
import ldbc.mysql.net.*
import ldbc.mysql.net.protocol.*
import ldbc.mysql.telemetry.{ DatabaseMetrics, TelemetryConfig }

import ldbc.authentication.plugin.*

type Connection[F[_]] = ldbc.sql.Connection[F]

object Connection:

  /** The default timeout for establishing the TCP connection. */
  private val defaultConnectTimeout: FiniteDuration = 30.seconds

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

  private val unitBefore: Connection[Fx] => Fx[Unit]         = _ => Fx.unit
  private val unitAfter:  (Unit, Connection[Fx]) => Fx[Unit] = (_, _) => Fx.unit

  def apply(host: String, port: Int, user: String): Tracer ?=> Resource[LdbcConnection] =
    default[Unit](host, port, user, before = unitBefore, after = unitAfter)

  def apply(
    host:                        String,
    port:                        Int,
    user:                        String,
    password:                    Option[String]                        = None,
    database:                    Option[String]                        = None,
    debug:                       Boolean                               = false,
    ssl:                         SSL                                   = SSL.None,
    socketOptions:               SocketOptions                         = SocketOptions.default,
    readTimeout:                 Duration                              = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean                               = false,
    useCursorFetch:              Boolean                               = false,
    useServerPrepStmts:          Boolean                               = false,
    maxAllowedPacket:            Int                                   = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]]      = None,
    plugins:                     List[AuthenticationPlugin[Fx]]        = List.empty[AuthenticationPlugin[Fx]],
    telemetryConfig:             TelemetryConfig                       = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics]               = None
  ): Tracer ?=> Resource[LdbcConnection] = default[Unit](
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
    useCursorFetch,
    useServerPrepStmts,
    maxAllowedPacket,
    databaseTerm,
    defaultAuthenticationPlugin,
    plugins,
    telemetryConfig,
    databaseMetrics,
    unitBefore,
    unitAfter
  )

  def withBeforeAfter[A](
    host:                        String,
    port:                        Int,
    user:                        String,
    before:                      Connection[Fx] => Fx[A],
    after:                       (A, Connection[Fx]) => Fx[Unit],
    password:                    Option[String]                        = None,
    database:                    Option[String]                        = None,
    debug:                       Boolean                               = false,
    ssl:                         SSL                                   = SSL.None,
    socketOptions:               SocketOptions                         = SocketOptions.default,
    readTimeout:                 Duration                              = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean                               = false,
    useCursorFetch:              Boolean                               = false,
    useServerPrepStmts:          Boolean                               = false,
    maxAllowedPacket:            Int                                   = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]]      = None,
    plugins:                     List[AuthenticationPlugin[Fx]]        = List.empty[AuthenticationPlugin[Fx]],
    telemetryConfig:             TelemetryConfig                       = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics]               = None
  ): Tracer ?=> Resource[LdbcConnection] = default(
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
    useCursorFetch,
    useServerPrepStmts,
    maxAllowedPacket,
    databaseTerm,
    defaultAuthenticationPlugin,
    plugins,
    telemetryConfig,
    databaseMetrics,
    before,
    after
  )

  def default[A](
    host:                        String,
    port:                        Int,
    user:                        String,
    password:                    Option[String]                        = None,
    database:                    Option[String]                        = None,
    debug:                       Boolean                               = false,
    ssl:                         SSL                                   = SSL.None,
    socketOptions:               SocketOptions                         = SocketOptions.default,
    readTimeout:                 Duration                              = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean                               = false,
    useCursorFetch:              Boolean                               = false,
    useServerPrepStmts:          Boolean                               = false,
    maxAllowedPacket:            Int                                   = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]]      = None,
    plugins:                     List[AuthenticationPlugin[Fx]]        = List.empty[AuthenticationPlugin[Fx]],
    telemetryConfig:             TelemetryConfig                       = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics]               = None,
    before:                      Connection[Fx] => Fx[A],
    after:                       (A, Connection[Fx]) => Fx[Unit]
  ): Tracer ?=> Resource[LdbcConnection] =
    val sslOptions: Option[SSLNegotiation.Options] = ssl match
      case SSL.None => None
      case _        =>
        Some(SSLNegotiation.Options(ssl, host, port, fallbackOk = false, logger = None))

    val validateEndpoint: Fx[Unit] =
      if host.trim.isEmpty then
        Fx.raiseError(new SQLClientInfoException(s"""Hostname: "$host" is not syntactically valid."""))
      else if port < 0 || port > 65535 then
        Fx.raiseError(new SQLClientInfoException(s"""Port: "$port" is not valid."""))
      else Fx.unit

    val sockets: Resource[Socket] =
      Resource
        .eval(validateEndpoint)
        .flatMap(_ => Resource.make(IoEngine.global.connect(host, port, defaultConnectTimeout, socketOptions))(_.close()))

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
      useCursorFetch,
      useServerPrepStmts,
      maxAllowedPacket,
      databaseTerm,
      defaultAuthenticationPlugin,
      plugins,
      telemetryConfig,
      databaseMetrics,
      before,
      after
    )

  def fromSockets[A](
    sockets:                     Resource[Socket],
    host:                        String,
    port:                        Int,
    user:                        String,
    password:                    Option[String]                        = None,
    database:                    Option[String]                        = None,
    debug:                       Boolean                               = false,
    sslOptions:                  Option[SSLNegotiation.Options],
    readTimeout:                 Duration                              = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean                               = false,
    useCursorFetch:              Boolean                               = false,
    useServerPrepStmts:          Boolean                               = false,
    maxAllowedPacket:            Int                                   = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = None,
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[Fx]],
    plugins:                     List[AuthenticationPlugin[Fx]],
    telemetryConfig:             TelemetryConfig                       = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics]               = None,
    acquire:                     Connection[Fx] => Fx[A],
    release:                     (A, Connection[Fx]) => Fx[Unit]
  ): Tracer ?=> Resource[LdbcConnection] =
    val resolvedMetrics = databaseMetrics.getOrElse(DatabaseMetrics.noop)
    val pluginMap       = plugins.map(plugin => plugin.name.toString -> plugin).toMap
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then Set(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else Set.empty) ++
      (if sslOptions.isDefined then Set(CapabilitiesFlags.CLIENT_SSL) else Set.empty)
    val hostInfo = HostInfo(host, port, user, password, database)
    for
      given Exchange <- Resource.eval(Exchange.apply)
      protocol       <-
        Protocol(
          sockets,
          hostInfo,
          debug,
          sslOptions,
          allowPublicKeyRetrieval,
          readTimeout,
          capabilityFlags,
          maxAllowedPacket,
          defaultAuthenticationPlugin,
          pluginMap
        )
      _                <- Resource.eval(protocol.startAuthentication(user, password.getOrElse("")))
      serverVariables  <- Resource.eval(protocol.serverVariables())
      readOnly         <- Resource.eval(Ref.of[Boolean](false))
      autoCommit       <- Resource.eval(Ref.of[Boolean](true))
      connectionClosed <- Resource.eval(Ref.of[Boolean](false))
      connection       <-
        Resource.make(
          Fx.pure(
            ConnectionImpl(
              protocol,
              serverVariables,
              database,
              readOnly,
              autoCommit,
              connectionClosed,
              useCursorFetch,
              useServerPrepStmts,
              databaseTerm.getOrElse(DatabaseMetaData.DatabaseTerm.CATALOG),
              telemetryConfig,
              resolvedMetrics
            )
          )
        )(_.close())
      _ <- Resource.make(acquire(connection))(v => release(v, connection))
    yield connection
