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
import cats.kernel.Semigroup
import cats.syntax.all.*

import ldbc.sql.util.FactoryCompat
import ldbc.sql.logging.*

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
  def toList[T](using reader: ResultSetReader[F, T], logHandler: LogHandler[F]): Kleisli[F, Connection[F], List[T]] =
    given Kleisli[F, ResultSet[F], T] = Kleisli(resultSet => reader.read(resultSet, 1))
    connection[List[T]](statement, params, summon[ResultSetConsumer[F, List[T]]])

  inline def toList[P <: Product](using
    mirror:     Mirror.ProductOf[P],
    logHandler: LogHandler[F],
    factory:    FactoryCompat[P, List[P]]
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

    connection[List[P]](statement, params, summon[ResultSetConsumer[F, List[P]]])

  /**
   * A method to return the data to be retrieved from the database as Option type. If there are multiple data, the
   * first one is retrieved.
   */
  def headOption[T](using
    reader:     ResultSetReader[F, T],
    logHandler: LogHandler[F]
  ): Kleisli[F, Connection[F], Option[T]] =
    given Kleisli[F, ResultSet[F], T] = Kleisli(resultSet => reader.read(resultSet, 1))
    connection[Option[T]](statement, params, summon[ResultSetConsumer[F, Option[T]]])

  inline def headOption[P <: Product](using
    mirror:     Mirror.ProductOf[P],
    logHandler: LogHandler[F]
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

    connection[Option[P]](statement, params, summon[ResultSetConsumer[F, Option[P]]])

  /**
   * A method to return the data to be retrieved from the database as is. If the data does not exist, an exception is
   * raised. Use the [[headOption]] method if you want to retrieve individual data.
   */
  def unsafe[T](using
    reader:     ResultSetReader[F, T],
    logHandler: LogHandler[F],
    ev:         MonadError[F, Throwable]
  ): Kleisli[F, Connection[F], T] =
    given Kleisli[F, ResultSet[F], T] = Kleisli(resultSet => reader.read(resultSet, 1))
    connection[T](statement, params, summon[ResultSetConsumer[F, T]])

  inline def unsafe[P <: Product](using
    mirror:     Mirror.ProductOf[P],
    logHandler: LogHandler[F],
    ev:         MonadError[F, Throwable]
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

    connection[P](statement, params, summon[ResultSetConsumer[F, P]])

  /**
   * A method to return the number of rows updated by the SQL statement.
   */
  def update(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], Int]

  def returning[T <: String | Int | Long](using
    reader:     ResultSetReader[F, T],
    logHandler: LogHandler[F]
  ): Kleisli[F, Connection[F], T]

  private[ldbc] def connection[T](
    statement: String,
    params:    Seq[ParameterBinder[F]],
    consumer:  ResultSetConsumer[F, T]
  )(using logHandler: LogHandler[F]): Kleisli[F, Connection[F], T]

object SQL:

  given [F[_]]: Semigroup[SQL[F]] with
    override def combine(x: SQL[F], y: SQL[F]): SQL[F] = x ++ y
