/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

/**
 * Current status of the connection pool.
 *
 * @param total the total number of connections
 * @param active the number of active connections
 * @param idle the number of idle connections
 * @param waiting the number of waiting requests
 */
case class PoolStatus(
  total:   Int,
  active:  Int,
  idle:    Int,
  waiting: Int
)
