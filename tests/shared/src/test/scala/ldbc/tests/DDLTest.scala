/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.Future

import cats.syntax.all.*

import cats.effect.*

import munit.*

import ldbc.dsl.*

import ldbc.schema.*

import ldbc.connector.*

import ldbc.fx.Fx
import ldbc.Connector

class LdbcDDLTest extends DDLTest[IO] with IODatabaseSuite:

  private val datasource = MySQLDataSource
    .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
    .setPassword(MySQLTestConfig.password)
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)

  override def connector: Connector[IO] = Connector.fromDataSource(datasource)

class MysqlDDLTest extends DDLTest[IO] with IODatabaseSuite:
  import ldbc.catseffect.concurrentIO
  import ldbc.mysql.Connector as MysqlConnector
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  private val datasource = MySQLDataSource
    .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
    .setPassword(MySQLTestConfig.password)
    .setDatabase("connector_test")
    .setSSL(MysqlSSL.Trusted)

  override def connector: Connector[IO] = MysqlConnector.fromDataSource(datasource)

class MysqlFxDDLTest extends DDLTest[Fx] with FxDatabaseSuite:
  import ldbc.fx.concurrentFx
  import ldbc.mysql.{ Connector as MysqlConnector, MySQLDataSource }
  import ldbc.net.SSL as MysqlSSL

  override def connector: Connector[Fx] =
    MysqlConnector.fromDataSource(
      MySQLDataSource
        .build[Fx](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("connector_test")
        .setSSL(MysqlSSL.Trusted)
    )

class MysqlFutureDDLTest extends DDLTest[Future] with FutureDatabaseSuite:
  import ldbc.fx.concurrentFx
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  override def connector: Connector[Future] =
    ldbc.future.Connector.fromDataSource(
      MySQLDataSource
        .build[Fx](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("connector_test")
        .setSSL(MysqlSSL.Trusted)
    )

trait DDLTest[F[_]] extends DatabaseSuite[F]:

  def connector: Connector[F]

  final case class User(
    id:   Long,
    name: String,
    age:  Option[Int]
  )

  class UserTable extends Table[User]("user"):
    def id:   Column[Long]        = bigint().autoIncrement
    def name: Column[String]      = varchar(255)
    def age:  Column[Option[Int]] = int()

    override def keys: List[Key] = List(PRIMARY_KEY(id))

    def * : Column[User] = (id *: name *: age).to[User]

  final val userTable = TableQuery[UserTable]

  override def munitFixtures = List(
    suiteFixture(
      "Database Setup",
      DBIO
        .sequence(
          userTable.schema.create,
          userTable.schema.createIfNotExists,
          userTable.schema.dropIfExists,
          userTable.schema.create
        )
        .commit(connector)
        .void,
      DBIO.sequence(userTable.schema.drop).commit(connector).void
    )
  )

  test("When the table is created, the number of records is 0.") {
    assertF(
      userTable.select(_.id.count).query.unsafe.readOnly(connector),
      0
    )
  }

  test("Records can be inserted into tables created before the test begins.") {
    assertF(
      userTable
        .insertInto(user => user.name *: user.age)
        .values(
          ("Alice", Some(20)),
          ("Bob", Some(25)),
          ("Charlie", None)
        )
        .update
        .commit(connector),
      3
    )
  }
