/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.sql.Connection

import ldbc.effect.Resource
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * `aliveBypassWindow` lets the pool skip the validation round trip for a connection it has just
 * seen. It must not also skip noticing that the connection is already dead: a driver that reports a
 * failed transport through `isClosed` costs nothing to consult, and handing such a connection back
 * out would fail the next caller's first statement.
 */
class DeadConnectionEvictionTest extends FxSuite:

  /** A window wide enough that validation is always bypassed for a freshly used connection. */
  private def config: ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections    = 1,
      maxConnections    = 1,
      connectionTimeout = 5.seconds,
      aliveBypassWindow = 1.hour,
      adaptiveSizing    = false
    )

  /** Hands the pool one connection we keep a handle on, so the test can kill it mid-use. */
  private def poolOver(mock: MockConnection): Resource[Fx, PooledDataSource[Fx]] =
    PooledDataSource.fromConfig(config, Resource.make(Fx.pure(mock: Connection[Fx]))(_ => Fx.unit))

  test("a connection that died while in use is evicted on release, even inside aliveBypassWindow") {
    for
      mock   <- MockConnection()
      status <- poolOver(mock).use { datasource =>
                  datasource.use(_ => mock.closedRef.set(true)) >> datasource.status
                }
    yield
      assertEquals(status.idle, 0, "死亡した接続がアイドルとしてプールに戻っている")
      assertEquals(status.total, 0, "死亡した接続がプールから除去されていない")
  }

  test("a healthy connection is still pooled inside aliveBypassWindow") {
    for
      mock   <- MockConnection()
      status <- poolOver(mock).use { datasource =>
                  datasource.use(conn => conn.isValid(1).void) >> datasource.status
                }
    yield
      assertEquals(status.idle, 1, "健全な接続が不必要に除去されている")
      assertEquals(status.total, 1)
  }
