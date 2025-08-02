/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*

import munit.*

import ldbc.dsl.*

import ldbc.schema.*

import ldbc.Connector
import ldbc.connector.*

class LdbcDDLTest extends DDLTest:

  private val datasource = MySQLDataSource
    .build[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)

  override def connector: Connector[IO] = Connector.fromDataSource(datasource)

trait DDLTest extends CatsEffectSuite:

  def connector: Connector[IO]

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
    ResourceSuiteLocalFixture(
      "Database Setup",
      Resource.make[IO, Unit](
        DBIO
          .sequence(
            userTable.schema.create,
            userTable.schema.createIfNotExists,
            userTable.schema.dropIfExists,
            userTable.schema.create
          )
          .commit(connector) *> IO.unit
      ) { _ =>
        DBIO.sequence(userTable.schema.drop).commit(connector) *> IO.unit
      }
    )
  )

  test("When the table is created, the number of records is 0.") {
    assertIO(
      userTable.select(_.id.count).query.unsafe.readOnly(connector),
      0
    )
  }

  test("Records can be inserted into tables created before the test begins.") {
    assertIO(
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
