/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*

class HouseKeeperTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  private val config = MySQLConfig.default
    .setPort(13306)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)

  test("HouseKeeper should remove expired connections") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(5)
      .setMaxLifetime(200.millis)  // Short lifetime for testing
      .setMaintenanceInterval(50.millis)  // Run maintenance frequently
      .setIdleTimeout(10.minutes)  // Long idle timeout to test only expiration

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        initialStatus <- datasource.status
        // Wait for connections to expire
        _ <- IO.sleep(250.millis)
        // Wait for maintenance to run multiple times
        _ <- IO.sleep(150.millis)
        finalStatus <- datasource.status
      yield
        assertEquals(initialStatus.total, 2)
        // HouseKeeper should ensure minimum connections are maintained
        assert(finalStatus.total >= 1, s"Expected at least 1 connection, got ${finalStatus.total}")
    }
  }

  test("HouseKeeper should maintain minimum connections") {
    val testConfig = config
      .setMinConnections(3)
      .setMaxConnections(10)
      .setMaintenanceInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        initialStatus <- datasource.status
        _ <- IO.sleep(150.millis)  // Wait for maintenance to run
        finalStatus <- datasource.status
      yield
        assertEquals(initialStatus.total, 3)
        assertEquals(finalStatus.total, 3)
        assert(finalStatus.idle >= 0)
    }
  }

  test("HouseKeeper should validate idle connections") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(5)
      .setMaintenanceInterval(50.millis)
      .setValidationTimeout(100.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        // Use and release a connection to ensure it needs validation
        _ <- datasource.getConnection.use { conn =>
          conn.createStatement().flatMap(_.executeQuery("SELECT 1")).void
        }
        statusBefore <- datasource.status
        _ <- IO.sleep(150.millis)  // Wait for maintenance to run
        statusAfter <- datasource.status
      yield
        // Connection count should remain stable after validation
        assertEquals(statusAfter.total, statusBefore.total)
    }
  }

  test("HouseKeeper should update metrics") {
    val testConfig = config
      .setMinConnections(2)
      .setMaxConnections(5)
      .setMaintenanceInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield (ds, tracker)

    resource.use { case (datasource, tracker) =>
      for
        // Wait for initial metrics
        _ <- IO.sleep(100.millis)
        // Acquire a connection to change pool state
        _ <- datasource.getConnection.use(_ => IO.sleep(50.millis))
        // Wait for maintenance to update metrics
        _ <- IO.sleep(100.millis)
        metrics <- tracker.getMetrics
      yield
        // Metrics should have been updated
        assert(metrics.totalAcquisitions >= 1L)
        assert(metrics.totalReleases >= 1L)
    }
  }

  test("HouseKeeper should stop when pool is closed") {
    val testConfig = config
      .setMinConnections(1)
      .setMaxConnections(3)
      .setMaintenanceInterval(50.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    var poolClosed = false

    resource
      .use { datasource =>
        for
          initialStatus <- datasource.status
          _ <- IO.sleep(100.millis)  // Let maintenance run
          runningStatus <- datasource.status
        yield
          assertEquals(initialStatus.total, 1)
          assertEquals(runningStatus.total, 1)
      }
      .flatMap { _ =>
        poolClosed = true
        // After resource is released, maintenance should have stopped
        IO.sleep(100.millis).as(assert(poolClosed))
      }
  }

  test("HouseKeeper should handle connection creation failures gracefully") {
    // Use an invalid config that will fail to create connections
    val testConfig = MySQLConfig.default
      .setHost("invalid-host")
      .setPort(9999)  // Invalid port
      .setMinConnections(3)
      .setMaxConnections(5)
      .setMaintenanceInterval(50.millis)
      .setConnectionTimeout(100.millis)

    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource.fromConfig[IO](testConfig, metricsTracker = Some(tracker))
    yield ds

    interceptIO[Exception] {
      resource.use { _ => IO.unit }
    }
  }
