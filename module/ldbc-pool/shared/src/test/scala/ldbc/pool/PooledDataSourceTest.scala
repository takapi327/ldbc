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
 * Behaviour tests for the ported [[PooledDataSource]] orchestrator (acquire/release, pool growth, the
 * max-connection limit, validation, idle-connection tracking, shutdown), driven end-to-end by a
 * [[MockConnection]] factory. The connector's real-database tests (SQL round-trips, prepared
 * statements, MySQL transaction/reset semantics) are deferred to the ldbc-mysql end-to-end suite.
 */
class PooledDataSourceTest extends FxSuite:

  private def mockCreate: Resource[Fx, Connection[Fx]] =
    Resource.make(MockConnection().map(c => (c: Connection[Fx])))(c => c.close())

  private def config(min: Int, max: Int, connectionTimeout: FiniteDuration = 5.seconds): ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections    = min,
      maxConnections    = max,
      connectionTimeout = connectionTimeout,
      adaptiveSizing    = false
    )

  private def pool(config: ConnectionPoolConfig): Resource[Fx, PooledDataSource[Fx]] =
    PooledDataSource.fromConfig(config, mockCreate)

  test("PooledDataSource should be created with the configured minimum connections") {
    pool(config(3, 5)).use { datasource =>
      for status <- datasource.status
      yield
        assertEquals(status.total, 3)
        assertEquals(status.idle, 3)
        assertEquals(status.active, 0)
        assertEquals(status.waiting, 0)
    }
  }

  test("PooledDataSource should acquire and release connections") {
    pool(config(2, 5)).use { datasource =>
      datasource.use { conn =>
        for
          statusAfterAcquire <- datasource.status
          valid              <- conn.isValid(5)
        yield
          assert(valid)
          assertEquals(statusAfterAcquire.active, 1)
          assertEquals(statusAfterAcquire.idle, 1)
      } >> datasource.status.map { statusAfterRelease =>
        assertEquals(statusAfterRelease.active, 0)
        assertEquals(statusAfterRelease.idle, 2)
      }
    }
  }

  test("PooledDataSource should grow the pool when needed") {
    pool(config(2, 5)).use { datasource =>
      for
        initialStatus <- datasource.status
        _             <- datasource.use { _ =>
               datasource.use { _ =>
                 datasource.use { _ =>
                   datasource.status.map { statusAfterGrowth =>
                     assert(statusAfterGrowth.total >= 3)
                     assertEquals(statusAfterGrowth.active, 3)
                   }
                 }
               }
             }
        finalStatus <- datasource.status
      yield
        assertEquals(initialStatus.total, 2)
        assertEquals(finalStatus.active, 0)
    }
  }

  test("PooledDataSource should respect the maximum connections limit") {
    pool(config(1, 2, connectionTimeout = 500.millis)).use { datasource =>
      datasource.use { _ =>
        datasource.use { _ =>
          datasource.use(_ => Fx.unit).timeout(600.millis).attempt.map { result =>
            assert(result.isLeft, "Third connection acquisition should time out")
          }
        }
      }
    }
  }

  test("PooledDataSource should validate connections") {
    pool(config(1, 2)).use { datasource =>
      datasource.use { conn =>
        conn.isValid(5).map(isValid => assert(isValid, "Connection should be valid"))
      }
    }
  }

  test("PooledDataSource should clean up on shutdown") {
    pool(config(3, 5))
      .use { datasource =>
        datasource.use { _ =>
          datasource.use { _ =>
            datasource.status
          }
        }
      }
      .map(finalStatus => assert(finalStatus.total >= 2))
  }

  test("PooledDataSource should add connections to idleConnections on initialization") {
    pool(config(3, 5)).use { datasource =>
      for
        state  <- datasource.poolState.get
        status <- datasource.status
      yield
        assertEquals(state.idleConnections.size, 3)
        assertEquals(status.idle, 3)
        assertEquals(status.active, 0)
        state.connections.foreach { conn =>
          assert(state.idleConnections.contains(conn.id), s"Connection ${ conn.id } should be in idleConnections")
        }
    }
  }

  test("PooledDataSource should remove a connection from idleConnections on acquire") {
    pool(config(2, 5)).use { datasource =>
      for
        initialState <- datasource.poolState.get
        _ = assertEquals(initialState.idleConnections.size, 2)
        _ <- datasource.use { _ =>
               for
                 stateWhileAcquired <- datasource.poolState.get
                 status             <- datasource.status
               yield
                 assertEquals(stateWhileAcquired.idleConnections.size, 1)
                 assertEquals(status.active, 1)
                 assertEquals(status.idle, 1)
             }
        finalState <- datasource.poolState.get
      yield assertEquals(finalState.idleConnections.size, 2)
    }
  }

  test("PooledDataSource should add a connection back to idleConnections on release") {
    pool(config(1, 3)).use { datasource =>
      for
        initialState <- datasource.poolState.get
        initialSize = initialState.idleConnections.size
        _ <- datasource.use { _ =>
               datasource.poolState.get.map(during => assertEquals(during.idleConnections.size, initialSize - 1))
             }
        finalState <- datasource.poolState.get
      yield assertEquals(finalState.idleConnections.size, initialSize)
    }
  }

  test("PooledDataSource should track idleConnections across concurrent acquisitions") {
    pool(config(3, 5)).use { datasource =>
      for
        initialState <- datasource.poolState.get
        _ = assertEquals(initialState.idleConnections.size, 3)
        _ <- (
               datasource.use(_ => datasource.poolState.get.map(_.idleConnections.size)),
               datasource.use(_ => datasource.poolState.get.map(_.idleConnections.size))
             ).parTupled
        finalState <- datasource.poolState.get
      yield assertEquals(finalState.idleConnections.size, finalState.connections.size)
    }
  }

  test("PooledDataSource should record acquisition metrics") {
    for
      tracker <- PoolMetricsTracker.inMemory
      _ <- PooledDataSource.fromConfig(config(1, 3), mockCreate, metricsTracker = Some(tracker)).use { datasource =>
             datasource.use(_ => Fx.unit)
           }
      metrics <- tracker.getMetrics
    yield assert(metrics.totalAcquisitions >= 1L, "At least one acquisition should be recorded")
  }
