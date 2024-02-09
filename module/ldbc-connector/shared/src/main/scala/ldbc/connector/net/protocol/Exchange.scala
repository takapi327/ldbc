/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import cats.syntax.all.*

import cats.effect.implicits.*
import cats.effect.std.Mutex
import cats.effect.Concurrent

trait Exchange[F[_]]:
  def apply[A](fa: F[A]): F[A]

object Exchange:
  def apply[F[_]: Concurrent]: F[Exchange[F]] =
    Mutex[F].map { mut =>
      new Exchange[F]:
        override def apply[A](fa: F[A]): F[A] =
          mut.lock.surround(fa).uncancelable
    }
