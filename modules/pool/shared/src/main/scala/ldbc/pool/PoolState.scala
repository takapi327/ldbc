/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.sql.Connection

import ldbc.effect.Deferred

case class PoolState[F[_]](
  connections:     Vector[PooledConnection[F]],
  idleConnections: Set[String],
  waitQueue:       Vector[Deferred[F, Either[Throwable, Connection[F]]]],
  metrics:         PoolMetrics,
  closed:          Boolean = false
)

object PoolState:
  def empty[F[_]]: PoolState[F] = PoolState(
    connections     = Vector.empty,
    idleConnections = Set.empty,
    waitQueue       = Vector.empty,
    metrics         = PoolMetrics.empty,
    closed          = false
  )
