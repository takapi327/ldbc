/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.sql.Connection

import ldbc.effect.{ Ref, Resource }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

class DeadConnectionEvictionTest extends FxSuite:
  private def bypassing: ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections    = 1,
      maxConnections    = 1,
      connectionTimeout = 5.seconds,
      aliveBypassWindow = 1.hour,
      adaptiveSizing    = false
    )

  private def poolOver(mock: MockConnection, config: ConnectionPoolConfig): Resource[Fx, PooledDataSource[Fx]] =
    PooledDataSource.fromConfig(config, Resource.make(Fx.pure(mock: Connection[Fx]))(_ => Fx.unit))

  test("a connection that died while in use is evicted on release, even inside aliveBypassWindow") {
    for
      mock   <- MockConnection()
      status <- poolOver(mock, bypassing).use { datasource =>
                  datasource.use(_ => mock.closedRef.set(true)) >> datasource.status
                }
      validations <- mock.validationCount.get
    yield
      assertEquals(status.idle, 0, "死亡した接続がアイドルとしてプールに戻っている")
      assertEquals(status.total, 0, "死亡した接続がプールから除去されていない")
      assertEquals(validations, 0, "バイパス窓の中なのに検証の往復が走っている（バイパスが効いていない）")
  }

  test("a connection that died while idle is evicted on lease, even inside aliveBypassWindow") {
    for
      created <- Ref.of[Fx, Vector[MockConnection]](Vector.empty)
      create = Resource.make(
                 MockConnection().flatTap(m => created.update(_ :+ m)).map(c => (c: Connection[Fx]))
               )(_ => Fx.unit)
      leasedWasClosed <- PooledDataSource.fromConfig(bypassing, create).use { datasource =>
                           for
                             startup <- created.get
                             _       <- startup.head.closedRef.set(true)
                             closed  <- datasource.use(conn => conn.isClosed())
                           yield closed
                         }
      total <- created.get.map(_.size)
    yield
      assertEquals(leasedWasClosed, false, "死亡した接続がリース時に除去されず、そのまま払い出されている")
      assert(total >= 2, s"死亡した接続を捨てたあと代替が生成されていない（生成数: $total）")
  }

  test("a healthy connection is still pooled inside aliveBypassWindow") {
    for
      mock   <- MockConnection()
      status <- poolOver(mock, bypassing).use { datasource =>
                  datasource.use(conn => conn.isValid(1).void) >> datasource.status
                }
      validations <- mock.validationCount.get
    yield
      assertEquals(status.idle, 1, "健全な接続が不必要に除去されている")
      assertEquals(status.total, 1)
      assertEquals(validations, 1, "テスト自身の isValid 以外に検証の往復が走っている")
  }

  test("with the bypass disabled the validation round trip does run") {
    val validating = bypassing.copy(aliveBypassWindow = Duration.Zero)
    for
      mock <- MockConnection()
      _    <- poolOver(mock, validating).use { datasource =>
             datasource.use(_ => Fx.unit) >> datasource.status
           }
      validations <- mock.validationCount.get
    yield assert(validations > 0, "aliveBypassWindow = 0 でも検証の往復が走っていない")
  }
