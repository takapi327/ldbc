/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.io.PrintWriter

import cats.syntax.all.*

import cats.effect.Sync
import cats.effect.std.Console

import ldbc.sql.{ Connection, DataSource }
import ldbc.sql.logging.{LogEvent, LogHandler}

case class MysqlDataSource[F[_]: Sync](dataSource: javax.sql.DataSource, logHandler: LogHandler[F]) extends DataSource[F]:

  override def getConnection: F[Connection[F]] = Sync[F].blocking(dataSource.getConnection).map(conn => ConnectionImpl(conn, logHandler))

  override def getConnection(username: String, password: String): F[Connection[F]] =
    Sync[F].blocking(dataSource.getConnection(username, password)).map(conn => ConnectionImpl(conn, logHandler))

  override def getLogWriter: F[PrintWriter] = Sync[F].blocking(dataSource.getLogWriter)

  override def setLogWriter(out: PrintWriter): F[Unit] = Sync[F].blocking(dataSource.setLogWriter(out))

  override def setLoginTimeout(seconds: Int): F[Unit] = Sync[F].blocking(dataSource.setLoginTimeout(seconds))

  override def getLoginTimeout: F[Int] = Sync[F].blocking(dataSource.getLoginTimeout)

object MysqlDataSource:
  private def consoleLogger[F[_] : Console : Sync]: LogHandler[F] =
    case LogEvent.Success(sql, args) =>
      Console[F].println(
        s"""Successful Statement Execution:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      )
    case LogEvent.ProcessingFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed ResultSet Processing:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
    case LogEvent.ExecFailure(sql, args, failure) =>
      Console[F].errorln(
        s"""Failed Statement Execution:
           |  $sql
           |
           | arguments = [${args.mkString(",")}]
           |""".stripMargin
      ) >> Console[F].printStackTrace(failure)
      
  def apply[F[_]: Sync : Console](dataSource: javax.sql.DataSource): MysqlDataSource[F] =
    MysqlDataSource(dataSource, consoleLogger[F])
