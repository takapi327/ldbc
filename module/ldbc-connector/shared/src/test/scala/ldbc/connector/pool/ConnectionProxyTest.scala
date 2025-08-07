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

class ConnectionProxyTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  def connection: Resource[IO, Connection[IO]] = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  // Helper to create a pooled connection (same as PooledConnectionTest)
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
      finalizer       = IO.unit,
      state           = stateRef,
      createdAt       = currentTime,
      lastUsedAt      = lastUsedRef,
      useCount        = useCountRef,
      lastValidatedAt = lastValidatedRef,
      leakDetection   = leakDetectionRef
    )

  test("ConnectionProxy should delegate close() to release callback") {
    connection.use { conn =>
      for
        pooledConn      <- createPooledConnection("test-1", conn)
        callbackInvoked <- Ref[IO].of(false)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => callbackInvoked.set(true)
                )

        _          <- proxy.close()
        wasInvoked <- callbackInvoked.get
      yield assert(wasInvoked, "Release callback should have been invoked")
    }
  }

  test("ConnectionProxy should not close underlying connection on close()") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit // No-op callback
                )

        _ <- proxy.close()

        // Verify underlying connection is still valid
        isValid  <- pooledConn.connection.isValid(5)
        isClosed <- pooledConn.connection.isClosed()
      yield
        assert(isValid, "Underlying connection should still be valid")
        assert(!isClosed, "Underlying connection should not be closed")
    }
  }

  test("ConnectionProxy should delegate database operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        // Test statement creation and query execution
        stmt   <- proxy.createStatement()
        rs     <- stmt.executeQuery("SELECT 1 as num")
        _      <- rs.next()
        result <- rs.getInt("num")
      yield assertEquals(result, 1)
    }
  }

  test("ConnectionProxy should delegate transaction operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        // Test transaction operations
        _          <- proxy.setAutoCommit(false)
        autoCommit <- proxy.getAutoCommit()

        stmt <- proxy.createStatement()
        _    <- stmt.executeUpdate("CREATE TEMPORARY TABLE test_proxy (id INT)")
        _    <- stmt.executeUpdate("INSERT INTO test_proxy VALUES (1)")

        _ <- proxy.commit()

        rs    <- stmt.executeQuery("SELECT COUNT(*) FROM test_proxy")
        _     <- rs.next()
        count <- rs.getInt(1)

        _ <- proxy.setAutoCommit(true)
      yield
        assert(!autoCommit, "Auto-commit should be false")
        assertEquals(count, 1)
    }
  }

  test("ConnectionProxy should delegate metadata operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        // Test metadata operations
        metadata <- proxy.getMetaData()
        catalog  <- proxy.getCatalog()
        schema   <- proxy.getSchema()
        isValid  <- proxy.isValid(5)
        isClosed <- proxy.isClosed()
      yield
        assert(metadata != null, "Metadata should not be null")
        assert(isValid, "Connection should be valid")
        assert(!isClosed, "Connection should not be closed")
    }
  }

  test("ConnectionProxy should delegate prepared statement operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        // Test prepared statements
        pstmt <- proxy.prepareStatement("SELECT ? + ? as sum")
        _     <- pstmt.setInt(1, 10)
        _     <- pstmt.setInt(2, 20)

        rs     <- pstmt.executeQuery()
        _      <- rs.next()
        result <- rs.getInt("sum")
      yield assertEquals(result, 30)
    }
  }

  test("ConnectionProxy should track release callback with connection state") {
    connection.use { conn =>
      for
        pooledConn      <- createPooledConnection("test-1", conn)
        releasedConnRef <- Ref[IO].of(Option.empty[Connection[IO]])

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  conn => releasedConnRef.set(Some(conn))
                )

        _ <- pooledConn.state.set(ConnectionState.InUse)
        _ <- proxy.close()

        releasedConn <- releasedConnRef.get
      yield assert(releasedConn.isDefined, "Released connection should be captured")
    }
  }

  test("ConnectionProxy should handle multiple close() calls") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)
        closeCount <- Ref[IO].of(0)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => closeCount.update(_ + 1)
                )

        // Call close multiple times
        _ <- proxy.close()
        _ <- proxy.close()
        _ <- proxy.close()

        count <- closeCount.get
      yield assertEquals(count, 3, "Close should be called 3 times")
    }
  }

  test("ConnectionProxy should delegate savepoint operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        _ <- proxy.setAutoCommit(false)

        // Create savepoint
        savepoint <- proxy.setSavepoint("test_savepoint")

        stmt <- proxy.createStatement()
        _    <- stmt.executeUpdate("CREATE TEMPORARY TABLE test_savepoint (id INT)")
        _    <- stmt.executeUpdate("INSERT INTO test_savepoint VALUES (1)")

        // Rollback to savepoint
        _ <- proxy.rollback(savepoint)

        // Release savepoint
        _ <- proxy.releaseSavepoint(savepoint)

        _ <- proxy.commit()
        _ <- proxy.setAutoCommit(true)
      yield assert(true, "Savepoint operations should complete successfully")
    }
  }

  test("ConnectionProxy should delegate isolation level operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        originalLevel <- proxy.getTransactionIsolation()

        // Try different isolation levels
        _             <- proxy.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED)
        readCommitted <- proxy.getTransactionIsolation()

        _        <- proxy.setTransactionIsolation(originalLevel)
        restored <- proxy.getTransactionIsolation()
      yield
        assertEquals(readCommitted, java.sql.Connection.TRANSACTION_READ_COMMITTED)
        assertEquals(restored, originalLevel)
    }
  }

  test("ConnectionProxy should delegate read-only operations") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )

        originalReadOnly <- proxy.isReadOnly

        _            <- proxy.setReadOnly(true)
        afterSetTrue <- proxy.isReadOnly

        _             <- proxy.setReadOnly(false)
        afterSetFalse <- proxy.isReadOnly
      yield
        assert(!originalReadOnly, "Connection should not be read-only by default")
        assert(afterSetTrue, "Connection should be read-only after setting to true")
        assert(!afterSetFalse, "Connection should not be read-only after setting to false")
    }
  }

  test("ConnectionProxy should handle errors in release callback") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.raiseError(new RuntimeException("Release failed"))
                )

        // Close should propagate the error
        result <- proxy.close().attempt
      yield assert(result.isLeft, "Close should fail with callback error")
    }
  }

  test("ConnectionProxy should work with actual pool-like behavior") {
    connection.use { conn =>
      for
        pooledConn         <- createPooledConnection("test-1", conn)
        connectionReturned <- Ref[IO].of(false)

        // Simulate pool behavior
        releaseCallback = (_: Connection[IO]) =>
                            for
                              _ <- pooledConn.state.set(ConnectionState.Idle)
                              _ <- connectionReturned.set(true)
                            yield ()

        proxy = new ConnectionProxy[IO](pooledConn, releaseCallback)

        // Simulate usage
        _ <- pooledConn.state.set(ConnectionState.InUse)

        // Do some work
        stmt <- proxy.createStatement()
        rs   <- stmt.executeQuery("SELECT 1")
        _    <- rs.next()

        // Return to pool
        _ <- proxy.close()

        finalState  <- pooledConn.state.get
        wasReturned <- connectionReturned.get
      yield
        assertEquals(finalState, ConnectionState.Idle)
        assert(wasReturned, "Connection should be returned to pool")
    }
  }

  test("ConnectionProxy should preserve pooled connection reference") {
    connection.use { conn =>
      for
        pooledConn <- createPooledConnection("test-1", conn)

        proxy = new ConnectionProxy[IO](
                  pooledConn,
                  _ => IO.unit
                )
      yield
        assertEquals(proxy.pooled.id, "test-1")
        assertEquals(proxy.pooled, pooledConn)
    }
  }
