/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.Connection as SqlConnection

import ldbc.connector.*
import ldbc.connector.syntax.*

class ConnectionStateResetTest extends FTestPlatform:

  override def munitIOTimeout: Duration = 60.seconds

  given Tracer[IO] = Tracer.noop[IO]

  private val config = MySQLConfig.default
    .setPort(TestConfig.port)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)
    .setMinConnections(1)
    .setMaxConnections(1)

  private def queryString(conn: SqlConnection[IO], sql: String, column: Int = 1): IO[String] =
    for
      statement <- conn.createStatement()
      resultSet <- statement.executeQuery(sql)
      _         <- resultSet.next()
      value     <- resultSet.getString(column)
      _         <- statement.close()
    yield value

  test("session state changed by one borrower is reset before the next borrower gets the connection") {
    PooledDataSource.fromConfig[IO](config).use { datasource =>
      for
        _ <- datasource.use { conn =>
               conn.setReadOnly(true) *>
                 conn.setTransactionIsolation(SqlConnection.TRANSACTION_SERIALIZABLE) *>
                 conn.setCatalog("world")
             }
        state <- datasource.use { conn =>
                   (
                     queryString(conn, "SELECT @@session.transaction_read_only"),
                     queryString(conn, "SELECT @@session.transaction_isolation"),
                     queryString(conn, "SELECT DATABASE()")
                   ).tupled
                 }
      yield assertEquals(state, ("0", "REPEATABLE-READ", "connector_test"))
    }
  }

  test("a healthy connection is handed to the next borrower after it is returned") {
    val connectionId = (conn: SqlConnection[IO]) => queryString(conn, "SELECT CONNECTION_ID()")
    PooledDataSource.fromConfig[IO](config).use { datasource =>
      for
        first  <- datasource.use(connectionId)
        second <- datasource.use(connectionId)
      yield assertEquals(second, first)
    }
  }

  test("an open transaction is rolled back when the connection is returned") {
    val database = "ldbc_tmp_connector_pool_rollback"
    val table    = s"$database.pending"
    val admin    = Connection[IO](
      host     = "127.0.0.1",
      port     = TestConfig.port,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    val setup = admin.use { conn =>
      conn.createStatement().flatMap { statement =>
        statement.executeUpdate(s"CREATE DATABASE IF NOT EXISTS $database") *>
          statement.executeUpdate(s"CREATE TABLE IF NOT EXISTS $table (id INT PRIMARY KEY) ENGINE=InnoDB") *>
          statement.executeUpdate(s"DELETE FROM $table")
      }
    }
    val teardown = admin.use(_.createStatement().flatMap(_.executeUpdate(s"DROP DATABASE IF EXISTS $database"))).void
    (setup *> PooledDataSource.fromConfig[IO](config).use { datasource =>
      for
        _ <- datasource.use { conn =>
               conn.setAutoCommit(false) *>
                 conn.createStatement().flatMap(_.executeUpdate(s"INSERT INTO $table (id) VALUES (1)"))
             }
        state <- datasource.use { conn =>
                   (
                     queryString(conn, s"SELECT COUNT(*) FROM $table"),
                     queryString(conn, "SELECT @@session.autocommit")
                   ).tupled
                 }
      yield assertEquals(state, ("0", "1"))
    }).guarantee(teardown)
  }

  test("a property changed several times by one borrower is restored to the value it had when borrowed") {
    PooledDataSource.fromConfig[IO](config).use { datasource =>
      for
        _ <- datasource.use { conn =>
               conn.setTransactionIsolation(SqlConnection.TRANSACTION_SERIALIZABLE) *>
                 conn.setTransactionIsolation(SqlConnection.TRANSACTION_READ_COMMITTED) *>
                 conn.setReadOnly(true) *>
                 conn.setReadOnly(false) *>
                 conn.setReadOnly(true) *>
                 conn.setCatalog("world") *>
                 conn.setCatalog("world2")
             }
        state <- datasource.use { conn =>
                   (
                     queryString(conn, "SELECT @@session.transaction_read_only"),
                     queryString(conn, "SELECT @@session.transaction_isolation"),
                     queryString(conn, "SELECT DATABASE()")
                   ).tupled
                 }
      yield assertEquals(state, ("0", "REPEATABLE-READ", "connector_test"))
    }
  }

  test("a connection whose reset fails is not handed to the next borrower") {
    val admin = Connection[IO](
      host     = "127.0.0.1",
      port     = TestConfig.port,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    PooledDataSource.fromConfig[IO](config).use { datasource =>
      for
        killed <- datasource.use { conn =>
                    for
                      _  <- conn.setAutoCommit(false)
                      id <- queryString(conn, "SELECT CONNECTION_ID()")
                      _  <- admin.use(_.createStatement().flatMap(_.executeUpdate(s"KILL CONNECTION $id")))
                    yield id
                  }
        next   <- datasource.use(conn => queryString(conn, "SELECT CONNECTION_ID()"))
        status <- datasource.status
      yield
        assertNotEquals(next, killed)
        assertEquals(status.total, 1, "the connection that failed to reset is still tracked by the pool")
    }
  }
