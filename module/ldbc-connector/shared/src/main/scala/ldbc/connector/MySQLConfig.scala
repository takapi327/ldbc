/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.*

import fs2.io.net.*

import ldbc.sql.DatabaseMetaData

/**
 * Configuration for MySQL database connections.
 * 
 * This trait provides a fluent API for configuring MySQL connection parameters including
 * host, port, authentication, SSL settings, and various connection options.
 * 
 * @example {{{  
 * val config = MySQLConfig.default
 *   .setHost("localhost")
 *   .setPort(3306)
 *   .setUser("myuser")
 *   .setPassword("mypassword")
 *   .setDatabase("mydatabase")
 *   .setSSL(SSL.Trusted)
 * }}}
 */
trait MySQLConfig:

  /** The hostname or IP address of the MySQL server. */
  def host: String

  /** Sets the hostname or IP address of the MySQL server.
    * @param host the hostname or IP address
    * @return a new MySQLConfig with the updated host
    */
  def setHost(host: String): MySQLConfig

  /** The port number on which the MySQL server is listening. */
  def port: Int

  /** Sets the port number for the MySQL connection.
    * @param port the port number (typically 3306)
    * @return a new MySQLConfig with the updated port
    */
  def setPort(port: Int): MySQLConfig

  /** The username for authenticating with the MySQL server. */
  def user: String

  /** Sets the username for MySQL authentication.
    * @param user the username
    * @return a new MySQLConfig with the updated user
    */
  def setUser(user: String): MySQLConfig

  /** The password for authenticating with the MySQL server, if required. */
  def password: Option[String]

  /** Sets the password for MySQL authentication.
    * @param password the password
    * @return a new MySQLConfig with the updated password
    */
  def setPassword(password: String): MySQLConfig

  /** The default database to use upon connection. */
  def database: Option[String]

  /** Sets the default database to use upon connection.
    * @param database the database name
    * @return a new MySQLConfig with the updated database
    */
  def setDatabase(database: String): MySQLConfig

  /** Whether to enable debug logging for the connection. */
  def debug: Boolean

  /** Enables or disables debug logging for the connection.
    * @param debug true to enable debug logging, false to disable
    * @return a new MySQLConfig with the updated debug setting
    */
  def setDebug(debug: Boolean): MySQLConfig

  /** The SSL configuration for secure connections. */
  def ssl: SSL

  /** Sets the SSL configuration for secure connections.
    * @param ssl the SSL configuration (None, Trusted, or System)
    * @return a new MySQLConfig with the updated SSL setting
    */
  def setSSL(ssl: SSL): MySQLConfig

  /** Socket-level options for the TCP connection. */
  def socketOptions: List[SocketOption]

  /** Sets socket-level options for the TCP connection.
    * @param socketOptions list of socket options to apply
    * @return a new MySQLConfig with the updated socket options
    */
  def setSocketOptions(socketOptions: List[SocketOption]): MySQLConfig

  /** The timeout duration for read operations. */
  def readTimeout: Duration

  /** Sets the timeout duration for read operations.
    * @param readTimeout the read timeout duration, or Duration.Inf for no timeout
    * @return a new MySQLConfig with the updated read timeout
    */
  def setReadTimeout(readTimeout: Duration): MySQLConfig

  /** Whether to allow retrieval of RSA public keys from the server. */
  def allowPublicKeyRetrieval: Boolean

  /** Sets whether to allow retrieval of RSA public keys from the server.
    * This is required for certain authentication plugins when SSL is not used.
    * @param allowPublicKeyRetrieval true to allow public key retrieval
    * @return a new MySQLConfig with the updated setting
    */
  def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLConfig

  /** The database terminology to use (CATALOG or SCHEMA). */
  def databaseTerm: Option[DatabaseMetaData.DatabaseTerm]

  /** Sets the database terminology to use.
    * MySQL traditionally uses CATALOG, but this can be configured.
    * @param databaseTerm the database term (CATALOG or SCHEMA)
    * @return a new MySQLConfig with the updated database term
    */
  def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): MySQLConfig

  /** Whether to use cursor-based fetching for result sets. */
  def useCursorFetch: Boolean

  /** Sets whether to use cursor-based fetching for result sets.
    * This can improve memory usage for large result sets.
    * @param useCursorFetch true to enable cursor-based fetching
    * @return a new MySQLConfig with the updated setting
    */
  def setUseCursorFetch(useCursorFetch: Boolean): MySQLConfig

  /** Whether to use server-side prepared statements. */
  def useServerPrepStmts: Boolean

  /** Sets whether to use server-side prepared statements.
    * Server-side prepared statements can improve performance for repeated queries.
    * @param useServerPrepStmts true to enable server-side prepared statements
    * @return a new MySQLConfig with the updated setting
    */
  def setUseServerPrepStmts(useServerPrepStmts: Boolean): MySQLConfig

  /** 
   * Gets the minimum number of connections to maintain in the connection pool.
   * Only used when creating a pooled DataSource.
   * @return the minimum number of connections
   */
  def minConnections: Int

  /** 
   * Sets the minimum number of connections to maintain in the connection pool.
   * This ensures that at least this many connections are always available for use.
   * Only applicable when using connection pooling.
   * @param min the minimum number of connections (must be >= 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if min < 0
   */
  def setMinConnections(min: Int): MySQLConfig

  /** 
   * Gets the maximum number of connections allowed in the connection pool.
   * Only used when creating a pooled DataSource.
   * @return the maximum number of connections
   */
  def maxConnections: Int

  /** 
   * Sets the maximum number of connections allowed in the connection pool.
   * The pool will not create more connections than this limit.
   * Only applicable when using connection pooling.
   * @param max the maximum number of connections (must be >= minConnections)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if max < minConnections
   */
  def setMaxConnections(max: Int): MySQLConfig

  /** 
   * Gets the maximum time to wait for a connection from the pool.
   * Only used when creating a pooled DataSource.
   * @return the connection timeout duration
   */
  def connectionTimeout: FiniteDuration

  /** 
   * Sets the maximum time to wait for a connection to become available from the pool.
   * If no connection is available within this timeout, an error will be returned.
   * Only applicable when using connection pooling.
   * @param timeout the connection timeout (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if timeout <= 0
   */
  def setConnectionTimeout(timeout: FiniteDuration): MySQLConfig

  /** 
   * Gets the maximum time a connection can remain idle in the pool.
   * Only used when creating a pooled DataSource.
   * @return the idle timeout duration
   */
  def idleTimeout: FiniteDuration

  /** 
   * Sets the maximum time a connection can remain idle in the pool before being closed.
   * Idle connections exceeding this timeout will be removed to free resources.
   * Only applicable when using connection pooling.
   * @param timeout the idle timeout (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if timeout <= 0
   */
  def setIdleTimeout(timeout: FiniteDuration): MySQLConfig

  /** 
   * Gets the maximum lifetime of a connection in the pool.
   * Only used when creating a pooled DataSource.
   * @return the maximum lifetime duration
   */
  def maxLifetime: FiniteDuration

  /** 
   * Sets the maximum lifetime of a connection in the pool.
   * Connections older than this will be retired and replaced with fresh connections.
   * This helps prevent issues with long-lived connections.
   * Only applicable when using connection pooling.
   * @param maxLifetime the maximum lifetime (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if maxLifetime <= 0
   */
  def setMaxLifetime(maxLifetime: FiniteDuration): MySQLConfig

  /** 
   * Gets the maximum time to wait for connection validation.
   * Only used when creating a pooled DataSource.
   * @return the validation timeout duration
   */
  def validationTimeout: FiniteDuration

  /** 
   * Sets the maximum time to wait for connection validation to complete.
   * Connections are validated before being handed out from the pool.
   * Only applicable when using connection pooling.
   * @param timeout the validation timeout (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if timeout <= 0
   */
  def setValidationTimeout(timeout: FiniteDuration): MySQLConfig

  /** 
   * Gets the threshold for connection leak detection.
   * Only used when creating a pooled DataSource.
   * @return the leak detection threshold, or None if disabled
   */
  def leakDetectionThreshold: Option[FiniteDuration]

  /** 
   * Sets the threshold for connection leak detection.
   * If a connection is not returned to the pool within this time, it's considered leaked.
   * A warning will be logged to help identify connection leaks in the application.
   * Only applicable when using connection pooling.
   * @param threshold the leak detection threshold
   * @return a new MySQLConfig with the updated setting
   */
  def setLeakDetectionThreshold(threshold: FiniteDuration): MySQLConfig

  /** 
   * Gets the interval for pool maintenance tasks.
   * Only used when creating a pooled DataSource.
   * @return the maintenance interval duration
   */
  def maintenanceInterval: FiniteDuration

  /** 
   * Sets the interval at which pool maintenance tasks are performed.
   * Maintenance includes removing idle connections, validating connections, etc.
   * Only applicable when using connection pooling.
   * @param interval the maintenance interval (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if interval <= 0
   */
  def setMaintenanceInterval(interval: FiniteDuration): MySQLConfig

  /** 
   * Gets whether adaptive pool sizing is enabled.
   * Only used when creating a pooled DataSource.
   * @return true if adaptive sizing is enabled
   */
  def adaptiveSizing: Boolean

  /** 
   * Sets whether to enable adaptive pool sizing.
   * When enabled, the pool size will be dynamically adjusted based on load patterns.
   * This can help optimize resource usage under varying workloads.
   * Only applicable when using connection pooling.
   * @param enabled true to enable adaptive sizing
   * @return a new MySQLConfig with the updated setting
   */
  def setAdaptiveSizing(enabled: Boolean): MySQLConfig

  /** 
   * Gets the interval for adaptive pool sizing calculations.
   * Only used when creating a pooled DataSource.
   * @return the adaptive sizing interval duration
   */
  def adaptiveInterval: FiniteDuration

  /** 
   * Sets the interval at which the adaptive sizing algorithm runs.
   * The algorithm analyzes pool usage patterns and adjusts the pool size accordingly.
   * Only applicable when using connection pooling with adaptive sizing enabled.
   * @param interval the adaptive sizing interval (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if interval <= 0
   */
  def setAdaptiveInterval(interval: FiniteDuration): MySQLConfig

  /** 
   * Gets the alive bypass window for connection validation optimization.
   * Connections used within this window will skip validation checks.
   * @return the alive bypass window duration
   */
  def aliveBypassWindow: FiniteDuration

  /** 
   * Sets the window during which recently used connections skip validation.
   * This optimization reduces unnecessary database round-trips for frequently used connections.
   * Set to 0 to disable this optimization and always validate connections.
   * @param window the bypass window duration (must be >= 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if window < 0
   */
  def setAliveBypassWindow(window: FiniteDuration): MySQLConfig

  /** 
   * Gets the keepalive time for idle connections.
   * @return the keepalive time duration, or None if disabled
   */
  def keepaliveTime: Option[FiniteDuration]

  /** 
   * Sets the interval at which idle connections are validated.
   * This helps prevent connection timeouts due to firewalls or idle timeouts.
   * The actual keepalive time will vary by up to 20% to avoid synchronized validations.
   * @param time the keepalive interval (must be >= 30 seconds)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if time < 30 seconds
   */
  def setKeepaliveTime(time: FiniteDuration): MySQLConfig

  /** 
   * Gets the connection test query used for validation.
   * @return the test query, or None to use JDBC4 isValid()
   */
  def connectionTestQuery: Option[String]

  /** 
   * Sets a custom query for connection validation.
   * If not set, JDBC4's isValid() method will be used (recommended).
   * Only set this if your driver doesn't support isValid() properly.
   * @param query the test query (e.g., "SELECT 1")
   * @return a new MySQLConfig with the updated setting
   */
  def setConnectionTestQuery(query: String): MySQLConfig

  /**
   * Gets whether pool state logging is enabled.
   * When enabled, the pool will periodically log its state (connections, active, idle, waiting).
   * @return true if pool state logging is enabled
   */
  def logPoolState: Boolean

  /**
   * Sets whether to enable periodic logging of pool state.
   * When enabled, the pool will log statistics at regular intervals to help monitor pool health.
   * This is useful for debugging connection pool issues and monitoring pool usage patterns.
   * @param enabled true to enable pool state logging
   * @return a new MySQLConfig with the updated setting
   */
  def setLogPoolState(enabled: Boolean): MySQLConfig

  /**
   * Gets the interval for pool state logging.
   * @return the logging interval duration
   */
  def poolStateLogInterval: FiniteDuration

  /**
   * Sets the interval at which pool state is logged.
   * This controls how often the pool logs its current state when logging is enabled.
   * Shorter intervals provide more granular monitoring but may produce excessive logs.
   * @param interval the logging interval (must be > 0)
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if interval <= 0
   */
  def setPoolStateLogInterval(interval: FiniteDuration): MySQLConfig

  /**
   * Gets the name of the connection pool for logging purposes.
   * @return the pool name
   */
  def poolName: String

  /**
   * Sets the name of the connection pool.
   * This name is used in log messages to identify the pool when multiple pools are in use.
   * @param name the pool name
   * @return a new MySQLConfig with the updated setting
   */
  def setPoolName(name: String): MySQLConfig

/**
 * Companion object for MySQLConfig providing factory methods.
 */
object MySQLConfig:

  /** Default socket options applied to all connections. */
  private[ldbc] val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  /** Private implementation of MySQLConfig trait. */
  private case class Impl(
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String]                        = None,
    database:                Option[String]                        = None,
    debug:                   Boolean                               = false,
    ssl:                     SSL                                   = SSL.None,
    socketOptions:           List[SocketOption]                    = defaultSocketOptions,
    readTimeout:             Duration                              = Duration.Inf,
    allowPublicKeyRetrieval: Boolean                               = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    useCursorFetch:          Boolean                               = false,
    useServerPrepStmts:      Boolean                               = false,
    minConnections:          Int                                   = 5,
    maxConnections:          Int                                   = 10,
    connectionTimeout:       FiniteDuration                        = 30.seconds,
    idleTimeout:             FiniteDuration                        = 10.minutes,
    maxLifetime:             FiniteDuration                        = 30.minutes,
    validationTimeout:       FiniteDuration                        = 5.seconds,
    leakDetectionThreshold:  Option[FiniteDuration]                = None,
    maintenanceInterval:     FiniteDuration                        = 30.seconds,
    adaptiveSizing:          Boolean                               = false,
    adaptiveInterval:        FiniteDuration                        = 1.minute,
    aliveBypassWindow:       FiniteDuration                        = 500.milliseconds,
    keepaliveTime:           Option[FiniteDuration]                = Some(2.minutes),
    connectionTestQuery:     Option[String]                        = None,
    logPoolState:            Boolean                               = false,
    poolStateLogInterval:    FiniteDuration                        = 30.seconds,
    poolName:                String                                = "ldbc-pool"
  ) extends MySQLConfig:

    override def setHost(host:                   String):             MySQLConfig = copy(host = host)
    override def setPort(port:                   Int):                MySQLConfig = copy(port = port)
    override def setUser(user:                   String):             MySQLConfig = copy(user = user)
    override def setPassword(password:           String):             MySQLConfig = copy(password = Some(password))
    override def setDatabase(database:           String):             MySQLConfig = copy(database = Some(database))
    override def setDebug(debug:                 Boolean):            MySQLConfig = copy(debug = debug)
    override def setSSL(ssl:                     SSL):                MySQLConfig = copy(ssl = ssl)
    override def setSocketOptions(socketOptions: List[SocketOption]): MySQLConfig = copy(socketOptions = socketOptions)
    override def setReadTimeout(readTimeout:     Duration):           MySQLConfig = copy(readTimeout = readTimeout)
    override def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLConfig =
      copy(allowPublicKeyRetrieval = allowPublicKeyRetrieval)
    override def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): MySQLConfig =
      copy(databaseTerm = Some(databaseTerm))
    override def setUseCursorFetch(useCursorFetch: Boolean):         MySQLConfig = copy(useCursorFetch = useCursorFetch)
    override def setUseServerPrepStmts(useServerPrepStmts: Boolean): MySQLConfig =
      copy(useServerPrepStmts = useServerPrepStmts)
    override def setMinConnections(min:        Int):                   MySQLConfig = copy(minConnections = min)
    override def setMaxConnections(max:        Int):                   MySQLConfig = copy(maxConnections = max)
    override def setConnectionTimeout(timeout: FiniteDuration):        MySQLConfig = copy(connectionTimeout = timeout)
    override def setIdleTimeout(timeout:       FiniteDuration):        MySQLConfig = copy(idleTimeout = timeout)
    override def setMaxLifetime(maxLifetime:   FiniteDuration):        MySQLConfig = copy(maxLifetime = maxLifetime)
    override def setValidationTimeout(timeout: FiniteDuration):        MySQLConfig = copy(validationTimeout = timeout)
    override def setLeakDetectionThreshold(threshold: FiniteDuration): MySQLConfig =
      copy(leakDetectionThreshold = Some(threshold))
    override def setMaintenanceInterval(interval: FiniteDuration): MySQLConfig = copy(maintenanceInterval = interval)
    override def setAdaptiveSizing(enabled:       Boolean):        MySQLConfig = copy(adaptiveSizing = enabled)
    override def setAdaptiveInterval(interval:    FiniteDuration): MySQLConfig = copy(adaptiveInterval = interval)
    override def setAliveBypassWindow(window:     FiniteDuration): MySQLConfig = copy(aliveBypassWindow = window)
    override def setKeepaliveTime(time:           FiniteDuration): MySQLConfig = copy(keepaliveTime = Some(time))
    override def setConnectionTestQuery(query:    String):         MySQLConfig = copy(connectionTestQuery = Some(query))
    override def setLogPoolState(enabled:         Boolean):        MySQLConfig = copy(logPoolState = enabled)
    override def setPoolStateLogInterval(interval: FiniteDuration): MySQLConfig = copy(poolStateLogInterval = interval)
    override def setPoolName(name:                 String):         MySQLConfig = copy(poolName = name)

  /**
   * Creates a default MySQLConfig with standard connection parameters.
   * 
   * Default values:
   * - host: "127.0.0.1" 
   * - port: 3306
   * - user: "root"
   * - password: None
   * - database: None
   * - debug: false
   * - ssl: SSL.None
   * - socketOptions: List(SocketOption.noDelay(true))
   * - readTimeout: Duration.Inf
   * - allowPublicKeyRetrieval: false
   * - databaseTerm: Some(DatabaseMetaData.DatabaseTerm.CATALOG)
   * - useCursorFetch: false
   * - useServerPrepStmts: false
   * 
   * @return a new MySQLConfig with default settings
   */
  def default: MySQLConfig = Impl("127.0.0.1", 3306, "root")
