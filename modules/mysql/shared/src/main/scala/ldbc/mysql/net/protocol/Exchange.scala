/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.protocol

import ldbc.effect.{ Async, Semaphore }

/**
 * Serialises effects through a semaphore so that request/response exchanges on a single connection never
 * interleave. Each exchange is atomic and uncancelable.
 */
trait Exchange[F[_]]:
  def apply[A](fa: F[A]): F[A]

object Exchange:

  /** Creates an [[Exchange]] backed by a fresh single-permit [[ldbc.effect.Semaphore]]. */
  def apply[F[_]](using F: Async[F]): F[Exchange[F]] =
    F.map(Semaphore[F](1)) { semaphore =>
      new Exchange[F]:
        override def apply[A](fa: F[A]): F[A] =
          F.uncancelable(semaphore.withPermit(fa))
    }
