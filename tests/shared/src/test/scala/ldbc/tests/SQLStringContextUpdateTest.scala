/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.syntax.all.*

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.dsl.*

import ldbc.connector.*

import ldbc.fx.Fx

class LdbcSQLStringContextUpdateTest extends SQLStringContextUpdateTest[IO] with IODatabaseSuite:
  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: ConnectionFixture[IO] =
    ConnectionFixture(
      "connection",
      MySQLDataSource
        .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("connector_test")
        .setSSL(SSL.Trusted)
    )

class MysqlSQLStringContextUpdateTest extends SQLStringContextUpdateTest[IO] with IODatabaseSuite:
  import ldbc.catseffect.concurrentIO
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  override def prefix: "mysql" = "mysql"

  override def connection: ConnectionFixture[IO] =
    ConnectionFixture(
      "connection",
      MySQLDataSource
        .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("connector_test")
        .setSSL(MysqlSSL.Trusted)
    )

class MysqlFxSQLStringContextUpdateTest extends SQLStringContextUpdateTest[Fx] with FxDatabaseSuite:
  import ldbc.fx.concurrentFx
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  override def prefix: "mysql" = "mysql"

  override def connection: ConnectionFixture[Fx] =
    ConnectionFixture(
      "connection",
      MySQLDataSource
        .build[Fx](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("connector_test")
        .setSSL(MysqlSSL.Trusted)
    )

trait SQLStringContextUpdateTest[F[_]] extends DatabaseSuite[F]:

  def prefix: "jdbc" | "ldbc" | "mysql"

  def connection: ConnectionFixture[F]

  private lazy val connectionFixture = connection
    .withBeforeAll(conn =>
      sql"CREATE TABLE $table (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `c1` VARCHAR(255) NOT NULL)".update
        .commit(conn)
        .void
    )
    .withAfterAll(conn => sql"DROP TABLE $table".update.commit(conn).void)
    .withBeforeEach(conn => sql"TRUNCATE TABLE $table".update.commit(conn).void)
    .fixture

  lazy val table = ident(s"${ prefix }_${ effectLabel }_sql_string_context_table")

  override def munitFixtures = List(connectionFixture)

  test("As a result of entering one case of data, there will be one affected row.") {
    assertF(
      sql"INSERT INTO $table (`c1`) VALUES ('value1')".update.commit(connectionFixture()),
      1
    )
  }

  test("As a result of entering data for two cases, there will be two affected rows.") {
    assertF(
      sql"INSERT INTO $table (`c1`) VALUES ('value1'),('value2')".update.commit(connectionFixture()),
      2
    )
  }

  test("The value generated when adding a record of AUTO_INCREMENT is returned.") {
    assertF(
      (for
        _         <- sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 1" })".update
        generated <- sql"INSERT INTO $table (`id`, `c1`) VALUES ($None, ${ "column 2" })".returning[Long]
      yield generated).transaction(connectionFixture()),
      2L
    )
  }

  test("Not a single submission of result data rolled back in transaction has been reflected.") {
    assertF(
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
