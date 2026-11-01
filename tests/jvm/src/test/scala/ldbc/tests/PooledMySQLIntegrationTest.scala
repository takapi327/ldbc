/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.catseffect.concurrentIO
import ldbc.catseffect.toIOResource
import ldbc.catseffect.Connector
import ldbc.mysql.{ MySQLConfig, MySQLDataSource }
import ldbc.net.SSL
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }

/**
 * End-to-end proof that an externally-built [[ldbc.pool.PooledDataSource]] drives the MySQL driver
 * natively at `F = IO`: the pool is created from a `MySQLDataSource[IO]` and handed to
 * [[ldbc.catseffect.Connector.fromDataSource]], then a `SELECT` round-trips through a borrowed connection.
 * This is the pool ⇄ driver integration point — the driver itself never creates a pool. Requires the
 * Docker MySQL at 127.0.0.1:13306.
 */
class PooledMySQLIntegrationTest extends CatsEffectSuite:

  private val config: MySQLConfig =
    MySQLConfig.default
      .setHost("127.0.0.1")
      .setPort(13306)
      .setUser("ldbc")
      .setPassword("password")
      .setDatabase("connector_test")
      .setSSL(SSL.Trusted)

  test("native IO: SELECT round-trips through an externally-built Concurrent[IO] pool") {
    val poolConfig = ConnectionPoolConfig(minConnections = 1, maxConnections = 2)
    val pool       = PooledDataSource.fromDataSource[IO](poolConfig, MySQLDataSource.fromConfig[IO](config))
    val program    = toIOResource(pool).use { ds =>
      sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(ds))
    }
    assertIO(program, Some(7))
  }

  test("session state changed by one borrower is reset before the next borrower gets the connection") {
    val poolConfig = ConnectionPoolConfig(minConnections = 1, maxConnections = 1)
    val pool       = PooledDataSource.fromDataSource[IO](poolConfig, MySQLDataSource.fromConfig[IO](config))
    val program    = toIOResource(pool).use { ds =>
      for
        first <- ds.getConnection
        (dirty, release1) = first
        _      <- dirty.setReadOnly(true)
        _      <- dirty.setTransactionIsolation(ldbc.sql.Connection.TRANSACTION_SERIALIZABLE)
        _      <- dirty.setCatalog("world")
        _      <- release1
        second <- ds.getConnection
        (clean, release2) = second
        resultSet <-
          clean
            .createStatement()
            .flatMap(
              _.executeQuery("SELECT @@session.transaction_read_only, @@session.transaction_isolation, DATABASE()")
            )
        _         <- resultSet.next()
        readOnly  <- resultSet.getString(1)
        isolation <- resultSet.getString(2)
        database  <- resultSet.getString(3)
        _         <- release2
      yield (readOnly, isolation, database)
    }
    assertIO(program, ("0", "REPEATABLE-READ", "connector_test"))
  }
