/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import cats.effect.*

import ldbc.connector.Connection

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
  /**
   * Creates an empty PoolState instance with no connections and empty wait queue.
   * 
   * This represents the initial state of a connection pool before any connections
   * have been created. The pool is marked as open (not closed) and all collections
   * are empty.
   * 
   * @tparam F the effect type
   * @return a new PoolState instance with empty collections and initial metrics
   */
  def empty[F[_]]: PoolState[F] = PoolState(
    connections     = Vector.empty,
    idleConnections = Set.empty,
    waitQueue       = Vector.empty,
    metrics         = PoolMetrics.empty,
    closed          = false
  )
