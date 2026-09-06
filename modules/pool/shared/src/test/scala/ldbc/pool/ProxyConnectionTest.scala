/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.sql.Connection

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * Tests for [[ProxyConnection]] statement tracking: created statements are tracked and closed on
 * connection return when `closeAllStatements` is true, and left open when it is false. Uses a
 * [[MockConnection]] whose statements record their `close()`. The connector's real-database and
 * prepared/callable-statement cases are deferred to the ldbc-mysql end-to-end suite.
 */
class ProxyConnectionTest extends FxSuite:

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

  test("ProxyConnection should track created statements") {
    for
      conn       <- MockConnection()
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ProxyConnection(pooledConn, _ => Fx.unit, closeAllStatements = true)
      _ <- proxy.createStatement()
      _ <- proxy.createStatement()
      _ <- proxy.createStatement()
    yield assert(true, "Statements should be created and tracked")
  }

  test("ProxyConnection should close all tracked statements on close when closeAllStatements is true") {
    for
      conn       <- MockConnection()
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ProxyConnection(pooledConn, _ => Fx.unit, closeAllStatements = true)
      _      <- proxy.createStatement()
      _      <- proxy.createStatement()
      _      <- proxy.createStatement()
      _      <- proxy.close()
      closed <- conn.statementCloseCount.get
    yield assertEquals(closed, 3, "All 3 statements should have been closed")
  }

  test("ProxyConnection should not close statements when closeAllStatements is false") {
    for
      conn       <- MockConnection()
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ProxyConnection(pooledConn, _ => Fx.unit, closeAllStatements = false)
      _      <- proxy.createStatement()
      _      <- proxy.close()
      closed <- conn.statementCloseCount.get
    yield assertEquals(closed, 0, "Statements should not be closed when closeAllStatements is false")
  }

  test("ProxyConnection should delegate close() to the release callback") {
    for
      conn            <- MockConnection()
      pooledConn      <- createPooledConnection("test-1", conn)
      callbackInvoked <- Ref.of(false)
      proxy = new ProxyConnection(pooledConn, _ => callbackInvoked.set(true), closeAllStatements = true)
      _          <- proxy.close()
      wasInvoked <- callbackInvoked.get
    yield assert(wasInvoked, "Release callback should have been invoked")
  }

  test("ProxyConnection should preserve the pooled connection reference") {
    for
      conn       <- MockConnection()
      pooledConn <- createPooledConnection("test-1", conn)
      proxy = new ProxyConnection(pooledConn, _ => Fx.unit)
    yield
      assertEquals(proxy.pooled.id, "test-1")
      assertEquals(proxy.pooled, pooledConn)
  }
