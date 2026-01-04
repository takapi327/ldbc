/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.*

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.connector.Connection

class PoolStatusReporterTest extends CatsEffectSuite:

  class TestPoolLogger[F[_]: Applicative](var logCount: Int = 0) extends PoolLogger[F]:
    override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): F[Unit] =
      Applicative[F].pure {
        logCount += 1
        ()
      }
    override def debug(message: String):                           F[Unit]    = Applicative[F].unit
    override def info(message:  String):                           F[Unit]    = Applicative[F].unit
    override def warn(message:  String):                           F[Unit]    = Applicative[F].unit
    override def error(message: String, error: Option[Throwable]): F[Unit]    = Applicative[F].unit
    override def isDebugEnabled:                                   F[Boolean] = Applicative[F].pure(true)

  test("PoolStatusReporter should report pool status periodically when enabled") {
    val testLogger     = new TestPoolLogger[IO]()
    val metricsTracker = PoolMetricsTracker.noop[IO]
    val reporter       = PoolStatusReporter[IO](
      reportInterval = 100.milliseconds,
      poolLogger     = testLogger,
      metricsTracker = metricsTracker
    )

    // Mock PooledDataSource
    val pool = new PooledDataSource[IO] {
      def minConnections               = 5
      def maxConnections               = 10
      def connectionTimeout            = 30.seconds
      def idleTimeout                  = 10.minutes
      def maxLifetime                  = 30.minutes
      def validationTimeout            = 5.seconds
      def leakDetectionThreshold       = None
      def adaptiveSizing               = false
      def adaptiveInterval             = 1.minute
      def metricsTracker               = PoolMetricsTracker.noop[IO]
      def poolState                    = ???
      def idGenerator                  = IO.pure("test-id")
      def aliveBypassWindow            = 500.milliseconds
      def keepaliveTime                = None
      def connectionTestQuery          = None
      def poolLogger                   = testLogger
      def getConnection                = ???
      def status                       = IO.pure(PoolStatus(total = 10, active = 3, idle = 7, waiting = 0))
      def metrics                      = metricsTracker.getMetrics
      def close                        = IO.unit
      def createNewConnection()        = ???
      def circuitBreaker               = ???
      def createNewConnectionForPool() = ???
      def returnToPool(pooled:     PooledConnection[IO]) = ???
      def removeConnection(pooled: PooledConnection[IO]) = ???
      def validateConnection(conn: Connection[IO])       = ???
    }

    reporter.start(pool, "test-pool").use { _ =>
      // Wait for at least 2 report cycles (extended to account for system load and timing variations)
      IO.sleep(350.milliseconds).map { _ =>
        assert(testLogger.logCount >= 2, s"Expected at least 2 logs, but got ${ testLogger.logCount }")
      }
    }
  }

  test("PoolStatusReporter.noop should not report anything") {
    val reporter   = PoolStatusReporter.noop[IO]
    val testLogger = new TestPoolLogger[IO]()

    // Even with a mock pool, noop reporter should not do anything
    val pool = new PooledDataSource[IO] {
      def minConnections               = 5
      def maxConnections               = 10
      def connectionTimeout            = 30.seconds
      def idleTimeout                  = 10.minutes
      def maxLifetime                  = 30.minutes
      def validationTimeout            = 5.seconds
      def leakDetectionThreshold       = None
      def adaptiveSizing               = false
      def adaptiveInterval             = 1.minute
      def metricsTracker               = PoolMetricsTracker.noop[IO]
      def poolState                    = ???
      def idGenerator                  = IO.pure("test-id")
      def aliveBypassWindow            = 500.milliseconds
      def keepaliveTime                = None
      def connectionTestQuery          = None
      def poolLogger                   = testLogger
      def getConnection                = ???
      def status                       = IO.pure(PoolStatus(total = 10, active = 3, idle = 7, waiting = 0))
      def metrics                      = ???
      def close                        = IO.unit
      def createNewConnection()        = ???
      def circuitBreaker               = ???
      def createNewConnectionForPool() = ???
      def returnToPool(pooled:     PooledConnection[IO]) = ???
      def removeConnection(pooled: PooledConnection[IO]) = ???
      def validateConnection(conn: Connection[IO])       = ???
    }

    reporter.start(pool, "test-pool").use { _ =>
      IO.sleep(200.milliseconds).map { _ =>
        assertEquals(testLogger.logCount, 0)
      }
    }
  }

  test("PoolStatusReporter should only log when debug is enabled") {
    val metricsTracker = PoolMetricsTracker.noop[IO]

    // Logger with debug disabled
    var logCalledRef        = false
    val debugDisabledLogger = new PoolLogger[IO] {
      override def logPoolState(poolName: String, status: PoolStatus, metrics: Option[PoolMetrics]): IO[Unit] =
        IO {
          logCalledRef = true
        }
      override def debug(message: String):                           IO[Unit]    = IO.unit
      override def info(message:  String):                           IO[Unit]    = IO.unit
      override def warn(message:  String):                           IO[Unit]    = IO.unit
      override def error(message: String, error: Option[Throwable]): IO[Unit]    = IO.unit
      override def isDebugEnabled:                                   IO[Boolean] = IO.pure(false)
    }

    val reporter = PoolStatusReporter[IO](
      reportInterval = 50.milliseconds,
      poolLogger     = debugDisabledLogger,
      metricsTracker = metricsTracker
    )

    // Mock pool
    val pool = new PooledDataSource[IO] {
      def minConnections               = 5
      def maxConnections               = 10
      def connectionTimeout            = 30.seconds
      def idleTimeout                  = 10.minutes
      def maxLifetime                  = 30.minutes
      def validationTimeout            = 5.seconds
      def leakDetectionThreshold       = None
      def adaptiveSizing               = false
      def adaptiveInterval             = 1.minute
      def metricsTracker               = PoolMetricsTracker.noop[IO]
      def poolState                    = ???
      def idGenerator                  = IO.pure("test-id")
      def aliveBypassWindow            = 500.milliseconds
      def keepaliveTime                = None
      def connectionTestQuery          = None
      def poolLogger                   = debugDisabledLogger
      def getConnection                = ???
      def status                       = IO.pure(PoolStatus(total = 10, active = 3, idle = 7, waiting = 0))
      def metrics                      = metricsTracker.getMetrics
      def close                        = IO.unit
      def createNewConnection()        = ???
      def circuitBreaker               = ???
      def createNewConnectionForPool() = ???
      def returnToPool(pooled:     PooledConnection[IO]) = ???
      def removeConnection(pooled: PooledConnection[IO]) = ???
      def validateConnection(conn: Connection[IO])       = ???
    }

    reporter.start(pool, "test-pool").use { _ =>
      IO.sleep(150.milliseconds).map { _ =>
        assert(!logCalledRef, "Expected no logs when debug is disabled")
      }
    }
  }
