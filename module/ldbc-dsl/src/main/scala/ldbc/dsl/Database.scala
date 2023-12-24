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
import ldbc.sql.{ DataSource, Connection as BaseConnection }

case class Database[F[_]: Sync](
  databaseType: CoreDatabase.Type,
  name:         String,
  host:         String,
  port:         Option[Int],
  connectionF:  () => F[BaseConnection[F]],
  character:    Option[Character]       = None,
  collate:      Option[Collate[String]] = None,
  tables:       Set[Table[?]]           = Set.empty
) extends CoreDatabase:

  override val schema:     String         = name
  override val schemaMeta: Option[String] = None
  override val catalog:    Option[String] = None

  private def buildConnectionResource(acquire: F[BaseConnection[F]]): Resource[F, BaseConnection[F]] =
    val release: BaseConnection[F] => F[Unit] = connection => connection.close()
    Resource.make(acquire)(release)

  def setCharacter(character: Character):   Database[F] = this.copy(character = Some(character))
  def setCollate(collate: Collate[String]): Database[F] = this.copy(collate = Some(collate))
  def setTables(tables: Set[Table[?]]):     Database[F] = this.copy(tables = tables)

  /** Functions to manage the processing of connections independently.
    *
    * @param connectionKleisli
    *   A Kleisli function that receives a Connection.
    * @tparam T
    *   Type of data to be retrieved after database processing is executed.
    */
  def connection[T](connectionKleisli: BaseConnection[F] => F[T]): F[T] =
    buildConnectionResource(connectionF()).use(connection => connectionKleisli(connection))

  /** Functions for managing the processing of connections in a read-only manner.
    *
    * @param connectionKleisli
    *   A Kleisli function that receives a Connection.
    * @tparam T
    *   Type of data to be retrieved after database processing is executed.
    */
  def readOnly[T](connectionKleisli: Kleisli[F, BaseConnection[F], T]): F[T] =
    connection(connection => connection.setReadOnly(true) >> connectionKleisli.run(connection))

  /** Functions to manage the processing of connections for writing.
    *
    * @param connectionKleisli
    *   A Kleisli function that receives a Connection.
    * @tparam T
    *   Type of data to be retrieved after database processing is executed.
    */
  def autoCommit[T](connectionKleisli: Kleisli[F, BaseConnection[F], T]): F[T] =
    connection(connection =>
      connection.setReadOnly(false) >> connection.setAutoCommit(true) >> connectionKleisli.run(connection)
    )

  /** Functions to manage the processing of connections in a transaction.
    *
    * @param connectionKleisli
    *   A Kleisli function that receives a Connection.
    * @tparam T
    *   Type of data to be retrieved after database processing is executed.
    */
  def transaction[T](connectionKleisli: Kleisli[F, BaseConnection[F], T]): F[T] =
    connection(connection =>
      connection.setReadOnly(false) >> connection.setAutoCommit(false) >> Resource
        .makeCase(Sync[F].pure(connection)) {
          case (conn, ExitCase.Errored(e)) => conn.rollback() >> Sync[F].raiseError(e)
          case (conn, _)                   => conn.commit()
        }
        .use(connectionKleisli.run)
    )

  /** Functions to manage the processing of connections, always rolling back.
    *
    * @param connectionKleisli
    *   A Kleisli function that receives a Connection.
    * @tparam T
    *   Type of data to be retrieved after database processing is executed.
    */
  def rollback[T](connectionKleisli: Kleisli[F, BaseConnection[F], T]): F[T] =
    connection(connection =>
      connection.setAutoCommit(false) >> connectionKleisli.run(connection) <* connection.rollback()
    )

object Database:

  def fromDriverManager[F[_]: Sync](
    databaseType: CoreDatabase.Type,
    name:         String,
    host:         String,
    port:         Option[Int],
    user:         Option[String],
    password:     Option[String]
  ): Database[F] =
    val jdbcUrl: String = port match
      case Some(p) => s"jdbc:${ databaseType.name }://$host:$p/$name"
      case None    => s"jdbc:${ databaseType.name }://$host/$name"

    val connection: F[BaseConnection[F]] =
      Sync[F]
        .blocking {
          Class.forName(databaseType.driver)
          (user, password) match
            case (Some(u), Some(p)) => DriverManager.getConnection(jdbcUrl, u, p)
            case _                  => DriverManager.getConnection(jdbcUrl)
        }
        .map(Connection[F])

    Database[F](databaseType, name, host, port, () => connection)

  def fromMySQLDriver[F[_]: Sync](name: String, host: String, port: Int): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.MySQL, name, host, Some(port), None, None)

  def fromMySQLDriver[F[_]: Sync](name: String, host: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.MySQL, name, host, None, None, None)

  def fromMySQLDriver[F[_]: Sync](name: String, host: String, port: Int, user: String, password: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.MySQL, name, host, Some(port), Some(user), Some(password))

  def fromMySQLDriver[F[_]: Sync](name: String, host: String, user: String, password: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.MySQL, name, host, None, Some(user), Some(password))

  def fromAwsDriver[F[_]: Sync](name: String, host: String, port: Int): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.AWSMySQL, name, host, Some(port), None, None)

  def fromAwsDriver[F[_]: Sync](name: String, host: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.AWSMySQL, name, host, None, None, None)

  def fromAwsDriver[F[_]: Sync](name: String, host: String, port: Int, user: String, password: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.AWSMySQL, name, host, Some(port), Some(user), Some(password))

  def fromAwsDriver[F[_]: Sync](name: String, host: String, user: String, password: String): Database[F] =
    fromDriverManager[F](CoreDatabase.Type.AWSMySQL, name, host, None, Some(user), Some(password))

  def fromDataSource[F[_]: Sync](
    databaseType: CoreDatabase.Type,
    name:         String,
    host:         String,
    port:         Option[Int],
    dataSource:   DataSource[F]
  ): Database[F] =
    Database[F](databaseType, name, host, port, () => dataSource.getConnection)

  def fromMySQLDataSource[F[_]: Sync](name: String, host: String, port: Int, dataSource: DataSource[F]): Database[F] =
    fromDataSource[F](CoreDatabase.Type.MySQL, name, host, Some(port), dataSource)

  def fromMySQLDataSource[F[_]: Sync](name: String, host: String, dataSource: DataSource[F]): Database[F] =
    fromDataSource[F](CoreDatabase.Type.MySQL, name, host, None, dataSource)

  def fromAwsDataSource[F[_]: Sync](name: String, host: String, port: Int, dataSource: DataSource[F]): Database[F] =
    fromDataSource[F](CoreDatabase.Type.AWSMySQL, name, host, Some(port), dataSource)

  def fromAwsDataSource[F[_]: Sync](name: String, host: String, dataSource: DataSource[F]): Database[F] =
    fromDataSource[F](CoreDatabase.Type.AWSMySQL, name, host, None, dataSource)
