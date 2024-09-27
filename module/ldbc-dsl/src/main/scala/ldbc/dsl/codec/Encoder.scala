/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.*

import scala.compiletime.*

/**
 * Trait for converting Scala types to types that can be handled by PreparedStatement.
 *
 * @tparam A
 *   Types handled in Scala
 */
trait Encoder[A]:

  /**
   * Method to convert Scala types to types that can be handled by PreparedStatement.
   *
   * @param value
   *   Scala types
   * @return
   *   Types that can be handled by PreparedStatement
   */
  def encode(value: A): Encoder.Supported

object Encoder:

  /** Types that can be handled by PreparedStatement. */
  type Supported = Boolean | Byte | Short | Int | Long | Float | Double | BigDecimal | String | Array[Byte] |
    LocalTime | LocalDate | LocalDateTime | None.type

  given Encoder[Boolean] with
    override def encode(value: Boolean): Boolean = value

  given Encoder[Byte] with
    override def encode(value: Byte): Byte = value

  given Encoder[Short] with
    override def encode(value: Short): Short = value

  given Encoder[Int] with
    override def encode(value: Int): Int = value

  given Encoder[Long] with
    override def encode(value: Long): Long = value

  given Encoder[Float] with
    override def encode(value: Float): Float = value

  given Encoder[Double] with
    override def encode(value: Double): Double = value

  given Encoder[BigDecimal] with
    override def encode(value: BigDecimal): BigDecimal = value

  given Encoder[String] with
    override def encode(value: String): String = value

  given Encoder[Array[Byte]] with
    override def encode(value: Array[Byte]): Array[Byte] = value

  given Encoder[LocalTime] with
    override def encode(value: LocalTime): LocalTime = value

  given Encoder[LocalDate] with
    override def encode(value: LocalDate): LocalDate = value

  given Encoder[LocalDateTime] with
    override def encode(value: LocalDateTime): LocalDateTime = value

  given Encoder[Year] with
    override def encode(value: Year): String = value.toString

  given Encoder[YearMonth] with
    override def encode(value: YearMonth): String = value.toString

  given Encoder[None.type] with
    override def encode(value: None.type): None.type = value

  given [A](using encoder: Encoder[A]): Encoder[Option[A]] with
    override def encode(value: Option[A]): Encoder.Supported =
      value match
        case Some(value) => encoder.encode(value)
        case None        => None

  type MapToTuple[T] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => Encoder[h] *: EmptyTuple
    case h *: t          => Encoder[h] *: MapToTuple[t]

  inline def infer[T]: Encoder[T] =
    summonFrom[Encoder[T]] {
      case parameter: Encoder[T] => parameter
      case _                     => error("Parameter cannot be inferred")
    }

  inline def fold[T]: MapToTuple[T] =
    inline erasedValue[T] match
      case _: EmptyTuple        => EmptyTuple
      case _: (h *: EmptyTuple) => infer[h] *: EmptyTuple
      case _: (h *: t)          => infer[h] *: fold[t]
