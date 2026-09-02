/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.syntax

import scala.collection.mutable
import scala.collection.Factory

import ldbc.sql.ResultSet

import ldbc.effect.syntax.*
import ldbc.effect.MonadThrow

trait ResultSetOps:

  extension [F[_]](resultSet: ResultSet[F])(using F: MonadThrow[F])

    private def loop[G[_], T](acc: mutable.Builder[T, G[T]], func: => F[T]): F[G[T]] =
      resultSet.next().flatMap { hasNext =>
        if hasNext then func.flatMap(value => loop(acc += value, func))
        else F.pure(acc.result())
      }

    def whileM[G[_], T](func: => F[T])(using factory: Factory[T, G[T]]): F[G[T]] =
      val builder = factory.newBuilder
      loop(builder, func)
