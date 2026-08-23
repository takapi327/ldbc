/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.net.SSL

import ldbc.catseffect.concurrentIO
import ldbc.catseffect.toIOResource

import ldbc.mysql.{ Connector, MySQLConfig, MySQLDataSource }

/**
 * End-to-end proof that an externally-built [[PooledDataSource]] drives the MySQL driver natively at
 * `F = IO`: the pool is created from a `MySQLDataSource[IO]` and handed to
 * [[ldbc.mysql.Connector.fromDataSource]], then a `SELECT` round-trips through a borrowed connection.
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
    val program = toIOResource(pool).use { ds =>
      sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(ds))
    }
    assertIO(program, Some(7))
  }
