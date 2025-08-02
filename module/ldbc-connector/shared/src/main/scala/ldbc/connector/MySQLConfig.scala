/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import fs2.io.net.*

import ldbc.sql.DatabaseMetaData

trait MySQLConfig:

  def host: String
  def setHost(host: String): MySQLConfig

  def port: Int
  def setPort(port: Int): MySQLConfig

  def user: String
  def setUser(user: String): MySQLConfig

  def password: Option[String]
  def setPassword(password: Option[String]): MySQLConfig

  def database: Option[String]
  def setDatabase(database: Option[String]): MySQLConfig

  def debug: Boolean
  def setDebug(debug: Boolean): MySQLConfig

  def ssl: SSL
  def setSSL(ssl: SSL): MySQLConfig

  def socketOptions: List[SocketOption]
  def setSocketOptions(socketOptions: List[SocketOption]): MySQLConfig

  def readTimeout: Duration
  def setReadTimeout(readTimeout: Duration): MySQLConfig

  def allowPublicKeyRetrieval: Boolean
  def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLConfig

  def databaseTerm: Option[DatabaseMetaData.DatabaseTerm]
  def setDatabaseTerm(databaseTerm: Option[DatabaseMetaData.DatabaseTerm]): MySQLConfig

  def useCursorFetch: Boolean
  def setUseCursorFetch(useCursorFetch: Boolean): MySQLConfig

  def useServerPrepStmts: Boolean
  def setUseServerPrepStmts(useServerPrepStmts: Boolean): MySQLConfig

object MySQLConfig:

  private[ldbc] val defaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

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
                         ) extends MySQLConfig:

    override def setHost(host: String): MySQLConfig = copy(host = host)
    override def setPort(port: Int): MySQLConfig = copy(port = port)
    override def setUser(user: String): MySQLConfig = copy(user = user)
    override def setPassword(password: Option[String]): MySQLConfig = copy(password = password)
    override def setDatabase(database: Option[String]): MySQLConfig = copy(database = database)
    override def setDebug(debug: Boolean): MySQLConfig = copy(debug = debug)
    override def setSSL(ssl: SSL): MySQLConfig = copy(ssl = ssl)
    override def setSocketOptions(socketOptions: List[SocketOption]): MySQLConfig = copy(socketOptions = socketOptions)
    override def setReadTimeout(readTimeout: Duration): MySQLConfig = copy(readTimeout = readTimeout)
    override def setAllowPublicKeyRetrieval(allowPublicKeyRetrieval: Boolean): MySQLConfig =
      copy(allowPublicKeyRetrieval = allowPublicKeyRetrieval)
    override def setDatabaseTerm(databaseTerm: Option[DatabaseMetaData.DatabaseTerm]): MySQLConfig =
      copy(databaseTerm = databaseTerm)
    override def setUseCursorFetch(useCursorFetch: Boolean): MySQLConfig = copy(useCursorFetch = useCursorFetch)
    override def setUseServerPrepStmts(useServerPrepStmts: Boolean): MySQLConfig =
      copy(useServerPrepStmts = useServerPrepStmts)

  def default: MySQLConfig = Impl("127..0.1", 3306, "root")
