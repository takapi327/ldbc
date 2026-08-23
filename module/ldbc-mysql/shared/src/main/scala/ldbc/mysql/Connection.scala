/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scala.concurrent.duration.*

import ldbc.sql.DatabaseMetaData
import ldbc.sql.SQLClientInfoException

import ldbc.authentication.plugin.*
import ldbc.effect.{ Concurrent, Ref, Resource }
import ldbc.mysql.data.*
import ldbc.mysql.net.*
import ldbc.mysql.net.protocol.*
import ldbc.mysql.telemetry.{ DatabaseMetrics, TelemetryConfig }
import ldbc.mysql.telemetry.Tracer
import ldbc.net.{ SSL, SocketOptions }
import ldbc.net.effect.{ IoEngine, Socket, TlsUpgrade }

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

  private def unitBefore[F[_]](using F: Concurrent[F]): Connection[F] => F[Unit]        = _ => F.unit
  private def unitAfter[F[_]](using F: Concurrent[F]):  (Unit, Connection[F]) => F[Unit] = (_, _) => F.unit

  def apply[F[_]](host: String, port: Int, user: String)(using
    Concurrent[F],
    IoEngine[F],
    TlsUpgrade[F]
  ): Tracer[F] ?=> Resource[F, LdbcConnection[F]] =
    default[F, Unit](host, port, user, before = unitBefore[F], after = unitAfter[F])

  def apply[F[_]](
    host:                        String,
    port:                        Int,
    user:                        String,
    password:                    Option[String] = None,
    database:                    Option[String] = None,
    debug:                       Boolean = false,
    ssl:                         SSL = SSL.None,
    socketOptions:               SocketOptions = SocketOptions.default,
    readTimeout:                 Duration = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean = false,
    useCursorFetch:              Boolean = false,
    useServerPrepStmts:          Boolean = false,
    maxAllowedPacket:            Int = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]] = None,
    plugins:                     List[AuthenticationPlugin[F]] = List.empty[AuthenticationPlugin[F]],
    telemetryConfig:             TelemetryConfig = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics[F]] = None
  )(using Concurrent[F], IoEngine[F], TlsUpgrade[F]): Tracer[F] ?=> Resource[F, LdbcConnection[F]] = default[F, Unit](
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
    unitBefore[F],
    unitAfter[F]
  )

  def withBeforeAfter[F[_], A](
    host:                        String,
    port:                        Int,
    user:                        String,
    before:                      Connection[F] => F[A],
    after:                       (A, Connection[F]) => F[Unit],
    password:                    Option[String] = None,
    database:                    Option[String] = None,
    debug:                       Boolean = false,
    ssl:                         SSL = SSL.None,
    socketOptions:               SocketOptions = SocketOptions.default,
    readTimeout:                 Duration = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean = false,
    useCursorFetch:              Boolean = false,
    useServerPrepStmts:          Boolean = false,
    maxAllowedPacket:            Int = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]] = None,
    plugins:                     List[AuthenticationPlugin[F]] = List.empty[AuthenticationPlugin[F]],
    telemetryConfig:             TelemetryConfig = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics[F]] = None
  )(using Concurrent[F], IoEngine[F], TlsUpgrade[F]): Tracer[F] ?=> Resource[F, LdbcConnection[F]] = default[F, A](
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

  def default[F[_], A](
    host:                        String,
    port:                        Int,
    user:                        String,
    password:                    Option[String] = None,
    database:                    Option[String] = None,
    debug:                       Boolean = false,
    ssl:                         SSL = SSL.None,
    socketOptions:               SocketOptions = SocketOptions.default,
    readTimeout:                 Duration = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean = false,
    useCursorFetch:              Boolean = false,
    useServerPrepStmts:          Boolean = false,
    maxAllowedPacket:            Int = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]] = None,
    plugins:                     List[AuthenticationPlugin[F]] = List.empty[AuthenticationPlugin[F]],
    telemetryConfig:             TelemetryConfig = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics[F]] = None,
    before:                      Connection[F] => F[A],
    after:                       (A, Connection[F]) => F[Unit]
  )(using F: Concurrent[F], engine: IoEngine[F], tls: TlsUpgrade[F]): Tracer[F] ?=> Resource[F, LdbcConnection[F]] =
    val sslOptions: Option[SSLNegotiation.Options[F]] = ssl match
      case SSL.None => None
      case _        =>
        Some(SSLNegotiation.Options[F](ssl, host, port, fallbackOk = false, logger = None))

    val validateEndpoint: F[Unit] =
      if host.trim.isEmpty then
        F.raiseError(new SQLClientInfoException(s"""Hostname: "$host" is not syntactically valid."""))
      else if port < 0 || port > 65535 then
        F.raiseError(new SQLClientInfoException(s"""Port: "$port" is not valid."""))
      else F.unit

    val sockets: Resource[F, Socket[F]] =
      Resource
        .eval(validateEndpoint)
        .flatMap(_ =>
          Resource.make(engine.connect(host, port, defaultConnectTimeout, socketOptions))(_.close())
        )

    fromSockets[F, A](
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

  def fromSockets[F[_], A](
    sockets:                     Resource[F, Socket[F]],
    host:                        String,
    port:                        Int,
    user:                        String,
    password:                    Option[String] = None,
    database:                    Option[String] = None,
    debug:                       Boolean = false,
    sslOptions:                  Option[SSLNegotiation.Options[F]],
    readTimeout:                 Duration = Duration.Inf,
    allowPublicKeyRetrieval:     Boolean = false,
    useCursorFetch:              Boolean = false,
    useServerPrepStmts:          Boolean = false,
    maxAllowedPacket:            Int = MySQLConfig.DEFAULT_PACKET_SIZE,
    databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = None,
    defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]],
    plugins:                     List[AuthenticationPlugin[F]],
    telemetryConfig:             TelemetryConfig = TelemetryConfig.default,
    databaseMetrics:             Option[DatabaseMetrics[F]] = None,
    acquire:                     Connection[F] => F[A],
    release:                     (A, Connection[F]) => F[Unit]
  )(using F: Concurrent[F], tls: TlsUpgrade[F]): Tracer[F] ?=> Resource[F, LdbcConnection[F]] =
    val resolvedMetrics = databaseMetrics.getOrElse(DatabaseMetrics.noop[F])
    val pluginMap       = plugins.map(plugin => plugin.name.toString -> plugin).toMap
    val capabilityFlags = defaultCapabilityFlags ++
      (if database.isDefined then Set(CapabilitiesFlags.CLIENT_CONNECT_WITH_DB) else Set.empty) ++
      (if sslOptions.isDefined then Set(CapabilitiesFlags.CLIENT_SSL) else Set.empty)
    val hostInfo = HostInfo(host, port, user, password, database)
    for
      given Exchange[F] <- Resource.eval(Exchange.apply[F])
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
      readOnly         <- Resource.eval(Ref.of[F, Boolean](false))
      autoCommit       <- Resource.eval(Ref.of[F, Boolean](true))
      connectionClosed <- Resource.eval(Ref.of[F, Boolean](false))
      connection       <-
        Resource.make(
          F.pure(
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
