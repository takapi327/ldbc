/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.*
import cats.syntax.all.*

import cats.effect.MonadCancelThrow

import ldbc.dsl.codec.Decoder
import ldbc.dsl.util.FactoryCompat

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
  def to[G[_]](using FactoryCompat[T, G[T]]): DBIO[F, G[T]]

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[to]] method if you want to retrieve individual data.
   */
  def unsafe: DBIO[F, T]

object Query:

  private[ldbc] case class Impl[F[_]: MonadCancelThrow, T](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[T]
  ) extends Query[F, T],
            ParamBinder:

    given Decoder[T] = decoder

    override def to[G[_]](using FactoryCompat[T, G[T]]): DBIO[F, G[T]] =
      DBIO.Impl[F, G[T]](
        statement,
        params,
        connection =>
          for
            prepareStatement <- connection.prepareStatement(statement)
            resultSet        <- paramBind(prepareStatement, params) >> prepareStatement.executeQuery()
            result <- summon[ResultSetConsumer[F, G[T]]].consume(resultSet, statement) <* prepareStatement.close()
          yield result
      )

    override def unsafe: DBIO[F, T] =
      DBIO.Impl[F, T](
        statement,
        params,
        connection =>
          for
            prepareStatement <- connection.prepareStatement(statement)
            resultSet        <- paramBind(prepareStatement, params) >> prepareStatement.executeQuery()
            result <- summon[ResultSetConsumer[F, T]].consume(resultSet, statement) <* prepareStatement.close()
          yield result
      )
