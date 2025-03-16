/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.sql.DriverManager

import javax.sql.DataSource

import scala.concurrent.ExecutionContext

import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import ldbc.sql.{ Connection, Provider }
import ldbc.sql.logging.{ LogEvent, LogHandler }

trait MySQLProvider[F[_]] extends Provider[F]:

  /**
   * Create a connection managed by Resource.
   *
   * {{{
   *   provider.createConnection().user { connection =>
   *     ???
   *   }
   * }}}
   */
  def createConnection(): Resource[F, Connection[F]]

object MySQLProvider:

  def consoleLogger[F[_]: Console: Sync]: LogHandler[F] =
    case LogEvent.Success(sql, args) =>
      Console[F].println(
        s"""Successful Statement Execution:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      )
    case LogEvent.ProcessingFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed ResultSet Processing:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
    case LogEvent.ExecFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed Statement Execution:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)

  private case class DataSourceProvider[F[_]](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: LogHandler[F]
  )(using ev: Async[F])
    extends MySQLProvider[F]:
    override def createConnection(): Resource[F, Connection[F]] =
      Resource
        .fromAutoCloseable(ev.evalOn(ev.delay(dataSource.getConnection()), connectEC))
        .map(conn => ConnectionImpl(conn, logHandler))

    override def use[A](f: Connection[F] => F[A]): F[A] =
      createConnection().use(f)

  private case class ConnectionProvider[F[_]: Sync](
    connection: java.sql.Connection,
    logHandler: LogHandler[F]
  ) extends MySQLProvider[F]:
    override def createConnection(): Resource[F, Connection[F]] =
      Resource.pure(ConnectionImpl(connection, logHandler))

    override def use[A](f: Connection[F] => F[A]): F[A] =
      createConnection().use(f)

  class DriverProvider[F[_]: Console](using ev: Async[F]):

    private def create(
      driver:      String,
      conn:        () => java.sql.Connection,
      _logHandler: LogHandler[F]
    ): MySQLProvider[F] =
      new MySQLProvider[F]:
        override def logHandler: LogHandler[F] = _logHandler
        override def createConnection(): Resource[F, Connection[F]] =
          Resource
            .fromAutoCloseable(ev.blocking {
              Class.forName(driver)
              conn()
            })
            .map(conn => ConnectionImpl(conn, logHandler))

        override def use[A](f: Connection[F] => F[A]): F[A] =
          createConnection().use(f)

    /** Construct a new `Provider` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     * @param logHandler
     *   Handler for outputting logs of process execution using connections.
     */
    def apply(
      driver:     String,
      url:        String,
      logHandler: Option[LogHandler[F]]
    ): MySQLProvider[F] =
      create(driver, () => DriverManager.getConnection(url), logHandler.getOrElse(consoleLogger))

    /** Construct a new `Provider` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     * @param user
     *   database username
     * @param password
     *   database password
     * @param logHandler
     *   Handler for outputting logs of process execution using connections.
     */
    def apply(
      driver:     String,
      url:        String,
      user:       String,
      password:   String,
      logHandler: Option[LogHandler[F]]
    ): MySQLProvider[F] =
      create(driver, () => DriverManager.getConnection(url, user, password), logHandler.getOrElse(consoleLogger))

    /** Construct a new `Provider` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     * @param info
     *   a `Properties` containing connection information (see `DriverManager.getConnection`)
     * @param logHandler
     *   Handler for outputting logs of process execution using connections.
     */
    def apply(
      driver:     String,
      url:        String,
      info:       java.util.Properties,
      logHandler: Option[LogHandler[F]]
    ): MySQLProvider[F] =
      create(driver, () => DriverManager.getConnection(url, info), logHandler.getOrElse(consoleLogger))

  /**
   *  Construct a constructor of `Provider[F]` for some `D <: DataSource` When calling this constructor you
   * should explicitly supply the effect type M e.g. MySQLProvider.fromDataSource[IO](myDataSource, connectEC)
   *
   * @param dataSource
   *   A data source that manages connection information to MySQL.
   * @param connectEC
   *   Execution context dedicated to database connection.
   * @param logHandler
   *   Handler for outputting logs of process execution using connections.
   */
  def fromDataSource[F[_]: Console: Async](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: Option[LogHandler[F]] = None
  ): MySQLProvider[F] = DataSourceProvider(dataSource, connectEC, logHandler.getOrElse(consoleLogger))

  /**
   * Construct a `Provider` that wraps an existing `Connection`. Closing the connection is the responsibility of
   * the caller.
   *
   * @param connection
   *   a raw JDBC `Connection` to wrap
   * @param logHandler
   *   Handler for outputting logs of process execution using connections.
   */
  def fromConnection[F[_]: Console: Sync](
    connection: java.sql.Connection,
    logHandler: Option[LogHandler[F]] = None
  ): MySQLProvider[F] = ConnectionProvider(connection, logHandler.getOrElse(consoleLogger))

  /** Module of constructors for `Provider` that use the JDBC `DriverManager` to allocate connections. Note that
   * `DriverManager` is unbounded and will happily allocate new connections until server resources are exhausted. It
   * is usually preferable to use `DataSourceTransactor` with an underlying bounded connection pool. Blocking operations on `DriverProvider` are
   * executed on an unbounded cached daemon thread pool by default, so you are also at risk of exhausting system
   * threads. TL;DR this is fine for console apps but don't use it for a web application.
   */
  def fromDriverManager[F[_]: Console: Async]: DriverProvider[F] = new DriverProvider[F]
