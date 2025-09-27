/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.*

import munit.*

import ldbc.dsl.codec.*

import ldbc.schema.*

import ldbc.connector.*
import ldbc.connector.pool.*

import ldbc.tests.model.*

class LdbcConnectionPoolDslTest extends ConnectionPoolDslTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def config: MySQLConfig = MySQLConfig.default
    .setPort(13306)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("world")
    .setSSL(SSL.Trusted)
    .setMinConnections(2)
    .setMaxConnections(5)
    .setIdleTimeout(10.minutes)
    .setMaxLifetime(30.minutes)
    .setConnectionTimeout(60.seconds)
    .setMaintenanceInterval(1.second)
    .setAdaptiveInterval(30.seconds)

trait ConnectionPoolDslTest extends CatsEffectSuite:

  def prefix: "jdbc" | "ldbc"
  def config: MySQLConfig

  private final val country = TableQuery[CountryTable]
  private final val city    = TableQuery[CityTable]

  test(s"$prefix: ConnectionPool.use with simple select query") {
    PooledDataSource
      .fromConfig[IO](config)
      .use { pool =>
        for
          countries <- country.selectAll.query.to[List].readOnly(Connector.fromDataSource(pool))

          // Check pool status after query
          status <- pool.status
        yield
          assertEquals(countries.length, 239)
          assert(status.idle >= 1)       // At least one connection should be idle
          assertEquals(status.active, 0) // No active connections after query completes
      }
  }

  test(s"$prefix: ConnectionPool.use with filtered query") {

    PooledDataSource
      .fromConfig[IO](config.setMinConnections(1).setMaxConnections(3))
      .use { pool =>
        country
          .select(_.name)
          .where(_.code === "JPN")
          .query
          .to[Option]
          .readOnly(Connector.fromDataSource(pool))
          .map { result =>
            assertEquals(result, Some("Japan"))
          }
      }
  }

  test(s"$prefix: ConnectionPool.use with join query") {
    PooledDataSource
      .fromConfig[IO](config.setMinConnections(1).setMaxConnections(3))
      .use { pool =>
        (country join city)
          .on((country, city) => country.code === city.countryCode)
          .select((country, city) => country.name *: city.name)
          .where((country, _) => country.code === "JPN")
          .limit(5)
          .query
          .to[List]
          .readOnly(Connector.fromDataSource(pool))
          .map { results =>
            assert(results.nonEmpty)
            assert(results.length <= 5)
            results.foreach {
              case (countryName, cityName) =>
                assertEquals(countryName, "Japan")
            }
          }
      }
  }

  test(s"$prefix: ConnectionPool.use with concurrent queries") {
    PooledDataSource
      .fromConfig[IO](config.setMinConnections(2).setMaxConnections(5))
      .use { pool =>
        // Execute multiple queries concurrently
        val queries = (1 to 5).toList.parTraverse { i =>
          country.selectAll
            .limit(1)
            .offset(i)
            .queryTo[Country]
            .to[Option]
            .readOnly(Connector.fromDataSource(pool))
        }

        for {
          results <- queries
          status  <- pool.status
        } yield {
          val flattened = results.flatten
          assertEquals(flattened.length, 5)
          assert(status.total <= 5) // Should not exceed max connections
        }
      }
  }

  test(s"$prefix: ConnectionPool.use handles query exceptions properly") {
    PooledDataSource
      .fromConfig[IO](config.setMinConnections(1).setMaxConnections(2))
      .use { pool =>
        for
          // Try to query a non-existent table
          result <- sql"SELECT * FROM non_existent_table"
                      .query[String]
                      .to[List]
                      .readOnly(Connector.fromDataSource(pool))
                      .attempt

          // Pool should still be functional after error
          status <- pool.status

          // Execute a valid query to confirm pool is still working
          countries <- country.selectAll.limit(1).query.to[List].readOnly(Connector.fromDataSource(pool))
        yield
          assert(result.isLeft)
          assertEquals(status.idle, 1)
          assertEquals(status.active, 0)
          assertEquals(countries.length, 1)
      }
  }

  test(s"$prefix: ConnectionPool metrics tracking with dsl queries") {
    val metricsTracker = PoolMetricsTracker.inMemory[IO]

    for
      tracker <- metricsTracker
      pool    <- PooledDataSource
                .fromConfig[IO](config.setMinConnections(1).setMaxConnections(3), Some(tracker))
                .allocated
                .map(_._1)

      connector = Connector.fromDataSource(pool)

      // Execute several queries
      _ <- country.selectAll.limit(10).query.to[List].readOnly(connector)

      _ <- city.selectAll.limit(5).query.to[List].readOnly(connector)

      // Get metrics
      metrics <- pool.metrics
      _       <- pool.close
    yield
      assertEquals(metrics.totalAcquisitions, 2L)
      assertEquals(metrics.totalReleases, 2L)
      assert(metrics.acquisitionTime > Duration.Zero)
      assert(metrics.usageTime > Duration.Zero)
  }
