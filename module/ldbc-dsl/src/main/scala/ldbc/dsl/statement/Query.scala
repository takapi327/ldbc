/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.statement

import scala.deriving.Mirror

import cats.*
import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.Temporal

import ldbc.sql.{ Parameter, ResultSet }
import ldbc.dsl.*
import ldbc.dsl.interpreter.Tuples

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
  def to[G[_]: Traverse: Alternative]: Executor[F, G[T]]

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[to]] method if you want to retrieve individual data.
   */
  def unsafe: Executor[F, T]

object Query:

  trait Provider[T] extends SQL:
    
    inline def query[F[_]: Temporal]: Query[F, Tuples.InverseColumnMap[T]] =
      given Kleisli[F, ResultSet[F], Tuples.InverseColumnMap[T]] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, Tuples.InverseColumnMap[T]]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => Tuple.fromArray(list.toArray).asInstanceOf[Tuples.InverseColumnMap[T]])
      }
      Impl[F, Tuples.InverseColumnMap[T]](statement, params)

    inline def query[F[_]: Temporal, P <: Product](using mirror: Mirror.ProductOf[P], check: Tuples.InverseColumnMap[T] =:= mirror.MirroredElemTypes): Query[F, P] =
      given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
        ResultSetReader
          .fold[F, Tuples.InverseColumnMap[T]]
          .toList
          .zipWithIndex
          .traverse {
            case (reader, index) => reader.asInstanceOf[ResultSetReader[F, Any]].read(resultSet, index + 1)
          }
          .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
      }
      Impl[F, P](statement, params)

  private[ldbc] case class Impl[F[_]: Temporal, T](
    statement: String,
    params:    List[Parameter.DynamicBinder]
  )(using Kleisli[F, ResultSet[F], T])
    extends Query[F, T]:

    override def to[G[_]: Traverse: Alternative]: Executor[F, G[T]] =
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
