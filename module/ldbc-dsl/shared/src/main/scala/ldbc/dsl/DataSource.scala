/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import java.io.PrintWriter

import cats.implicits.*
import cats.effect.Sync

import ldbc.sql.{ DataSource, Connection }
import ldbc.dsl.internal.*

object DataSource:

  def apply[F[_]: Sync](dataSource: javax.sql.DataSource): DataSource[F] = new DataSource[F]:
    override def getConnection: F[Connection[F]] = Sync[F].blocking(dataSource.getConnection).map(Connection[F](_))

    override def getConnection(username: String, password: String): F[Connection[F]] =
      Sync[F].blocking(dataSource.getConnection(username, password)).map(Connection[F](_))

    override def getLogWriter: F[PrintWriter] = Sync[F].blocking(dataSource.getLogWriter)

    override def setLogWriter(out: PrintWriter): F[Unit] = Sync[F].blocking(dataSource.setLogWriter(out))

    override def setLoginTimeout(seconds: Int): F[Unit] = Sync[F].blocking(dataSource.setLoginTimeout(seconds))

    override def getLoginTimeout: F[Int] = Sync[F].blocking(dataSource.getLoginTimeout)
