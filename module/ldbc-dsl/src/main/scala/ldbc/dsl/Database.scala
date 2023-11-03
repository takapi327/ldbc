/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import java.sql.DriverManager

import javax.sql.DataSource

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
  connectionF:  () => F[ConnectionIO[F]],
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

  def setCharacter(character: Character):   Database[F] = this.copy(character = Some(character))
  def setCollate(collate: Collate[String]): Database[F] = this.copy(collate = Some(collate))
  def setTables(tables: Set[Table[?]]):     Database[F] = this.copy(tables = tables)

  def readOnly[T](connectionKleisli: Kleisli[F, Connection[F], T]): F[T] =
    buildConnectionResource {
      for
        connection <- connectionF()
        _          <- connection.setReadOnly(true)
      yield connection
    }
      .use(connectionKleisli.run)

  def autoCommit[T](connectionKleisli: Kleisli[F, Connection[F], T]): F[T] =
    buildConnectionResource {
      for
        connection <- connectionF()
        _          <- connection.setReadOnly(false) >> connection.setAutoCommit(true)
      yield connection
    }
      .use(connectionKleisli.run)

  def transaction[T](connectionKleisli: Kleisli[F, Connection[F], T]): F[T] =
    (for
      connection <- buildConnectionResource {
                      for
                        connection <- connectionF()
                        _          <- connection.setReadOnly(false) >> connection.setAutoCommit(false)
                      yield connection
                    }
      transact <- Resource.makeCase(Sync[F].pure(connection)) {
                    case (conn, ExitCase.Errored(e)) => conn.rollback() >> Sync[F].raiseError(e)
                    case (conn, _)                   => conn.commit()
                  }
    yield transact).use(connectionKleisli.run)

object Database:

  def fromDriverManager[F[_]: Sync](
    databaseType: CoreDatabase.Type,
    name: String,
    host: String,
    port: Int,
    user: Option[String] = None,
    password: Option[String] = None,
  ): Database[F] =
    val jdbcUrl: String = s"jdbc:${databaseType.name}://$host:$port/$name"

    val connection: F[ConnectionIO[F]] =
      Sync[F]
        .blocking {
          Class.forName(databaseType.driver)
          (user, password) match
            case (Some(u), Some(p)) => DriverManager.getConnection(jdbcUrl, u, p)
            case _ => DriverManager.getConnection(jdbcUrl)
        }
        .map(ConnectionIO[F])

    Database[F](databaseType, name, host, port, () => connection)

  def mysqlDriver[F[_]: Sync](name: String, host: String, port: Int): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.MySQL, name, host, port)

  def mysqlDriver[F[_]: Sync](name: String, host: String, port: Int, user: String, password: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.MySQL, name, host, port, Some(user), Some(password))

  def awsDriver[F[_]: Sync](name: String, host: String, port: Int): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.AWSMySQL, name, host, port)

  def awsDriver[F[_]: Sync](name: String, host: String, port: Int, user: String, password: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.AWSMySQL, name, host, port, Some(user), Some(password))

  def fromDataSource[F[_]: Sync](
    databaseType: CoreDatabase.Type,
    name: String,
    host: String,
    port: Int,
    dataSource: DataSource
  ): Database[F] =
    val connection: F[ConnectionIO[F]] = Sync[F].blocking(dataSource.getConnection).map(ConnectionIO[F])
    Database[F](databaseType, name, host, port, () => connection)

  def mysqlDataSource[F[_] : Sync](name: String, host: String, port: Int, dataSource: DataSource): Database[F] =
    fromDataSource[F](CoreDatabase.Type.MySQL, name, host, port, dataSource)

  def awsDataSource[F[_] : Sync](name: String, host: String, port: Int, dataSource: DataSource): Database[F] =
    fromDataSource[F](CoreDatabase.Type.AWSMySQL, name, host, port, dataSource)
