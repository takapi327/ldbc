/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import java.sql.DriverManager

import cats.data.Kleisli
import cats.implicits.*

import cats.effect.{ Resource, Sync }
import cats.effect.kernel.Resource.ExitCase

import ldbc.core.{ Character, Collate, Table, Database as CoreDatabase }
import ldbc.sql.Connection

case class Database[F[_]: Sync](
  databaseType: CoreDatabase.Type,
  name:         String,
  host:         String,
  port:         Int,
  user:         Option[String]          = None,
  password:     Option[String]          = None,
  character:    Option[Character]       = None,
  collate:      Option[Collate[String]] = None,
  tables:       Set[Table[?]]           = Set.empty
) extends CoreDatabase:

  override val schema:     String         = name
  override val schemaMeta: Option[String] = None
  override val catalog:    Option[String] = None

  private def buildConnectionResource(acquire: F[Connection[F]]): Resource[F, Connection[F]] =
    val release: Connection[F] => F[Unit] = connection => connection.close()
    Resource.make(acquire)(release)

  private val jdbcUrl: String = s"jdbc:${ databaseType.name }://$host:$port/$name"

  private def getConnection: F[ConnectionIO[F]] =
    Sync[F]
      .blocking {
        Class.forName(databaseType.driver)
        (user, password) match
          case (Some(u), Some(p)) => DriverManager.getConnection(jdbcUrl, u, p)
          case _                  => DriverManager.getConnection(jdbcUrl)
      }
      .map(ConnectionIO[F])

  def readOnly[T](connectionKleisli: Kleisli[F, Connection[F], T]): F[T] =
    buildConnectionResource {
      for
        connection <- getConnection
        _          <- connection.setReadOnly(true)
      yield connection
    }
      .use(connectionKleisli.run)

  def autoCommit[T](connectionKleisli: Kleisli[F, Connection[F], T]): F[T] =
    buildConnectionResource {
      for
        connection <- getConnection
        _          <- connection.setReadOnly(false) >> connection.setAutoCommit(true)
      yield connection
    }
      .use(connectionKleisli.run)

  def transaction[T](connectionKleisli: Kleisli[F, Connection[F], T]): F[T] =
    (for
      connection <- buildConnectionResource {
                      for
                        connection <- getConnection
                        _          <- connection.setReadOnly(false) >> connection.setAutoCommit(false)
                      yield connection
                    }
      transact <- Resource.makeCase(Sync[F].pure(connection)) {
                    case (conn, ExitCase.Errored(e)) => conn.rollback() >> Sync[F].raiseError(e)
                    case (conn, _)                   => conn.commit()
                  }
    yield transact).use(connectionKleisli.run)

object Database:

  def mysql[F[_]: Sync](name: String, host: String, port: Int): Database[F] =
    Database[F](CoreDatabase.Type.MySQL, name, host, port)

  def mysql[F[_]: Sync](name: String, host: String, port: Int, user: String, password: String): Database[F] =
    Database[F](CoreDatabase.Type.MySQL, name, host, port, Some(user), Some(password))
