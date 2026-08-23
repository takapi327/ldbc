/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.sql.Connection

import ldbc.effect.{ Deferred, Ref }
import ldbc.fx.concurrentFx
import ldbc.fx.Fx
import ldbc.fx.FxSuite

class PoolStateTest extends FxSuite:

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

  test("PoolState.empty[Fx] should create an empty pool state") {
    val emptyState = PoolState.empty[Fx]
    assertEquals(emptyState.connections.size, 0)
    assertEquals(emptyState.idleConnections.size, 0)
    assertEquals(emptyState.waitQueue.size, 0)
    assertEquals(emptyState.metrics, PoolMetrics.empty)
    assertEquals(emptyState.closed, false)
  }

  test("PoolState[Fx] should be created with connections") {
    for
      conn        <- mock
      pooledConn1 <- createPooledConnection("conn-1", conn)
      pooledConn2 <- createPooledConnection("conn-2", conn)
      pooledConn3 <- createPooledConnection("conn-3", conn)
      state = PoolState[Fx](
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

  test("PoolState[Fx] should track idle connections separately") {
    for
      conn        <- mock
      pooledConn1 <- createPooledConnection("conn-1", conn)
      pooledConn2 <- createPooledConnection("conn-2", conn)
      pooledConn3 <- createPooledConnection("conn-3", conn)
      _           <- pooledConn2.state.set(ConnectionState.InUse)
      state = PoolState[Fx](
                connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                idleConnections = Set("conn-1", "conn-3"),
                waitQueue       = Vector.empty,
                metrics         = PoolMetrics.empty,
                closed          = false
              )
    yield
      assertEquals(state.connections.size, 3)
      assertEquals(state.idleConnections.size, 2)
      assert(state.idleConnections.contains("conn-1"))
      assert(!state.idleConnections.contains("conn-2"))
      assert(state.idleConnections.contains("conn-3"))
  }

  test("PoolState[Fx] should handle wait queue") {
    for
      deferred1 <- Deferred[Fx, Either[Throwable, Connection[Fx]]]
      deferred2 <- Deferred[Fx, Either[Throwable, Connection[Fx]]]
      deferred3 <- Deferred[Fx, Either[Throwable, Connection[Fx]]]
      state = PoolState[Fx](
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

  test("PoolState[Fx] should store metrics") {
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
    val state = PoolState[Fx](
      connections     = Vector.empty,
      idleConnections = Set.empty,
      waitQueue       = Vector.empty,
      metrics         = metrics,
      closed          = false
    )
    assertEquals(state.metrics, metrics)
  }

  test("PoolState[Fx] should track closed status") {
    val openState = PoolState[Fx](
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

  test("PoolState[Fx] should maintain consistency between connections and idleConnections") {
    for
      conn        <- mock
      pooledConn1 <- createPooledConnection("conn-1", conn)
      pooledConn2 <- createPooledConnection("conn-2", conn)
      pooledConn3 <- createPooledConnection("conn-3", conn)
      _           <- pooledConn1.state.set(ConnectionState.Idle)
      _           <- pooledConn2.state.set(ConnectionState.InUse)
      _           <- pooledConn3.state.set(ConnectionState.Idle)
      state = PoolState[Fx](
                connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                idleConnections = Set("conn-1", "conn-3"),
                waitQueue       = Vector.empty,
                metrics         = PoolMetrics.empty,
                closed          = false
              )
      conn1State <- pooledConn1.state.get
      conn2State <- pooledConn2.state.get
      conn3State <- pooledConn3.state.get
    yield
      assertEquals(conn1State, ConnectionState.Idle)
      assert(state.idleConnections.contains("conn-1"))
      assertEquals(conn2State, ConnectionState.InUse)
      assert(!state.idleConnections.contains("conn-2"))
      assertEquals(conn3State, ConnectionState.Idle)
      assert(state.idleConnections.contains("conn-3"))
  }

  test("PoolState[Fx] should support copy operations") {
    for
      conn       <- mock
      pooledConn <- createPooledConnection("conn-1", conn)
      deferred   <- Deferred[Fx, Either[Throwable, Connection[Fx]]]
      originalState = PoolState[Fx](
                        connections     = Vector(pooledConn),
                        idleConnections = Set("conn-1"),
                        waitQueue       = Vector(deferred),
                        metrics         = PoolMetrics.empty,
                        closed          = false
                      )
      closedState     = originalState.copy(closed = true)
      emptyQueueState = originalState.copy(waitQueue = Vector.empty)
      updatedMetrics  = originalState.copy(metrics = PoolMetrics.empty.copy(timeouts = 10))
    yield
      assertEquals(originalState.closed, false)
      assertEquals(originalState.waitQueue.size, 1)
      assertEquals(originalState.metrics.timeouts, 0L)
      assertEquals(closedState.closed, true)
      assertEquals(closedState.connections, originalState.connections)
      assertEquals(emptyQueueState.waitQueue.size, 0)
      assertEquals(emptyQueueState.connections, originalState.connections)
      assertEquals(updatedMetrics.metrics.timeouts, 10L)
      assertEquals(updatedMetrics.connections, originalState.connections)
  }

  test("PoolState[Fx] should handle empty and non-empty states correctly") {
    for
      conn        <- mock
      pooledConn1 <- createPooledConnection("conn-1", conn)
      pooledConn2 <- createPooledConnection("conn-2", conn)
      emptyState    = PoolState.empty[Fx]
      nonEmptyState = PoolState[Fx](
                        connections     = Vector(pooledConn1, pooledConn2),
                        idleConnections = Set("conn-1", "conn-2"),
                        waitQueue       = Vector.empty,
                        metrics         = PoolMetrics.empty,
                        closed          = false
                      )
    yield
      assert(emptyState.connections.isEmpty)
      assert(emptyState.idleConnections.isEmpty)
      assert(emptyState.waitQueue.isEmpty)
      assert(nonEmptyState.connections.nonEmpty)
      assertEquals(nonEmptyState.connections.size, 2)
      assert(nonEmptyState.idleConnections.nonEmpty)
      assertEquals(nonEmptyState.idleConnections.size, 2)
  }

  test("PoolState[Fx] should support finding connections by ID") {
    for
      conn        <- mock
      pooledConn1 <- createPooledConnection("conn-1", conn)
      pooledConn2 <- createPooledConnection("conn-2", conn)
      pooledConn3 <- createPooledConnection("conn-3", conn)
      state = PoolState[Fx](
                connections     = Vector(pooledConn1, pooledConn2, pooledConn3),
                idleConnections = Set("conn-1", "conn-2", "conn-3"),
                waitQueue       = Vector.empty,
                metrics         = PoolMetrics.empty,
                closed          = false
              )
      foundConn1 = state.connections.find(_.id == "conn-1")
      foundConn2 = state.connections.find(_.id == "conn-2")
      foundConn4 = state.connections.find(_.id == "conn-4")
    yield
      assert(foundConn1.isDefined)
      assertEquals(foundConn1.get.id, "conn-1")
      assert(foundConn2.isDefined)
      assertEquals(foundConn2.get.id, "conn-2")
      assert(foundConn4.isEmpty)
  }
