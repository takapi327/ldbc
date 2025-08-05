/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool


import cats.effect.*

import ldbc.connector.Connection

/**
 * A pooled connection with metadata.
 *
 * @param id unique identifier for the connection
 * @param connection the actual database connection
 * @param state current state of the connection
 * @param createdAt timestamp when the connection was created
 * @param lastUsedAt timestamp when the connection was last used
 * @param useCount number of times the connection has been used
 * @param lastValidatedAt timestamp when the connection was last validated
 * @param leakDetection optional leak detection handle
 * @tparam F the effect type
 */
case class PooledConnection[F[_]](
  id:               String,
  connection:       Connection[F],
  state:            Ref[F, ConnectionState],
  createdAt:        Long,
  lastUsedAt:       Ref[F, Long],
  useCount:         Ref[F, Long],
  lastValidatedAt:  Ref[F, Long],
  leakDetection:    Ref[F, Option[Fiber[F, Throwable, Unit]]]
)

/**
 * State of a pooled connection.
 */
enum ConnectionState:
  case Idle
  case InUse
  case Removed
  case Reserved // Intermediate state during acquisition

/**
 * Pool adjustment decision for adaptive sizing.
 */
enum PoolAdjustment:
  case Grow(by: Int)
  case Shrink(by: Int)
  case NoChange

/**
 * Internal state of the connection pool.
 *
 * @param connections all connections in the pool
 * @param idleConnections IDs of idle connections (for efficient lookup)
 * @param waitQueue queue of waiting acquisition requests
 * @param metrics current pool metrics
 * @param closed whether the pool is closed
 * @tparam F the effect type
 */
case class PoolState[F[_]](
  connections:     Vector[PooledConnection[F]],
  idleConnections: Set[String], // Track idle connection IDs
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