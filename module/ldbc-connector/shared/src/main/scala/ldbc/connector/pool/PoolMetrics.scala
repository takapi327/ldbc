/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

/**
 * Metrics collected by the connection pool.
 *
 * @param acquisitionTime average time to acquire a connection
 * @param usageTime average time a connection is used
 * @param creationTime average time to create a connection
 * @param timeouts number of acquisition timeouts
 * @param leaks number of detected connection leaks
 * @param totalAcquisitions total number of acquisitions
 * @param totalReleases total number of releases
 * @param totalCreations total number of connections created
 * @param totalRemovals total number of connections removed
 * @param gauges current gauge values (e.g., pool.total, pool.active, pool.idle, pool.waiting)
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
  /**
   * Creates an empty PoolMetrics instance with all values initialized to zero.
   * 
   * This is typically used as the initial state for metrics tracking before
   * any pool operations have occurred.
   * 
   * @return a new PoolMetrics instance with all metrics set to zero
   */
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
