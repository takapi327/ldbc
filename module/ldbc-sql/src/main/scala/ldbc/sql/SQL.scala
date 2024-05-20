/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import scala.deriving.Mirror
import scala.annotation.targetName

import cats.{ Monad, MonadError }
import cats.data.Kleisli
import cats.syntax.all.*

import ldbc.sql.util.FactoryCompat

/**
 * A model with a query string and parameters to be bound to the query string that is executed by PreparedStatement,
 * etc.
 *
 * @tparam F
 *   The effect type
 */
trait SQL[F[_]: Monad]:

  /**
   * an SQL statement that may contain one or more '?' IN parameter placeholders
   */
  def statement: String

  /**
   * statement has '?' that the statement has.
   */
  def params: Seq[ParameterBinder[F]]

  @targetName("combine")
  def ++(sql: SQL[F]): SQL[F]

  /**
   * Methods for returning an array of data to be retrieved from the database.
   */
  inline def toList[T <: Tuple](using FactoryCompat[T, List[T]]): Kleisli[F, Connection[F], List[T]] =
    given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, T]
        .toList
        .zipWithIndex
        .traverse {
          case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
    }

    connectionToList[T](statement, params)

  inline def toList[P <: Product](using
    mirror:  Mirror.ProductOf[P],
    factory: FactoryCompat[P, List[P]]
  ): Kleisli[F, Connection[F], List[P]] =
    given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, mirror.MirroredElemTypes]
        .toList
        .zipWithIndex
        .traverse {
          case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
    }

    connectionToList[P](statement, params)

  /**
   * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
   * first one is retrieved.
   */
  inline def headOption[T <: Tuple]: Kleisli[F, Connection[F], Option[T]] =
    given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, T]
        .toList
        .zipWithIndex
        .traverse {
          case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
    }

    connectionToHeadOption[T](statement, params)

  inline def headOption[P <: Product](using
    mirror: Mirror.ProductOf[P]
  ): Kleisli[F, Connection[F], Option[P]] =
    given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, mirror.MirroredElemTypes]
        .toList
        .zipWithIndex
        .traverse {
          case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
    }

    connectionToHeadOption[P](statement, params)

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[headOption]] method if you want to retrieve individual data.
   */
  inline def unsafe[T <: Tuple](using MonadError[F, Throwable]): Kleisli[F, Connection[F], T] =
    given Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, T]
        .toList
        .zipWithIndex
        .traverse {
          case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
        }
        .map(list => Tuple.fromArray(list.toArray).asInstanceOf[T])
    }

    connectionToUnsafe[T](statement, params)

  inline def unsafe[P <: Product](using
    mirror: Mirror.ProductOf[P],
    ev:     MonadError[F, Throwable]
  ): Kleisli[F, Connection[F], P] =
    given Kleisli[F, ResultSet[F], P] = Kleisli { resultSet =>
      ResultSetReader
        .fold[F, mirror.MirroredElemTypes]
        .toList
        .zipWithIndex
        .traverse {
          case (reader: ResultSetReader[F, Any], index) => reader.read(resultSet, index + 1)
        }
        .map(list => mirror.fromProduct(Tuple.fromArray(list.toArray)))
    }

    connectionToUnsafe[P](statement, params)

  /**
   * A method to return the number of rows updated by the SQL statement.
   */
  def update: Kleisli[F, Connection[F], Int] = ???

  private[ldbc] def connection[T](
    statement: String,
    params:    Seq[ParameterBinder[F]],
    consumer:  ResultSetConsumer[F, T]
  ): Kleisli[F, Connection[F], T]

  /**
   * Methods for returning an array of data to be retrieved from the database.
   */
  private def connectionToList[T](
    statement: String,
    params:    Seq[ParameterBinder[F]]
  )(using Kleisli[F, ResultSet[F], T], FactoryCompat[T, List[T]]): Kleisli[F, Connection[F], List[T]] =
    connection[List[T]](statement, params, summon[ResultSetConsumer[F, List[T]]])

  /**
   * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the first
   * one is retrieved.
   */
  private def connectionToHeadOption[T](
    statement: String,
    params:    Seq[ParameterBinder[F]]
  )(using Kleisli[F, ResultSet[F], T]): Kleisli[F, Connection[F], Option[T]] =
    connection[Option[T]](statement, params, summon[ResultSetConsumer[F, Option[T]]])

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[connectionToHeadOption]] method if you want to retrieve individual data.
   */
  private def connectionToUnsafe[T](
    statement: String,
    params:    Seq[ParameterBinder[F]]
  )(using Kleisli[F, ResultSet[F], T], MonadError[F, Throwable]): Kleisli[F, Connection[F], T] =
    connection[T](statement, params, summon[ResultSetConsumer[F, T]])
