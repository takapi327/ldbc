/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scala.concurrent.duration.*

import ldbc.sql.DatabaseMetaData

import ldbc.net.{ SSL, SocketOptions }

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
  def socketOptions: SocketOptions

  /** Sets socket-level options for the TCP connection.
    * @param socketOptions list of socket options to apply
    * @return a new MySQLConfig with the updated socket options
    */
  def setSocketOptions(socketOptions: SocketOptions): MySQLConfig

  /** The timeout duration for read operations. */
  def readTimeout: Duration

  /** Sets the timeout duration for read operations.
    * @param readTimeout the read timeout duration, or Duration.Inf for no timeout
    * @return a new MySQLConfig with the updated read timeout
    */
  def setReadTimeout(readTimeout: Duration): MySQLConfig

  /** How long to wait for the TCP connection to be established. */
  def connectTimeout: FiniteDuration

  /** Sets how long to wait for the TCP connection to be established.
    *
    * Unlike the read timeout this cannot be disabled, because a connect that never completes gives
    * the caller nothing to act on: the operating system retransmits the SYN for minutes before
    * reporting a failure.
    *
    * @param connectTimeout the connect timeout duration
    * @return a new MySQLConfig with the updated connect timeout
    */
  def setConnectTimeout(connectTimeout: FiniteDuration): MySQLConfig

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
   * Gets the maximum allowed packet size for network communication with MySQL server.
   * 
   * This setting controls the maximum size of packets that can be sent to or received from
   * the MySQL server. It helps prevent memory exhaustion attacks and ensures compatibility
   * with the MySQL protocol limits.
   * 
   * The value corresponds to the MySQL server's `max_allowed_packet` system variable.
   * 
   * @return the maximum packet size in bytes
   */
  def maxAllowedPacket: Int

  /**
   * Sets the maximum allowed packet size for network communication.
   * 
   * This setting provides protection against:
   * - Memory exhaustion attacks through oversized packets
   * - Denial of Service (DoS) attacks via large data payloads
   * - Accidental transmission of extremely large data sets
   * 
   * @param maxAllowedPacket the maximum packet size in bytes
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if the value is outside the valid range (1024 to 16,777,215)
   * 
   * @example {{{
   * // Set conservative 64KB limit (default)
   * config.setMaxAllowedPacket(65535)
   * 
   * // Set practical 1MB limit for applications with moderate BLOB usage
   * config.setMaxAllowedPacket(1048576)
   * 
   * // Set maximum protocol limit for applications requiring large data transfers
   * config.setMaxAllowedPacket(16777215)
   * }}}
   * 
   * @note The default value of 65,535 bytes (64KB) is compatible with MySQL JDBC Driver defaults
   *       and provides good security against packet-based attacks while accommodating most use cases.
   * @note Valid range: 1,024 bytes (1KB) minimum to 16,777,215 bytes (16MB) maximum (MySQL protocol limit)
   * @see [[https://dev.mysql.com/doc/refman/en/packet-too-large.html MySQL Protocol Packet Limits]]
   */
  def setMaxAllowedPacket(maxAllowedPacket: Int): MySQLConfig

/**
 * Companion object for MySQLConfig providing factory methods.
 */
object MySQLConfig:

  /** Minimum allowed packet size in bytes (1KB) */
  val MIN_PACKET_SIZE: Int = 1024

  /** Maximum allowed packet size in bytes (16MB - MySQL protocol limit) */
  val MAX_PACKET_SIZE: Int = 16777215

  /**
   * Default packet size in bytes: the protocol maximum, narrowed to the server's own
   * `max_allowed_packet` once the session is up.
   *
   * Left at a fixed small value, this would cap every connection at that size regardless of what the
   * server permits, and a row larger than it could not be read at all. Starting at the protocol
   * maximum makes "unconfigured" mean "as much as the protocol allows", and the server's value — read
   * with the other session variables right after authentication — supplies the real ceiling.
   */
  val DEFAULT_PACKET_SIZE: Int = MAX_PACKET_SIZE

  /** Default socket options applied to all connections. */
  private[ldbc] val defaultSocketOptions: SocketOptions =
    SocketOptions.default

  /** Private implementation of MySQLConfig trait. */
  private case class Impl(
    host:                    String,
    port:                    Int,
    user:                    String,
    password:                Option[String]                        = None,
    database:                Option[String]                        = None,
    debug:                   Boolean                               = false,
    ssl:                     SSL                                   = SSL.None,
    socketOptions:           SocketOptions                         = defaultSocketOptions,
    readTimeout:             Duration                              = Duration.Inf,
    connectTimeout:          FiniteDuration                        = Connection.defaultConnectTimeout,
    allowPublicKeyRetrieval: Boolean                               = false,
    databaseTerm:            Option[DatabaseMetaData.DatabaseTerm] = Some(DatabaseMetaData.DatabaseTerm.CATALOG),
    useCursorFetch:          Boolean                               = false,
    useServerPrepStmts:      Boolean                               = false,
    maxAllowedPacket:        Int                                   = DEFAULT_PACKET_SIZE
  ) extends MySQLConfig:

    /**
     * Returns a string representation of this configuration without exposing sensitive information.
     *
     * The password is intentionally redacted (rendered as `***` when set) so that it is never
     * leaked through logs, exception messages, or crash reports. Non-sensitive settings useful
     * for debugging are retained.
     *
     * @return a secure string representation of the configuration
     */
    override def toString: String =
      s"MySQLConfig(host=$host, port=$port, user=$user, password=${ password.fold("None")(_ => "***") }, " +
        s"database=$database, debug=$debug, ssl=$ssl, allowPublicKeyRetrieval=$allowPublicKeyRetrieval, " +
        s"useCursorFetch=$useCursorFetch, useServerPrepStmts=$useServerPrepStmts, " +
        s"maxAllowedPacket=$maxAllowedPacket)"

    override def setHost(host:                   String):           MySQLConfig = copy(host = host)
    override def setPort(port:                   Int):              MySQLConfig = copy(port = port)
    override def setUser(user:                   String):           MySQLConfig = copy(user = user)
    override def setPassword(password:           String):           MySQLConfig = copy(password = Some(password))
    override def setDatabase(database:           String):           MySQLConfig = copy(database = Some(database))
    override def setDebug(debug:                 Boolean):          MySQLConfig = copy(debug = debug)
    override def setSSL(ssl:                     SSL):              MySQLConfig = copy(ssl = ssl)
    override def setSocketOptions(socketOptions: SocketOptions):    MySQLConfig = copy(socketOptions = socketOptions)
    override def setReadTimeout(readTimeout:     Duration):         MySQLConfig = copy(readTimeout = readTimeout)
    override def setConnectTimeout(connectTimeout: FiniteDuration): MySQLConfig =
      copy(connectTimeout = connectTimeout)
    override def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLConfig =
      copy(allowPublicKeyRetrieval = allowPublicKeyRetrieval)
    override def setDatabaseTerm(databaseTerm: DatabaseMetaData.DatabaseTerm): MySQLConfig =
      copy(databaseTerm = Some(databaseTerm))
    override def setUseCursorFetch(useCursorFetch: Boolean):         MySQLConfig = copy(useCursorFetch = useCursorFetch)
    override def setUseServerPrepStmts(useServerPrepStmts: Boolean): MySQLConfig =
      copy(useServerPrepStmts = useServerPrepStmts)
    override def setMaxAllowedPacket(maxAllowedPacket: Int): MySQLConfig = {
      require(
        maxAllowedPacket >= MIN_PACKET_SIZE,
        s"maxAllowedPacket must be at least $MIN_PACKET_SIZE bytes, but got $maxAllowedPacket"
      )
      require(
        maxAllowedPacket <= MAX_PACKET_SIZE,
        s"maxAllowedPacket must not exceed $MAX_PACKET_SIZE bytes (MySQL protocol limit), but got $maxAllowedPacket"
      )
      copy(maxAllowedPacket = maxAllowedPacket)
    }

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
   * - socketOptions: SocketOptions.default
   * - readTimeout: Duration.Inf
   * - connectTimeout: 30.seconds
   * - allowPublicKeyRetrieval: false
   * - databaseTerm: Some(DatabaseMetaData.DatabaseTerm.CATALOG)
   * - useCursorFetch: false
   * - useServerPrepStmts: false
   * 
   * @return a new MySQLConfig with default settings
   */
  def default: MySQLConfig = Impl("127.0.0.1", 3306, "root")
