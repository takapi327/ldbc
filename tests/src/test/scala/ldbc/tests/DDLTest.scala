/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.sql.Connection

import ldbc.schema.{ Key, Table, TableQuery }
import ldbc.schema.syntax.io.*

import ldbc.connector.SSL

class LdbcDDLTest extends DDLTest:

  override def connection: Resource[IO, Connection[IO]] =
    ldbc.connector.Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

class JdbcDDLTest extends DDLTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("connector_test")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

trait DDLTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  def connection: Resource[IO, Connection[IO]]

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
