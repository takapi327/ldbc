/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.*
import cats.syntax.all.*

import cats.effect.Temporal

import ldbc.dsl.util.FactoryCompat
import ldbc.dsl.codec.Decoder

/**
 * Trait for determining what type of search system statements are retrieved from the database.
 *
 * @tparam F
 *   The effect type
 * @tparam T
 *   Column Tuples
 */
trait Query[F[_], T]:

  /**
   * Functions for safely retrieving data from a database in an array or Option type.
   */
  def to[G[_]](using FactoryCompat[T, G[T]]): Executor[F, G[T]]

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[to]] method if you want to retrieve individual data.
   */
  def unsafe: Executor[F, T]

object Query:

  private[ldbc] case class Impl[F[_]: Temporal, T](
    statement: String,
    params:    List[Parameter.DynamicBinder],
    decoder:   Decoder[T]
  ) extends Query[F, T]:

    given Decoder[T] = decoder

    override def to[G[_]](using FactoryCompat[T, G[T]]): Executor[F, G[T]] =
      Executor.Impl[F, G[T]](
        statement,
        params,
        connection =>
          for
            prepareStatement <- connection.prepareStatement(statement)
            resultSet <- params.zipWithIndex.traverse {
                           case (param, index) => param.bind[F](prepareStatement, index + 1)
                         } >> prepareStatement.executeQuery()
            result <- summon[ResultSetConsumer[F, G[T]]].consume(resultSet) <* prepareStatement.close()
          yield result
      )

    override def unsafe: Executor[F, T] =
      Executor.Impl[F, T](
        statement,
        params,
        connection =>
          for
            prepareStatement <- connection.prepareStatement(statement)
            resultSet <- params.zipWithIndex.traverse {
                           case (param, index) => param.bind[F](prepareStatement, index + 1)
                         } >> prepareStatement.executeQuery()
            result <- summon[ResultSetConsumer[F, T]].consume(resultSet) <* prepareStatement.close()
          yield result
      )
