/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

/**
 * The lifecycle state of a connection within the pool. A connection starts `Idle`, becomes
 * `Reserved` then `InUse` when acquired, returns to `Idle` on release, and enters `Removed` when
 * marked for removal.
 */
enum ConnectionState:
  /** Available in the pool and ready to be acquired. */
  case Idle

  /** Currently being used by a client. */
  case InUse

  /** Marked for removal and to be closed. */
  case Removed

  /** Intermediate state while being acquired but not yet in use. */
  case Reserved

/**
 * A decision made by the adaptive pool-sizing algorithm from the observed pool metrics.
 */
enum PoolAdjustment:
  /**
   * Grow the pool by `by` connections.
   *
   * @param by the number of connections to add
   */
  case Grow(by: Int)

  /**
   * Shrink the pool by `by` connections.
   *
   * @param by the number of connections to remove
   */
  case Shrink(by: Int)

  /** No size change. */
  case NoChange

/**
 * A snapshot of the pool's connection counts.
 *
 * @param total   the total number of connections
 * @param active  the number of connections in use
 * @param idle    the number of idle connections
 * @param waiting the number of borrowers waiting for a connection
 */
case class PoolStatus(total: Int, active: Int, idle: Int, waiting: Int)

/**
 * Aggregated pool metrics.
 *
 * @param acquisitionTime   the most recent acquisition time
 * @param usageTime         the most recent usage time
 * @param creationTime      the most recent creation time
 * @param timeouts          the number of acquisition timeouts
 * @param leaks             the number of detected connection leaks
 * @param totalAcquisitions the total number of acquisitions
 * @param totalReleases     the total number of releases
 * @param totalCreations    the total number of connection creations
 * @param totalRemovals     the total number of connection removals
 * @param gauges            arbitrary named gauge values
 */
case class PoolMetrics(
  acquisitionTime:   FiniteDuration,
  usageTime:         FiniteDuration,
  creationTime:      FiniteDuration,
  timeouts:          Long,
  leaks:             Long,
  totalAcquisitions: Long,
  totalReleases:     Long,
  totalCreations:    Long,
  totalRemovals:     Long,
  gauges:            Map[String, Long]
)

object PoolMetrics:
  /** The zero metrics, before any pool operations have occurred. */
  def empty: PoolMetrics = PoolMetrics(
    acquisitionTime   = Duration.Zero,
    usageTime         = Duration.Zero,
    creationTime      = Duration.Zero,
    timeouts          = 0,
    leaks             = 0,
    totalAcquisitions = 0,
    totalReleases     = 0,
    totalCreations    = 0,
    totalRemovals     = 0,
    gauges            = Map.empty
  )
