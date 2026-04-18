/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.testkit.munit

import cats.effect.kernel.Resource
import cats.effect.IO

import munit.*

import ldbc.dsl.*

import ldbc.connector.{ Connector as LdbcConnector, MySQLDataSource, SSL }

import ldbc.testkit.RollbackHandler

class RollbackHandlerTest extends LdbcSuite:

  private val host = sys.env.getOrElse("MYSQL_HOST", "127.0.0.1")
  private val port = sys.env.getOrElse("MYSQL_PORT", "13306").toInt

  override def dataSource: MySQLDataSource[IO, Unit] =
    MySQLDataSource
      .build[IO](host, port, "ldbc")
      .setPassword("password")
      .setDatabase("connector_test")
      .setSSL(SSL.Trusted)

  private val tableFixture = ResourceSuiteLocalFixture(
    "table-setup",
    Resource.make(
      dataSource.getConnection.use { conn =>
        sql"CREATE TABLE ldbc_rollback_handler_test (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, value VARCHAR(255) NOT NULL)".update
          .commit(LdbcConnector.fromConnection(conn))
      }
    )(_ =>
      dataSource.getConnection.use { conn =>
        sql"DROP TABLE IF EXISTS ldbc_rollback_handler_test".update
          .commit(LdbcConnector.fromConnection(conn))
      }
    )
  )

  override def munitFixtures = List(tableFixture)

  test("changes are rolled back on resource release") {
    for
      _ <- RollbackHandler.resource[IO](dataSource).use { connector =>
             sql"INSERT INTO ldbc_rollback_handler_test (value) VALUES ('ephemeral')".update
               .commit(connector)
           }
      count <- dataSource.getConnection.use { conn =>
                 sql"SELECT COUNT(*) FROM ldbc_rollback_handler_test WHERE value = 'ephemeral'"
                   .query[Int]
                   .to[Option]
                   .readOnly(LdbcConnector.fromConnection(conn))
               }
    yield assertEquals(count, Some(0))
  }

  test("rollback occurs even when the resource body raises an error") {
    for
      _ <- RollbackHandler
             .resource[IO](dataSource)
             .use { connector =>
               sql"INSERT INTO ldbc_rollback_handler_test (value) VALUES ('ephemeral_error')".update
                 .commit(connector) *>
                 IO.raiseError(new RuntimeException("intentional error"))
             }
             .attempt
      count <- dataSource.getConnection.use { conn =>
                 sql"SELECT COUNT(*) FROM ldbc_rollback_handler_test WHERE value = 'ephemeral_error'"
                   .query[Int]
                   .to[Option]
                   .readOnly(LdbcConnector.fromConnection(conn))
               }
    yield assertEquals(count, Some(0))
  }

  ephemeralTest("data inserted in ephemeralTest is visible within the test body") { connector =>
    for
      _ <- sql"INSERT INTO ldbc_rollback_handler_test (value) VALUES ('visible_within')".update
             .commit(connector)
      count <- sql"SELECT COUNT(*) FROM ldbc_rollback_handler_test WHERE value = 'visible_within'"
                 .query[Int]
                 .to[Option]
                 .readOnly(connector)
    yield assertEquals(count, Some(1))
  }
