/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.*

import scala.deriving.Mirror

import cats.InvariantSemigroupal

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.sql.ResultSet

trait Codec[A] extends Encoder[A], Decoder[A]:
  self =>

  /** Forget this value is a `Codec` and treat it as an `Encoder`. */
  def asEncoder: Encoder[A] = this

  /** Forget this value is a `Codec` and treat it as a `Decoder`. */
  def asDecoder: Decoder[A] = this

  /** `Codec` is semigroupal: a pair of codecs make a codec for a pair. */
  def product[B](fb: Codec[B]): Codec[(A, B)] = new Codec[(A, B)]:
    private val pe = self.asEncoder product fb.asEncoder
    private val pd = self.asDecoder product fb.asDecoder

    override def offset: Int = self.offset + fb.offset
    override def encode(value: (A, B)): Encoder.Encoded = pe.encode(value)
    override def decode(resultSet: ResultSet, index: Int): (A, B) = pd.decode(resultSet, index)

  /** Contramap inputs from, and map outputs to, a new type `B`, yielding a `Codec[B]`. */
  def imap[B](f: A => B)(g: B => A): Codec[B] = new Codec[B]:
    override def offset: Int = self.offset
    override def encode(value: B): Encoder.Encoded = self.encode(g(value))
    override def decode(resultSet: ResultSet, index: Int): B = f(self.decode(resultSet, index))

  /** Lift this `Codec` into `Option`, where `None` is mapped to and from a vector of `NULL`. */
  override def opt: Codec[Option[A]] = new Codec[Option[A]]:
    override def offset: Int = self.offset
    override def encode(value: Option[A]): Encoder.Encoded = value.fold(Encoder.Encoded.success(List(None)))(self.encode)
    override def decode(resultSet: ResultSet, index: Int): Option[A] =
      val value = self.decode(resultSet, index)
      if resultSet.wasNull() then None else Some(value)

object Codec extends TwiddleSyntax[Codec]:
  
  def apply[A](using codec: Codec[A]): Codec[A] = codec
  def one[A](using encoder: Encoder[A], decoder: Decoder[A]): Codec[A] = new Codec[A]:
    override def offset: Int = decoder.offset
    override def encode(value: A): Encoder.Encoded = encoder.encode(value)
    override def decode(resultSet: ResultSet, index: Int): A = decoder.decode(resultSet, index)

  given InvariantSemigroupal[Codec] with
    override def imap[A, B](fa: Codec[A])(f: A => B)(g: B => A): Codec[B] = fa.imap(f)(g)
    override def product[A, B](fa: Codec[A], fb: Codec[B]): Codec[(A, B)] = fa product fb

  given Codec[Boolean] with
    override def offset: Int = 1
    override def encode(value: Boolean): Encoder.Encoded = Encoder[Boolean].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Boolean = Decoder[Boolean].decode(resultSet, index)

  given Codec[Byte] with
    override def offset: Int = 1
    override def encode(value: Byte): Encoder.Encoded = Encoder[Byte].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Byte = Decoder[Byte].decode(resultSet, index)

  given Codec[Short] with
    override def offset: Int = 1
    override def encode(value: Short): Encoder.Encoded = Encoder[Short].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Short = Decoder[Short].decode(resultSet, index)

  given Codec[Int] with
    override def offset: Int = 1
    override def encode(value: Int): Encoder.Encoded = Encoder[Int].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Int = Decoder[Int].decode(resultSet, index)

  given Codec[Long] with
    override def offset: Int = 1
    override def encode(value: Long): Encoder.Encoded = Encoder[Long].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Long = Decoder[Long].decode(resultSet, index)

  given Codec[Float] with
    override def offset: Int = 1
    override def encode(value: Float): Encoder.Encoded = Encoder[Float].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Float = Decoder[Float].decode(resultSet, index)

  given Codec[Double] with
    override def offset: Int = 1
    override def encode(value: Double): Encoder.Encoded = Encoder[Double].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Double = Decoder[Double].decode(resultSet, index)

  given Codec[BigDecimal] with
    override def offset: Int = 1
    override def encode(value: BigDecimal): Encoder.Encoded = Encoder[BigDecimal].encode(value)
    override def decode(resultSet: ResultSet, index: Int): BigDecimal = Decoder[BigDecimal].decode(resultSet, index)

  given Codec[String] with
    override def offset: Int = 1
    override def encode(value: String): Encoder.Encoded = Encoder[String].encode(value)
    override def decode(resultSet: ResultSet, index: Int): String = Decoder[String].decode(resultSet, index)

  given Codec[Array[Byte]] with
    override def offset: Int = 1
    override def encode(value: Array[Byte]): Encoder.Encoded = Encoder[Array[Byte]].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Array[Byte] = Decoder[Array[Byte]].decode(resultSet, index)

  given Codec[LocalTime] with
    override def offset: Int = 1
    override def encode(value: LocalTime): Encoder.Encoded = Encoder[LocalTime].encode(value)
    override def decode(resultSet: ResultSet, index: Int): LocalTime = Decoder[LocalTime].decode(resultSet, index)

  given Codec[LocalDate] with
    override def offset: Int = 1
    override def encode(value: LocalDate): Encoder.Encoded = Encoder[LocalDate].encode(value)
    override def decode(resultSet: ResultSet, index: Int): LocalDate = Decoder[LocalDate].decode(resultSet, index)

  given Codec[LocalDateTime] with
    override def offset: Int = 1
    override def encode(value: LocalDateTime): Encoder.Encoded = Encoder[LocalDateTime].encode(value)
    override def decode(resultSet: ResultSet, index: Int): LocalDateTime = Decoder[LocalDateTime].decode(resultSet, index)

  given Codec[Year] with
    override def offset: Int = 1
    override def encode(value: Year): Encoder.Encoded = Encoder[Year].encode(value)
    override def decode(resultSet: ResultSet, index: Int): Year = Decoder[Year].decode(resultSet, index)

  given Codec[YearMonth] with
    override def offset: Int = 1
    override def encode(value: YearMonth): Encoder.Encoded = Encoder[YearMonth].encode(value)
    override def decode(resultSet: ResultSet, index: Int): YearMonth = Decoder[YearMonth].decode(resultSet, index)

  given Codec[BigInt] with
    override def offset: Int = 1
    override def encode(value: BigInt): Encoder.Encoded = Encoder[BigInt].encode(value)
    override def decode(resultSet: ResultSet, index: Int): BigInt = Decoder[BigInt].decode(resultSet, index)
  
  given [A](using codec: Codec[A]): Codec[Option[A]] = codec.opt
  
  given [A, B](using ca: Codec[A], cb: Codec[B]): Codec[(A, B)] = ca product cb
  
  given [H, T <: Tuple](using dh: Codec[H], dt: Codec[T]): Codec[H *: T] = dh.product(dt).imap { case (h, t) => h *: t }(tuple => (tuple.head, tuple.tail))

  given [P <: Product](using mirror: Mirror.ProductOf[P], codec: Codec[mirror.MirroredElemTypes]): Codec[P] = codec.to[P]

  given [A]: Conversion[Codec[A], Encoder[A]] = _.asEncoder
  given [A]: Conversion[Codec[A], Decoder[A]] = _.asDecoder
