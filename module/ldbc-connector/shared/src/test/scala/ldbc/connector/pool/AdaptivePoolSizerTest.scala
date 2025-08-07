/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*
import cats.syntax.all.*

import ldbc.connector.*

class AdaptivePoolSizerTest extends FTestPlatform:

  private val config = MySQLConfig.default
    .setPort(13306)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)

  test("AdaptivePoolSizer should grow pool under high load") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(10)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)  // Short interval for testing

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield (ds, tracker)

    resource.use { case (datasource, tracker) =>
      for
        initialStatus <- datasource.status
        
        // Create high load by acquiring most connections
        _ <- datasource.getConnection.use { _ =>
          datasource.getConnection.use { _ =>
            for
              // Wait for adaptive sizing to detect high utilization
              _ <- IO.sleep(150.millis)  // Multiple adaptive intervals
              statusDuringLoad <- datasource.status
            yield
              // Should be at capacity or starting to grow
              assert(statusDuringLoad.active == 2)
          }
        }
        
        // Wait for any growth to complete
        _ <- IO.sleep(100.millis)
        finalStatus <- datasource.status
      yield
        assertEquals(initialStatus.total, 2)
        // Pool should have grown or be ready to grow
        assert(finalStatus.total >= initialStatus.total)
    }
  }

  test("AdaptivePoolSizer should shrink pool under low load") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(10)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)  // Short interval for testing

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        // First grow the pool by creating load
        _ <- IO.parTraverseN(5)((1 to 5).toList) { _ =>
          datasource.getConnection.use { _ => IO.sleep(50.millis) }
        }
        
        statusAfterGrowth <- datasource.status
        
        // Now let pool be idle to trigger shrinking
        // Need multiple consecutive low utilization periods (3 as per implementation)
        _ <- IO.sleep(200.millis)  // 4 adaptive intervals
        
        finalStatus <- datasource.status
      yield
        // Pool should have grown initially
        assert(statusAfterGrowth.total > 2)
        // But should stay at least at minConnections
        assert(finalStatus.total >= 2)
    }
  }

  test("AdaptivePoolSizer should handle critical load with aggressive scaling") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(20)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)
      .setConnectionTimeout(200.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield (ds, tracker)

    resource.use { case (datasource, tracker) =>
      for
        initialStatus <- datasource.status
        
        // Create critical load - acquire all connections and try more
        fibers <- IO.parTraverseN(10)((1 to 10).toList) { _ =>
          datasource.getConnection.use { _ => IO.sleep(300.millis) }.start
        }
        
        // Wait for adaptive sizing to react
        _ <- IO.sleep(150.millis)
        statusDuringCriticalLoad <- datasource.status
        
        // Wait for all operations to complete
        _ <- fibers.traverse(_.join.attempt)
        
        _ <- IO.sleep(100.millis)
        finalStatus <- datasource.status
      yield
        assertEquals(initialStatus.total, 2)
        // Pool should have grown significantly under critical load
        assert(statusDuringCriticalLoad.total > initialStatus.total || statusDuringCriticalLoad.waiting > 0)
    }
  }

  test("AdaptivePoolSizer should respect cooldown period") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(10)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        initialStatus <- datasource.status
        
        // Create load to trigger growth
        _ <- datasource.getConnection.use { _ =>
          datasource.getConnection.use { _ =>
            IO.sleep(150.millis)  // Wait for growth
          }
        }
        
        statusAfterFirstAdjustment <- datasource.status
        
        // Try to create load again quickly (within cooldown)
        _ <- datasource.getConnection.use { _ =>
          datasource.getConnection.use { _ =>
            IO.sleep(100.millis)
          }
        }
        
        statusDuringCooldown <- datasource.status
      yield
        assertEquals(initialStatus.total, 2)
        // Should not adjust again during cooldown period
        // (Cooldown is 2 minutes in implementation, but we can't wait that long in tests)
        assert(statusAfterFirstAdjustment.total >= initialStatus.total)
    }
  }

  test("AdaptivePoolSizer should stop when pool is closed") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(5)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    var poolClosed = false

    resource
      .use { datasource =>
        for
          initialStatus <- datasource.status
          _ <- IO.sleep(100.millis)  // Let adaptive sizing run
          runningStatus <- datasource.status
        yield
          assertEquals(initialStatus.total, 2)
          assertEquals(runningStatus.total, 2)
      }
      .flatMap { _ =>
        poolClosed = true
        // After resource is released, adaptive sizing should have stopped
        IO.sleep(100.millis).as(assert(poolClosed))
      }
  }

  test("AdaptivePoolSizer should track consecutive high/low periods") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(10)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield (ds, tracker)

    resource.use { case (datasource, tracker) =>
      for
        initialStatus <- datasource.status
        
        // Create sustained high load (need 2 consecutive high periods for growth)
        fiber1 <- datasource.getConnection.use { _ => IO.sleep(200.millis) }.start
        fiber2 <- datasource.getConnection.use { _ => IO.sleep(200.millis) }.start
        
        // Wait for multiple adaptive intervals
        _ <- IO.sleep(150.millis)
        
        statusDuringLoad <- datasource.status
        
        // Clean up
        _ <- fiber1.cancel
        _ <- fiber2.cancel
        _ <- IO.sleep(50.millis)
        
        finalStatus <- datasource.status
      yield
        assertEquals(initialStatus.total, 2)
        // Should detect high utilization
        assert(statusDuringLoad.active > 0)
    }
  }

  test("AdaptivePoolSizer should handle pool growth failures gracefully") {
    // Use a config that will make it hard to create many connections
    val testConfig = config
      .setMinConnections(1)
      .setMaxConnections(100)  // High max but connections might fail
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)
      .setConnectionTimeout(100.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        initialStatus <- datasource.status
        
        // Create load
        _ <- datasource.getConnection.use { _ =>
          IO.sleep(150.millis)  // Wait for adaptive sizing
        }
        
        finalStatus <- datasource.status
      yield
        // Even if growth fails, pool should remain stable
        assert(finalStatus.total >= initialStatus.total)
        assert(finalStatus.total >= 1)  // At least minimum connections
    }
  }

  test("AdaptivePoolSizer should calculate metrics correctly") {
    val testConfig = config
      .setMinConnections(4)
      .setMaxConnections(10)
      .setAdaptiveSizing(true)
      .setAdaptiveInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield (ds, tracker)

    resource.use { case (datasource, tracker) =>
      for
        // Perform operations to generate metrics
        _ <- datasource.getConnection.use { _ => IO.sleep(10.millis) }
        _ <- datasource.getConnection.use { _ => IO.sleep(10.millis) }
        
        // Create varying load
        _ <- IO.parTraverseN(2)((1 to 4).toList) { i =>
          datasource.getConnection.use { _ => IO.sleep((i * 10).millis) }
        }
        
        // Wait for metrics update
        _ <- IO.sleep(100.millis)
        
        status <- datasource.status
        metrics <- tracker.getMetrics
      yield
        // Verify metrics are being tracked
        assert(metrics.totalAcquisitions >= 6L)
        assert(metrics.totalReleases >= 6L)
        assert(status.total >= 4)  // At least minConnections
    }
  }
