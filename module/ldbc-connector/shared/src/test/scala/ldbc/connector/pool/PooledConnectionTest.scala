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

class PooledConnectionTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  def connection: Resource[IO, Connection[IO]] = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  // Helper to create a pooled connection
  def createPooledConnection(id: String, conn: Connection[IO]): IO[PooledConnection[IO]] =
    for
      currentTime      <- IO.realTime.map(_.toMillis)
      stateRef         <- Ref[IO].of[ConnectionState](ConnectionState.Idle)
      lastUsedRef      <- Ref[IO].of(currentTime)
      useCountRef      <- Ref[IO].of(0L)
      lastValidatedRef <- Ref[IO].of(currentTime)
      leakDetectionRef <- Ref[IO].of(Option.empty[Fiber[IO, Throwable, Unit]])
    yield PooledConnection[IO](
      id              = id,
      connection      = conn,
      finalizer       = IO.unit,  // For testing, use a no-op finalizer
      state           = stateRef,
      createdAt       = currentTime,
      lastUsedAt      = lastUsedRef,
      useCount        = useCountRef,
      lastValidatedAt = lastValidatedRef,
      leakDetection   = leakDetectionRef
    )

  test("PooledConnection should be created with correct initial values") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        state      <- pooledConn.state.get
        useCount   <- pooledConn.useCount.get
        leak       <- pooledConn.leakDetection.get
      yield
        assertEquals(pooledConn.id, "test-1")
        assertEquals(state, ConnectionState.Idle)
        assertEquals(useCount, 0L)
        assertEquals(leak, None)
    }
  }

  test("PooledConnection state should be modifiable") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        _          <- pooledConn.state.set(ConnectionState.InUse)
        newState   <- pooledConn.state.get
      yield assertEquals(newState, ConnectionState.InUse)
    }
  }

  test("PooledConnection use count should increment correctly") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        _          <- pooledConn.useCount.update(_ + 1)
        _          <- pooledConn.useCount.update(_ + 1)
        count      <- pooledConn.useCount.get
      yield assertEquals(count, 2L)
    }
  }

  test("PooledConnection last used time should be updatable") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        newTime = System.currentTimeMillis() + 1000
        _           <- pooledConn.lastUsedAt.set(newTime)
        updatedTime <- pooledConn.lastUsedAt.get
      yield assertEquals(updatedTime, newTime)
    }
  }

  test("PooledConnection last validated time should be updatable") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        newTime = System.currentTimeMillis() + 2000
        _           <- pooledConn.lastValidatedAt.set(newTime)
        updatedTime <- pooledConn.lastValidatedAt.get
      yield assertEquals(updatedTime, newTime)
    }
  }

  test("PooledConnection leak detection should handle fiber references") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        fiber      <- IO.sleep(1.hour).start
        _          <- pooledConn.leakDetection.set(Some(fiber))
        leak       <- pooledConn.leakDetection.get
        _          <- fiber.cancel // Clean up
      yield assert(leak.isDefined, "Leak detection fiber should be set")
    }
  }

  test("Multiple PooledConnections should have unique IDs") {
    connection.use { conn =>
      for
        conn1 <- createPooledConnection("conn-1", conn)
        conn2 <- createPooledConnection("conn-2", conn)
        conn3 <- createPooledConnection("conn-3", conn)
      yield
        assertNotEquals(conn1.id, conn2.id)
        assertNotEquals(conn2.id, conn3.id)
        assertNotEquals(conn1.id, conn3.id)
    }
  }

  test("PooledConnection state transitions should work correctly") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        // Idle -> Reserved
        _        <- pooledConn.state.set(ConnectionState.Reserved)
        reserved <- pooledConn.state.get

        // Reserved -> InUse
        _     <- pooledConn.state.set(ConnectionState.InUse)
        inUse <- pooledConn.state.get

        // InUse -> Idle
        _    <- pooledConn.state.set(ConnectionState.Idle)
        idle <- pooledConn.state.get

        // Idle -> Removed
        _       <- pooledConn.state.set(ConnectionState.Removed)
        removed <- pooledConn.state.get
      yield
        assertEquals(reserved, ConnectionState.Reserved)
        assertEquals(inUse, ConnectionState.InUse)
        assertEquals(idle, ConnectionState.Idle)
        assertEquals(removed, ConnectionState.Removed)
    }
  }

  test("PooledConnection fields should be accessible") {
    connection.use { conn =>
      for
        currentTime <- IO.realTime.map(_.toMillis)
        pooledConn  <- createPooledConnection("test-1", conn)
      yield
        assert(pooledConn.createdAt > 0, "Created time should be positive")
        assert(pooledConn.id.nonEmpty, "ID should not be empty")
        assert(pooledConn.id == "test-1", "ID should match the provided ID")
        assert(pooledConn.createdAt <= currentTime, "Created time should be at or before current time")
    }
  }

  test("PooledConnection should handle concurrent state modifications") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        // Simulate concurrent state changes
        _ <- IO.parTraverseN(10)((1 to 100).toList) { i =>
               if i % 2 == 0 then pooledConn.state.set(ConnectionState.InUse)
               else pooledConn.state.set(ConnectionState.Idle)
             }
        finalState <- pooledConn.state.get
      yield
        // Final state should be one of the two states we set
        assert(
          finalState == ConnectionState.InUse || finalState == ConnectionState.Idle,
          s"Final state $finalState should be either InUse or Idle"
        )
    }
  }

  test("PooledConnection should handle concurrent use count updates") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        // Increment use count concurrently
        _ <- IO.parTraverseN(10)((1 to 100).toList) { _ =>
               pooledConn.useCount.update(_ + 1)
             }
        finalCount <- pooledConn.useCount.get
      yield assertEquals(finalCount, 100L)
    }
  }

  test("PooledConnection should verify actual database connection") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        isValid    <- pooledConn.connection.isValid(5)
        isClosed   <- pooledConn.connection.isClosed()
      yield
        assert(isValid, "Connection should be valid")
        assert(!isClosed, "Connection should not be closed")
    }
  }

  test("PooledConnection should work with actual database operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        _          <- pooledConn.state.set(ConnectionState.InUse)
        stmt       <- pooledConn.connection.createStatement()
        resultSet  <- stmt.executeQuery("SELECT 1 as num")
        _          <- resultSet.next()
        result     <- resultSet.getInt("num")
        _          <- pooledConn.state.set(ConnectionState.Idle)
      yield assertEquals(result, 1)
    }
  }
