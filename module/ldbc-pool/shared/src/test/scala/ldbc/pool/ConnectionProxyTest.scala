/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.sql.Connection

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * Tests for [[ConnectionProxy]] delegation semantics against a [[MockConnection]]: `close()` invokes
 * the release callback (never closing the underlying connection), the callback sees the proxy, and
 * callback errors propagate. The connector's real-database delegation checks (SQL, metadata,
 * savepoints, isolation levels, prepared statements) are deferred to the ldbc-mysql end-to-end suite.
 */
class ConnectionProxyTest extends FxSuite:

  private def mock: Fx[Connection[Fx]] = MockConnection().map(identity)

  private def createPooledConnection(id: String, conn: Connection[Fx]): Fx[PooledConnection[Fx]] =
    for
      currentTime      <- Fx.realTime.map(_.toMillis)
      stateRef         <- Ref.of[Fx, ConnectionState](ConnectionState.Idle)
      lastUsedRef      <- Ref.of(currentTime)
      useCountRef      <- Ref.of(0L)
      lastValidatedRef <- Ref.of(currentTime)
      leakDetectionRef <- Ref.of(Option.empty[ldbc.effect.Fiber[Fx, Unit]])
      bagStateRef      <- Ref.of(BagEntry.STATE_NOT_IN_USE)
    yield PooledConnection[Fx](
      id              = id,
      connection      = conn,
      finalizer       = Fx.unit,
      state           = stateRef,
      createdAt       = currentTime,
      lastUsedAt      = lastUsedRef,
      useCount        = useCountRef,
      lastValidatedAt = lastValidatedRef,
      leakDetection   = leakDetectionRef,
      bagState        = bagStateRef
    )

  test("ConnectionProxy should delegate close() to the release callback") {
    for
      conn            <- mock
      pooledConn      <- createPooledConnection("test-1", conn)
      callbackInvoked <- Ref.of(false)
      proxy = new ConnectionProxy(pooledConn, _ => callbackInvoked.set(true))
      _          <- proxy.close()
      wasInvoked <- callbackInvoked.get
    yield assert(wasInvoked, "Release callback should have been invoked")
  }

  test("ConnectionProxy should not close the underlying connection on close()") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ConnectionProxy(pooledConn, _ => Fx.unit)
      _        <- proxy.close()
      isValid  <- pooledConn.connection.isValid(5)
      isClosed <- pooledConn.connection.isClosed()
    yield
      assert(isValid, "Underlying connection should still be valid")
      assert(!isClosed, "Underlying connection should not be closed")
  }

  test("ConnectionProxy should capture the released connection in the callback") {
    for
      conn            <- mock
      pooledConn      <- createPooledConnection("test-1", conn)
      releasedConnRef <- Ref.of(Option.empty[Connection[Fx]])
      proxy = new ConnectionProxy(pooledConn, c => releasedConnRef.set(Some(c)))
      _            <- pooledConn.state.set(ConnectionState.InUse)
      _            <- proxy.close()
      releasedConn <- releasedConnRef.get
    yield assert(releasedConn.isDefined, "Released connection should be captured")
  }

  test("ConnectionProxy should handle multiple close() calls") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      closeCount <- Ref.of(0)
      proxy = new ConnectionProxy(pooledConn, _ => closeCount.update(_ + 1))
      _     <- proxy.close()
      _     <- proxy.close()
      _     <- proxy.close()
      count <- closeCount.get
    yield assertEquals(count, 3, "Close should be called 3 times")
  }

  test("ConnectionProxy should propagate errors from the release callback") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ConnectionProxy(pooledConn, _ => Fx.raiseError(new RuntimeException("Release failed")))
      result <- proxy.close().attempt
    yield assert(result.isLeft, "Close should fail with callback error")
  }

  test("ConnectionProxy should work with pool-like release behaviour") {
    for
      conn               <- mock
      pooledConn         <- createPooledConnection("test-1", conn)
      connectionReturned <- Ref.of(false)
      releaseCallback =
        (_: Connection[Fx]) => pooledConn.state.set(ConnectionState.Idle) >> connectionReturned.set(true)
      proxy = new ConnectionProxy(pooledConn, releaseCallback)
      _           <- pooledConn.state.set(ConnectionState.InUse)
      stmt        <- proxy.createStatement()
      _           <- stmt.execute("SELECT 1")
      _           <- proxy.close()
      finalState  <- pooledConn.state.get
      wasReturned <- connectionReturned.get
    yield
      assertEquals(finalState, ConnectionState.Idle)
      assert(wasReturned, "Connection should be returned to pool")
  }

  test("ConnectionProxy should preserve the pooled connection reference") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ConnectionProxy(pooledConn, _ => Fx.unit)
    yield
      assertEquals(proxy.pooled.id, "test-1")
      assertEquals(proxy.pooled, pooledConn)
  }
