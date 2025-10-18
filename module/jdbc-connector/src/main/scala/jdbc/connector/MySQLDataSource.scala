/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.sql.DriverManager

import javax.sql.DataSource

import scala.concurrent.ExecutionContext

import cats.effect.*

import ldbc.sql.Connection

import ldbc.DataSource as LdbcDataSource

object MySQLDataSource:

  private case class Impl[F[_]](
    dataSource: DataSource,
    connectEC:  ExecutionContext
  )(using ev: Async[F])
    extends LdbcDataSource[F]:
    override def getConnection: Resource[F, Connection[F]] =
      Resource
        .fromAutoCloseable(ev.evalOn(ev.delay(dataSource.getConnection()), connectEC))
        .map(conn => ConnectionImpl(conn))

  private case class JavaConnection[F[_]: Sync](
    connection: java.sql.Connection
  ) extends LdbcDataSource[F]:
    override def getConnection: Resource[F, Connection[F]] =
      Resource.pure(ConnectionImpl(connection))

  class Driver[F[_]](using ev: Async[F]):

    private def create(
      driver: String,
      conn:   () => java.sql.Connection
    ): LdbcDataSource[F] = new LdbcDataSource[F]:
      override def getConnection: Resource[F, Connection[F]] =
        Resource
          .fromAutoCloseable(ev.blocking {
            Class.forName(driver)
            conn()
          })
          .map(conn => ConnectionImpl(conn))

    /** Construct a new `DataSource` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     */
    def apply(
      driver: String,
      url:    String
    ): LdbcDataSource[F] =
      create(driver, () => DriverManager.getConnection(url))

    /** Construct a new `DataSource` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     * @param user
     *   database username
     * @param password
     *   database password
     */
    def apply(
      driver:     String,
      url:        String,
      user:       String,
      password:   String
    ): LdbcDataSource[F] =
      create(driver, () => DriverManager.getConnection(url, user, password))

    /** Construct a new `DataSource` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     * @param info
     *   a `Properties` containing connection information (see `DriverManager.getConnection`)
     */
    def apply(
      driver:     String,
      url:        String,
      info:       java.util.Properties
    ): LdbcDataSource[F] =
      create(driver, () => DriverManager.getConnection(url, info))

  /**
   *  Construct a constructor of `DataSource[F]` for some `D <: DataSource` When calling this constructor you
   * should explicitly supply the effect type M e.g. ConnectionProvider.fromDataSource[IO](myDataSource, connectEC)
   *
   * @param dataSource
   *   A data source that manages connection information to MySQL.
   * @param connectEC
   *   Execution context dedicated to database connection.
   */
  def fromDataSource[F[_]: Async](
    dataSource: DataSource,
    connectEC:  ExecutionContext
  ): LdbcDataSource[F] = Impl[F](dataSource, connectEC)

  /**
   * Construct a `DataSource` that wraps an existing `Connection`. Closing the connection is the responsibility of
   * the caller.
   *
   * @param connection
   *   a raw JDBC `Connection` to wrap
   */
  def fromConnection[F[_]: Sync](
    connection: java.sql.Connection
  ): LdbcDataSource[F] = JavaConnection[F](connection)

  /** Module of constructors for `DataSource` that use the JDBC `DriverManager` to allocate connections. Note that
   * `DriverManager` is unbounded and will happily allocate new connections until server resources are exhausted. It
   * is usually preferable to use `DataSourceTransactor` with an underlying bounded connection pool. Blocking operations on `DriverProvider` are
   * executed on an unbounded cached daemon thread pool by default, so you are also at risk of exhausting system
   * threads. TL;DR this is fine for console apps but don't use it for a web application.
   */
  def fromDriverManager[F[_]: Async]: Driver[F] = new Driver[F]
