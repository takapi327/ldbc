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

    /**
     * A method to convert a query to a [[ldbc.dsl.Query]].
     * 
     * {{{
     *   Table[User].select(v => (v.name, v.age)).query
     * }}}
     *
     * @return
     *   A [[ldbc.dsl.Query]] instance
     */
    inline def query(using
      mirror: Mirror.ProductOf[Tuples.InverseColumnMap[T]]
    ): DslQuery[F, Tuples.InverseColumnMap[T]]

    /**
     * A method to convert a query to a [[ldbc.dsl.Query]].
     *
     * {{{
     *   Table[User].selectAll.queryTo[User]
     * }}}
     *
     * @return
     *   A [[ldbc.dsl.Query]] instance
     */
    inline def queryTo[P <: Product](using
      mirror: Mirror.ProductOf[P],
      check:  Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes
    ): DslQuery[F, P]
