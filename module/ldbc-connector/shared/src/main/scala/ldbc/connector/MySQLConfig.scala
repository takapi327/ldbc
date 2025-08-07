/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

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
    useServerPrepStmts:      Boolean                               = false
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
