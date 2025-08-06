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
  leakDetection:   Ref[F, Option[Fiber[F, Throwable, Unit]]]
)
