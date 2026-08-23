/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.sql.Connection

import ldbc.effect.Ref
import ldbc.fx.Fx

/**
 * A configurable mock [[PooledDataSource]] for testing the background maintenance tasks (housekeeper,
 * keepalive, adaptive sizer, status reporter) in isolation. It exposes a real [[poolState]] and
 * pluggable `status`, `validateConnection`, `removeConnection`, and `createNewConnectionForPool`
 * behaviours; the members the maintenance tasks never touch raise on access.
 *
 * @param poolState      the live pool state the task reads and mutates
 * @param metricsTracker the metrics tracker
 * @param poolLogger     the pool logger
 * @param config         the configuration backing the accessor members
 * @param statusEffect   an optional fixed status (otherwise derived from [[poolState]])
 * @param validateFn     the `validateConnection` behaviour
 * @param removeFn       the `removeConnection` behaviour
 * @param createForPool  the `createNewConnectionForPool` behaviour
 */
final class MockPool(
  val poolState:      Ref[Fx, PoolState[Fx]],
  val metricsTracker: PoolMetricsTracker[Fx],
  val poolLogger:     PoolLogger[Fx],
  config:             ConnectionPoolConfig             = ConnectionPoolConfig(minConnections = 2, maxConnections = 5),
  statusEffect:       Option[Fx[PoolStatus]]           = None,
  validateFn:         Connection[Fx] => Fx[Boolean]    = _ => Fx.pure(true),
  removeFn:           PooledConnection[Fx] => Fx[Unit] = _ => Fx.unit,
  createForPool: Fx[PooledConnection[Fx]] = Fx.raiseError(new NotImplementedError("createNewConnectionForPool"))
) extends PooledDataSource[Fx]:

  override def minConnections         = config.minConnections
  override def maxConnections         = config.maxConnections
  override def connectionTimeout      = config.connectionTimeout
  override def idleTimeout            = config.idleTimeout
  override def maxLifetime            = config.maxLifetime
  override def validationTimeout      = config.validationTimeout
  override def leakDetectionThreshold = config.leakDetectionThreshold
  override def adaptiveSizing         = config.adaptiveSizing
  override def adaptiveInterval       = config.adaptiveInterval
  override def aliveBypassWindow      = config.aliveBypassWindow
  override def keepaliveTime          = config.keepaliveTime
  override def connectionTestQuery    = None
  override def idGenerator            = Fx.pure("test-id")

  override def status: Fx[PoolStatus] =
    statusEffect.getOrElse(
      poolState.get.map(s =>
        PoolStatus(
          total   = s.connections.size,
          active  = s.connections.size - s.idleConnections.size,
          idle    = s.idleConnections.size,
          waiting = s.waitQueue.size
        )
      )
    )

  override def metrics:                                        Fx[PoolMetrics]          = metricsTracker.getMetrics
  override def close:                                          Fx[Unit]                 = Fx.unit
  override def returnToPool(pooled:     PooledConnection[Fx]): Fx[Unit]                 = Fx.unit
  override def removeConnection(pooled: PooledConnection[Fx]): Fx[Unit]                 = removeFn(pooled)
  override def validateConnection(conn: Connection[Fx]):       Fx[Boolean]              = validateFn(conn)
  override def createNewConnectionForPool():                   Fx[PooledConnection[Fx]] = createForPool
  override def createNewConnection():                          Fx[PooledConnection[Fx]] =
    Fx.raiseError(new NotImplementedError("createNewConnection"))
  override def getConnection: Fx[(Connection[Fx], Fx[Unit])] =
    Fx.raiseError(new NotImplementedError("getConnection"))
  override def circuitBreaker: CircuitBreaker[Fx] = throw new NotImplementedError("circuitBreaker")
