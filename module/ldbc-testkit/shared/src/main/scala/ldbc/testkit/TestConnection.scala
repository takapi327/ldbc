/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.testkit

import cats.Applicative

import ldbc.sql.Connection

/**
 * A [[Connection]] wrapper that intercepts `commit()` and `setAutoCommit(true)` to no-ops.
 *
 * Used by [[RollbackHandler]] to prevent actual database commits during tests,
 * ensuring all changes can be rolled back after each test.
 */
private[testkit] class TestConnection[F[_]: Applicative](underlying: Connection[F])
  extends Connection[F]:

  export underlying.{ commit as _, setAutoCommit as _, * }

  // Prevent actual commits so the test transaction can be rolled back
  override def commit(): F[Unit] = Applicative[F].unit

  // Prevent autocommit from being re-enabled, which would make rollback ineffective
  override def setAutoCommit(autoCommit: Boolean): F[Unit] = Applicative[F].unit
