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

import ldbc.sql.Connection

/**
 * Tests for [[KeepaliveExecutor]]: its `Fx.sleep`-recursion loop periodically validates idle
 * connections, removes ones that fail validation, and stops when the pool is closed. A [[MockPool]]
 * with [[MockConnection]]s stands in for a real pool.
 */
class KeepaliveExecutorTest extends FxSuite:

  private def createPooledConnection(
    id:         String,
    connection: Connection[Fx],
    createdAt:  Long,
    lastUsedAt: Long
  ): Fx[PooledConnection[Fx]] =
    for
      stateRef         <- Ref.of(ConnectionState.Idle)
      lastUsedRef      <- Ref.of(lastUsedAt)
      useCountRef      <- Ref.of(0L)
      lastValidatedRef <- Ref.of(createdAt)
      leakDetectionRef <- Ref.of(Option.empty[ldbc.effect.Fiber[Fx, Unit]])
      bagStateRef      <- Ref.of(BagEntry.STATE_NOT_IN_USE)
    yield PooledConnection[Fx](
      id              = id,
      connection      = connection,
      finalizer       = Fx.unit,
      state           = stateRef,
      createdAt       = createdAt,
      lastUsedAt      = lastUsedRef,
      useCount        = useCountRef,
      lastValidatedAt = lastValidatedRef,
      leakDetection   = leakDetectionRef,
      bagState        = bagStateRef
    )

  test("KeepaliveExecutor should start and stop correctly") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      state   <- Ref.of(PoolState.empty[Fx])
      pool = new MockPool(state, tracker, PoolLogger.noop[Fx])
      keepalive = KeepaliveExecutor[Fx](100.milliseconds, tracker)
      _ <- keepalive.start(pool).use(_ => Fx.sleep(50.milliseconds))
    yield assert(true)
  }

  test("KeepaliveExecutor should validate idle connections") {
    for
      tracker <- PoolMetricsTracker.inMemory[Fx]
      conn1   <- MockConnection()
      conn2   <- MockConnection()
      now     <- Fx.realTime.map(_.toMillis)
      pooledConn1 <- createPooledConnection("conn-1", conn1, now, now - 1000)
      pooledConn2 <- createPooledConnection("conn-2", conn2, now, now - 2000)
      state <- Ref.of(
                 PoolState[Fx](
                   connections     = Vector(pooledConn1, pooledConn2),
                   idleConnections = Set("conn-1", "conn-2"),
                   waitQueue       = Vector.empty,
                   metrics         = PoolMetrics.empty,
                   closed          = false
                 )
               )
      pool = new MockPool(state, tracker, PoolLogger.noop[Fx], validateFn = _.isValid(1))
      keepalive = KeepaliveExecutor[Fx](100.milliseconds, tracker)
      _     <- keepalive.start(pool).use(_ => Fx.sleep(250.milliseconds))
      count <- conn1.validationCount.get.flatMap(c1 => conn2.validationCount.get.map(_ + c1))
    yield assert(count >= 2, s"Expected at least 2 validations, got $count")
  }

  test("KeepaliveExecutor should handle validation failures") {
    for
      tracker     <- PoolMetricsTracker.inMemory[Fx]
      validConn   <- MockConnection(isValidResult = true)
      invalidConn <- MockConnection(isValidResult = false)
      now         <- Fx.realTime.map(_.toMillis)
      pooledConn1 <- createPooledConnection("conn-1", validConn, now, now)
      pooledConn2 <- createPooledConnection("conn-2", invalidConn, now, now)
      state <- Ref.of(
                 PoolState[Fx](
                   connections     = Vector(pooledConn1, pooledConn2),
                   idleConnections = Set("conn-1", "conn-2"),
                   waitQueue       = Vector.empty,
                   metrics         = PoolMetrics.empty,
                   closed          = false
                 )
               )
      removed <- Ref.of(List.empty[String])
      pool = new MockPool(
               state,
               tracker,
               PoolLogger.noop[Fx],
               validateFn = _.isValid(1),
               removeFn   = pooled => removed.update(_ :+ pooled.id)
             )
      keepalive = KeepaliveExecutor[Fx](100.milliseconds, tracker)
      _            <- keepalive.start(pool).use(_ => Fx.sleep(250.milliseconds))
      validations  <- validConn.validationCount.get.flatMap(v => invalidConn.validationCount.get.map(_ + v))
      removedConns <- removed.get
    yield
      assert(validations >= 2, s"Expected at least 2 validation attempts, got $validations")
      assert(removedConns.contains("conn-2"), s"Invalid connection should be removed, removed: $removedConns")
  }

  test("KeepaliveExecutor should stop when pool is closed") {
    for
      tracker    <- PoolMetricsTracker.inMemory[Fx]
      conn       <- MockConnection()
      now        <- Fx.realTime.map(_.toMillis)
      pooledConn <- createPooledConnection("conn-1", conn, now, now)
      state <- Ref.of(
                 PoolState[Fx](
                   connections     = Vector(pooledConn),
                   idleConnections = Set("conn-1"),
                   waitQueue       = Vector.empty,
                   metrics         = PoolMetrics.empty,
                   closed          = false
                 )
               )
      pool = new MockPool(state, tracker, PoolLogger.noop[Fx], validateFn = _.isValid(1))
      keepalive = KeepaliveExecutor[Fx](100.milliseconds, tracker)
      _ <- keepalive.start(pool).use { _ =>
             for
               _      <- Fx.sleep(150.milliseconds)
               count1 <- conn.validationCount.get
               _      <- state.update(_.copy(closed = true))
               _      <- Fx.sleep(300.milliseconds)
               count2 <- conn.validationCount.get
             yield
               assert(count1 > 0, "Should have validated before closing")
               assertEquals(count1, count2, "Should not validate after pool is closed")
           }
    yield ()
  }
