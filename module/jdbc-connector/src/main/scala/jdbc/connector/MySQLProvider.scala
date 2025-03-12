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

trait MySQLProvider[F[_]] extends Provider[F]

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

  private case class Impl[F[_]](
    logHandler: LogHandler[F]
  ) extends MySQLProvider[F]:
    override def use[A](f: Connection[F] => F[A]): F[A] = ???

  private case class DataSourceProvider[F[_]](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: LogHandler[F]
  )(using ev: Async[F])
    extends MySQLProvider[F]:
    override def use[A](f: Connection[F] => F[A]): F[A] =
      val connect = Resource.fromAutoCloseable(ev.evalOn(ev.delay(dataSource.getConnection()), connectEC))
      connect.use(conn => f(ConnectionImpl(conn, logHandler)))

  private case class ConnectionProvider[F[_]: Sync](
    connection: java.sql.Connection,
    logHandler: LogHandler[F]
  ) extends MySQLProvider[F]:
    override def use[A](f: Connection[F] => F[A]): F[A] =
      Resource
        .pure(ConnectionImpl(connection, logHandler))
        .use(f)

  class DriverProvider[F[_]: Console](using ev: Async[F]):

    private def create(
      driver:      String,
      conn:        () => java.sql.Connection,
      _logHandler: LogHandler[F]
    ): MySQLProvider[F] =
      new MySQLProvider[F]:
        override def logHandler: LogHandler[F] = _logHandler
        override def use[A](f: Connection[F] => F[A]): F[A] =
          val connect = Resource.fromAutoCloseable(ev.blocking {
            Class.forName(driver)
            conn()
          })
          connect.use(conn => f(ConnectionImpl(conn, logHandler)))

    def apply(
      driver:     String,
      url:        String,
      logHandler: Option[LogHandler[F]]
    ): MySQLProvider[F] =
      create(driver, () => DriverManager.getConnection(url), logHandler.getOrElse(consoleLogger))

    def apply(
      driver:     String,
      url:        String,
      user:       String,
      password:   String,
      logHandler: Option[LogHandler[F]]
    ): MySQLProvider[F] =
      create(driver, () => DriverManager.getConnection(url, user, password), logHandler.getOrElse(consoleLogger))

    def apply(
      driver:     String,
      url:        String,
      info:       java.util.Properties,
      logHandler: Option[LogHandler[F]]
    ): MySQLProvider[F] =
      create(driver, () => DriverManager.getConnection(url, info), logHandler.getOrElse(consoleLogger))

  def default[F[_]](logHandler: LogHandler[F]): MySQLProvider[F] = Impl(logHandler)

  def fromDataSource[F[_]: Console: Async](
    dataSource: DataSource,
    connectEC:  ExecutionContext,
    logHandler: Option[LogHandler[F]] = None
  ): MySQLProvider[F] = DataSourceProvider(dataSource, connectEC, logHandler.getOrElse(consoleLogger))

  def fromConnection[F[_]: Console: Sync](
    connection: java.sql.Connection,
    logHandler: Option[LogHandler[F]] = None
  ): MySQLProvider[F] = ConnectionProvider(connection, logHandler.getOrElse(consoleLogger))

  def fromDriverManager[F[_]: Console: Async]: DriverProvider[F] = new DriverProvider[F]
