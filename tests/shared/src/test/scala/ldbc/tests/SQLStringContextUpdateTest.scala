/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.dsl.*

import ldbc.connector.*

class LdbcSQLStringContextUpdateTest extends SQLStringContextUpdateTest:
  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: ConnectionFixture =
    ConnectionFixture(
      "connection",
      MySQLDataSource
        .build[IO]("127.0.0.1", 13306, "ldbc")
        .setPassword("password")
        .setDatabase("connector_test")
        .setSSL(SSL.Trusted)
    )

trait SQLStringContextUpdateTest extends CatsEffectSuite:

  def prefix: "jdbc" | "ldbc"

  def connection: ConnectionFixture

  private lazy val connectionFixture = connection
    .withBeforeAll(conn =>
      sql"CREATE TABLE $table (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `c1` VARCHAR(255) NOT NULL)".update
        .commit(conn) *> IO.unit
    )
    .withAfterAll(conn => sql"DROP TABLE $table".update.commit(conn) *> IO.unit)
    .withBeforeEach(conn => sql"TRUNCATE TABLE $table".update.commit(conn) *> IO.unit)
    .fixture

  final val table = prefix match
    case "jdbc" => sc("`jdbc_sql_string_context_table`")
    case "ldbc" => sc("`ldbc_sql_string_context_table`")

  override def munitFixtures = List(connectionFixture)

  test("As a result of entering one case of data, there will be one affected row.") {
    assertIO(
      sql"INSERT INTO $table (`c1`) VALUES ('value1')".update.commit(connectionFixture()),
      1
    )
  }

  test("As a result of entering data for two cases, there will be two affected rows.") {
    assertIO(
      sql"INSERT INTO $table (`c1`) VALUES ('value1'),('value2')".update.commit(connectionFixture()),
      2
    )
  }

  test("The value generated when adding a record of AUTO_INCREMENT is returned.") {
    assertIO(
      (for
        _         <- sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 1" })".update
        generated <- sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 2" })".returning[Long]
      yield generated).transaction(connectionFixture()),
      2L
    )
  }

  test("Not a single submission of result data rolled back in transaction has been reflected.") {
    assertIO(
      for
        _ <-
          sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 1" })".update
            .flatMap(_ => sql"INSERT INTO $table (`id`, `xxx`) VALUES ($None, ${ "column 2" })".update)
            .transaction(connectionFixture())
            .attempt
        count <- sql"SELECT count(*) FROM $table".query[Int].unsafe.readOnly(connectionFixture())
      yield count,
      0
    )
  }
