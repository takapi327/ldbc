/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scala.concurrent.duration.*

import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.telemetry.*

class ReadTimeoutClosesConnectionTest extends FTestPlatform:
  given Tracer[Fx] = Tracer.noop[Fx]

  private def connection(readTimeout: Duration) =
    Connection[Fx](
      host        = TestConfig.host,
      port        = TestConfig.port,
      user        = TestConfig.user,
      password    = Some(TestConfig.password),
      database    = Some("connector_test"),
      readTimeout = readTimeout
    )

  test("a read timeout marks the connection closed"):
    assertFx(
      connection(500.millis).use { conn =>
        for
          outcome  <- conn.createStatement().flatMap(_.executeQuery("SELECT SLEEP(3)")).attempt
          isClosed <- conn.isClosed()
        yield (outcome.isLeft, isClosed)
      },
      (true, true)
    )

  test("statements on a timed-out connection fail fast instead of touching the network"):
    assertFxBoolean(
      connection(500.millis).use { conn =>
        for
          _      <- conn.createStatement().flatMap(_.executeQuery("SELECT SLEEP(3)")).attempt
          second <- conn.createStatement().flatMap(_.executeQuery("SELECT 1")).attempt
        yield second.left.exists(_.getMessage.contains("transport has failed"))
      },
      "故障後の文が fail-fast していない"
    )

  test("a healthy connection is not marked closed"):
    assertFx(
      connection(Duration.Inf).use { conn =>
        for
          _        <- conn.createStatement().flatMap(_.executeQuery("SELECT 1"))
          isClosed <- conn.isClosed()
        yield isClosed
      },
      false
    )
