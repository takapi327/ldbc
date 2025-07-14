/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.syntax

import scala.collection.mutable
import scala.collection.Factory

import cats.*
import cats.syntax.all.*

import ldbc.sql.ResultSet

trait ResultSetOps:

  extension [F[_]: Monad](resultSet: ResultSet[F])

    private def loop[G[_], T](acc: mutable.Builder[T, G[T]], func: => F[T]): F[G[T]] =
      resultSet.next().flatMap { hasNext =>
        if hasNext then func.flatMap(value => loop(acc += value, func))
        else acc.result().pure[F]
      }

    def whileM[G[_], T](func: => F[T])(using factory: Factory[T, G[T]]): F[G[T]] =
      val builder = factory.newBuilder
      loop(builder, func)
