/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.catseffect.concurrentIO
import ldbc.net.SSL

/**
 * End-to-end proof that the MySQL driver runs **natively at `F = IO`** through the generic
 * [[ldbc.mysql.Connector]] against a real server — no `Fx` backend and no cross-effect bridge. Both a
 * plaintext and a TLS connection round-trip a `SELECT`, so the generic `IoEngine[IO]` (over the raw
 * engine) and `TlsUpgrade[IO]` (generic TLS) are exercised natively on `IO`. Requires the Docker MySQL at
 * 127.0.0.1:13306.
 */
class NativeIOIntegrationTest extends CatsEffectSuite:

  private val base: MySQLConfig =
    MySQLConfig.default
      .setHost("127.0.0.1")
      .setPort(13306)
      .setUser("ldbc")
      .setPassword("password")
      .setDatabase("connector_test")

  test("native IO over plaintext: SELECT round-trips") {
    val connector = Connector.fromConfig[IO](base.setSSL(SSL.None))
    assertIO(sql"SELECT 1".query[Int].to[Option].readOnly(connector), Some(1))
  }

  test("native IO over TLS: SELECT round-trips (generic TlsUpgrade[IO] + raw IoEngine[IO])") {
    val connector = Connector.fromConfig[IO](base.setSSL(SSL.Trusted))
    assertIO(sql"SELECT 42".query[Int].to[Option].readOnly(connector), Some(42))
  }
