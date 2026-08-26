/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * A [[ResultSet]] whose rows are **fully materialized in memory** (i.e. not backed by an incremental
 * server-side cursor). Such a result set can be drained synchronously inside a single effect, avoiding the
 * per-column `F` bind that the effectful [[ResultSet]] accessors otherwise incur.
 *
 * Streaming result sets (which fetch rows from the server on demand) intentionally do **not** implement this
 * trait, so a consumer that special-cases `BufferedResultSet` can never accidentally collapse a streaming read
 * into a blocking full drain.
 */
trait BufferedResultSet[F[_]] extends ResultSet[F]:

  /**
   * Folds over every remaining buffered row synchronously within a single effect.
   *
   * `step` receives a reusable [[SyncRow]] positioned on the current row and the running accumulator. It may
   * throw (e.g. a decode failure); the implementation runs the whole drain inside one effect so the throwable
   * surfaces as `F.raiseError`. Implementations must drive iteration with an independent cursor that does not
   * disturb the effectful [[ResultSet.next]] position, so a caller can fall back to the per-row path unchanged.
   *
   * @param zero the initial accumulator
   * @param step applied once per row, in row order
   */
  def foldRowsSync[B](zero: B)(step: (B, SyncRow) => B): F[B]
