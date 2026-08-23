/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.fx.FxSuite

import scala.concurrent.duration.*

import ldbc.fx.Fx
import ldbc.fx.concurrentFx
import ldbc.effect.Ref
import ldbc.fx.syntax.*

import ldbc.sql.Connection

/**
 * Tests for [[PooledConnection]] over [[ldbc.fx.Fx]], using a [[MockConnection]] as the wrapped
 * physical connection. The connector's real-database round-trip test (`executeQuery`) is deferred to
 * the ldbc-mysql end-to-end suite, since it needs a live result set.
 */
class PooledConnectionTest extends FxSuite:

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
    yield PooledConnection(
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

  test("PooledConnection should be created with correct initial values") {
    for
      conn       <- mock
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

  test("PooledConnection state should be modifiable") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      _          <- pooledConn.state.set(ConnectionState.InUse)
      newState   <- pooledConn.state.get
    yield assertEquals(newState, ConnectionState.InUse)
  }

  test("PooledConnection use count should increment correctly") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      _          <- pooledConn.useCount.update(_ + 1)
      _          <- pooledConn.useCount.update(_ + 1)
      count      <- pooledConn.useCount.get
    yield assertEquals(count, 2L)
  }

  test("PooledConnection last used time should be updatable") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      newTime = System.currentTimeMillis() + 1000
      _           <- pooledConn.lastUsedAt.set(newTime)
      updatedTime <- pooledConn.lastUsedAt.get
    yield assertEquals(updatedTime, newTime)
  }

  test("PooledConnection last validated time should be updatable") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      newTime = System.currentTimeMillis() + 2000
      _           <- pooledConn.lastValidatedAt.set(newTime)
      updatedTime <- pooledConn.lastValidatedAt.get
    yield assertEquals(updatedTime, newTime)
  }

  test("PooledConnection leak detection should handle canceler references") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      canceler   <- concurrentFx.start(Fx.sleep(1.hour))
      _          <- pooledConn.leakDetection.set(Some(canceler))
      leak       <- pooledConn.leakDetection.get
      _          <- canceler.cancel
    yield assert(leak.isDefined, "Leak detection canceler should be set")
  }

  test("Multiple PooledConnections should have unique IDs") {
    for
      conn  <- mock
      conn1 <- createPooledConnection("conn-1", conn)
      conn2 <- createPooledConnection("conn-2", conn)
      conn3 <- createPooledConnection("conn-3", conn)
    yield
      assertNotEquals(conn1.id, conn2.id)
      assertNotEquals(conn2.id, conn3.id)
      assertNotEquals(conn1.id, conn3.id)
  }

  test("PooledConnection state transitions should work correctly") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      _          <- pooledConn.state.set(ConnectionState.Reserved)
      reserved   <- pooledConn.state.get
      _          <- pooledConn.state.set(ConnectionState.InUse)
      inUse      <- pooledConn.state.get
      _          <- pooledConn.state.set(ConnectionState.Idle)
      idle       <- pooledConn.state.get
      _          <- pooledConn.state.set(ConnectionState.Removed)
      removed    <- pooledConn.state.get
    yield
      assertEquals(reserved, ConnectionState.Reserved)
      assertEquals(inUse, ConnectionState.InUse)
      assertEquals(idle, ConnectionState.Idle)
      assertEquals(removed, ConnectionState.Removed)
  }

  test("PooledConnection fields should be accessible") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
    yield assert(pooledConn.createdAt > 0, "Created time should be positive")
  }

  test("PooledConnection should handle concurrent state modifications") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      _ <- (1 to 100).toList.parTraverseN(10) { i =>
             if i % 2 == 0 then pooledConn.state.set(ConnectionState.InUse)
             else pooledConn.state.set(ConnectionState.Idle)
           }
      finalState <- pooledConn.state.get
    yield assert(
      finalState == ConnectionState.InUse || finalState == ConnectionState.Idle,
      s"Final state $finalState should be either InUse or Idle"
    )
  }

  test("PooledConnection should handle concurrent use count updates") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      _          <- (1 to 100).toList.parTraverseN(10)(_ => pooledConn.useCount.update(_ + 1))
      finalCount <- pooledConn.useCount.get
    yield assertEquals(finalCount, 100L)
  }

  test("PooledConnection should verify the wrapped connection") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-1", conn)
      isValid    <- pooledConn.connection.isValid(5)
      isClosed   <- pooledConn.connection.isClosed()
    yield
      assert(isValid, "Connection should be valid")
      assert(!isClosed, "Connection should not be closed")
  }

  test("PooledConnection should implement BagEntry correctly") {
    for
      conn         <- mock
      pooledConn   <- createPooledConnection("test-bag", conn)
      initialState <- pooledConn.getState
      _ = assertEquals(initialState, BagEntry.STATE_NOT_IN_USE)
      _        <- pooledConn.setState(BagEntry.STATE_IN_USE)
      newState <- pooledConn.getState
      _ = assertEquals(newState, BagEntry.STATE_IN_USE)
      success <- pooledConn.compareAndSet(BagEntry.STATE_IN_USE, BagEntry.STATE_NOT_IN_USE)
      _ = assertEquals(success, true)
      state1 <- pooledConn.getState
      _ = assertEquals(state1, BagEntry.STATE_NOT_IN_USE)
      failure <- pooledConn.compareAndSet(BagEntry.STATE_IN_USE, BagEntry.STATE_REMOVED)
      _ = assertEquals(failure, false)
      state2 <- pooledConn.getState
      _ = assertEquals(state2, BagEntry.STATE_NOT_IN_USE)
    yield ()
  }

  test("PooledConnection BagEntry state should be independent from ConnectionState") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("test-states", conn)
      _          <- pooledConn.state.set(ConnectionState.InUse)
      _          <- pooledConn.setState(BagEntry.STATE_IN_USE)
      connState  <- pooledConn.state.get
      bagState   <- pooledConn.getState
      _ = assertEquals(connState, ConnectionState.InUse)
      _ = assertEquals(bagState, BagEntry.STATE_IN_USE)
      _          <- pooledConn.state.set(ConnectionState.Idle)
      connState2 <- pooledConn.state.get
      bagState2  <- pooledConn.getState
      _ = assertEquals(connState2, ConnectionState.Idle)
      _ = assertEquals(bagState2, BagEntry.STATE_IN_USE)
    yield ()
  }
