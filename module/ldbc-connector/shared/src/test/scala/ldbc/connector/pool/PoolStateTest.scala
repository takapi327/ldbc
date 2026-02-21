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

class PoolStateTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  def connection: Resource[IO, Connection[IO]] = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  // Helper to create a pooled connection (based on PooledConnectionTest)
  def createPooledConnection(id: String, conn: Connection[IO]): IO[PooledConnection[IO]] =
    for
      currentTime      <- IO.realTime.map(_.toMillis)
      stateRef         <- Ref[IO].of[ConnectionState](ConnectionState.Idle)
      lastUsedRef      <- Ref[IO].of(currentTime)
      useCountRef      <- Ref[IO].of(0L)
      lastValidatedRef <- Ref[IO].of(currentTime)
      leakDetectionRef <- Ref[IO].of(Option.empty[Fiber[IO, Throwable, Unit]])
      bagStateRef      <- Ref[IO].of(BagEntry.STATE_NOT_IN_USE)
    yield PooledConnection[IO](
      id              = id,
      connection      = conn,
      finalizer       = IO.unit, // For testing, use a no-op finalizer
      state           = stateRef,
      createdAt       = currentTime,
      lastUsedAt      = lastUsedRef,
      useCount        = useCountRef,
      lastValidatedAt = lastValidatedRef,
      leakDetection   = leakDetectionRef,
      bagState        = bagStateRef
    )

  test("PoolState.empty should create an empty pool state") {
    val emptyState = PoolState.empty[IO]

    assertEquals(emptyState.connections.size, 0)
    assertEquals(emptyState.idleConnections.size, 0)
    assertEquals(emptyState.waitQueue.size, 0)
    assertEquals(emptyState.metrics, PoolMetrics.empty)
    assertEquals(emptyState.closed, false)
  }

  test("PoolState should be created with connections") {
    connection.use { conn =>
      for
        pooledConn1 <- createPooledConnection("conn-1", conn)
        pooledConn2 <- createPooledConnection("conn-2", conn)
        pooledConn3 <- createPooledConnection("conn-3", conn)

        state = PoolState[IO](
                  connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                  idleConnections = Set("conn-1", "conn-2", "conn-3"),
                  waitQueue       = Vector.empty,
                  metrics         = PoolMetrics.empty,
                  closed          = false
                )
      yield
        assertEquals(state.connections.size, 3)
        assertEquals(state.idleConnections.size, 3)
        assert(state.idleConnections.contains("conn-1"))
        assert(state.idleConnections.contains("conn-2"))
        assert(state.idleConnections.contains("conn-3"))
    }
  }

  test("PoolState should track idle connections separately") {
    connection.use { conn =>
      for
        pooledConn1 <- createPooledConnection("conn-1", conn)
        pooledConn2 <- createPooledConnection("conn-2", conn)
        pooledConn3 <- createPooledConnection("conn-3", conn)
        _           <- pooledConn2.state.set(ConnectionState.InUse)

        state = PoolState[IO](
                  connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                  idleConnections = Set("conn-1", "conn-3"), // Only idle connections
                  waitQueue       = Vector.empty,
                  metrics         = PoolMetrics.empty,
                  closed          = false
                )
      yield
        assertEquals(state.connections.size, 3)
        assertEquals(state.idleConnections.size, 2)
        assert(state.idleConnections.contains("conn-1"))
        assert(!state.idleConnections.contains("conn-2")) // conn-2 is in use
        assert(state.idleConnections.contains("conn-3"))
    }
  }

  test("PoolState should handle wait queue") {
    for
      deferred1 <- Deferred[IO, Either[Throwable, Connection[IO]]]
      deferred2 <- Deferred[IO, Either[Throwable, Connection[IO]]]
      deferred3 <- Deferred[IO, Either[Throwable, Connection[IO]]]

      state = PoolState[IO](
                connections     = Vector.empty,
                idleConnections = Set.empty,
                waitQueue       = Vector(deferred1, deferred2, deferred3),
                metrics         = PoolMetrics.empty,
                closed          = false
              )
    yield
      assertEquals(state.waitQueue.size, 3)
      assertEquals(state.connections.size, 0)
  }

  test("PoolState should store metrics") {
    val metrics = PoolMetrics(
      acquisitionTime   = 100.millis,
      usageTime         = 500.millis,
      creationTime      = 50.millis,
      timeouts          = 5,
      leaks             = 2,
      totalAcquisitions = 1000,
      totalReleases     = 995,
      totalCreations    = 100,
      totalRemovals     = 10,
      gauges            = Map.empty
    )

    val state = PoolState[IO](
      connections     = Vector.empty,
      idleConnections = Set.empty,
      waitQueue       = Vector.empty,
      metrics         = metrics,
      closed          = false
    )

    assertEquals(state.metrics, metrics)
  }

  test("PoolState should track closed status") {
    val openState = PoolState[IO](
      connections     = Vector.empty,
      idleConnections = Set.empty,
      waitQueue       = Vector.empty,
      metrics         = PoolMetrics.empty,
      closed          = false
    )

    val closedState = openState.copy(closed = true)

    assertEquals(openState.closed, false)
    assertEquals(closedState.closed, true)
  }

  test("PoolState should maintain consistency between connections and idleConnections") {
    connection.use { conn =>
      for
        pooledConn1 <- createPooledConnection("conn-1", conn)
        pooledConn2 <- createPooledConnection("conn-2", conn)
        pooledConn3 <- createPooledConnection("conn-3", conn)

        // Set different states
        _ <- pooledConn1.state.set(ConnectionState.Idle)
        _ <- pooledConn2.state.set(ConnectionState.InUse)
        _ <- pooledConn3.state.set(ConnectionState.Idle)

        state = PoolState[IO](
                  connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                  idleConnections = Set("conn-1", "conn-3"),
                  waitQueue       = Vector.empty,
                  metrics         = PoolMetrics.empty,
                  closed          = false
                )

        // Verify connection states
        conn1State <- pooledConn1.state.get
        conn2State <- pooledConn2.state.get
        conn3State <- pooledConn3.state.get
      yield
        // Idle connections should be in idleConnections set
        assertEquals(conn1State, ConnectionState.Idle)
        assert(state.idleConnections.contains("conn-1"))

        // In-use connections should not be in idleConnections set
        assertEquals(conn2State, ConnectionState.InUse)
        assert(!state.idleConnections.contains("conn-2"))

        assertEquals(conn3State, ConnectionState.Idle)
        assert(state.idleConnections.contains("conn-3"))
    }
  }

  test("PoolState should support copy operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("conn-1", conn)
        deferred   <- Deferred[IO, Either[Throwable, Connection[IO]]]

        originalState = PoolState[IO](
                          connections     = Vector(pooledConn),
                          idleConnections = Set("conn-1"),
                          waitQueue       = Vector(deferred),
                          metrics         = PoolMetrics.empty,
                          closed          = false
                        )

        // Test various copy operations
        closedState     = originalState.copy(closed = true)
        emptyQueueState = originalState.copy(waitQueue = Vector.empty)
        updatedMetrics  = originalState.copy(metrics = PoolMetrics.empty.copy(timeouts = 10))
      yield
        // Original state should remain unchanged
        assertEquals(originalState.closed, false)
        assertEquals(originalState.waitQueue.size, 1)
        assertEquals(originalState.metrics.timeouts, 0L)

        // Copied states should have updated values
        assertEquals(closedState.closed, true)
        assertEquals(closedState.connections, originalState.connections)

        assertEquals(emptyQueueState.waitQueue.size, 0)
        assertEquals(emptyQueueState.connections, originalState.connections)

        assertEquals(updatedMetrics.metrics.timeouts, 10L)
        assertEquals(updatedMetrics.connections, originalState.connections)
    }
  }

  test("PoolState should handle empty and non-empty states correctly") {
    connection.use { conn =>
      for
        pooledConn1 <- createPooledConnection("conn-1", conn)
        pooledConn2 <- createPooledConnection("conn-2", conn)

        emptyState = PoolState.empty[IO]

        nonEmptyState = PoolState[IO](
                          connections     = Vector(pooledConn1, pooledConn2),
                          idleConnections = Set("conn-1", "conn-2"),
                          waitQueue       = Vector.empty,
                          metrics         = PoolMetrics.empty,
                          closed          = false
                        )
      yield
        // Empty state checks
        assert(emptyState.connections.isEmpty)
        assert(emptyState.idleConnections.isEmpty)
        assert(emptyState.waitQueue.isEmpty)

        // Non-empty state checks
        assert(nonEmptyState.connections.nonEmpty)
        assertEquals(nonEmptyState.connections.size, 2)
        assert(nonEmptyState.idleConnections.nonEmpty)
        assertEquals(nonEmptyState.idleConnections.size, 2)
    }
  }

  test("PoolState should support finding connections by ID") {
    connection.use { conn =>
      for
        pooledConn1 <- createPooledConnection("conn-1", conn)
        pooledConn2 <- createPooledConnection("conn-2", conn)
        pooledConn3 <- createPooledConnection("conn-3", conn)

        state = PoolState[IO](
                  connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                  idleConnections = Set("conn-1", "conn-2", "conn-3"),
                  waitQueue       = Vector.empty,
                  metrics         = PoolMetrics.empty,
                  closed          = false
                )

        // Find connections by ID
        foundConn1 = state.connections.find(_.id == "conn-1")
        foundConn2 = state.connections.find(_.id == "conn-2")
        foundConn4 = state.connections.find(_.id == "conn-4")
      yield
        assert(foundConn1.isDefined)
        assertEquals(foundConn1.get.id, "conn-1")

        assert(foundConn2.isDefined)
        assertEquals(foundConn2.get.id, "conn-2")

        assert(foundConn4.isEmpty) // Non-existent connection
    }
  }
