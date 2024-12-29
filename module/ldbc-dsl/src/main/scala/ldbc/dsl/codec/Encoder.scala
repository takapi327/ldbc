/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.*

import scala.compiletime.*

import cats.ContravariantSemigroupal
import cats.data.NonEmptyList
import cats.syntax.all.*

import org.typelevel.twiddles.TwiddleSyntax

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
  def encode(value: A): Encoder.Encoded

  /** Contramap inputs from a new type `B`, yielding an `Encoder[B]`. */
  def contramap[B](func: B => A): Encoder[B] = (value: B) => encode(func(value))

  /** Map outputs to a new type `B`, yielding an `Encoder[B]`. */
  def either[B](func: B => Either[String, A]): Encoder[B] = (value: B) =>
    func(value) match
      case Right(value) => encode(value)
      case Left(error)  => Encoder.Encoded.failure(error)

  /** `Encoder` is semigroupal: a pair of encoders make a encoder for a pair. */
  def product[B](that: Encoder[B]): Encoder[(A, B)] = (value: (A, B)) => encode(value._1) product that.encode(value._2)

object Encoder extends TwiddleSyntax[Encoder]:

  /** Types that can be handled by PreparedStatement. */
  type Supported = Boolean | Byte | Short | Int | Long | Float | Double | BigDecimal | String | Array[Byte] |
    LocalTime | LocalDate | LocalDateTime | None.type

  def apply[A](using encoder: Encoder[A]): Encoder[A] = encoder
  
  given ContravariantSemigroupal[Encoder] with
    override def contramap[A, B](fa: Encoder[A])(f: B => A): Encoder[B] = fa.contramap(f)
    override def product[A, B](fa: Encoder[A], fb: Encoder[B]): Encoder[(A, B)] = fa.product(fb)

  given Encoder[Boolean] with
    override def encode(value: Boolean): Encoded =
      Encoded.success(List(value))

  given Encoder[Byte] with
    override def encode(value: Byte): Encoded =
      Encoded.success(List(value))

  given Encoder[Short] with
    override def encode(value: Short): Encoded =
      Encoded.success(List(value))

  given Encoder[Int] with
    override def encode(value: Int): Encoded =
      Encoded.success(List(value))

  given Encoder[Long] with
    override def encode(value: Long): Encoded =
      Encoded.success(List(value))

  given Encoder[Float] with
    override def encode(value: Float): Encoded =
      Encoded.success(List(value))

  given Encoder[Double] with
    override def encode(value: Double): Encoded =
      Encoded.success(List(value))

  given Encoder[BigDecimal] with
    override def encode(value: BigDecimal): Encoded =
      Encoded.success(List(value))

  given Encoder[String] with
    override def encode(value: String): Encoded =
      Encoded.success(List(value))

  given Encoder[Array[Byte]] with
    override def encode(value: Array[Byte]): Encoded =
      Encoded.success(List(value))

  given Encoder[LocalTime] with
    override def encode(value: LocalTime): Encoded =
      Encoded.success(List(value))

  given Encoder[LocalDate] with
    override def encode(value: LocalDate): Encoded =
      Encoded.success(List(value))

  given Encoder[LocalDateTime] with
    override def encode(value: LocalDateTime): Encoded =
      Encoded.success(List(value))

  given (using encoder: Encoder[String]): Encoder[Year] = encoder.contramap(_.toString)

  given (using encoder: Encoder[String]): Encoder[YearMonth] = encoder.contramap(_.toString)

  given (using encoder: Encoder[String]): Encoder[BigInt] = encoder.contramap(_.toString)

  given Encoder[None.type] with
    override def encode(value: None.type): Encoded =
      Encoded.success(List(None))

  given [A](using encoder: Encoder[A]): Encoder[Option[A]] with
    override def encode(value: Option[A]): Encoded =
      value match
        case Some(value) => encoder.encode(value)
        case None        => Encoded.success(List(None))

  type MapToTuple[T] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => Encoder[h] *: EmptyTuple
    case h *: t          => Encoder[h] *: MapToTuple[t]

  inline def fold[T]: MapToTuple[T] = summonAll[MapToTuple[T]]

  sealed trait Encoded:
    def product(that: Encoded): Encoded
  object Encoded:
    case class Success(value: List[Encoder.Supported]) extends Encoded:
      override def product(that: Encoded): Encoded = that match
        case Success(value)  => Success(this.value ::: value)
        case Failure(errors) => Failure(errors)
    case class Failure(errors: NonEmptyList[String]) extends Encoded:
      override def product(that: Encoded): Encoded = that match
        case Success(value)  => Failure(errors)
        case Failure(errors) => Failure(errors ::: this.errors)

    def success(value: List[Encoder.Supported]): Encoded = Success(value)
    def failure(error: String, errors: String*): Encoded =
      Failure(NonEmptyList(error, errors.toList))
