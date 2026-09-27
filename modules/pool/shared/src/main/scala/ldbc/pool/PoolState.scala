/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

/**
 * The pool's authoritative state.
 *
 * `creating` counts physical connections whose creation has been started but not yet registered in
 * `connections`. Capacity is judged on `connections.size + creating`, so that concurrent callers
 * cannot each start a creation that the pool has no room for.
 */
case class PoolState[F[_]](
  connections:     Vector[PooledConnection[F]],
  idleConnections: Set[String],
  creating:        Int,
  metrics:         PoolMetrics,
  closed:          Boolean = false
)

object PoolState:
  def empty[F[_]]: PoolState[F] = PoolState(
    connections     = Vector.empty,
    idleConnections = Set.empty,
    creating        = 0,
    metrics         = PoolMetrics.empty,
    closed          = false
  )
