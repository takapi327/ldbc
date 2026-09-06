/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.IO

import org.typelevel.otel4s.sdk.testkit.OpenTelemetrySdkTestkit

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.catseffect.{ concurrentIO, toIOResource, Connector }
import ldbc.effect.Resource
import ldbc.mysql.{ MySQLConfig, MySQLDataSource }
import ldbc.mysql.syntax.*
import ldbc.net.SSL
import ldbc.otel4s.Otel4sTelemetry
import ldbc.pool.{ ConnectionPoolConfig, PooledDataSource }
import ldbc.telemetry.{ DatabaseMetrics, Meter }

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

  /** Counts how many times the driver asks its meter to build the metric instruments. */
  private final class CountingMeter(delegate: Meter[IO]) extends Meter[IO]:
    val builds: java.util.concurrent.atomic.AtomicInteger = new java.util.concurrent.atomic.AtomicInteger(0)
    override def databaseMetrics: Resource[IO, DatabaseMetrics[IO]] =
      Resource.eval(IO(builds.incrementAndGet())).flatMap(_ => delegate.databaseMetrics)

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
