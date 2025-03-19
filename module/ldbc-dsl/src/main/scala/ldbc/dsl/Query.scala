/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.*
import cats.syntax.all.*

import ldbc.dsl.codec.Decoder
import ldbc.dsl.util.FactoryCompat

/**
 * Trait for determining what type of search system statements are retrieved from the database.
 *
 * @tparam T
 *   Column Tuples
 */
trait Query[T]:

  /**
   * Functions for safely retrieving data from a database in an array or Option type.
   */
  def to[G[_]](using FactoryCompat[T, G[T]]): DBIO[G[T]]

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[to]] method if you want to retrieve individual data.
   */
  def unsafe: DBIO[T]

object Query:

  private[ldbc] case class Impl[T](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[T]
  ) extends Query[T],
            ParamBinder:

    override def to[G[_]](using factory: FactoryCompat[T, G[T]]): DBIO[G[T]] =
      DBIO.queryTo(statement, params, decoder, factory)

    override def unsafe: DBIO[T] =
      DBIO.queryA(statement, params, decoder)
