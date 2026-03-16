/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

/**
 * Snapshot of pool state used by Observable BatchCallback to report metrics.
 *
 * @param idleCount the number of idle connections
 * @param usedCount the number of connections currently in use
 * @param pendingRequestCount the number of pending requests waiting for a connection
 */
case class PoolMetricsState(
  idleCount:           Long,
  usedCount:           Long,
  pendingRequestCount: Long
)
