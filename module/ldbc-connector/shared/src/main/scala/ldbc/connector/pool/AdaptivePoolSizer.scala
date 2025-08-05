/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.spawn.*

import fs2.Stream

import ldbc.connector.MySQLConfig

/**
 * Adaptive pool sizing algorithm that adjusts pool size based on usage patterns.
 *
 * The algorithm monitors:
 * - Connection utilization rate
 * - Wait queue length
 * - Acquisition timeouts
 * - Historical usage patterns
 */
object AdaptivePoolSizer:

  /**
   * Start the adaptive sizing background task.
   *
   * @param pool the connection pool implementation
   * @param config pool configuration
   * @param metricsTracker metrics tracker
   * @tparam F the effect type
   * @return a resource that manages the background task
   */
  def start[F[_]: Async](
    pool:           MySQLPooledDataSource[F, Unit],
    config:         MySQLConfig,
    metricsTracker: PoolMetricsTracker[F]
  ): Resource[F, Unit] =

    val task = Stream
      .fixedDelay[F](config.adaptiveInterval)
      .evalMapAccumulate(AdaptiveState.initial) { (state, _) =>
        adjustPoolSize(pool, config, metricsTracker, state)
      }
      .compile
      .drain

    Resource
      .make(task.start)(_.cancel)
      .void

  /**
   * State for adaptive sizing algorithm.
   */
  private case class AdaptiveState(
    history:          Vector[PoolSnapshot],
    lastAdjustment:   Long,
    consecutiveHighs: Int,
    consecutiveLows:  Int
  )

  private object AdaptiveState:
    def initial: AdaptiveState = AdaptiveState(
      history          = Vector.empty,
      lastAdjustment   = 0,
      consecutiveHighs = 0,
      consecutiveLows  = 0
    )

  /**
   * Snapshot of pool state at a point in time.
   */
  private case class PoolSnapshot(
    timestamp:       Long,
    utilizationRate: Double,
    waitQueueLength: Int,
    timeouts:        Long,
    avgAcquireTime:  FiniteDuration
  )

  /**
   * Adjust pool size based on current metrics and historical data.
   */
  private def adjustPoolSize[F[_]: Async](
    pool:           PooledDataSource[F],
    config:         MySQLConfig,
    metricsTracker: PoolMetricsTracker[F],
    state:          AdaptiveState
  ): F[(AdaptiveState, Unit)] = for
    now     <- Clock[F].realTime.map(_.toMillis)
    status  <- pool.status
    metrics <- metricsTracker.getMetrics

    snapshot = PoolSnapshot(
                 timestamp       = now,
                 utilizationRate = if status.total > 0 then status.active.toDouble / status.total else 0.0,
                 waitQueueLength = status.waiting,
                 timeouts        = metrics.timeouts,
                 avgAcquireTime  = metrics.acquisitionTime
               )

    // Add to history (keep last 10 snapshots)
    newHistory = (state.history :+ snapshot).takeRight(10)

    // Calculate adjustment
    adjustment = calculateAdjustment(status, snapshot, newHistory, config)

    // Apply adjustment if needed
    newState <- applyAdjustment(pool, state, adjustment, now)
  yield (newState.copy(history = newHistory), ())

  /**
   * Calculate the pool adjustment based on metrics.
   */
  private def calculateAdjustment(
    status:   PoolStatus,
    snapshot: PoolSnapshot,
    history:  Vector[PoolSnapshot],
    config:   MySQLConfig
  ): PoolAdjustment =

    // High utilization thresholds
    val highUtilizationThreshold     = 0.8
    val criticalUtilizationThreshold = 0.95

    // Low utilization thresholds
    val lowUtilizationThreshold     = 0.2
    val veryLowUtilizationThreshold = 0.1

    // Wait queue thresholds
    val waitQueueThreshold         = status.total * 0.1
    val criticalWaitQueueThreshold = status.total * 0.25

    // Analyze recent trends
    val recentSnapshots = history.takeRight(5)
    val avgUtilization  =
      if recentSnapshots.nonEmpty then recentSnapshots.map(_.utilizationRate).sum / recentSnapshots.size
      else snapshot.utilizationRate

    val avgWaitQueue =
      if recentSnapshots.nonEmpty then recentSnapshots.map(_.waitQueueLength).sum / recentSnapshots.size
      else snapshot.waitQueueLength

    // Decision logic
    if snapshot.utilizationRate > criticalUtilizationThreshold ||
      snapshot.waitQueueLength > criticalWaitQueueThreshold
    then
      // Critical load - aggressive scaling
      val increase = Math.min(
        Math.max(5, (status.total * 0.5).toInt),
        config.maxConnections - status.total
      )
      if increase > 0 then PoolAdjustment.Grow(increase) else PoolAdjustment.NoChange
    else if avgUtilization > highUtilizationThreshold ||
      avgWaitQueue > waitQueueThreshold
    then
      // High load - moderate scaling
      val increase = Math.min(
        Math.max(2, (status.total * 0.2).toInt),
        config.maxConnections - status.total
      )
      if increase > 0 then PoolAdjustment.Grow(increase) else PoolAdjustment.NoChange
    else if avgUtilization < veryLowUtilizationThreshold &&
      status.total > config.minConnections
    then
      // Very low utilization - aggressive downsizing
      val decrease = Math.min(
        Math.max(2, (status.idle * 0.5).toInt),
        status.total - config.minConnections
      )
      if decrease > 0 then PoolAdjustment.Shrink(decrease) else PoolAdjustment.NoChange
    else if avgUtilization < lowUtilizationThreshold &&
      status.total > config.minConnections
    then
      // Low utilization - moderate downsizing
      val decrease = Math.min(
        Math.max(1, (status.idle * 0.2).toInt),
        status.total - config.minConnections
      )
      if decrease > 0 then PoolAdjustment.Shrink(decrease) else PoolAdjustment.NoChange
    else PoolAdjustment.NoChange

  /**
   * Apply the calculated adjustment to the pool.
   */
  private def applyAdjustment[F[_]: Async](
    pool:       PooledDataSource[F],
    state:      AdaptiveState,
    adjustment: PoolAdjustment,
    now:        Long
  ): F[AdaptiveState] =

    // Cooldown period to prevent thrashing (2 minutes)
    val cooldownPeriod          = 2.minutes.toMillis
    val timeSinceLastAdjustment = now - state.lastAdjustment

    if timeSinceLastAdjustment < cooldownPeriod then
      // Still in cooldown period
      Temporal[F].pure(state)
    else
      adjustment match
        case PoolAdjustment.Grow(by) =>
          // Check consecutive high utilization periods
          val newConsecutiveHighs = state.consecutiveHighs + 1
          if newConsecutiveHighs >= 2 || by >= 5 then
            // Grow the pool
            growPool(pool, by).as(
              state.copy(
                lastAdjustment   = now,
                consecutiveHighs = 0,
                consecutiveLows  = 0
              )
            )
          else
            Temporal[F].pure(
              state.copy(consecutiveHighs = newConsecutiveHighs, consecutiveLows = 0)
            )

        case PoolAdjustment.Shrink(by) =>
          // Check consecutive low utilization periods
          val newConsecutiveLows = state.consecutiveLows + 1
          if newConsecutiveLows >= 3 then
            // Shrink the pool
            shrinkPool(pool, by).as(
              state.copy(
                lastAdjustment   = now,
                consecutiveHighs = 0,
                consecutiveLows  = 0
              )
            )
          else
            Temporal[F].pure(
              state.copy(consecutiveHighs = 0, consecutiveLows = newConsecutiveLows)
            )

        case PoolAdjustment.NoChange =>
          Temporal[F].pure(
            state.copy(consecutiveHighs = 0, consecutiveLows = 0)
          )

  /**
   * Grow the pool by creating new connections.
   */
  private def growPool[F[_]: Async](
    pool: PooledDataSource[F],
    by:   Int
  ): F[Unit] =
    (1 to by).toList.traverse_ { _ =>
      pool
        .createNewConnection()
        .flatMap { pooled =>
          pooled.state.set(ConnectionState.Idle) *> pool.returnToPool(pooled)
        }
        .handleErrorWith(_ => Temporal[F].unit) // Ignore creation failures
    }

  /**
   * Shrink the pool by removing idle connections.
   */
  private def shrinkPool[F[_]: Async](
    pool: PooledDataSource[F],
    by:   Int
  ): F[Unit] =
    pool.poolState.get.flatMap { state =>
      // Simplified for compilation - will check state properly later
      val idleConnections = state.connections

      // Sort by last used time (oldest first)
      // Simplified for compilation - will sort properly later
      val toRemove = idleConnections.take(by)

      toRemove.traverse_(pool.removeConnection)
    }
