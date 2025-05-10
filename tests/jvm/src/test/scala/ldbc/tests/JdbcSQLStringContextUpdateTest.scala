/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import munit.catseffect.IOFixture
import munit.catseffect.ResourceFixture.FixtureNotInstantiatedException

import ldbc.sql.*

import ldbc.dsl.*

import jdbc.connector.*

class JdbcSQLStringContextUpdateTest extends SQLStringContextUpdateTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")
  ds.setDatabaseName("connector_test")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: IOFixture[Connection[IO]] =
    new IOFixture[Connection[IO]]("connection"):
      @volatile private var value: Option[(Connection[IO], IO[Unit])] = None

      override def apply(): Connection[IO] = value match
        case Some(v) => v._1
        case None    => throw new FixtureNotInstantiatedException("connection")

      override def beforeAll(): IO[Unit] =
        ConnectionProvider
          .fromDataSource[IO](ds, ExecutionContexts.synchronous)
          .createConnection()
          .allocated
          .flatMap {
            case (conn, close) =>
              sql"CREATE TABLE $table (`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, `c1` VARCHAR(255) NOT NULL)".update
                .commit(conn) *>
                IO(this.value = Some((conn, close)))
          }

      override def afterAll(): IO[Unit] = value.fold(IO.unit) {
        case (conn, close) =>
          sql"DROP TABLE $table".update.commit(conn) *>
            close
      }

      override def afterEach(context: AfterEach): IO[Unit] = value.fold(IO.unit) {
        case (conn, _) => sql"TRUNCATE TABLE $table".update.commit(conn) *> IO.unit
      }
