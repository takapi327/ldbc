/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.{ ZoneId, Instant, ZonedDateTime, LocalTime, LocalDate, LocalDateTime }

import scala.compiletime.*
import scala.deriving.Mirror

import cats.Functor
import cats.syntax.all.*

import ldbc.sql.ResultSet

/**
 * Trait to get the DataType that matches the Scala type information from the ResultSet.
 *
 * @tparam A
 *   Scala types that match SQL DataType
 */
trait Decoder[A]:

  /**
   * Method to retrieve data from a ResultSet
   *
   * @param resultSet
   *   A table of data representing a database result set, which is usually generated by executing a statement that
   *   queries the database.
   */
  def decode(resultSet: ResultSet): A

object Decoder:

  /**
   * Trait to get the DataType that matches the Scala type information from the ResultSet.
   *
   * @tparam A
   *   Scala types that match SQL DataType
   */
  trait Elem[A]:

    /**
     * Method to retrieve data from a ResultSet using column names.
     *
     * @param resultSet
     * A table of data representing a database result set, which is usually generated by executing a statement that
     * queries the database.
     * @param columnLabel
     * Column name of the data to be retrieved from the ResultSet.
     */
    def decode(resultSet: ResultSet, columnLabel: String): A

    /**
     * Method to retrieve data from a ResultSet using an Index number.
     *
     * @param resultSet
     * A table of data representing a database result set, which is usually generated by executing a statement that
     * queries the database.
     * @param index
     * Index number of the data to be retrieved from the ResultSet.
     */
    def decode(resultSet: ResultSet, index: Int): A

  object Elem:
    def apply[T](
      decodeLabel: ResultSet => String => T,
      decodeIndex: ResultSet => Int => T
    ): Elem[T] =
      new Elem[T]:
        override def decode(resultSet: ResultSet, columnLabel: String): T =
          decodeLabel(resultSet)(columnLabel)

        override def decode(resultSet: ResultSet, index: Int): T =
          decodeIndex(resultSet)(index)

    given Functor[[T] =>> Elem[T]] with
      override def map[A, B](fa: Elem[A])(f: A => B): Elem[B] =
        Elem(
          resultSet => columnLabel => f(fa.decode(resultSet, columnLabel)),
          resultSet => index => f(fa.decode(resultSet, index))
        )

    given Elem[String]        = Elem(_.getString, _.getString)
    given Elem[Boolean]       = Elem(_.getBoolean, _.getBoolean)
    given Elem[Byte]          = Elem(_.getByte, _.getByte)
    given Elem[Array[Byte]]   = Elem(_.getBytes, _.getBytes)
    given Elem[Short]         = Elem(_.getShort, _.getShort)
    given Elem[Int]           = Elem(_.getInt, _.getInt)
    given Elem[Long]          = Elem(_.getLong, _.getLong)
    given Elem[Float]         = Elem(_.getFloat, _.getFloat)
    given Elem[Double]        = Elem(_.getDouble, _.getDouble)
    given Elem[LocalDate]     = Elem(_.getDate, _.getDate)
    given Elem[LocalTime]     = Elem(_.getTime, _.getTime)
    given Elem[LocalDateTime] = Elem(_.getTimestamp, _.getTimestamp)
    given Elem[BigDecimal]    = Elem(_.getBigDecimal, _.getBigDecimal)

    given (using decoder: Elem[String]): Elem[BigInt] =
      decoder.map(str => if str == null then null else BigInt(str))

    given (using decoder: Elem[Instant]): Elem[ZonedDateTime] =
      decoder.map(instant => if instant == null then null else ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))

    given [A](using decoder: Elem[A]): Elem[Option[A]] with
      override def decode(resultSet: ResultSet, columnLabel: String): Option[A] =
        val value = decoder.decode(resultSet, columnLabel)
        if resultSet.wasNull() then None else Some(value)

      override def decode(resultSet: ResultSet, index: Int): Option[A] =
        val value = decoder.decode(resultSet, index)
        if resultSet.wasNull() then None else Some(value)

  def one[A](using decoder: Decoder.Elem[A]): Decoder[A] =
    (resultSet: ResultSet) => decoder.decode(resultSet, 1)

  inline given derived[A](using mirror: Mirror.Of[A]): Decoder[A] =
    inline mirror match
      case s: Mirror.SumOf[A]     => error("Sum type is not supported")
      case p: Mirror.ProductOf[A] => derivedProduct(p)

  private[ldbc] inline def derivedProduct[A](mirror: Mirror.ProductOf[A]): Decoder[A] =
    val labels  = constValueTuple[mirror.MirroredElemLabels].toArray.map(_.toString)
    val decodes = getDecoders[mirror.MirroredElemTypes].toArray

    (resultSet: ResultSet) =>
      val results = labels.zip(decodes).map { (label, decoder) =>
        decoder match
          case dm: Decoder.Elem[t] => dm.decode(resultSet, label)
          case d: Decoder[t]       => d.decode(resultSet)
      }

      mirror.fromTuple(Tuple.fromArray(results).asInstanceOf[mirror.MirroredElemTypes])

  private[ldbc] inline def derivedTuple[A](mirror: Mirror.ProductOf[A]): Decoder[A] =
    val decodes = getDecoders[mirror.MirroredElemTypes].toArray

    (resultSet: ResultSet) =>
      val results = decodes.zipWithIndex.map { (decoder, index) =>
        decoder match
          case dm: Decoder.Elem[t] => dm.decode(resultSet, index + 1)
          case d: Decoder[t]       => d.decode(resultSet)
      }

      mirror.fromTuple(Tuple.fromArray(results).asInstanceOf[mirror.MirroredElemTypes])

  private inline def getDecoders[T <: Tuple]: Tuple =
    inline erasedValue[T] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts) =>
        summonFrom {
          case dm: Decoder.Elem[`t`] => dm
          case d: Decoder[`t`]       => d
        } *: getDecoders[ts]
