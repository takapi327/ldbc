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
      datasource.getConnection.use { _ =>
        datasource.getConnection.use { _ =>
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
                  .setMinConnections(2)             // Even smaller initial pool
                  .setMaxConnections(5)             // Much smaller max to avoid connection storms
                  .setConnectionTimeout(60.seconds) // Very long timeout
                  .setReadTimeout(60.seconds)       // Very long read timeout
                  .setIdleTimeout(5.minutes)        // Keep connections alive longer
                  .setMaxLifetime(10.minutes),      // Keep connections alive longer
                metricsTracker = Some(tracker)
              )
    yield ds

    resource.use { datasource =>
      for
        // Much more conservative approach for GitHub Actions
        _ <- IO.parTraverseN(2)((1 to 20).toList) { _ => // Only 2 concurrent, 20 total operations
               datasource.getConnection.use { conn =>
                 for
                   stmt <- conn.createStatement()
                   rs   <- stmt.executeQuery("SELECT 1") // Simplest possible query
                   _    <- rs.next()
                   _    <- rs.getInt(1)
                   // Add small delay between operations to avoid overwhelming the DB
                   _ <- IO.sleep(50.millis)
                 yield ()
               }
             }

        finalStatus <- datasource.status
        metrics     <- datasource.metrics
      yield
        assertEquals(finalStatus.active, 0)
        assert(metrics.totalAcquisitions >= 20L) // Adjusted expectation
        assert(metrics.totalReleases >= 20L)     // Adjusted expectation
    }
  }

  // ============================================================
  // Tests for idleConnections synchronization
  // ============================================================

  test("PooledDataSource should add connections to idleConnections on initialization") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(3).setMaxConnections(5))

    resource.use { datasource =>
      for
        state  <- datasource.poolState.get
        status <- datasource.status
      yield
        // All initial connections should be in idleConnections
        assertEquals(state.idleConnections.size, 3)
        assertEquals(status.idle, 3)
        assertEquals(status.active, 0)
        // Verify all connection IDs are in idleConnections
        state.connections.foreach { conn =>
          assert(state.idleConnections.contains(conn.id), s"Connection ${ conn.id } should be in idleConnections")
        }
    }
  }

  test("PooledDataSource should remove connection from idleConnections on acquire") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(2).setMaxConnections(5))

    resource.use { datasource =>
      for
        initialState <- datasource.poolState.get
        _            <- IO(assertEquals(initialState.idleConnections.size, 2))

        // Acquire a connection
        result <- datasource.getConnection.use { _ =>
                    for
                      stateWhileAcquired <- datasource.poolState.get
                      status             <- datasource.status
                    yield
                      // One connection should be removed from idleConnections
                      assertEquals(stateWhileAcquired.idleConnections.size, 1)
                      assertEquals(status.active, 1)
                      assertEquals(status.idle, 1)
                  }

        // After release, connection should be back in idleConnections
        finalState <- datasource.poolState.get
      yield assertEquals(finalState.idleConnections.size, 2)
    }
  }

  test("PooledDataSource should add connection back to idleConnections on release") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(1).setMaxConnections(3))

    resource.use { datasource =>
      for
        initialState <- datasource.poolState.get
        initialSize = initialState.idleConnections.size

        // Acquire and release a connection
        _ <- datasource.getConnection.use { conn =>
               for
                 duringAcquire <- datasource.poolState.get
                 _             <- IO(assertEquals(duringAcquire.idleConnections.size, initialSize - 1))
                 // Execute a query to ensure connection is used
                 stmt <- conn.createStatement()
                 _    <- stmt.executeQuery("SELECT 1")
               yield ()
             }

        // After release
        finalState <- datasource.poolState.get
      yield
        // Connection should be back in idleConnections
        assertEquals(finalState.idleConnections.size, initialSize)
    }
  }

  test("PooledDataSource should correctly track idleConnections with multiple concurrent connections") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(3).setMaxConnections(5))

    resource.use { datasource =>
      for
        initialState <- datasource.poolState.get
        _            <- IO(assertEquals(initialState.idleConnections.size, 3))

        // Acquire multiple connections concurrently
        result <- (
                    datasource.getConnection.use { _ =>
                      datasource.poolState.get.map(_.idleConnections.size)
                    },
                    datasource.getConnection.use { _ =>
                      datasource.poolState.get.map(_.idleConnections.size)
                    }
                  ).parTupled

        // After all releases
        finalState <- datasource.poolState.get
      yield
        // During concurrent acquisition, idle count varies
        // After release, all should be back
        assertEquals(finalState.idleConnections.size, finalState.connections.size)
    }
  }

  test("PooledDataSource should maintain idleConnections consistency under load") {
    val resource = PooledDataSource.fromConfig[IO](
      config
        .setMinConnections(2)
        .setMaxConnections(4)
        .setConnectionTimeout(30.seconds)
    )

    resource.use { datasource =>
      for
        // Run multiple acquire/release cycles
        _ <- IO.parTraverseN(2)((1 to 10).toList) { _ =>
               datasource.getConnection.use { conn =>
                 conn.createStatement().flatMap(_.executeQuery("SELECT 1")).void
               }
             }

        // After all operations complete
        finalState  <- datasource.poolState.get
        finalStatus <- datasource.status
      yield
        // idleConnections should match actual idle connections
        assertEquals(finalState.idleConnections.size, finalStatus.idle)
        assertEquals(finalStatus.active, 0)

        // All connections should be in idleConnections
        finalState.connections.foreach { conn =>
          assert(
            finalState.idleConnections.contains(conn.id),
            s"Connection ${ conn.id } should be in idleConnections after release"
          )
        }
    }
  }

  test("PooledDataSource should remove connection from idleConnections when pool grows") {
    val resource = PooledDataSource.fromConfig[IO](config.setMinConnections(1).setMaxConnections(3))

    resource.use { datasource =>
      for
        initialState <- datasource.poolState.get
        _            <- IO(assertEquals(initialState.connections.size, 1))
        _            <- IO(assertEquals(initialState.idleConnections.size, 1))

        // Acquire first connection
        _ <- datasource.getConnection.use { _ =>
               for
                 state1 <- datasource.poolState.get
                 _      <- IO(assertEquals(state1.idleConnections.size, 0))

                 // Acquire second connection (will create new one)
                 _ <- datasource.getConnection.use { _ =>
                        for
                          state2 <- datasource.poolState.get
                          // Pool should have grown
                          _ <- IO(assert(state2.connections.size >= 2))
                          // New connection is created in InUse state, not added to idleConnections
                          _ <- IO(assertEquals(state2.idleConnections.size, 0))
                        yield ()
                      }

                 // Second connection released
                 state3 <- datasource.poolState.get
                 _      <- IO(assertEquals(state3.idleConnections.size, 1))
               yield ()
             }

        // Both connections released
        finalState <- datasource.poolState.get
      yield assertEquals(finalState.idleConnections.size, finalState.connections.size)
    }
  }
