/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.*

import cats.syntax.all.*

import cats.effect.IO

import org.typelevel.otel4s.sdk.testkit.OpenTelemetrySdkTestkit

import munit.CatsEffectSuite

import ldbc.sql.Attribute

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.catseffect.{ concurrentIO, toIOResource, Connector }
import ldbc.effect.Resource
import ldbc.mysql.{ MySQLConfig, MySQLDataSource }
import ldbc.mysql.syntax.*
import ldbc.net.SSL
import ldbc.otel4s.Otel4sTelemetry
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }
import ldbc.telemetry.{ DatabaseMetrics, Meter, PoolMetricsState }

/**
 * End-to-end proof that the telemetry chain is connected for the effect-agnostic stack: a real MySQL
 * query, run through [[ldbc.pool.PooledDataSource]] over [[ldbc.mysql.MySQLDataSource]], produces both
 * the driver's operation metrics and the pool's connection metrics on a real otel4s SDK.
 *
 * `ldbc.pool.PoolTelemetryTest` covers the pool's call sites against a recording SPI, and
 * `ldbc.otel4s.Otel4sTelemetryTest` covers the SPI-to-otel4s mapping; this joins the two against the
 * Docker MySQL at 127.0.0.1:13306.
 */
class PooledTelemetryIntegrationTest extends CatsEffectSuite:

  private val config: MySQLConfig =
    MySQLConfig.default
      .setHost("127.0.0.1")
      .setPort(13306)
      .setUser("ldbc")
      .setPassword("password")
      .setDatabase("connector_test")
      .setSSL(SSL.Trusted)

  /**
   * A [[DatabaseMetrics]] that refuses to be used once its owning resource has been released, so a test can
   * detect a data source handing out metrics it has already torn down.
   */
  private final class ReleasableMetrics extends DatabaseMetrics[IO]:
    @volatile private var released = false

    def release(): Unit = released = true

    private def guard: IO[Unit] =
      IO.raiseWhen(released)(new IllegalStateException("used after the metrics resource was released"))

    override def recordOperationDuration(duration:    FiniteDuration, attributes: Attribute[?]*): IO[Unit] = guard
    override def recordReturnedRows(rows:             Long, attributes:           Attribute[?]*): IO[Unit] = guard
    override def recordConnectionCreateTime(duration: FiniteDuration, poolName:   String):        IO[Unit] = guard
    override def recordConnectionWaitTime(duration:   FiniteDuration, poolName:   String):        IO[Unit] = guard
    override def recordConnectionUseTime(duration:    FiniteDuration, poolName:   String):        IO[Unit] = guard
    override def recordConnectionTimeout(poolName:    String):                                    IO[Unit] = guard
    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  IO[PoolMetricsState]
    ): Resource[IO, Unit] = Resource.pure(())

  /** A [[DatabaseMetrics]] that reports which instance recorded, so a test can tell instances apart. */
  private final class TaggedMetrics(tag: Int, used: java.util.concurrent.ConcurrentLinkedQueue[Int])
    extends DatabaseMetrics[IO]:
    override def recordOperationDuration(duration: FiniteDuration, attributes: Attribute[?]*): IO[Unit] =
      IO { used.add(tag); () }
    override def recordReturnedRows(rows:             Long, attributes:         Attribute[?]*): IO[Unit] = IO.unit
    override def recordConnectionCreateTime(duration: FiniteDuration, poolName: String):        IO[Unit] = IO.unit
    override def recordConnectionWaitTime(duration:   FiniteDuration, poolName: String):        IO[Unit] = IO.unit
    override def recordConnectionUseTime(duration:    FiniteDuration, poolName: String):        IO[Unit] = IO.unit
    override def recordConnectionTimeout(poolName:    String):                                  IO[Unit] = IO.unit
    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  IO[PoolMetricsState]
    ): Resource[IO, Unit] = Resource.pure(())

  /** Counts how many times the driver asks its meter to build the metric instruments. */
  private final class CountingMeter(delegate: Meter[IO]) extends Meter[IO]:
    val builds: java.util.concurrent.atomic.AtomicInteger = new java.util.concurrent.atomic.AtomicInteger(0)
    override def databaseMetrics: IO[DatabaseMetrics[IO]] =
      IO(builds.incrementAndGet()) *> delegate.databaseMetrics

  test("a pooled query exports both the driver's operation metrics and the pool's connection metrics") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter <- meterProvider.meter("ldbc").get
        datasource = MySQLDataSource.fromConfig[IO](config).setMeter(meter)
        poolConfig = ConnectionPoolConfig(minConnections = 1, maxConnections = 2)
        pool       = PooledDataSource.fromDataSource[IO](poolConfig, datasource, meter = Some(meter))
        result <- toIOResource(pool).use { ds =>
                    sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(ds)) *>
                      testkit.collectMetrics
                  }
      yield
        val names = result.map(_.name).toSet
        assert(names.contains("db.client.operation.duration"), names.toString)
        assert(names.contains("db.client.response.returned_rows"), names.toString)
        assert(names.contains("db.client.connection.create_time"), names.toString)
        assert(names.contains("db.client.connection.wait_time"), names.toString)
        assert(names.contains("db.client.connection.use_time"), names.toString)
        assert(names.contains("db.client.connection.count"), names.toString)
        assert(names.contains("db.client.connection.max"), names.toString)
        assert(names.contains("db.client.connection.pending_requests"), names.toString)
    }
  }

  test("the pool's connection metrics carry the configured pool name") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val meterProvider = Otel4sTelemetry.meterProvider(testkit.meterProvider)
      for
        meter <- meterProvider.meter("ldbc").get
        datasource = MySQLDataSource.fromConfig[IO](config)
        poolConfig = ConnectionPoolConfig(minConnections = 1, maxConnections = 2, poolName = "integration-pool")
        pool       = PooledDataSource.fromDataSource[IO](poolConfig, datasource, meter = Some(meter))
        result <- toIOResource(pool).use { ds =>
                    sql"SELECT 1".query[Int].to[Option].readOnly(Connector.fromDataSource(ds)) *>
                      testkit.collectMetrics
                  }
      yield
        val waitTime = result.find(_.name == "db.client.connection.wait_time")
        assert(waitTime.isDefined, s"db.client.connection.wait_time was exported: ${ result.map(_.name) }")
        assert(
          waitTime.get.data.points.exists(_.attributes.exists { attribute =>
            attribute.key.name == "db.client.connection.pool.name" && attribute.value == "integration-pool"
          }),
          s"the pool name is attached to the data point: ${ waitTime.get.data.points }"
        )
    }
  }

  test("without a meter the pooled path still runs and exports nothing") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      val datasource = MySQLDataSource.fromConfig[IO](config)
      val poolConfig = ConnectionPoolConfig(minConnections = 1, maxConnections = 2)
      val pool       = PooledDataSource.fromDataSource[IO](poolConfig, datasource)
      for
        queried <- toIOResource(pool).use { ds =>
                     sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(ds))
                   }
        metrics <- testkit.collectMetrics
      yield
        assertEquals(queried, Some(7))
        assertEquals(metrics.toList, Nil)
    }
  }

  test("the data source builds its metric instruments once, not on every connection") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        meter <- Otel4sTelemetry.meterProvider(testkit.meterProvider).meter("ldbc").get
        counting   = new CountingMeter(meter)
        datasource = MySQLDataSource.fromConfig[IO](config).setMeter(counting)
        _ <- datasource.use(_ => IO.unit)
        _ <- datasource.use(_ => IO.unit)
        _ <- datasource.use(_ => IO.unit)
      yield assertEquals(
        counting.builds.get(),
        1,
        "the instruments are cached on the data source rather than rebuilt per getConnection"
      )
    }
  }

  test("withTraced wires both the tracer and the meter into the data source") {
    OpenTelemetrySdkTestkit.inMemory[IO]().use { testkit =>
      for
        datasource <- MySQLDataSource.withTraced[IO](
                        config,
                        tracerProvider = Otel4sTelemetry.tracerProvider(testkit.tracerProvider),
                        meterProvider  = Otel4sTelemetry.meterProvider(testkit.meterProvider)
                      )
        queried <- sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(datasource))
        spans   <- testkit.finishedSpans
        metrics <- testkit.collectMetrics
      yield
        assertEquals(queried, Some(7))
        assert(spans.nonEmpty, "withTraced produced spans")
        assert(
          metrics.map(_.name).contains("db.client.operation.duration"),
          s"withTraced produced operation metrics, but got ${ metrics.map(_.name) }"
        )
    }
  }

  test("withTraced falls back to the no-op providers when none are given") {
    for
      datasource <- MySQLDataSource.withTraced[IO](config)
      queried    <- sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(datasource))
    yield assertEquals(queried, Some(7))
  }

  test("a data source never hands out metrics it has already torn down") {
    val metrics = new ReleasableMetrics
    val meter   = new Meter[IO]:
      override def databaseMetrics: IO[DatabaseMetrics[IO]] = IO.pure(metrics)
    val datasource = MySQLDataSource.fromConfig[IO](config).setMeter(meter)
    for
      _       <- datasource.use(_ => IO.unit)
      queried <- sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(datasource))
    yield assertEquals(queried, Some(7))
  }

  test("concurrent first connections all record through one metrics instance") {
    val used  = new java.util.concurrent.ConcurrentLinkedQueue[Int]()
    val built = new java.util.concurrent.atomic.AtomicInteger(0)
    val meter = new Meter[IO]:
      override def databaseMetrics: IO[DatabaseMetrics[IO]] =
        IO(new TaggedMetrics(built.incrementAndGet(), used))
    val datasource = MySQLDataSource.fromConfig[IO](config).setMeter(meter)
    val query      = sql"SELECT 7".query[Int].to[Option].readOnly(Connector.fromDataSource(datasource))
    for
      _ <- List.fill(4)(query).parSequence_
      tags = used.iterator().asScala.toList
    yield
      assert(tags.nonEmpty, "the queries recorded their operation duration")
      assertEquals(
        tags.distinct.size,
        1,
        s"every connection must record through one metrics instance, but ${ tags.distinct.size } were used"
      )
  }
