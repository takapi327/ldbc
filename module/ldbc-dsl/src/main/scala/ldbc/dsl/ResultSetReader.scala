/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import java.time.{ ZoneId, Instant, ZonedDateTime, LocalTime, LocalDate, LocalDateTime }

import scala.compiletime.*

import cats.Functor
import cats.syntax.all.*

import ldbc.sql.ResultSet

/**
 * Trait to get the DataType that matches the Scala type information from the ResultSet.
 *
 * @tparam T
 *   Scala types that match SQL DataType
 */
trait ResultSetReader[T]:

  /**
   * Method to retrieve data from a ResultSet using column names.
   *
   * @param resultSet
   *   A table of data representing a database result set, which is usually generated by executing a statement that
   *   queries the database.
   * @param columnLabel
   *   Column name of the data to be retrieved from the ResultSet.
   */
  def read(resultSet: ResultSet, columnLabel: String): T

  /**
   * Method to retrieve data from a ResultSet using an Index number.
   *
   * @param resultSet
   *   A table of data representing a database result set, which is usually generated by executing a statement that
   *   queries the database.
   * @param index
   *   Index number of the data to be retrieved from the ResultSet.
   */
  def read(resultSet: ResultSet, index: Int): T

object ResultSetReader:

  def apply[T](
    readLabel: ResultSet => String => T,
    readIndex: ResultSet => Int => T
  ): ResultSetReader[T] =
    new ResultSetReader[T]:
      override def read(resultSet: ResultSet, columnLabel: String): T =
        readLabel(resultSet)(columnLabel)

      override def read(resultSet: ResultSet, index: Int): T =
        readIndex(resultSet)(index)

  /**
   * A method to convert the specified Scala type to an arbitrary type so that it can be handled by ResultSetReader.
   *
   * @param f
   *   Function to convert from type A to B.
   * @param reader
   *   ResultSetReader to retrieve the DataType matching the type A information from the ResultSet.
   * @tparam A
   *   The Scala type to be converted from.
   * @tparam B
   *   The Scala type to be converted to.
   */
  def mapping[A, B](f: A => B)(using reader: ResultSetReader[A]): ResultSetReader[B] =
    reader.map(f(_))

  given Functor[[T] =>> ResultSetReader[T]] with
    override def map[A, B](fa: ResultSetReader[A])(f: A => B): ResultSetReader[B] =
      ResultSetReader(
        resultSet => columnLabel => f(fa.read(resultSet, columnLabel)),
        resultSet => index => f(fa.read(resultSet, index))
      )

  given ResultSetReader[String]        = ResultSetReader(_.getString, _.getString)
  given ResultSetReader[Boolean]       = ResultSetReader(_.getBoolean, _.getBoolean)
  given ResultSetReader[Byte]          = ResultSetReader(_.getByte, _.getByte)
  given ResultSetReader[Array[Byte]]   = ResultSetReader(_.getBytes, _.getBytes)
  given ResultSetReader[Short]         = ResultSetReader(_.getShort, _.getShort)
  given ResultSetReader[Int]           = ResultSetReader(_.getInt, _.getInt)
  given ResultSetReader[Long]          = ResultSetReader(_.getLong, _.getLong)
  given ResultSetReader[Float]         = ResultSetReader(_.getFloat, _.getFloat)
  given ResultSetReader[Double]        = ResultSetReader(_.getDouble, _.getDouble)
  given ResultSetReader[LocalDate]     = ResultSetReader(_.getDate, _.getDate)
  given ResultSetReader[LocalTime]     = ResultSetReader(_.getTime, _.getTime)
  given ResultSetReader[LocalDateTime] = ResultSetReader(_.getTimestamp, _.getTimestamp)
  given ResultSetReader[BigDecimal]    = ResultSetReader(_.getBigDecimal, _.getBigDecimal)

  given (using reader: ResultSetReader[String]): ResultSetReader[BigInt] =
    reader.map(str => if str == null then null else BigInt(str))

  given (using reader: ResultSetReader[Instant]): ResultSetReader[ZonedDateTime] =
    reader.map(instant => if instant == null then null else ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))

  given [A](using reader: ResultSetReader[A]): ResultSetReader[Option[A]] with

    override def read(resultSet: ResultSet, columnLabel: String): Option[A] =
      val result = reader.read(resultSet, columnLabel)
      val bool = resultSet.wasNull()
      if bool then None else Some(result)

    override def read(resultSet: ResultSet, index: Int): Option[A] =
      val result = reader.read(resultSet, index)
      val bool = resultSet.wasNull()
      if bool then None else Some(result)

  type MapToTuple[T <: Tuple] <: Tuple = T match
    case EmptyTuple => EmptyTuple
    case h *: t     => ResultSetReader[h] *: MapToTuple[t]

  inline def infer[T]: ResultSetReader[T] =
    summonFrom[ResultSetReader[T]] {
      case reader: ResultSetReader[T] => reader
      case _                          => error("ResultSetReader cannot be inferred")
    }

  inline def fold[T <: Tuple]: MapToTuple[T] =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (h *: t)   => infer[h] *: fold[t]
