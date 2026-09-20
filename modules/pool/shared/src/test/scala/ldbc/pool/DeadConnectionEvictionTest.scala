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
      assertEquals(status.idle, 0, "a dead connection went back into the pool as idle")
      assertEquals(status.total, 0, "a dead connection was not removed from the pool")
      assertEquals(validations, 0, "a validation round trip ran inside the bypass window, so the bypass is not working")
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
      assertEquals(leasedWasClosed, false, "a dead connection was handed out instead of being evicted on lease")
      assert(total >= 2, s"no replacement was created after discarding the dead connection ($total created)")
  }

  test("a healthy connection is still pooled inside aliveBypassWindow") {
    for
      mock   <- MockConnection()
      status <- poolOver(mock, bypassing).use { datasource =>
                  datasource.use(conn => conn.isValid(1).void) >> datasource.status
                }
      validations <- mock.validationCount.get
    yield
      assertEquals(status.idle, 1, "a healthy connection was evicted for no reason")
      assertEquals(status.total, 1)
      assertEquals(validations, 1, "a validation round trip ran beyond the isValid this test performs itself")
  }

  test("with the bypass disabled the validation round trip does run") {
    val validating = bypassing.copy(aliveBypassWindow = Duration.Zero)
    for
      mock <- MockConnection()
      _    <- poolOver(mock, validating).use { datasource =>
             datasource.use(_ => Fx.unit) >> datasource.status
           }
      validations <- mock.validationCount.get
    yield assert(validations > 0, "no validation round trip ran even with aliveBypassWindow = 0")
  }
