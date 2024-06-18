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
import ldbc.connector.SSL
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.Table
import ldbc.query.builder.syntax.io.*

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

  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]

  def connection: Resource[IO, Connection[IO]]

  final case class User(
    id:   Long,
    name: String,
    age:  Option[Int]
  ) derives Table

  final val tableQuery = Table[User]

  override def beforeAll(): Unit =
    connection
      .use { conn =>
        sql"""
          CREATE TABLE `user` (
            `id` BIGINT NOT NULL AUTO_INCREMENT,
            `name` VARCHAR(255) NOT NULL,
            `age` INT,
            PRIMARY KEY (`id`)
          )
        """.update.commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  override def afterAll(): Unit =
    connection
      .use { conn =>
        tableQuery.dropTable.update.commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  override def afterEach(context: AfterEach): Unit =
    connection
      .use { conn =>
        tableQuery.truncateTable.update.commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  test("When the table is created, the number of records is 0.") {
    assertIO(
      connection.use { conn =>
        tableQuery.select(_.id.count).query.unsafe.readOnly(conn).map(_._1)
      },
      0
    )
  }

  test("Records can be inserted into tables created before the test begins.") {
    assertIO(
      connection.use { conn =>
        tableQuery
          .insertInto(user => (user.name, user.age))
          .values(
            List(
              ("Alice", Some(20)),
              ("Bob", Some(25)),
              ("Charlie", None)
            )
          )
          .update
          .commit(conn)
      },
      3
    )
  }
