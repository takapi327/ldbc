/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scala.concurrent.duration.*

import ldbc.fx.{ Fx, FxSuite }
import ldbc.fx.concurrentFx
import ldbc.net.ConnectTimeoutException
import ldbc.telemetry.*

class ConnectTimeoutTest extends FxSuite:

  given Tracer[Fx] = Tracer.noop[Fx]

  private val blackhole = "198.51.100.1"

  test("a connect that cannot complete gives up after the configured budget"):
    val connection = Connection[Fx](
      host           = blackhole,
      port           = 3306,
      user           = "root",
      connectTimeout = 1.second
    )
    for
      started <- Fx.delay(System.nanoTime())
      _       <- interceptFx[ConnectTimeoutException](connection.use(_ => Fx.unit))
      elapsed <- Fx.delay((System.nanoTime() - started) / 1000000)
    yield
      assert(
        elapsed < 15000,
        s"the configured connect timeout was ignored: gave up after ${ elapsed }ms rather than around 1000ms"
      )
      assert(
        elapsed >= 500,
        s"the connect failed after ${ elapsed }ms, too fast to have been the timeout — the host is answering"
      )

  test("the default budget is the one the connection object publishes"):
    assertEquals(Connection.defaultConnectTimeout, 30.seconds)

  test("a data source carries the configured budget into its connections"):
    val source = MySQLDataSource
      .build[Fx](blackhole, 3306, "root")
      .setConnectTimeout(1.second)
    assertEquals(source.connectTimeout, 1.second)

  test("a pool's budget caps the data source but never loosens it"):
    val base = MySQLDataSource.build[Fx](blackhole, 3306, "root")

    val capped = base.setConnectTimeout(20.seconds).withConnectTimeout(5.seconds)
    assertEquals(
      capped.asInstanceOf[MySQLDataSource[Fx, Unit]].connectTimeout,
      5.seconds,
      "a budget shorter than the configured timeout must win"
    )

    val kept = base.setConnectTimeout(2.seconds).withConnectTimeout(30.seconds)
    assertEquals(
      kept.asInstanceOf[MySQLDataSource[Fx, Unit]].connectTimeout,
      2.seconds,
      "pooling must not stretch a shorter timeout its owner chose deliberately"
    )
