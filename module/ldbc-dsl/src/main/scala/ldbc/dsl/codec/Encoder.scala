/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.*

import scala.deriving.Mirror

import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.ContravariantSemigroupal

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
  def contraemap[B](func: B => Either[String, A]): Encoder[B] = (value: B) =>
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
    override def contramap[A, B](fa: Encoder[A])(f:  B => A):     Encoder[B]      = fa.contramap(f)
    override def product[A, B](fa:   Encoder[A], fb: Encoder[B]): Encoder[(A, B)] = fa.product(fb)

  given [A](using codec: Codec[A]): Encoder[A] = codec.asEncoder

  given [A, B](using ea: Encoder[A], eb: Encoder[B]): Encoder[(A, B)] =
    ea.product(eb)

  given [H, T <: Tuple](using eh: Encoder[H], et: Encoder[T]): Encoder[H *: T] =
    eh.product(et).contramap { case h *: t => (h, t) }

  given [P <: Product](using mirror: Mirror.ProductOf[P], encoder: Encoder[mirror.MirroredElemTypes]): Encoder[P] =
    encoder.to[P]

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
