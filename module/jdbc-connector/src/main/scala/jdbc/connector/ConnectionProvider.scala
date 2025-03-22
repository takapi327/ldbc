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
import cats.Applicative

import cats.effect.*
import cats.effect.std.Console

import ldbc.sql.{ Connection, Provider }
import ldbc.sql.logging.{ LogEvent, LogHandler }

trait ConnectionProvider[F[_]] extends Provider[F]:

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

object ConnectionProvider:
  private def noopLogger[F[_]: Applicative]: LogHandler[F] = (logEvent: LogEvent) => Applicative[F].unit

  private case class DataSourceProvider[F[_]](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: Option[LogHandler[F]]
  )(using ev: Async[F])
    extends ConnectionProvider[F]:
    override def createConnection(): Resource[F, Connection[F]] =
      Resource
        .fromAutoCloseable(ev.evalOn(ev.delay(dataSource.getConnection()), connectEC))
        .map(conn => ConnectionImpl(conn, logHandler.getOrElse(noopLogger)))

    override def use[A](f: Connection[F] => F[A]): F[A] =
      createConnection().use(f)

  private case class ConnectionProvider[F[_]: Sync](
    connection: java.sql.Connection,
    logHandler: Option[LogHandler[F]]
  ) extends ConnectionProvider[F]:
    override def createConnection(): Resource[F, Connection[F]] =
      Resource.pure(ConnectionImpl(connection, logHandler.getOrElse(noopLogger)))

    override def use[A](f: Connection[F] => F[A]): F[A] =
      createConnection().use(f)

  class DriverProvider[F[_]: Console](using ev: Async[F]):

    private def create(
      driver:      String,
      conn:        () => java.sql.Connection,
      _logHandler: Option[LogHandler[F]]
    ): ConnectionProvider[F] =
      new ConnectionProvider[F]:
        override def logHandler: Option[LogHandler[F]] = _logHandler
        override def createConnection(): Resource[F, Connection[F]] =
          Resource
            .fromAutoCloseable(ev.blocking {
              Class.forName(driver)
              conn()
            })
            .map(conn => ConnectionImpl(conn, logHandler.getOrElse(noopLogger)))

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
    ): ConnectionProvider[F] =
      create(driver, () => DriverManager.getConnection(url), logHandler)

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
    ): ConnectionProvider[F] =
      create(driver, () => DriverManager.getConnection(url, user, password), logHandler)

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
    ): ConnectionProvider[F] =
      create(driver, () => DriverManager.getConnection(url, info), logHandler)

  /**
   *  Construct a constructor of `Provider[F]` for some `D <: DataSource` When calling this constructor you
   * should explicitly supply the effect type M e.g. ConnectionProvider.fromDataSource[IO](myDataSource, connectEC)
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
  ): ConnectionProvider[F] = DataSourceProvider(dataSource, connectEC, logHandler)

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
  ): ConnectionProvider[F] = ConnectionProvider(connection, logHandler)

  /** Module of constructors for `Provider` that use the JDBC `DriverManager` to allocate connections. Note that
   * `DriverManager` is unbounded and will happily allocate new connections until server resources are exhausted. It
   * is usually preferable to use `DataSourceTransactor` with an underlying bounded connection pool. Blocking operations on `DriverProvider` are
   * executed on an unbounded cached daemon thread pool by default, so you are also at risk of exhausting system
   * threads. TL;DR this is fine for console apps but don't use it for a web application.
   */
  def fromDriverManager[F[_]: Console: Async]: DriverProvider[F] = new DriverProvider[F]
