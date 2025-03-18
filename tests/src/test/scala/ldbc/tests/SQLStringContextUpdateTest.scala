/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

//import cats.syntax.all.*

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.sql.*

import ldbc.dsl.io.*

import ldbc.connector.{ MySQLProvider as LdbcProvider, * }

import jdbc.connector.{ MySQLProvider as JdbcProvider, * }

class LdbcSQLStringContextUpdateTest extends SQLStringContextUpdateTest:
  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: Provider[IO] =
    LdbcProvider
      .default[IO]("127.0.0.1", 13306, "ldbc", "password", "connector_test")
      .setSSL(SSL.Trusted)

class JdbcSQLStringContextUpdateTest extends SQLStringContextUpdateTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")
  ds.setDatabaseName("connector_test")

  override def prefix: "jdbc" | "ldbc" = "jdbc"
  override def connection: Provider[IO] =
    JdbcProvider.fromDataSource(ds, ExecutionContexts.synchronous)

trait SQLStringContextUpdateTest extends CatsEffectSuite:

  def prefix: "jdbc" | "ldbc"

  def connection: Provider[IO]

  final val table = prefix match
    case "jdbc" => sc("`jdbc_sql_string_context_table`")
    case "ldbc" => sc("`ldbc_sql_string_context_table`")

  override def beforeAll(): Unit =
    connection
      .use { conn =>
        sql"CREATE TABLE $table (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `c1` VARCHAR(255) NOT NULL)".update
          .commit(conn)
      }
      .unsafeRunSync()

  override def afterAll(): Unit =
    connection
      .use { conn =>
        sql"DROP TABLE $table".update.commit(conn)
      }
      .unsafeRunSync()

  override def afterEach(context: AfterEach): Unit =
    connection
      .use { conn =>
        sql"TRUNCATE TABLE $table".update.commit(conn)
      }
      .unsafeRunSync()

  test("As a result of entering one case of data, there will be one affected row.") {
    assertIO(
      connection.use { conn =>
        sql"INSERT INTO $table (`c1`) VALUES ('value1')".update.commit(conn)
      },
      1
    )
  }

  test("As a result of entering data for two cases, there will be two affected rows.") {
    assertIO(
      connection.use { conn =>
        sql"INSERT INTO $table (`c1`) VALUES ('value1'),('value2')".update.commit(conn)
      },
      2
    )
  }

  test("The value generated when adding a record of AUTO_INCREMENT is returned.") {
    assertIO(
      connection.use { conn =>
        (for
          _         <- sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 1" })".update
          generated <- sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 2" })".returning[Long]
        yield generated).transaction(conn)
      },
      2L
    )
  }

  test("Not a single submission of result data rolled back in transaction has been reflected.　") {
    assertIO(
      connection.use { conn =>
        for
          result <-
            sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 1" })".update
              .flatMap(_ => sql"INSERT INTO $table (`id`, `xxx`) VALUES ($None, ${ "column 2" })".update)
              .transaction(conn)
              .attempt
          count <- sql"SELECT count(*) FROM $table".query[Int].unsafe.readOnly(conn)
        yield count
      },
      0
    )
  }
