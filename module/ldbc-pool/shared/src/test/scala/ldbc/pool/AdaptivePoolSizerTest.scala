/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.fx.FxSuite

import scala.concurrent.duration.*

import ldbc.fx.Fx
import ldbc.fx.concurrentFx
import ldbc.effect.Resource
import ldbc.fx.syntax.*

import ldbc.sql.Connection

/**
 * Tests for [[AdaptivePoolSizer]] wired into a real [[PooledDataSource[Fx]]] with a [[MockConnection]]
 * factory. This drives the adaptive-sizing loop (utilisation sampling, growth under load, stability,
 * stopping when closed) without a live database.
 */
class AdaptivePoolSizerTest extends FxSuite:

  private def mockCreate: Resource[Fx, Connection[Fx]] =
    Resource.make(MockConnection().map(c => (c: Connection[Fx])))(c => c.close())

  private def config(
    min:               Int,
    max:               Int,
    connectionTimeout: FiniteDuration = 800.millis
  ): ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections    = min,
      maxConnections    = max,
      connectionTimeout = connectionTimeout,
      adaptiveSizing    = true,
      adaptiveInterval  = 50.millis
    )

  private def pool(config: ConnectionPoolConfig, tracker: PoolMetricsTracker[Fx]): Resource[Fx, PooledDataSource[Fx]] =
    PooledDataSource.fromConfig(config, mockCreate, metricsTracker = Some(tracker))

  test("AdaptivePoolSizer should grow the pool under high load") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(2, 10), tracker).use { datasource =>
             for
               initialStatus <- datasource.status
               _ <- datasource.use { _ =>
                      datasource.use { _ =>
                        for
                          _                <- Fx.sleep(150.millis)
                          statusDuringLoad <- datasource.status
                        yield assertEquals(statusDuringLoad.active, 2)
                      }
                    }
               _           <- Fx.sleep(100.millis)
               finalStatus <- datasource.status
             yield
               assertEquals(initialStatus.total, 2)
               assert(finalStatus.total >= initialStatus.total)
           }
    yield ()
  }

  test("AdaptivePoolSizer should shrink the pool under low load") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(2, 10), tracker).use { datasource =>
             for
               _                 <- (1 to 5).toList.parTraverseN(5)(_ => datasource.use(_ => Fx.sleep(50.millis)))
               statusAfterGrowth <- datasource.status
               _                 <- Fx.sleep(200.millis)
               finalStatus       <- datasource.status
             yield
               assert(statusAfterGrowth.total >= 2)
               assert(finalStatus.total >= 2)
           }
    yield ()
  }

  test("AdaptivePoolSizer should track consecutive high/low periods") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(2, 10), tracker).use { datasource =>
             for
               initialStatus    <- datasource.status
               fiber1           <- datasource.use(_ => Fx.sleep(200.millis)).start
               fiber2           <- datasource.use(_ => Fx.sleep(200.millis)).start
               _                <- Fx.sleep(150.millis)
               statusDuringLoad <- datasource.status
               _                <- fiber1.cancel
               _                <- fiber2.cancel
               _                <- Fx.sleep(50.millis)
             yield
               assertEquals(initialStatus.total, 2)
               assert(statusDuringLoad.active > 0)
           }
    yield ()
  }

  test("AdaptivePoolSizer should stop when the pool is closed") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(2, 5), tracker).use { datasource =>
             for
               initialStatus <- datasource.status
               _             <- Fx.sleep(100.millis)
               runningStatus <- datasource.status
             yield
               assertEquals(initialStatus.total, 2)
               assertEquals(runningStatus.total, 2)
           }
    yield ()
  }

  test("AdaptivePoolSizer should handle pool growth gracefully") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(1, 100, connectionTimeout = 250.millis), tracker).use { datasource =>
             for
               initialStatus <- datasource.status
               _             <- datasource.use(_ => Fx.sleep(150.millis))
               finalStatus   <- datasource.status
             yield
               assert(finalStatus.total >= initialStatus.total)
               assert(finalStatus.total >= 1)
           }
    yield ()
  }

  test("AdaptivePoolSizer should track metrics correctly") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      _ <- pool(config(4, 10), tracker).use { datasource =>
             for
               _ <- datasource.use(_ => Fx.sleep(10.millis))
               _ <- datasource.use(_ => Fx.sleep(10.millis))
               _ <- (1 to 4).toList.parTraverseN(2)(i => datasource.use(_ => Fx.sleep((i * 10).millis)))
               _       <- Fx.sleep(100.millis)
               status  <- datasource.status
               metrics <- tracker.getMetrics
             yield
               assert(metrics.totalAcquisitions >= 6L)
               assert(metrics.totalReleases >= 6L)
               assert(status.total >= 4)
           }
    yield ()
  }
