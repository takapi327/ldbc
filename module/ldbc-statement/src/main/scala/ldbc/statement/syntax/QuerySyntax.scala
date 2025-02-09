/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement.syntax

import scala.deriving.Mirror

import ldbc.dsl.{ Query as DslQuery, * }
import ldbc.dsl.codec.Decoder

import ldbc.statement.Query

trait QuerySyntax[F[_]]:

  extension [A, B](query: Query[A, B])

    /**
     * A method to convert a query to a [[ldbc.dsl.Query]].
     * 
     * {{{
     *   TableQuery[User].select(v => v.name *: v.age).query
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
     *   TableQuery[User].selectAll.queryTo[User]
     * }}}
     *
     * @return
     *   A [[ldbc.dsl.Query]] instance
     */
    def queryTo[P <: Product](using
      m1:      Mirror.ProductOf[P],
      m2:      Mirror.ProductOf[B],
      check:   m1.MirroredElemTypes =:= m2.MirroredElemTypes,
      decoder: Decoder[P]
    ): DslQuery[F, P]
