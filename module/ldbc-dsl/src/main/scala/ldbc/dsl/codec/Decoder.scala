/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import scala.compiletime.constValue
import scala.deriving.Mirror
import scala.reflect.Enum

import cats.{ Applicative, Eq }
import cats.syntax.all.*

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.dsl.exception.DecodeFailureException
import ldbc.dsl.free.ResultSetIO
import ldbc.dsl.util.Mirrors

/**
 * Trait to get the DataType that matches the Scala type information from the ResultSet.
 *
 * @tparam A
 *   Scala types that match SQL DataType
 */
trait Decoder[A]:
  self =>

  /**
   * Offset value for the next data to be retrieved from the ResultSet.
   */
  def offset: Int = 1

  /**
   * Method to retrieve data from a ResultSet using an Index number.
   *
   * @param index
   *   Index number of the data to be retrieved from the ResultSet.
   * @param statement
   *   Statement that configured the ResultSet, used for error messages.
   */
  def decode(index: Int, statement: String): ResultSetIO[A]

  /** Map decoded results to a new type `B`, yielding a `Decoder[B]`. */
  def map[B](f: A => B): Decoder[B] = new Decoder[B]:
    override def offset:                                Int            = self.offset
    override def decode(index: Int, statement: String): ResultSetIO[B] =
      self.decode(index, statement).map(f)

  /** Map decoded results to a new type `B` or an error, yielding a `Decoder[B]`. */
  def emap[B](f: A => Either[String, B]): Decoder[B] = new Decoder[B]:
    override def offset:                                Int            = self.offset
    override def decode(index: Int, statement: String): ResultSetIO[B] =
      self
        .decode(index, statement)
        .flatMap { a =>
          f(a) match
            case Left(error)  => ResultSetIO.raiseError(new DecodeFailureException(error, offset, statement, None))
            case Right(value) => ResultSetIO.pure(value)
        }

  /** `Decoder` is semigroupal: a pair of decoders make a decoder for a pair. */
  def product[B](fb: Decoder[B]): Decoder[(A, B)] = new Decoder[(A, B)]:
    override def offset:                                Int                 = self.offset + fb.offset
    override def decode(index: Int, statement: String): ResultSetIO[(A, B)] =
      for
        a <- self.decode(index, statement)
        b <- fb.decode(index + self.offset, statement)
      yield (a, b)

  /** Lift this `Decoder` into `Option`. */
  def opt: Decoder[Option[A]] = new Decoder[Option[A]]:
    override def offset:                                Int                    = self.offset
    override def decode(index: Int, statement: String): ResultSetIO[Option[A]] =
      self.decode(index, statement).map(Option(_))

object Decoder extends TwiddleSyntax[Decoder]:

  def apply[A](using decoder: Decoder[A]): Decoder[A] = decoder

  def derived[P <: Product](using mirror: Mirror.ProductOf[P], decoder: Decoder[mirror.MirroredElemTypes]): Decoder[P] =
    decoder.to[P]

  inline def derivedEnum[E <: Enum](using mirror: Mirror.SumOf[E]): Decoder[E] =
    Decoder[String].emap { name =>
      Mirrors.summonLabels[mirror.MirroredElemLabels].indexOf(name) match
        case -1 => Left(s"${ constValue[mirror.MirroredLabel] } Enum value $name not found")
        case i  => Right(Mirrors.summonEnumCases[mirror.MirroredElemTypes, E](constValue[mirror.MirroredLabel])(i))
    }

  /**
   * An error indicating that decoding a value starting at column `offset` and spanning `length`
   * columns failed with reason `error`.
   */
  case class Error(offset: Int, message: String, cause: Option[Throwable] = None)

  object Error:
    given Eq[Error] = Eq.fromUniversalEquals

  given Applicative[Decoder] with
    override def map[A, B](fa: Decoder[A])(f: A => B):           Decoder[B] = fa map f
    override def ap[A, B](fab: Decoder[A => B])(fa: Decoder[A]): Decoder[B] =
      map(fab.product(fa)) { case (fabb, a) => fabb(a) }
    override def pure[A](x: A): Decoder[A] = new Decoder[A]:
      override def offset:                                Int            = 0
      override def decode(index: Int, statement: String): ResultSetIO[A] = ResultSetIO.pure(x)

  given [A](using codec: Codec[A]): Decoder[A] = codec.asDecoder

  given [A](using decoder: Decoder[A]): Decoder[Option[A]] = decoder.opt

  given [A, B](using da: Decoder[A], db: Decoder[B]): Decoder[(A, B)] =
    da.product(db)

  given [NT](using m: Mirror.ProductOf[NT], decoder: Decoder[m.MirroredElemTypes]): Decoder[NT] =
    decoder.map(tuple => m.fromProduct(tuple))

  given [H, T <: Tuple](using dh: Decoder[H], dt: Decoder[T]): Decoder[H *: T] =
    dh.product(dt).map { case (h, t) => h *: t }
