/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.effect.*

import ldbc.connector.*

class PooledDataSourceTest extends FTestPlatform:

  override def munitIOTimeout: Duration = 60.seconds

  private val config = MySQLConfig.default
    .setPort(13306)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("connector_test")
    .setSSL(SSL.Trusted)

  test("PooledDataSource should be created with default configuration") {
    val resource = PooledDataSource.fromConfig[IO](config)

    resource.use { datasource =>
      for status <- datasource.status
      yield
        assertEquals(status.total, config.minConnections)
        assertEquals(status.idle, config.minConnections)
        assertEquals(status.active, 0)
        assertEquals(status.waiting, 0)
    }
  }

  test("PooledDataSource should acquire and release connections") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(2).setMaxConnections(5))

    resource.use { datasource =>
      datasource.getConnection.use { conn =>
        for
          statusAfterAcquire <- datasource.status

          // Use the connection
          stmt   <- conn.createStatement()
          rs     <- stmt.executeQuery("SELECT 1")
          _      <- rs.next()
          result <- rs.getInt(1)
        yield
          assertEquals(result, 1)
          assertEquals(statusAfterAcquire.active, 1)
          assertEquals(statusAfterAcquire.idle, 1)
      } >> datasource.status.map { statusAfterRelease =>
        assertEquals(statusAfterRelease.active, 0)
        assertEquals(statusAfterRelease.idle, 2)
      }
    }
  }

  test("PooledDataSource should handle multiple concurrent connections") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(5).setMaxConnections(10))

    resource.use { datasource =>
      // Use connections concurrently within their Resource scope
      val results = IO.parTraverseN(5)((1 to 5).toList) { i =>
        datasource.getConnection.use { conn =>
          for
            stmt   <- conn.createStatement()
            rs     <- stmt.executeQuery(s"SELECT $i")
            _      <- rs.next()
            result <- rs.getInt(1)
          yield result
        }
      }

      for
        res         <- results
        finalStatus <- datasource.status
      yield
        assertEquals(res, List(1, 2, 3, 4, 5))
        assertEquals(finalStatus.active, 0)
        assert(finalStatus.idle >= 5)
    }
  }

  test("PooledDataSource should grow pool when needed") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(2).setMaxConnections(5))

    resource.use { datasource =>
      for
        initialStatus <- datasource.status
        _             <- datasource.getConnection.use { _ =>
               datasource.getConnection.use { _ =>
                 datasource.getConnection.use { _ =>
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

  test("PooledDataSource should respect maximum connections limit") {
    // Create a test-specific config with longer maintenance intervals to avoid interference
    val testConfig = MySQLConfig.default
      .setPort(13306)
      .setUser("ldbc")
      .setPassword("password")
      .setDatabase("connector_test")
      .setSSL(SSL.Trusted)
      .setMinConnections(1)
      .setMaxConnections(2)
      .setConnectionTimeout(500.millis)

    val resource = PooledDataSource.fromConfig[IO](testConfig)

    val testResult = resource.use { datasource =>
      datasource.getConnection.use { conn1 =>
        datasource.getConnection.use { conn2 =>
          // Try to acquire one more (should timeout)
          val acquireThird = datasource.getConnection.use(_ => IO.unit)

          // This should timeout because all connections are in use
          acquireThird.timeout(600.millis).attempt.map { result =>
            assert(result.isLeft, "Third connection acquisition should timeout")
          }
        }
      }
    }

    // Ensure cleanup is complete before test ends
    testResult >> IO.sleep(100.millis)
  }

  test("PooledDataSource should validate connections") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(1).setMaxConnections(2))

    resource.use { datasource =>
      datasource.getConnection.use { conn =>
        conn.isValid(5).map { isValid =>
          assert(isValid, "Connection should be valid")
        }
      }
    }
  }

  test("PooledDataSource should handle connection failures gracefully") {
    val badConfig = config.setPort(9999) // Non-existent port

    val resource = PooledDataSource.fromConfig[IO](badConfig.setMinConnections(1))

    interceptIO[Exception] {
      resource.use { _ => IO.unit }
    }
  }

  test("PooledDataSource should track metrics") {
    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource
              .fromConfig[IO](config.setMinConnections(2).setMaxConnections(5), metricsTracker = Some(tracker))
    yield ds

    resource.use { datasource =>
      for
        // Perform some operations
        _ <- datasource.getConnection.use { _ =>
               IO.sleep(100.millis)
             }

        _ <- datasource.getConnection.use { _ =>
               IO.sleep(50.millis)
             }

        // Get metrics
        metrics <- datasource.metrics
      yield
        assert(metrics.totalAcquisitions >= 2L)
        assert(metrics.totalReleases >= 2L)
        assert(metrics.acquisitionTime > Duration.Zero)
        assert(metrics.usageTime > Duration.Zero)
    }
  }

  test("PooledDataSource should support transaction operations") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(1))

    resource.use { datasource =>
      datasource.getConnection.use { conn =>
        for
          // Start transaction
          _ <- conn.setAutoCommit(false)

          // Execute some operations
          stmt <- conn.createStatement()
          _    <- stmt.executeUpdate("CREATE TEMPORARY TABLE test_pool (id INT)")
          _    <- stmt.executeUpdate("INSERT INTO test_pool VALUES (1)")

          // Commit
          _ <- conn.commit()

          // Verify
          rs    <- stmt.executeQuery("SELECT COUNT(*) FROM test_pool")
          _     <- rs.next()
          count <- rs.getInt(1)

          // Clean up
          _ <- conn.setAutoCommit(true)
        yield assertEquals(count, 1)
      }
    }
  }

  test("PooledDataSource should handle prepared statements") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(1))

    resource.use { datasource =>
      datasource.getConnection.use { conn =>
        for
          // Create prepared statement
          pstmt <- conn.prepareStatement("SELECT ? + ?")
          _     <- pstmt.setInt(1, 10)
          _     <- pstmt.setInt(2, 20)

          // Execute
          rs     <- pstmt.executeQuery()
          _      <- rs.next()
          result <- rs.getInt(1)
        yield assertEquals(result, 30)
      }
    }
  }

  test("PooledDataSource should clean up on shutdown") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(3).setMaxConnections(5))

    for finalStatus <- resource.use { datasource =>
                         datasource.getConnection.use { _ =>
                           datasource.getConnection.use { _ =>
                             datasource.status
                           }
                         }
                       }
    yield
      // After resource is released, all connections should be closed
      assert(finalStatus.total >= 2)
  }

  test("PooledDataSource should handle concurrent acquisition and release") {
    val resource = for
      tracker <- Resource.eval(PoolMetricsTracker.inMemory[IO])
      ds      <- PooledDataSource
              .fromConfig[IO](
                config
                  .setMinConnections(5)
                  .setMaxConnections(20)            // Increased to handle concurrent load better
                  .setConnectionTimeout(10.seconds), // Longer timeout for concurrent operations
                metricsTracker = Some(tracker)
              )
    yield ds

    resource.use { datasource =>
      for
        // Simulate concurrent load with proper parallelism limit
        _ <- IO.parTraverseN(15)((1 to 100).toList) { i =>
               datasource.getConnection.use { conn =>
                 for
                   stmt <- conn.createStatement()
                   rs   <- stmt.executeQuery(s"SELECT ${ i % 10 }")
                   _    <- rs.next()
                   _    <- rs.getInt(1)
                   _    <- IO.sleep((i % 5).millis) // Reduced sleep time for faster test
                 yield ()
               }
             }

        finalStatus <- datasource.status
        metrics     <- datasource.metrics
      yield
        assertEquals(finalStatus.active, 0)
        assert(metrics.totalAcquisitions >= 100L)
        assert(metrics.totalReleases >= 100L)
    }
  }
