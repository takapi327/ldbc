/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.{ZoneId, Instant, ZonedDateTime, LocalTime, LocalDate, LocalDateTime}

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
   * Method to retrieve data from a ResultSet using column names.
   *
   * @param resultSet
   *   A table of data representing a database result set, which is usually generated by executing a statement that
   *   queries the database.
   * @param columnLabel
   *   Column name of the data to be retrieved from the ResultSet.
   */
  def decode(resultSet: ResultSet, columnLabel: String): A

  /**
   * Method to retrieve data from a ResultSet using an Index number.
   *
   * @param resultSet
   *   A table of data representing a database result set, which is usually generated by executing a statement that
   *   queries the database.
   * @param index
   *   Index number of the data to be retrieved from the ResultSet.
   */
  def decode(resultSet: ResultSet, index: Int): A

object Decoder:
  
  def apply[T](
    decodeLabel: ResultSet => String => T,
    decodeIndex: ResultSet => Int => T
  ): Decoder[T] =
    new Decoder[T]:
      override def decode(resultSet: ResultSet, columnLabel: String): T =
        decodeLabel(resultSet)(columnLabel)

      override def decode(resultSet: ResultSet, index: Int): T =
        decodeIndex(resultSet)(index)
  
  given Functor[[T] =>> Decoder[T]] with
    override def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] =
      Decoder(
        resultSet => columnLabel => f(fa.decode(resultSet, columnLabel)),
        resultSet => index => f(fa.decode(resultSet, index))
      )

  given Decoder[String] = Decoder(_.getString, _.getString)
  given Decoder[Boolean] = Decoder(_.getBoolean, _.getBoolean)
  given Decoder[Byte] = Decoder(_.getByte, _.getByte)
  given Decoder[Array[Byte]] = Decoder(_.getBytes, _.getBytes)
  given Decoder[Short] = Decoder(_.getShort, _.getShort)
  given Decoder[Int] = Decoder(_.getInt, _.getInt)
  given Decoder[Long] = Decoder(_.getLong, _.getLong)
  given Decoder[Float] = Decoder(_.getFloat, _.getFloat)
  given Decoder[Double] = Decoder(_.getDouble, _.getDouble)
  given Decoder[LocalDate] = Decoder(_.getDate, _.getDate)
  given Decoder[LocalTime] = Decoder(_.getTime, _.getTime)
  given Decoder[LocalDateTime] = Decoder(_.getTimestamp, _.getTimestamp)
  given Decoder[BigDecimal] = Decoder(_.getBigDecimal, _.getBigDecimal)

  given (using decoder: Decoder[String]): Decoder[BigInt] =
    decoder.map(str => if str == null then null else BigInt(str))

  given (using decoder: Decoder[Instant]): Decoder[ZonedDateTime] =
    decoder.map(instant => if instant == null then null else ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))

  given [A](using decoder: Decoder[A]): Decoder[Option[A]] with
    override def decode(resultSet: ResultSet, columnLabel: String): Option[A] =
      val value = decoder.decode(resultSet, columnLabel)
      if resultSet.wasNull() then None else Some(value)

    override def decode(resultSet: ResultSet, index: Int): Option[A] =
      val value = decoder.decode(resultSet, index)
      if resultSet.wasNull() then None else Some(value)

  trait Product[A]:
    def decode(resultSet: ResultSet): A

  object Product:

    inline given derived[A](using mirror: Mirror.Of[A]): Product[A] =
      inline mirror match
        case s: Mirror.SumOf[A] => error("Sum type is not supported")
        case p: Mirror.ProductOf[A] => derivedProduct(p)

    private inline def derivedProduct[A](mirror: Mirror.ProductOf[A]): Product[A] =
      val labels = constValueTuple[mirror.MirroredElemLabels].toArray.map(_.toString)
      val decodes = getDecoders[mirror.MirroredElemTypes].toArray

      (resultSet: ResultSet) =>
        val results = labels.zip(decodes).map { (label, decoder) =>
          decoder match
            case d: Decoder[t] => d.decode(resultSet, label)
            case dp: Decoder.Product[t] => dp.decode(resultSet)
        }
        
        mirror.fromTuple(Tuple.fromArray(results).asInstanceOf[mirror.MirroredElemTypes])

    private inline def getDecoders[T <: Tuple]: Tuple =
      inline erasedValue[T] match
        case _: EmptyTuple => EmptyTuple
        case _: (t *: ts) =>
          summonFrom {
            case d: Decoder[`t`] => d
            case dp: Decoder[`t`] => dp
          } *: getDecoders[ts]