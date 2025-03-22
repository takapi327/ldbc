/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import munit.*

import ldbc.sql.*

import ldbc.dsl.*

import ldbc.schema.*

import ldbc.connector.{ ConnectionProvider as LdbcProvider, * }

import jdbc.connector.{ ConnectionProvider as JdbcProvider, * }

class LdbcDDLTest extends DDLTest:

  override def connection: Provider[IO] =
    LdbcProvider
      .default[IO]("127.0.0.1", 13306, "ldbc", "password", "connector_test")
      .setSSL(SSL.Trusted)

class JdbcDDLTest extends DDLTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("connector_test")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def connection: Provider[IO] =
    JdbcProvider.fromDataSource(ds, ExecutionContexts.synchronous)

trait DDLTest extends CatsEffectSuite:

  def connection: Provider[IO]

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

  override def beforeAll(): Unit =
    connection
      .use { conn =>
        // The following implementation is used to test the possibility of multiple executions.
        DBIO
          .sequence(
            userTable.schema.create,
            userTable.schema.createIfNotExists,
            userTable.schema.dropIfExists,
            userTable.schema.create
          )
          .commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  override def afterAll(): Unit =
    connection
      .use { conn =>
        DBIO.sequence(userTable.schema.drop).commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  override def afterEach(context: AfterEach): Unit =
    connection
      .use { conn =>
        DBIO.sequence(userTable.schema.truncate).commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  test("When the table is created, the number of records is 0.") {
    assertIO(
      connection.use { conn =>
        userTable.select(_.id.count).query.unsafe.readOnly(conn)
      },
      0
    )
  }

  test("Records can be inserted into tables created before the test begins.") {
    assertIO(
      connection.use { conn =>
        userTable
          .insertInto(user => user.name *: user.age)
          .values(
            ("Alice", Some(20)),
            ("Bob", Some(25)),
            ("Charlie", None)
          )
          .update
          .commit(conn)
      },
      3
    )
  }
