/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.Connection as SqlConnection

import ldbc.connector.*
import ldbc.connector.syntax.*

class PoolRecoveryTest extends FTestPlatform:

  override def munitIOTimeout: Duration = 60.seconds

  given Tracer[IO] = Tracer.noop[IO]

  private val connectionTimeout = 10.seconds

  private val config = MySQLConfig.default
    .setPort(TestConfig.port)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)
    .setMinConnections(0)
    .setMaxConnections(1)
    .setConnectionTimeout(connectionTimeout)

  private val admin = Connection[IO](
    host     = "127.0.0.1",
    port     = TestConfig.port,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.Trusted
  )

  private def connectionId(conn: SqlConnection[IO]): IO[String] =
    for
      statement <- conn.createStatement()
      resultSet <- statement.executeQuery("SELECT CONNECTION_ID()")
      _         <- resultSet.next()
      id        <- resultSet.getString(1)
      _         <- statement.close()
    yield id

  private def killOwnSession(conn: SqlConnection[IO]): IO[Unit] =
    connectionId(conn)
      .flatMap(id => admin.use(_.createStatement().flatMap(_.executeUpdate(s"KILL CONNECTION $id"))))
      .void

  test("a borrower is served without waiting for the timeout when the pool has room for another connection") {
    PooledDataSource.fromConfig[IO](config).use { datasource =>
      datasource.use(connectionId).timed.map { (elapsed, _) =>
        assert(elapsed < connectionTimeout / 2, s"acquiring from an empty pool took $elapsed")
      }
    }
  }

  test("a waiting borrower is served promptly when the connection in use is discarded") {
    PooledDataSource.fromConfig[IO](config).use { datasource =>
      for
        holding <- Deferred[IO, Unit]
        waiting <- datasource.use { conn =>
                     conn.setAutoCommit(false) *> holding.complete(()) *> IO.sleep(1.second) *> killOwnSession(conn)
                   }.start
        _        <- holding.get
        borrower <- (IO.sleep(200.millis) *> datasource.use(connectionId).timed).start
        _        <- waiting.joinWithNever
        result   <- borrower.joinWithNever
      yield
        val (elapsed, _) = result
        assert(elapsed < connectionTimeout / 2, s"the waiting borrower was served after $elapsed")
    }
  }
