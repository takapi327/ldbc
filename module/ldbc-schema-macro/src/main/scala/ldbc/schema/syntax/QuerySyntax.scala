/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.syntax

import scala.deriving.Mirror

import ldbc.dsl.{ Query as DslQuery, * }

import ldbc.schema.statement.Query

trait QuerySyntax[F[_]]:

  extension [A, B](query: Query[A, B])

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
    def query: DslQuery[F, B]

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
                                     m1:    Mirror.ProductOf[P],
                                     m2:    Mirror.ProductOf[A],
                                     check: m1.MirroredElemTypes =:= m2.MirroredElemTypes
                                    ): DslQuery[F, P]
