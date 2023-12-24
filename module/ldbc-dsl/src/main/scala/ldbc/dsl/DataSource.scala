/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import cats.implicits.*
import cats.effect.Sync
import ldbc.sql.{ DataSource, Connection as BaseConnection }

import java.io.PrintWriter

object DataSource:

  def apply[F[_]: Sync](dataSource: javax.sql.DataSource): DataSource[F] = new DataSource[F]:
    override def getConnection: F[BaseConnection[F]] = Sync[F].blocking(dataSource.getConnection).map(Connection[F](_))

    override def getConnection(username: String, password: String): F[BaseConnection[F]] =
      Sync[F].blocking(dataSource.getConnection(username, password)).map(Connection[F](_))

    override def getLogWriter: F[PrintWriter] = Sync[F].blocking(dataSource.getLogWriter)

    override def setLogWriter(out: PrintWriter): F[Unit] = Sync[F].blocking(dataSource.setLogWriter(out))

    override def setLoginTimeout(seconds: Int): F[Unit] = Sync[F].blocking(dataSource.setLoginTimeout(seconds))

    override def getLoginTimeout: F[Int] = Sync[F].blocking(dataSource.getLoginTimeout)
