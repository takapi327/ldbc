/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import scala.deriving.Mirror

import cats.{ Applicative, Eq }
import cats.syntax.all.*

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.sql.ResultSet

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
   * @param resultSet
   * A table of data representing a database result set, which is usually generated by executing a statement that
   * queries the database.
   * @param index
   * Index number of the data to be retrieved from the ResultSet.
   */
  def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, A]

  /** Map decoded results to a new type `B`, yielding a `Decoder[B]`. */
  def map[B](f: A => B): Decoder[B] = new Decoder[B]:
    override def offset: Int = self.offset
    override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, B] =
      self.decode(resultSet, index).map(f)

  /** Map decoded results to a new type `B` or an error, yielding a `Decoder[B]`. */
  def emap[B](f: A => Either[String, B]): Decoder[B] = new Decoder[B]:
    override def offset: Int = self.offset
    override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, B] =
      self.decode(resultSet, index).flatMap(f(_).leftMap(Decoder.Error(offset, _)))

  /** `Decoder` is semigroupal: a pair of decoders make a decoder for a pair. */
  def product[B](fb: Decoder[B]): Decoder[(A, B)] = new Decoder[(A, B)]:
    override def offset: Int = self.offset + fb.offset
    override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, (A, B)] =
      for
        a <- self.decode(resultSet, index)
        b <- fb.decode(resultSet, index + self.offset)
      yield (a, b)

  /** Lift this `Decoder` into `Option`. */
  def opt: Decoder[Option[A]] = new Decoder[Option[A]]:
    override def offset: Int = self.offset
    override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, Option[A]] =
      val value = self.decode(resultSet, index)
      if resultSet.wasNull() then Right(None) else value.map(Some(_))

object Decoder extends TwiddleSyntax[Decoder]:

  def apply[A](using decoder: Decoder[A]): Decoder[A] = decoder

  /**
   * An error indicating that decoding a value starting at column `offset` and spanning `length`
   * columns failed with reason `error`.
   */
  case class Error(offset: Int, message: String, cause: Option[Throwable] = None)

  object Error:
    given Eq[Error] = Eq.fromUniversalEquals

  given Applicative[Decoder] with
    override def map[A, B](fa: Decoder[A])(f: A => B): Decoder[B] = fa map f
    override def ap[A, B](fab: Decoder[A => B])(fa: Decoder[A]): Decoder[B] =
      map(fab.product(fa)) { case (fabb, a) => fabb(a) }
    override def pure[A](x: A): Decoder[A] = new Decoder[A]:
      override def offset:                                   Int                      = 0
      override def decode(resultSet: ResultSet, index: Int): Either[Decoder.Error, A] = Right(x)

  given [A](using codec: Codec[A]): Decoder[A] = codec.asDecoder

  given [A](using decoder: Decoder[A]): Decoder[Option[A]] = decoder.opt

  given [A, B](using da: Decoder[A], db: Decoder[B]): Decoder[(A, B)] =
    da.product(db)

  given [H, T <: Tuple](using dh: Decoder[H], dt: Decoder[T]): Decoder[H *: T] =
    dh.product(dt).map { case (h, t) => h *: t }

  given [P <: Product](using mirror: Mirror.ProductOf[P], decoder: Decoder[mirror.MirroredElemTypes]): Decoder[P] =
    decoder.to[P]
