/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.protocol

import ldbc.fx.{ Fx, Mutex }

/**
 * Serialises effects through a mutex so that request/response exchanges on a single connection never
 * interleave. Each exchange is atomic and uncancelable.
 */
trait Exchange:
  def apply[A](fa: Fx[A]): Fx[A]

object Exchange:

  /** Creates an [[Exchange]] backed by a fresh [[ldbc.fx.Mutex]]. */
  def apply: Fx[Exchange] =
    Mutex.create.map { mutex =>
      new Exchange:
        override def apply[A](fa: Fx[A]): Fx[A] =
          Fx.uncancelable(mutex.surround(fa))
    }
