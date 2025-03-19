/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.deriving.Mirror

import ldbc.dsl.{ Query as DslQuery, SQL }
import ldbc.dsl.codec.Decoder

/**
 * Trait represents a read-only query in MySQL.
 *
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 * @tparam B
 *   Scala types to be converted by Decoder
 */
trait Query[A, B] extends SQL:

  /** Trait for generating SQL table information. */
  def table: A

  /** Union-type column list */
  def columns: Column[B]

  /**
   * A method to convert a query to a [[ldbc.dsl.Query]].
   *
   * {{{
   *   TableQuery[User].select(v => v.name *: v.age).query
   * }}}
   *
   * @return
   * A [[ldbc.dsl.Query]] instance
   */
  def query: DslQuery[B] = DslQuery.Impl[B](statement, params, columns.decoder)

  /**
   * A method to convert a query to a [[ldbc.dsl.Query]].
   *
   * {{{
   *   TableQuery[User].selectAll.queryTo[User]
   * }}}
   *
   * @return
   * A [[ldbc.dsl.Query]] instance
   */
  def queryTo[P <: Product](using
    m1:      Mirror.ProductOf[P],
    m2:      Mirror.ProductOf[B],
    check:   m1.MirroredElemTypes =:= m2.MirroredElemTypes,
    decoder: Decoder[P]
  ): DslQuery[P] =
    DslQuery.Impl[P](statement, params, decoder)
