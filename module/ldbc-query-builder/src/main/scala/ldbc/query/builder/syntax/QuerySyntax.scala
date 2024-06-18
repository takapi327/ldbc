/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.syntax

import scala.deriving.Mirror

import ldbc.dsl.{ Query as DslQuery, * }

import ldbc.query.builder.statement.Query
import ldbc.query.builder.interpreter.Tuples

trait QuerySyntax[F[_]]:

  extension [T](query: Query[T])

    inline def query: DslQuery[F, Tuples.InverseColumnMap[T]]

    inline def queryTo[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes
    ): DslQuery[F, P]
