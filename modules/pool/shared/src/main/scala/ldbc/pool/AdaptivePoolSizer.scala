/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.{ Concurrent, Resource }
import ldbc.effect.syntax.*

/**
 * The adaptive pool-sizing algorithm: it periodically samples pool utilisation, wait-queue length,
 * and timeouts, and grows or shrinks the pool within its bounds. Thrashing is avoided with a cooldown
 * and by requiring several consecutive observations before acting. The stateful loop is scheduled via
 * `F.sleep` recursion (threading the [[AdaptivePoolSizer.AdaptiveState]]) and cancelled when its
 * [[ldbc.effect.Resource]] is released.
 */
trait AdaptivePoolSizer[F[_]]:
  /**
   * Starts the adaptive sizer for `pool`.
   *
   * @param pool the pool to resize
   */
  def start(pool: PooledDataSource[F]): Resource[F, Unit]

object AdaptivePoolSizer:

  /**
   * Creates an adaptive sizer that runs every `config.adaptiveInterval`.
   *
   * @param config         the pool configuration (bounds and interval)
   * @param metricsTracker the tracker providing utilisation metrics
   */
  def apply[F[_]](config: ConnectionPoolConfig, metricsTracker: PoolMetricsTracker[F])(using
    F: Concurrent[F]
  ): AdaptivePoolSizer[F] =
    new Impl(config, metricsTracker)

  private final class Impl[F[_]](config: ConnectionPoolConfig, metricsTracker: PoolMetricsTracker[F])(using
    F: Concurrent[F]
  ) extends AdaptivePoolSizer[F]:

    override def start(pool: PooledDataSource[F]): Resource[F, Unit] =
      def loop(state: AdaptiveState): F[Unit] =
        F.sleep(config.adaptiveInterval)
          .flatMap(_ => adjustPoolSize(pool, state))
          .flatMap((newState, _) => loop(newState))
      Resource
        .make(loop(AdaptiveState.initial).start)(fiber => fiber.cancel)
        .map(_ => ())

    private def nowMillis: F[Long] = F.delay(System.currentTimeMillis())

    private def adjustPoolSize(pool: PooledDataSource[F], state: AdaptiveState): F[(AdaptiveState, Unit)] =
      pool.poolState.get.flatMap { poolState =>
        if poolState.closed then F.pure((state, ()))
        else
          for
            now     <- nowMillis
            status  <- pool.status
            metrics <- metricsTracker.getMetrics
            snapshot = PoolSnapshot(
                         timestamp       = now,
                         utilizationRate = if status.total > 0 then status.active.toDouble / status.total else 0.0,
                         waitQueueLength = status.waiting,
                         timeouts        = metrics.timeouts,
                         avgAcquireTime  = metrics.acquisitionTime
                       )
            newHistory = (state.history :+ snapshot).takeRight(10)
            adjustment = calculateAdjustment(status, snapshot, newHistory)
            newState <- applyAdjustment(pool, state, adjustment, now)
          yield (newState.copy(history = newHistory), ())
      }

    private def calculateAdjustment(
      status:   PoolStatus,
      snapshot: PoolSnapshot,
      history:  Vector[PoolSnapshot]
    ): PoolAdjustment =
      val highUtilizationThreshold     = 0.8
      val criticalUtilizationThreshold = 0.95
      val lowUtilizationThreshold      = 0.2
      val veryLowUtilizationThreshold  = 0.1
      val waitQueueThreshold           = status.total * 0.1
      val criticalWaitQueueThreshold   = status.total * 0.25
      val recentSnapshots              = history.takeRight(5)
      val avgUtilization               =
        if recentSnapshots.nonEmpty then recentSnapshots.map(_.utilizationRate).sum / recentSnapshots.size
        else snapshot.utilizationRate
      val avgWaitQueue =
        if recentSnapshots.nonEmpty then recentSnapshots.map(_.waitQueueLength).sum / recentSnapshots.size
        else snapshot.waitQueueLength

      if snapshot.utilizationRate > criticalUtilizationThreshold || snapshot.waitQueueLength > criticalWaitQueueThreshold
      then
        val increase = Math.min(Math.max(5, (status.total * 0.5).toInt), config.maxConnections - status.total)
        if increase > 0 then PoolAdjustment.Grow(increase) else PoolAdjustment.NoChange
      else if avgUtilization > highUtilizationThreshold || avgWaitQueue > waitQueueThreshold then
        val increase = Math.min(Math.max(2, (status.total * 0.2).toInt), config.maxConnections - status.total)
        if increase > 0 then PoolAdjustment.Grow(increase) else PoolAdjustment.NoChange
      else if avgUtilization < veryLowUtilizationThreshold && status.total > config.minConnections then
        val decrease = Math.min(Math.max(2, (status.idle * 0.5).toInt), status.total - config.minConnections)
        if decrease > 0 then PoolAdjustment.Shrink(decrease) else PoolAdjustment.NoChange
      else if avgUtilization < lowUtilizationThreshold && status.total > config.minConnections then
        val decrease = Math.min(Math.max(1, (status.idle * 0.2).toInt), status.total - config.minConnections)
        if decrease > 0 then PoolAdjustment.Shrink(decrease) else PoolAdjustment.NoChange
      else PoolAdjustment.NoChange

    private def applyAdjustment(
      pool:       PooledDataSource[F],
      state:      AdaptiveState,
      adjustment: PoolAdjustment,
      now:        Long
    ): F[AdaptiveState] =
      val cooldownPeriod          = 2.minutes.toMillis
      val timeSinceLastAdjustment = now - state.lastAdjustment
      if timeSinceLastAdjustment < cooldownPeriod then F.pure(state)
      else
        adjustment match
          case PoolAdjustment.Grow(by) =>
            val newConsecutiveHighs = state.consecutiveHighs + 1
            if newConsecutiveHighs >= 2 || by >= 5 then
              growPool(pool, by).as(state.copy(lastAdjustment = now, consecutiveHighs = 0, consecutiveLows = 0))
            else F.pure(state.copy(consecutiveHighs = newConsecutiveHighs, consecutiveLows = 0))

          case PoolAdjustment.Shrink(by) =>
            val newConsecutiveLows = state.consecutiveLows + 1
            if newConsecutiveLows >= 3 then
              shrinkPool(pool, by).as(state.copy(lastAdjustment = now, consecutiveHighs = 0, consecutiveLows = 0))
            else F.pure(state.copy(consecutiveHighs = 0, consecutiveLows = newConsecutiveLows))

          case PoolAdjustment.NoChange => F.pure(state.copy(consecutiveHighs = 0, consecutiveLows = 0))

    private def growPool(pool: PooledDataSource[F], by: Int): F[Unit] =
      (1 to by).toList.traverse_ { index =>
        pool.createNewConnectionForPool().void.handleErrorWith { error =>
          F.delay(
            System.err.println(
              s"[AdaptivePoolSizer] Failed to create connection $index/$by during pool growth: ${ error.getMessage }"
            )
          )
        }
      }

    private def shrinkPool(pool: PooledDataSource[F], by: Int): F[Unit] =
      pool.poolState.get.flatMap { poolState =>
        val idleConnections = poolState.connections.filter(conn => poolState.idleConnections.contains(conn.id))
        for
          connectionsWithTime <- idleConnections.traverse(conn => conn.lastUsedAt.get.map(time => (conn, time)))
          sortedConnections = connectionsWithTime.sortBy(_._2).map(_._1)
          toRemove          = sortedConnections.take(by)
          _ <- toRemove.traverse_(pool.removeConnection)
        yield ()
      }

  /** The evolving state of the adaptive sizer between ticks. */
  private case class AdaptiveState(
    history:          Vector[PoolSnapshot],
    lastAdjustment:   Long,
    consecutiveHighs: Int,
    consecutiveLows:  Int
  )

  private object AdaptiveState:
    def initial: AdaptiveState = AdaptiveState(Vector.empty, 0, 0, 0)

  /** A snapshot of the pool at a point in time. */
  private case class PoolSnapshot(
    timestamp:       Long,
    utilizationRate: Double,
    waitQueueLength: Int,
    timeouts:        Long,
    avgAcquireTime:  FiniteDuration
  )
