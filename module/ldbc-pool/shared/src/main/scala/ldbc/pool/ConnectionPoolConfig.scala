/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

/**
 * The DB-agnostic configuration for a [[PooledDataSource]]. It holds only pooling parameters — the
 * per-database connection parameters (host, credentials, TLS, …) live in the connector that supplies
 * the connection factory. A connector's own config maps its pool-related fields onto this.
 *
 * @param minConnections         the minimum number of connections to keep
 * @param maxConnections         the maximum number of connections
 * @param connectionTimeout      the maximum time to wait to acquire a connection
 * @param validationTimeout      the timeout for a connection validation check
 * @param idleTimeout            how long an idle connection may live before eviction
 * @param maxLifetime            the maximum lifetime of any connection
 * @param maintenanceInterval    how often the housekeeper runs
 * @param leakDetectionThreshold if set, warn when a connection is held longer than this
 * @param keepaliveTime          if set, how often to keepalive-probe idle connections
 * @param aliveBypassWindow      skip validation if validated within this window
 * @param adaptiveSizing         whether the adaptive pool sizer is enabled
 * @param poolName               a name used in logs and metrics
 * @param debug                  whether debug logging is enabled
 */
case class ConnectionPoolConfig(
  minConnections:         Int,
  maxConnections:         Int,
  connectionTimeout:      FiniteDuration         = 30.seconds,
  validationTimeout:      FiniteDuration         = 5.seconds,
  idleTimeout:            FiniteDuration         = 10.minutes,
  maxLifetime:            FiniteDuration         = 30.minutes,
  maintenanceInterval:    FiniteDuration         = 30.seconds,
  adaptiveInterval:       FiniteDuration         = 30.seconds,
  leakDetectionThreshold: Option[FiniteDuration] = None,
  keepaliveTime:          Option[FiniteDuration] = None,
  aliveBypassWindow:      FiniteDuration         = 500.millis,
  adaptiveSizing:         Boolean                = false,
  poolName:               String                 = "ldbc-pool",
  debug:                  Boolean                = false
)
