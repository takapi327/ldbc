/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import ldbc.sql.Connection

import ldbc.effect.{ Fiber, Ref }

case class PooledConnection[F[_]](
  id:              String,
  connection:      Connection[F],
  finalizer:       F[Unit],
  state:           Ref[F, ConnectionState],
  createdAt:       Long,
  lastUsedAt:      Ref[F, Long],
  useCount:        Ref[F, Long],
  lastValidatedAt: Ref[F, Long],
  leakDetection:   Ref[F, Option[Fiber[F, Unit]]],
  bagState:        Ref[F, Int]
) extends BagEntry[F]:

  override def getState: F[Int] = bagState.get

  override def setState(state: Int): F[Unit] = bagState.set(state)

  override def compareAndSet(expect: Int, update: Int): F[Boolean] =
    bagState.modify(current => if current == expect then (update, true) else (current, false))
