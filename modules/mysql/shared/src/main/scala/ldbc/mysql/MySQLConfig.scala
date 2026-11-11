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
   * Gets the largest message that may be sent to or received from the MySQL server.
   *
   * The limit applies to a whole message, not to a packet. A packet header carries its length in
   * three bytes, so a message past that is carried by several packets and put back together by the
   * receiver; this value bounds the total, which is what keeps a reply from consuming memory without
   * end.
   *
   * The value corresponds to the MySQL server's `max_allowed_packet` system variable, and the
   * effective ceiling is the smaller of this and what the server reports.
   *
   * @return the maximum message size in bytes
   */
  def maxAllowedPacket: Int

  /**
   * Sets the largest message that may be sent to or received from the MySQL server.
   *
   * This setting provides protection against:
   * - Memory exhaustion attacks through oversized messages
   * - Denial of Service (DoS) attacks via large data payloads
   * - Accidental transmission of extremely large data sets
   *
   * @param maxAllowedPacket the maximum message size in bytes
   * @return a new MySQLConfig with the updated setting
   * @throws IllegalArgumentException if the value is outside the valid range (1,024 to 1,073,741,824)
   *
   * A practical 1MB ceiling for an application with moderate BLOB usage:
   *
   * @example {{{
   * config.setMaxAllowedPacket(1048576)
   * }}}
   *
   * 64MB, matching the default `max_allowed_packet` of a MySQL 8 server:
   *
   * @example {{{
   * config.setMaxAllowedPacket(67108864)
   * }}}
   *
   * @note Left unset, a connection starts at the maximum and is narrowed to whatever the server
   *       reports, so this only needs setting to stay below the server's own limit.
   * @note Receiving a message needs roughly three times its size in memory while it is being
   *       assembled, so a large limit on a small heap is worth setting down deliberately.
   * @note Valid range: 1,024 bytes (1KB) minimum to 1,073,741,824 bytes (1GB) maximum, the largest
   *       value a MySQL server will accept for `max_allowed_packet`
   * @see [[https://dev.mysql.com/doc/refman/en/packet-too-large.html MySQL Protocol Packet Limits]]
   */
  def setMaxAllowedPacket(maxAllowedPacket: Int): MySQLConfig

/**
 * Companion object for MySQLConfig providing factory methods.
 */
object MySQLConfig:

  /** Minimum allowed message size in bytes (1KB) */
  val MIN_PACKET_SIZE: Int = 1024

  /**
   * Maximum allowed message size in bytes (1GB).
   *
   * This is not a protocol limit. A single packet can only describe 16,777,215 bytes, but a message
   * past that is carried by several packets, so framing puts no ceiling on a message. Two other
   * things do: a MySQL server clamps `max_allowed_packet` to this value however much more is asked
   * for, and a reassembled message has to be flattened into contiguous storage, which scodec refuses
   * to do beyond `Int.MaxValue` bytes. 1GB is the largest value that satisfies both.
   */
  val MAX_PACKET_SIZE: Int = 1073741824

  /**
   * Default message size in bytes: the maximum, narrowed to the server's own `max_allowed_packet`
   * once the session is up.
   *
   * Left at a fixed small value, this would cap every connection at that size regardless of what the
   * server permits, and a row larger than it could not be read at all. Starting at the maximum makes
   * "unconfigured" mean "as much as the server allows", and the server's value — read with the other
   * session variables right after authentication — supplies the real ceiling. This is also what
   * MySQL Connector/J does with its own default.
   *
   * It is a ceiling, not a recommendation. Assembling a message needs roughly three times its size
   * in memory, so a deployment with a large server limit and a small heap should set this down.
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
        s"maxAllowedPacket must not exceed $MAX_PACKET_SIZE bytes (the largest value a MySQL server " +
          s"accepts for max_allowed_packet), but got $maxAllowedPacket"
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
