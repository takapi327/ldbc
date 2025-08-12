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
 * @param finalizer the cleanup action for the connection resource
 * @param state current state of the connection
 * @param createdAt timestamp when the connection was created
 * @param lastUsedAt timestamp when the connection was last used
 * @param useCount number of times the connection has been used
 * @param lastValidatedAt timestamp when the connection was last validated
 * @param leakDetection optional leak detection handle
 * @param bagState atomic state for ConcurrentBag (separate from ConnectionState)
 * @tparam F the effect type
 */
case class PooledConnection[F[_]](
  id:              String,
  connection:      Connection[F],
  finalizer:       F[Unit],
  state:           Ref[F, ConnectionState],
  createdAt:       Long,
  lastUsedAt:      Ref[F, Long],
  useCount:        Ref[F, Long],
  lastValidatedAt: Ref[F, Long],
  leakDetection:   Ref[F, Option[Fiber[F, Throwable, Unit]]],
  bagState:        Ref[F, Int]
) extends BagEntry[F]:

  override def getState: F[Int] = bagState.get

  override def setState(state: Int): F[Unit] = bagState.set(state)

  override def compareAndSet(expect: Int, update: Int): F[Boolean] =
    bagState.modify { current =>
      if current == expect then (update, true)
      else (current, false)
    }
