/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.sql.DriverManager

import javax.sql.DataSource

import scala.concurrent.ExecutionContext

import cats.Applicative

import cats.effect.*

import ldbc.sql.*

import ldbc.*
import ldbc.free.*
import ldbc.logging.{ LogEvent, LogHandler }

object Connector:

  private def noopLogger[F[_]: Applicative]: LogHandler[F] = (_: LogEvent) => Applicative[F].unit

  private case class Ldbc[F[_]: Sync](
    logHandler: LogHandler[F],
    connection: Connection[F]
  ) extends Connector[F]:

    private val interpreter: Interpreter[F] = new KleisliInterpreter[F](logHandler)

    override def run[A](dbio: DBIO[A]): F[A] = dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  private case class Impl[F[_]](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: Option[LogHandler[F]]
  )(using ev: Async[F])
    extends Connector[F]:

    private val interpreter: Interpreter[F] = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger[F]))

    private val connection: Resource[F, Connection[F]] =
      Resource
        .fromAutoCloseable(ev.evalOn(ev.delay(dataSource.getConnection()), connectEC))
        .map(conn => ConnectionImpl(conn))

    override def run[A](dbio: DBIO[A]): F[A] = connection.use { conn =>
      dbio.foldMap(interpreter.ConnectionInterpreter).run(conn)
    }

  private case class JavaConnection[F[_]: Sync](
    connection: java.sql.Connection,
    logHandler: Option[LogHandler[F]]
  ) extends Connector[F]:
    private val interpreter: Interpreter[F] = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger[F]))

    private val connectionF: Resource[F, Connection[F]] =
      Resource.pure(ConnectionImpl(connection))

    override def run[A](dbio: DBIO[A]): F[A] = connectionF.use { conn =>
      dbio.foldMap(interpreter.ConnectionInterpreter).run(conn)
    }

  class Driver[F[_]](using ev: Async[F]):

    private def create(
      driver:     String,
      conn:       () => java.sql.Connection,
      logHandler: Option[LogHandler[F]]
    ): Connector[F] =
      new Connector[F]:
        private val interpreter: Interpreter[F] = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger[F]))

        private val connection: Resource[F, Connection[F]] =
          Resource
            .fromAutoCloseable(ev.blocking {
              Class.forName(driver)
              conn()
            })
            .map(conn => ConnectionImpl(conn))

        override def run[A](dbio: DBIO[A]): F[A] = connection.use { conn =>
          dbio.foldMap(interpreter.ConnectionInterpreter).run(conn)
        }

    /** Construct a new `Connector` that uses the JDBC `DriverManager` to allocate connections.
     *
     * @param driver
     *   the class name of the JDBC driver, like "com.mysql.cj.jdbc.MySQLDriver"
     * @param url
     *   a connection URL, specific to your driver
     */
    def apply(
      driver:     String,
      url:        String,
      logHandler: Option[LogHandler[F]]
    ): Connector[F] =
      create(driver, () => DriverManager.getConnection(url), logHandler)

    /** Construct a new `Connector` that uses the JDBC `DriverManager` to allocate connections.
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
      password:   String,
      logHandler: Option[LogHandler[F]]
    ): Connector[F] =
      create(driver, () => DriverManager.getConnection(url, user, password), logHandler)

    /** Construct a new `Connector` that uses the JDBC `DriverManager` to allocate connections.
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
      info:       java.util.Properties,
      logHandler: Option[LogHandler[F]]
    ): Connector[F] =
      create(driver, () => DriverManager.getConnection(url, info), logHandler)

  /**
   *  Construct a constructor of `Connector[F]` for some `D <: DataSource` When calling this constructor you
   * should explicitly supply the effect type M e.g. ConnectionProvider.fromDataSource[IO](myDataSource, connectEC)
   *
   * @param dataSource
   *   A data source that manages connection information to MySQL.
   * @param connectEC
   *   Execution context dedicated to database connection.
   */
  def fromDataSource[F[_]: Async](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: Option[LogHandler[F]] = None
  ): Connector[F] = Impl[F](dataSource, connectEC, logHandler)

  /**
   * Construct a `DataSource` that wraps an existing `Connection`. Closing the connection is the responsibility of
   * the caller.
   *
   * @param connection
   *   a raw JDBC `Connection` to wrap
   */
  def fromConnection[F[_]: Sync](
    connection: java.sql.Connection | Connection[F],
    logHandler: Option[LogHandler[F]] = None
  ): Connector[F] =
    connection match
      case conn: java.sql.Connection =>
        JavaConnection[F](conn, logHandler)
      case conn: Connection[?] =>
        fromConnection[F](conn, logHandler)

  /** Module of constructors for `Connector` that use the JDBC `DriverManager` to allocate connections. Note that
   * `DriverManager` is unbounded and will happily allocate new connections until server resources are exhausted. It
   * is usually preferable to use `DataSourceTransactor` with an underlying bounded connection pool. Blocking operations on `DriverProvider` are
   * executed on an unbounded cached daemon thread pool by default, so you are also at risk of exhausting system
   * threads. TL;DR this is fine for console apps but don't use it for a web application.
   */
  def fromDriverManager[F[_]: Async]: Driver[F] = new Driver[F]

  def fromConnection[F[_]: Sync](connection: Connection[F]): Connector[F] =
    Ldbc[F](
      logHandler = noopLogger[F],
      connection = connection
    )
