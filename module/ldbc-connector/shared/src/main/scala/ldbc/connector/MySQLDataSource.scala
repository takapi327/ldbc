/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.effect.std.Console
import cats.effect.std.UUIDGen

import fs2.hashing.Hashing
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

import ldbc.connector.pool.*

import ldbc.authentication.plugin.AuthenticationPlugin
import ldbc.DataSource

/**
 * A DataSource implementation for MySQL connections using the pure Scala MySQL wire protocol.
 * 
 * This DataSource provides connection pooling capabilities and manages MySQL connections
 * with support for SSL, authentication, prepared statements, and various connection options.
 * It also supports lifecycle hooks that can be executed before and after connection acquisition.
 * 
 * @tparam F the effect type (e.g., IO, Task) that must have Async, Network, Console, Hashing, and UUIDGen capabilities
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
 * @param tracer optional OpenTelemetry tracer for distributed tracing
 * @param useCursorFetch whether to use cursor-based fetching for result sets
 * @param useServerPrepStmts whether to use server-side prepared statements
 * @param defaultAuthenticationPlugin The authentication plugin used first for communication with the server
 * @param plugins Additional authentication plugins used for communication with the server
 * @param before optional hook to execute before a connection is acquired
 * @param after optional hook to execute after a connection is used
 * 
 * @example {{{
 * val dataSource = MySQLDataSource[IO, Unit](
 *   host = "localhost",
 *   port = 3306,
 *   user = "myuser",
 *   password = Some("mypassword"),
 *   database = Some("mydatabase"),
 *   ssl = SSL.Trusted
 * )
 * 
 * // With lifecycle hooks
 * val dataSourceWithHooks = dataSource
 *   .withBefore(conn => IO.println("Connection acquired"))
 *   .withAfter((_, conn) => IO.println("Connection released"))
 * }}}
 */
final case class MySQLDataSource[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
  host:                        String,
  port:                        Int,
  user:                        String,
  password:                    Option[String]                        = None,
  database:                    Option[String]                        = None,
  debug:                       Boolean                               = false,
  ssl:                         SSL                                   = SSL.None,
  socketOptions:               List[SocketOption]                    = MySQLConfig.defaultSocketOptions,
  readTimeout:                 Duration                              = Duration.Inf,
  allowPublicKeyRetrieval:     Boolean                               = false,
  databaseTerm:                Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
  tracer:                      Option[Tracer[F]]                     = None,
  useCursorFetch:              Boolean                               = false,
  useServerPrepStmts:          Boolean                               = false,
  defaultAuthenticationPlugin: Option[AuthenticationPlugin[F]]       = None,
  plugins:                     List[AuthenticationPlugin[F]]         = List.empty[AuthenticationPlugin[F]],
  before:                      Option[Connection[F] => F[A]]         = None,
  after:                       Option[(A, Connection[F]) => F[Unit]] = None
) extends DataSource[F]:
  given Tracer[F] = tracer.getOrElse(Tracer.noop[F])

  /**
   * Creates a new connection resource from this DataSource.
   * 
   * The connection is managed as a resource, ensuring proper cleanup when the resource
   * is released. If before/after hooks are configured, they will be executed during
   * the connection lifecycle.
   * 
   * @return a Resource that manages a MySQL connection
   */
  override def getConnection: Resource[F, Connection[F]] =
    (before, after) match
      case (Some(b), Some(a)) =>
        Connection.withBeforeAfter(
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
          databaseTerm                = databaseTerm,
          defaultAuthenticationPlugin = defaultAuthenticationPlugin,
          plugins                     = plugins
        )
      case (Some(b), None) =>
        Connection.withBeforeAfter(
          host                        = host,
          port                        = port,
          user                        = user,
          before                      = b,
          after                       = (_, _) => Async[F].unit,
          password                    = password,
          database                    = database,
          debug                       = debug,
          ssl                         = ssl,
          socketOptions               = socketOptions,
          readTimeout                 = readTimeout,
          allowPublicKeyRetrieval     = allowPublicKeyRetrieval,
          useCursorFetch              = useCursorFetch,
          useServerPrepStmts          = useServerPrepStmts,
          databaseTerm                = databaseTerm,
          defaultAuthenticationPlugin = defaultAuthenticationPlugin,
          plugins                     = plugins
        )
      case (None, _) =>
        Connection(
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
          databaseTerm                = databaseTerm,
          defaultAuthenticationPlugin = defaultAuthenticationPlugin,
          plugins                     = plugins
        )

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
    * @param newSocketOptions list of socket options to apply
    * @return a new MySQLDataSource with the updated socket options
    */
  def setSocketOptions(newSocketOptions: List[SocketOption]): MySQLDataSource[F, A] =
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

  /** Sets the OpenTelemetry tracer for distributed tracing.
    * @param newTracer the tracer instance
    * @return a new MySQLDataSource with the updated tracer
    */
  def setTracer(newTracer: Tracer[F]): MySQLDataSource[F, A] =
    copy(tracer = Some(newTracer))

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
      useCursorFetch          = useCursorFetch,
      useServerPrepStmts      = useServerPrepStmts,
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
   * 
   * @example {{{  
   * val dataSourceWithHooks = dataSource.withBeforeAfter(
   *   before = conn => IO.println("Starting transaction").as(System.currentTimeMillis),
   *   after = (startTime, conn) => IO.println(s"Completed in ${System.currentTimeMillis - startTime}ms")
   * )
   * }}}
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
      useCursorFetch          = useCursorFetch,
      useServerPrepStmts      = useServerPrepStmts,
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
   * @tparam F the effect type with required type class instances
   * @param config the MySQLConfig containing connection parameters
   * @return a new MySQLDataSource configured according to the provided config
   */
  def fromConfig[F[_]: Async: Network: Console: Hashing: UUIDGen](config: MySQLConfig): MySQLDataSource[F, Unit] =
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
      useServerPrepStmts      = config.useServerPrepStmts
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
   * @tparam F the effect type with required type class instances
   * @return a new MySQLDataSource with default settings
   */
  def default[F[_]: Async: Network: Console: Hashing: UUIDGen]: MySQLDataSource[F, Unit] =
    fromConfig(MySQLConfig.default)

  /**
   * Creates a MySQLDataSource with minimal required parameters.
   * 
   * This is a convenience factory method for creating a DataSource with just
   * the essential connection parameters. Other settings will use their defaults.
   * 
   * @tparam F the effect type with required type class instances
   * @param host the hostname or IP address of the MySQL server
   * @param port the port number on which the MySQL server is listening
   * @param user the username for authenticating with the MySQL server
   * @return a new MySQLDataSource with the specified parameters
   * 
   * @example {{{  
   * val dataSource = MySQLDataSource.build[IO](
   *   host = "localhost",
   *   port = 3306,
   *   user = "myuser"
   * )
   * }}}
   */
  def build[F[_]: Async: Network: Console: Hashing: UUIDGen](
    host: String,
    port: Int,
    user: String
  ): MySQLDataSource[F, Unit] =
    MySQLDataSource(
      host = host,
      port = port,
      user = user
    )

  /**
   * Creates a pooled DataSource from a MySQL configuration.
   * 
   * This factory method creates a connection pool that manages multiple MySQL connections,
   * providing efficient connection reuse and automatic connection lifecycle management.
   * The pool implements various optimization strategies including:
   * 
   * - Connection pooling with configurable min/max sizes
   * - Automatic connection validation and health checks
   * - Connection timeout and idle timeout management
   * - Leak detection for connections not properly returned
   * - Background maintenance tasks for pool health
   * - Optional adaptive sizing based on load patterns
   * - Comprehensive metrics tracking
   * 
   * The returned Resource ensures proper pool initialization and shutdown. When the
   * resource is released, all connections are closed and background tasks are cancelled.
   * 
   * @param config the MySQL configuration containing pool settings
   * @param metricsTracker optional tracker for pool metrics (defaults to in-memory)
   * @param tracer optional OpenTelemetry tracer for distributed tracing (defaults to no-op tracer)
   * @tparam F the effect type with required type class instances
   * @return a Resource managing the pooled data source lifecycle
   * 
   * @example {{{
   * val poolConfig = MySQLConfig.default
   *   .setHost("localhost")
   *   .setPort(3306)
   *   .setUser("myuser")
   *   .setPassword("mypassword")
   *   .setDatabase("mydb")
   *   .setMinConnections(5)
   *   .setMaxConnections(20)
   *   .setConnectionTimeout(30.seconds)
   * 
   * MySQLDataSource.pooling[IO](poolConfig).use { pool =>
   *   pool.getConnection.use { conn =>
   *     // Use connection
   *   }
   * }
   * }}}
   */
  def pooling[F[_]: Async: Network: Console: Hashing: UUIDGen](
    config:         MySQLConfig,
    metricsTracker: Option[PoolMetricsTracker[F]] = None,
    tracer:         Option[Tracer[F]] = None
  ): Resource[F, PooledDataSource[F]] = PooledDataSource.fromConfig(config, metricsTracker, tracer)

  /**
   * Creates a pooled DataSource with connection lifecycle hooks.
   * 
   * This variant of the pooling factory method allows you to specify callbacks that
   * will be executed before and after each connection is acquired from the pool.
   * This is particularly useful for:
   * 
   * - Setting session-specific variables or configuration
   * - Implementing connection-level auditing or logging
   * - Preparing the connection state before use
   * - Cleaning up resources after connection use
   * - Implementing custom connection validation logic
   * 
   * The before hook is called after a connection is acquired from the pool but before
   * it's returned to the client. The after hook is called when the connection is
   * returned to the pool. These hooks are executed for every connection acquisition,
   * not just when new connections are created.
   * 
   * @param config the MySQL configuration containing pool settings
   * @param metricsTracker optional tracker for pool metrics (defaults to in-memory)
   * @param tracer optional OpenTelemetry tracer for distributed tracing (defaults to no-op tracer)
   * @param before optional callback executed when acquiring a connection from the pool
   * @param after optional callback executed when returning a connection to the pool
   * @tparam F the effect type with required type class instances
   * @tparam A the type returned by the before callback and passed to after
   * @return a Resource managing the pooled data source lifecycle
   * @example {{{
   * case class SessionContext(userId: String, startTime: Long)
   *
   * val beforeHook: Connection[IO] => IO[SessionContext] = conn =>
   *   for {
   *     _ <- conn.createStatement().flatMap(_.executeUpdate("SET SESSION sql_mode = 'STRICT_ALL_TABLES'"))
   *     startTime = System.currentTimeMillis
   *   } yield SessionContext("user123", startTime)
   *
   * val afterHook: (SessionContext, Connection[IO]) => IO[Unit] = (ctx, conn) =>
   *   IO.println(s"Connection used by ${ctx.userId} for ${System.currentTimeMillis - ctx.startTime}ms")
   *
   * MySQLDataSource.poolingWithBeforeAfter[IO, SessionContext](
   *   config = poolConfig,
   *   before = Some(beforeHook),
   *   after = Some(afterHook)
   * ).use { pool =>
   *   pool.getConnection.use { conn =>
   *     // Connection has session variables set and usage will be logged
   *   }
   * }
   * }}}
   */
  def poolingWithBeforeAfter[F[_]: Async: Network: Console: Hashing: UUIDGen, A](
    config:         MySQLConfig,
    metricsTracker: Option[PoolMetricsTracker[F]] = None,
    tracer:         Option[Tracer[F]] = None,
    before:         Option[Connection[F] => F[A]] = None,
    after:          Option[(A, Connection[F]) => F[Unit]] = None
  ): Resource[F, PooledDataSource[F]] =
    PooledDataSource.fromConfigWithBeforeAfter(config, metricsTracker, tracer, before, after)
