/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import cats.*
import cats.data.NonEmptyList
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

  /**
   * A method to return the data to be retrieved from the database as an Option type. If the data does not exist, None
   * is returned.
   * If there is more than one row to be returned, an exception is raised.
   */
  def option: DBIO[Option[T]]

  /**
   * A method to return the data to be retrieved from the database as a NonEmptyList type.
   * If there is no data, an exception is raised.
   */
  def nel: DBIO[NonEmptyList[T]]

  /**
   * A method to return the data to be retrieved from the database as a stream.
   * If there is no data, an empty stream is returned.
   */
  def stream: fs2.Stream[DBIO, T]

  /**
   * A method to return the data to be retrieved from the database as a stream with a specified fetch size.
   * If there is no data, an empty stream is returned.
   *
   * @param fetchSize
   *   The number of rows to be fetched at a time (must be positive)
   * @throws IllegalArgumentException
   *   if fetchSize is zero or negative
   */
  def stream(fetchSize: Int): fs2.Stream[DBIO, T]

object Query:

  private[ldbc] case class Impl[T](
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[T]
  ) extends Query[T]:

    override def to[G[_]](using factory: FactoryCompat[T, G[T]]): DBIO[G[T]] =
      DBIO.queryTo(statement, params, decoder, factory)

    override def unsafe: DBIO[T] =
      DBIO.queryA(statement, params, decoder)

    override def option: DBIO[Option[T]] =
      DBIO.queryOption(statement, params, decoder)

    override def nel: DBIO[NonEmptyList[T]] =
      DBIO.queryNel(statement, params, decoder)

    override def stream: fs2.Stream[DBIO, T] = stream(1)

    override def stream(fetchSize: Int): fs2.Stream[DBIO, T] =
      if fetchSize <= 0 then
        fs2.Stream.raiseError[DBIO](new IllegalArgumentException(s"fetchSize must be positive, but was: $fetchSize"))
      else DBIO.stream(statement, params, decoder, fetchSize)
