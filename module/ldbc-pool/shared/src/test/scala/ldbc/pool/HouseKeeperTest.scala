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
 * Tests for [[HouseKeeper]] wired into a real [[PooledDataSource[Fx]]] whose physical connections come from
 * a [[MockConnection]] factory. This exercises the actual maintenance loop: maintaining the minimum,
 * evicting idle-timed-out connections, updating metrics, and stopping when the pool closes. The
 * connector's real-database round-trips are deferred to the ldbc-mysql end-to-end suite.
 */
class HouseKeeperTest extends FxSuite:

  private def mockCreate: Resource[Fx, Connection[Fx]] =
    Resource.make(MockConnection().map(c => (c: Connection[Fx])))(c => c.close())

  private def config(
    min:                 Int,
    max:                 Int,
    idleTimeout:         FiniteDuration = 20.seconds,
    maintenanceInterval: FiniteDuration = 1.second
  ): ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections      = min,
      maxConnections      = max,
      idleTimeout         = idleTimeout,
      maxLifetime         = 30.seconds,
      maintenanceInterval = maintenanceInterval,
      connectionTimeout   = 1.second,
      adaptiveSizing      = false
    )

  private def pool(config: ConnectionPoolConfig, tracker: PoolMetricsTracker[Fx]): Resource[Fx, PooledDataSource[Fx]] =
    PooledDataSource.fromConfig(config, mockCreate, metricsTracker = Some(tracker))

  test("HouseKeeper should maintain minimum connections") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _       <- pool(config(3, 10), tracker).use { datasource =>
             for
               initialStatus <- datasource.status
               _             <- Fx.sleep(1500.millis)
               finalStatus   <- datasource.status
             yield
               assertEquals(initialStatus.total, 3)
               assertEquals(finalStatus.total, 3)
               assert(finalStatus.idle >= 0)
           }
    yield ()
  }

  test("HouseKeeper should keep the pool healthy and update metrics") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _       <- pool(config(2, 5), tracker).use { datasource =>
             for
               _       <- datasource.use(conn => conn.isValid(5).void)
               _       <- Fx.sleep(1200.millis)
               metrics <- tracker.getMetrics
             yield
               assert(metrics.totalAcquisitions >= 1L)
               assert(metrics.totalReleases >= 1L)
           }
    yield ()
  }

  test("HouseKeeper should remove idle connections after the idle timeout") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(1, 5, idleTimeout = 1.second, maintenanceInterval = 500.millis), tracker).use { datasource =>
             for
               _           <- Fx.sleep(200.millis)
               grownStatus <- datasource.use { _ =>
                                datasource.use { _ =>
                                  datasource.use(_ => datasource.status)
                                }
                              }
               _           <- Fx.sleep(3.seconds)
               finalStatus <- datasource.status
             yield
               assert(grownStatus.total >= 3, s"Pool should have grown, got ${ grownStatus.total }")
               assert(
                 finalStatus.total < grownStatus.total,
                 s"Idle connections should be evicted, got ${ finalStatus.total }"
               )
               assert(finalStatus.total >= 1, s"Should keep the minimum, got ${ finalStatus.total }")
           }
    yield ()
  }

  test("HouseKeeper should handle concurrent pool operations during maintenance") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _       <- pool(config(2, 10, maintenanceInterval = 500.millis), tracker).use { datasource =>
             val operations = (1 to 20).toList.traverse_ { _ =>
               datasource.use(conn => conn.isValid(5).void)
             }
             for
               fiber           <- operations.start
               _               <- Fx.sleep(500.millis)
               statusDuringOps <- datasource.status
               _               <- fiber.join
               finalStatus     <- datasource.status
             yield
               assert(statusDuringOps.total >= 2, "Should maintain minimum connections during operations")
               assert(finalStatus.total >= 2, "Should maintain minimum connections after operations")
           }
    yield ()
  }

  test("HouseKeeper should not create connections when the pool is closed") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _       <- pool(config(3, 5, maintenanceInterval = 500.millis), tracker).use { datasource =>
             for
               initialStatus <- datasource.status
               closeFiber    <- (Fx.sleep(500.millis) >> datasource.close).start
               _             <- closeFiber.join
               finalState    <- datasource.poolState.get
               _ = assert(finalState.closed, "Pool should be closed")
               _           <- Fx.sleep(500.millis)
               finalState2 <- datasource.poolState.get
             yield
               assertEquals(initialStatus.total, 3)
               assert(finalState2.closed, "Pool should remain closed")
           }
    yield ()
  }
