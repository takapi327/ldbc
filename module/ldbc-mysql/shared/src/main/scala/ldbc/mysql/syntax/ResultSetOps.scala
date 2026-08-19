/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.syntax

import scala.collection.mutable
import scala.collection.Factory

import ldbc.fx.Fx

import ldbc.sql.ResultSet

trait ResultSetOps:

  extension (resultSet: ResultSet[Fx])

    private def loop[G[_], T](acc: mutable.Builder[T, G[T]], func: => Fx[T]): Fx[G[T]] =
      resultSet.next().flatMap { hasNext =>
        if hasNext then func.flatMap(value => loop(acc += value, func))
        else Fx.pure(acc.result())
      }

    def whileM[G[_], T](func: => Fx[T])(using factory: Factory[T, G[T]]): Fx[G[T]] =
      val builder = factory.newBuilder
      loop(builder, func)
