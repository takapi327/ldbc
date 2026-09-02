/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scala.concurrent.duration.Duration

import ldbc.sql.{ Connection as SqlConnection, DataSource, DatabaseMetaData }

import ldbc.authentication.plugin.AuthenticationPlugin
import ldbc.build.Version
import ldbc.effect.{ Concurrent, Resource }
import ldbc.effect.syntax.*
import ldbc.net.{ IoEngine, TlsUpgrade }
import ldbc.net.{ SSL, SocketOptions }
import ldbc.telemetry.*
import ldbc.telemetry.{ DatabaseMetrics, TelemetryConfig }

/**
 * A [[ldbc.sql.DataSource]] implementation for MySQL connections using the pure Scala MySQL wire protocol.
 *
 * This DataSource manages MySQL connections with support for SSL, authentication, prepared statements,
 * and various connection options. It also supports lifecycle hooks that can be executed before and
 * after connection acquisition. Effects are expressed with the effect-agnostic `ldbc.effect` type classes.
 *
 * @tparam A the type of value returned by the before hook
 *
 * @param host the hostname or IP address of the MySQL server
 * @param port the port number on which the MySQL server is listening
 * @param user the username for authenticating with the MySQL server
 * @param password the password for authenticating with the MySQL server
 * @param database the default database to use upon connection
 * @param debug whether to enable debug logging for connections
 * @param ssl the SSL configuration for secure connections
 * @param socketOptions socket-level options for the TCP connection
 * @param readTimeout the timeout duration for read operations
 * @param allowPublicKeyRetrieval whether to allow retrieval of RSA public keys from the server
 * @param databaseTerm the database terminology to use (CATALOG or SCHEMA)
 * @param tracer optional tracer for distributed tracing
 * @param telemetryConfig configuration for telemetry behavior
 * @param useCursorFetch whether to use cursor-based fetching for result sets
 * @param useServerPrepStmts whether to use server-side prepared statements
 * @param maxAllowedPacket Maximum allowed packet size for network communication in bytes.
 * @param defaultAuthenticationPlugin The authentication plugin used first for communication with the server
 * @param plugins Additional authentication plugins used for communication with the server
 * @param meter optional meter for database metrics
 * @param before optional hook to execute before a connection is acquired
 * @param after optional hook to execute after a connection is used
 *
 * @example {{{
 * val dataSource = MySQLDataSource(
 *   host = "localhost",
 *   port = 3306,
 *   user = "myuser",
 *   password = Some("mypassword"),
 *   database = Some("mydatabase"),
 *   ssl = SSL.Trusted
 * )
 * }}}
 */
final case class MySQLDataSource[F[_], A](
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
  databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
  tracer:                      Option[Tracer[F]]                     = None,
  telemetryConfig:             TelemetryConfig                       = TelemetryConfig.default,
  useCursorFetch:              Boolean                               = false,
  useServerPrepStmts:          Boolean                               = false,
  maxAllowedPacket:            Int                                   = MySQLConfig.DEFAULT_PACKET_SIZE,
  defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]]       = None,
  plugins:                     List[AuthenticationPlugin[F]]         = List.empty[AuthenticationPlugin[F]],
  meter:                       Option[Meter]                         = None,
  before:                      Option[Connection[F] => F[A]]         = None,
  after:                       Option[(A, Connection[F]) => F[Unit]] = None
)(using F: Concurrent[F], engine: IoEngine[F], tls: TlsUpgrade[F])
  extends DataSource[F]:

  /**
   * Returns a string representation of this DataSource without exposing sensitive information.
   *
   * The password is intentionally redacted (rendered as `***` when set) so that it is never
   * leaked through logs, exception messages, or crash reports. Non-sensitive connection settings
   * useful for debugging are retained.
   *
   * @return a secure string representation of the DataSource
   */
  override def toString: String =
    s"MySQLDataSource(host=$host, port=$port, user=$user, password=${ password.fold("None")(_ => "***") }, " +
      s"database=$database, debug=$debug, ssl=$ssl, allowPublicKeyRetrieval=$allowPublicKeyRetrieval, " +
      s"useCursorFetch=$useCursorFetch, useServerPrepStmts=$useServerPrepStmts, maxAllowedPacket=$maxAllowedPacket)"

  /** The tracer used for distributed tracing. Falls back to a no-op tracer if none is provided. */
  private given Tracer[F] = tracer.getOrElse(Tracer.noop[F])

  /**
   * Creates a new connection resource from this DataSource.
   *
   * The connection is managed as a resource, ensuring proper cleanup when the resource
   * is released. If before/after hooks are configured, they will be executed during
   * the connection lifecycle.
   *
   * @return a Resource that manages a MySQL connection
   */
  override def getConnection: F[(SqlConnection[F], F[Unit])] = connectionResource.allocatedCase

  private def connectionResource: Resource[F, SqlConnection[F]] =
    DatabaseMetrics.fromMeter[F](meter.getOrElse(Meter.noop)).flatMap { databaseMetrics =>
      val resource = (before, after) match
        case (Some(b), Some(a)) =>
          Connection.withBeforeAfter[F, A](
            host                        = host,
            port                        = port,
            user                        = user,
            before                      = b,
            after                       = a,
            password                    = password,
            database                    = database,
            debug                       = debug,
            ssl                         = ssl,
            socketOptions               = socketOptions,
            readTimeout                 = readTimeout,
            allowPublicKeyRetrieval     = allowPublicKeyRetrieval,
            useCursorFetch              = useCursorFetch,
            useServerPrepStmts          = useServerPrepStmts,
            maxAllowedPacket            = maxAllowedPacket,
            databaseTerm                = databaseTerm,
            defaultAuthenticationPlugin = defaultAuthenticationPlugin,
            plugins                     = plugins,
            telemetryConfig             = telemetryConfig,
            databaseMetrics             = Some(databaseMetrics)
          )
        case (Some(b), None) =>
          Connection.withBeforeAfter[F, A](
            host                        = host,
            port                        = port,
            user                        = user,
            before                      = b,
            after                       = (_, _) => F.unit,
            password                    = password,
            database                    = database,
            debug                       = debug,
            ssl                         = ssl,
            socketOptions               = socketOptions,
            readTimeout                 = readTimeout,
            allowPublicKeyRetrieval     = allowPublicKeyRetrieval,
            useCursorFetch              = useCursorFetch,
            useServerPrepStmts          = useServerPrepStmts,
            maxAllowedPacket            = maxAllowedPacket,
            databaseTerm                = databaseTerm,
            defaultAuthenticationPlugin = defaultAuthenticationPlugin,
            plugins                     = plugins,
            telemetryConfig             = telemetryConfig,
            databaseMetrics             = Some(databaseMetrics)
          )
        case (None, _) =>
          Connection[F](
            host                        = host,
            port                        = port,
            user                        = user,
            password                    = password,
            database                    = database,
            debug                       = debug,
            ssl                         = ssl,
            socketOptions               = socketOptions,
            readTimeout                 = readTimeout,
            allowPublicKeyRetrieval     = allowPublicKeyRetrieval,
            useCursorFetch              = useCursorFetch,
            useServerPrepStmts          = useServerPrepStmts,
            maxAllowedPacket            = maxAllowedPacket,
            databaseTerm                = databaseTerm,
            defaultAuthenticationPlugin = defaultAuthenticationPlugin,
            plugins                     = plugins,
            telemetryConfig             = telemetryConfig,
            databaseMetrics             = Some(databaseMetrics)
          )
      resource.map(conn => conn: SqlConnection[F])
    }

  /** Sets the hostname or IP address of the MySQL server.
    * @param newHost the hostname or IP address
    * @return a new MySQLDataSource with the updated host
    */
  def setHost(newHost: String): MySQLDataSource[F, A] = copy(host = newHost)

  /** Sets the port number for the MySQL connection.
    * @param newPort the port number (typically 3306)
    * @return a new MySQLDataSource with the updated port
    */
  def setPort(newPort: Int): MySQLDataSource[F, A] = copy(port = newPort)

  /** Sets the username for MySQL authentication.
    * @param newUser the username
    * @return a new MySQLDataSource with the updated user
    */
  def setUser(newUser: String): MySQLDataSource[F, A] = copy(user = newUser)

  /** Sets the password for MySQL authentication.
    * @param newPassword the password
    * @return a new MySQLDataSource with the updated password
    */
  def setPassword(newPassword: String): MySQLDataSource[F, A] = copy(password = Some(newPassword))

  /** Sets the default database to use upon connection.
    * @param newDatabase the database name
    * @return a new MySQLDataSource with the updated database
    */
  def setDatabase(newDatabase: String): MySQLDataSource[F, A] = copy(database = Some(newDatabase))

  /** Enables or disables debug logging for connections.
    * @param newDebug true to enable debug logging, false to disable
    * @return a new MySQLDataSource with the updated debug setting
    */
  def setDebug(newDebug: Boolean): MySQLDataSource[F, A] = copy(debug = newDebug)

  /** Sets the SSL configuration for secure connections.
    * @param newSSL the SSL configuration (None, Trusted, or System)
    * @return a new MySQLDataSource with the updated SSL setting
    */
  def setSSL(newSSL: SSL): MySQLDataSource[F, A] = copy(ssl = newSSL)

  /** Sets socket-level options for the TCP connection.
    * @param newSocketOptions the socket options to apply
    * @return a new MySQLDataSource with the updated socket options
    */
  def setSocketOptions(newSocketOptions: SocketOptions): MySQLDataSource[F, A] =
    copy(socketOptions = newSocketOptions)

  /** Sets the timeout duration for read operations.
    * @param newReadTimeout the read timeout duration, or Duration.Inf for no timeout
    * @return a new MySQLDataSource with the updated read timeout
    */
  def setReadTimeout(newReadTimeout: Duration): MySQLDataSource[F, A] =
    copy(readTimeout = newReadTimeout)

  /** Sets whether to allow retrieval of RSA public keys from the server.
    * This is required for certain authentication plugins when SSL is not used.
    * @param newAllowPublicKeyRetrieval true to allow public key retrieval
    * @return a new MySQLDataSource with the updated setting
    */
  def setAllowPublicKeyRetrieval(newAllowPublicKeyRetrieval: Boolean): MySQLDataSource[F, A] =
    copy(allowPublicKeyRetrieval = newAllowPublicKeyRetrieval)

  /** Sets the database terminology to use.
    * MySQL traditionally uses CATALOG, but this can be configured.
    * @param newDatabaseTerm the database term (CATALOG or SCHEMA)
    * @return a new MySQLDataSource with the updated database term
    */
  def setDatabaseTerm(newDatabaseTerm: DatabaseMetaData.DatabaseTerm): MySQLDataSource[F, A] =
    copy(databaseTerm = Some(newDatabaseTerm))

  /** Sets the tracer for distributed tracing.
    * @param newTracer the tracer instance
    * @return a new MySQLDataSource with the updated tracer
    */
  def setTracer(newTracer: Tracer[F]): MySQLDataSource[F, A] =
    copy(tracer = Some(newTracer))

  /** Sets the meter for database metrics.
    * @param newMeter the meter instance
    * @return a new MySQLDataSource with the updated meter
    */
  def setMeter(newMeter: Meter): MySQLDataSource[F, A] =
    copy(meter = Some(newMeter))

  /** Sets the telemetry configuration for telemetry behavior.
    *
    * Use this to control how telemetry data is collected and processed,
    * particularly regarding extraction of metadata from SQL query text.
    *
    * @param newTelemetryConfig the telemetry configuration
    * @return a new MySQLDataSource with the updated telemetry config
    */
  def setTelemetryConfig(newTelemetryConfig: TelemetryConfig): MySQLDataSource[F, A] =
    copy(telemetryConfig = newTelemetryConfig)

  /** Sets whether to use cursor-based fetching for result sets.
    * This can improve memory usage for large result sets.
    * @param newUseCursorFetch true to enable cursor-based fetching
    * @return a new MySQLDataSource with the updated setting
    */
  def setUseCursorFetch(newUseCursorFetch: Boolean): MySQLDataSource[F, A] =
    copy(useCursorFetch = newUseCursorFetch)

  /** Sets whether to use server-side prepared statements.
    * Server-side prepared statements can improve performance for repeated queries.
    * @param newUseServerPrepStmts true to enable server-side prepared statements
    * @return a new MySQLDataSource with the updated setting
    */
  def setUseServerPrepStmts(newUseServerPrepStmts: Boolean): MySQLDataSource[F, A] =
    copy(useServerPrepStmts = newUseServerPrepStmts)

  /** Sets the maximum allowed packet size for network communication.
   *
   * @param maxAllowedPacket the maximum packet size in bytes (1,024 to 16,777,215)
   * @return a new MySQLDataSource with the updated packet size limit
   * @throws IllegalArgumentException if the value is outside the valid range
   */
  def setMaxAllowedPacket(maxAllowedPacket: Int): MySQLDataSource[F, A] = {
    require(
      maxAllowedPacket >= MySQLConfig.MIN_PACKET_SIZE,
      s"maxAllowedPacket must be at least ${ MySQLConfig.MIN_PACKET_SIZE } bytes, but got $maxAllowedPacket"
    )
    require(
      maxAllowedPacket <= MySQLConfig.MAX_PACKET_SIZE,
      s"maxAllowedPacket must not exceed ${ MySQLConfig.MAX_PACKET_SIZE } bytes (MySQL protocol limit), but got $maxAllowedPacket"
    )
    copy(maxAllowedPacket = maxAllowedPacket)
  }

  /** Sets whether to authentication plugin to be used first for communication with the server.
   * @param defaultAuthenticationPlugin
   *   The authentication plugin used first for communication with the server
   * @return a new MySQLDataSource with the updated setting
   */
  def setDefaultAuthenticationPlugin(defaultAuthenticationPlugin: AuthenticationPlugin[F]): MySQLDataSource[F, A] =
    copy(defaultAuthenticationPlugin = Some(defaultAuthenticationPlugin))

  /**
   * Sets whether to authentication plugin to be used for communication with the server.
   *
   * @param p1
   *   The authentication plugin used for communication with the server
   * @param pn
   *   List of authentication plugins used for communication with the server
   * @return a new MySQLDataSource with the updated setting
   */
  def setPlugins(p1: AuthenticationPlugin[F], pn: AuthenticationPlugin[F]*): MySQLDataSource[F, A] =
    copy(plugins = p1 :: pn.toList)

  /**
   * Adds a before hook that will be executed when a connection is acquired.
   *
   * The before hook receives the connection and can perform initialization tasks
   * or return a value that will be passed to the after hook.
   *
   * @tparam B the type of value returned by the before hook
   * @param before the function to execute before using a connection
   * @return a new MySQLDataSource with the before hook configured
   */
  def withBefore[B](before: Connection[F] => F[B]): MySQLDataSource[F, B] =
    MySQLDataSource(
      host                    = host,
      port                    = port,
      user                    = user,
      password                = password,
      database                = database,
      debug                   = debug,
      ssl                     = ssl,
      socketOptions           = socketOptions,
      readTimeout             = readTimeout,
      allowPublicKeyRetrieval = allowPublicKeyRetrieval,
      databaseTerm            = databaseTerm,
      tracer                  = tracer,
      telemetryConfig         = telemetryConfig,
      useCursorFetch          = useCursorFetch,
      useServerPrepStmts      = useServerPrepStmts,
      meter                   = meter,
      before                  = Some(before),
      after                   = None
    )

  /**
   * Adds an after hook that will be executed when a connection is released.
   *
   * The after hook receives the value returned by the before hook (if any) and
   * the connection, allowing cleanup or finalization tasks.
   *
   * @param after the function to execute after using a connection
   * @return a new MySQLDataSource with the after hook configured
   */
  def withAfter(after: (A, Connection[F]) => F[Unit]): MySQLDataSource[F, A] =
    copy(after = Some(after))

  /**
   * Adds both before and after hooks for connection lifecycle management.
   *
   * This is a convenience method that combines withBefore and withAfter, allowing
   * you to set up both hooks in a single call. The before hook is executed when
   * a connection is acquired, and the after hook is executed when it's released.
   *
   * @tparam B the type of value returned by the before hook
   * @param before the function to execute before using a connection
   * @param after the function to execute after using a connection
   * @return a new MySQLDataSource with both hooks configured
   */
  def withBeforeAfter[B](
    before: Connection[F] => F[B],
    after:  (B, Connection[F]) => F[Unit]
  ): MySQLDataSource[F, B] =
    MySQLDataSource(
      host                    = host,
      port                    = port,
      user                    = user,
      password                = password,
      database                = database,
      debug                   = debug,
      ssl                     = ssl,
      socketOptions           = socketOptions,
      readTimeout             = readTimeout,
      allowPublicKeyRetrieval = allowPublicKeyRetrieval,
      databaseTerm            = databaseTerm,
      tracer                  = tracer,
      telemetryConfig         = telemetryConfig,
      useCursorFetch          = useCursorFetch,
      useServerPrepStmts      = useServerPrepStmts,
      meter                   = meter,
      before                  = Some(before),
      after                   = Some(after)
    )

/**
 * Companion object for MySQLDataSource providing factory methods.
 */
object MySQLDataSource:

  /**
   * Creates a MySQLDataSource from a MySQLConfig instance.
   *
   * This factory method simplifies DataSource creation by using a pre-configured
   * MySQLConfig object containing all connection parameters.
   *
   * @param config the MySQLConfig containing connection parameters
   * @return a new MySQLDataSource configured according to the provided config
   */
  def fromConfig[F[_]](config: MySQLConfig)(using
    Concurrent[F],
    IoEngine[F],
    TlsUpgrade[F]
  ): MySQLDataSource[F, Unit] =
    MySQLDataSource(
      host                    = config.host,
      port                    = config.port,
      user                    = config.user,
      password                = config.password,
      database                = config.database,
      debug                   = config.debug,
      ssl                     = config.ssl,
      socketOptions           = config.socketOptions,
      readTimeout             = config.readTimeout,
      allowPublicKeyRetrieval = config.allowPublicKeyRetrieval,
      databaseTerm            = config.databaseTerm,
      useCursorFetch          = config.useCursorFetch,
      useServerPrepStmts      = config.useServerPrepStmts,
      maxAllowedPacket        = config.maxAllowedPacket
    )

  /**
   * Creates a MySQLDataSource with default configuration.
   *
   * Uses the default MySQLConfig which connects to:
   * - host: "127.0.0.1"
   * - port: 3306
   * - user: "root"
   * - no password
   *
   * @return a new MySQLDataSource with default settings
   */
  def default[F[_]](using Concurrent[F], IoEngine[F], TlsUpgrade[F]): MySQLDataSource[F, Unit] =
    fromConfig[F](MySQLConfig.default)

  /**
   * Creates a MySQLDataSource with minimal required parameters.
   *
   * This is a convenience factory method for creating a DataSource with just
   * the essential connection parameters. Other settings will use their defaults.
   *
   * @param host the hostname or IP address of the MySQL server
   * @param port the port number on which the MySQL server is listening
   * @param user the username for authenticating with the MySQL server
   * @return a new MySQLDataSource with the specified parameters
   */
  def build[F[_]](
    host: String,
    port: Int,
    user: String
  )(using Concurrent[F], IoEngine[F], TlsUpgrade[F]): MySQLDataSource[F, Unit] =
    MySQLDataSource(
      host = host,
      port = port,
      user = user
    )

  /**
   * Creates a MySQLDataSource with tracing and metrics enabled.
   *
   * This factory method acquires a [[ldbc.telemetry.Tracer]] and a
   * [[ldbc.telemetry.Meter]] from the given [[ldbc.telemetry.TracerProvider]]
   * and [[ldbc.telemetry.MeterProvider]], then constructs a [[MySQLDataSource]]
   * configured to emit traces and metrics for every database operation.
   *
   * @param config         the [[MySQLConfig]] holding host, port, credentials, and other connection settings
   * @param tracerProvider the provider used to acquire the tracer (defaults to a no-op provider)
   * @param meterProvider  the provider used to acquire the meter (defaults to a no-op provider)
   * @return an effect that resolves to a [[MySQLDataSource]] with tracing and metrics enabled
   */
  def withTraced[F[_]](using
    Concurrent[F],
    IoEngine[F],
    TlsUpgrade[F]
  )(
    config:         MySQLConfig,
    tracerProvider: TracerProvider[F] = TracerProvider.noop[F],
    meterProvider:  MeterProvider[F] = MeterProvider.noop[F]
  ): F[MySQLDataSource[F, Unit]] =
    for
      tracer <- tracerProvider.tracer("ldbc").withVersion(Version.current).get
      meter  <- meterProvider
                 .meter("ldbc")
                 .withVersion(Version.current)
                 .withSchemaUrl(TelemetryAttribute.SCHEMA_URL_VALUE)
                 .get
    yield MySQLDataSource(
      host                    = config.host,
      port                    = config.port,
      user                    = config.user,
      password                = config.password,
      database                = config.database,
      debug                   = config.debug,
      ssl                     = config.ssl,
      socketOptions           = config.socketOptions,
      readTimeout             = config.readTimeout,
      allowPublicKeyRetrieval = config.allowPublicKeyRetrieval,
      databaseTerm            = config.databaseTerm,
      tracer                  = Some(tracer),
      useCursorFetch          = config.useCursorFetch,
      useServerPrepStmts      = config.useServerPrepStmts,
      maxAllowedPacket        = config.maxAllowedPacket,
      meter                   = Some(meter)
    )
