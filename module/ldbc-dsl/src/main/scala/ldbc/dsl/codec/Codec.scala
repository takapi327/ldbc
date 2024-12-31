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

/**
 * Symmetric encoder and decoder of MySQL data to and from Scala types.
 *
 * @tparam A
 *   Types handled in Scala
 */
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

    override def offset:                                   Int             = self.offset + fb.offset
    override def encode(value:     (A, B)):                Encoder.Encoded = pe.encode(value)
    override def decode(resultSet: ResultSet, index: Int): (A, B)          = pd.decode(resultSet, index)

  /** Contramap inputs from, and map outputs to, a new type `B`, yielding a `Codec[B]`. */
  def imap[B](f: A => B)(g: B => A): Codec[B] = new Codec[B]:
    override def offset:                                   Int             = self.offset
    override def encode(value:     B):                     Encoder.Encoded = self.encode(g(value))
    override def decode(resultSet: ResultSet, index: Int): B               = f(self.decode(resultSet, index))

  /** Lift this `Codec` into `Option`, where `None` is mapped to and from a vector of `NULL`. */
  override def opt: Codec[Option[A]] = new Codec[Option[A]]:
    override def offset: Int = self.offset
    override def encode(value: Option[A]): Encoder.Encoded =
      value.fold(Encoder.Encoded.success(List(None)))(self.encode)
    override def decode(resultSet: ResultSet, index: Int): Option[A] =
      val value = self.decode(resultSet, index)
      if resultSet.wasNull() then None else Some(value)

object Codec extends TwiddleSyntax[Codec]:

  def apply[A](using codec: Codec[A]): Codec[A] = codec

  given InvariantSemigroupal[Codec] with
    override def imap[A, B](fa:    Codec[A])(f:  A => B)(g: B => A): Codec[B]      = fa.imap(f)(g)
    override def product[A, B](fa: Codec[A], fb: Codec[B]):          Codec[(A, B)] = fa product fb

  given Codec[Boolean] with
    override def offset:                                   Int             = 1
    override def encode(value:     Boolean):               Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Boolean         = resultSet.getBoolean(index)

  given Codec[Byte] with
    override def offset:                                   Int             = 1
    override def encode(value:     Byte):                  Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Byte            = resultSet.getByte(index)

  given Codec[Short] with
    override def offset:                                   Int             = 1
    override def encode(value:     Short):                 Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Short           = resultSet.getShort(index)

  given Codec[Int] with
    override def offset:                                   Int             = 1
    override def encode(value:     Int):                   Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Int             = resultSet.getInt(index)

  given Codec[Long] with
    override def offset:                                   Int             = 1
    override def encode(value:     Long):                  Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Long            = resultSet.getLong(index)

  given Codec[Float] with
    override def offset:                                   Int             = 1
    override def encode(value:     Float):                 Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Float           = resultSet.getFloat(index)

  given Codec[Double] with
    override def offset:                                   Int             = 1
    override def encode(value:     Double):                Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Double          = resultSet.getDouble(index)

  given Codec[BigDecimal] with
    override def offset:                                   Int             = 1
    override def encode(value:     BigDecimal):            Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): BigDecimal      = resultSet.getBigDecimal(index)

  given Codec[String] with
    override def offset:                                   Int             = 1
    override def encode(value:     String):                Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): String          = resultSet.getString(index)

  given Codec[Array[Byte]] with
    override def offset:                                   Int             = 1
    override def encode(value:     Array[Byte]):           Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): Array[Byte]     = resultSet.getBytes(index)

  given Codec[LocalTime] with
    override def offset:                                   Int             = 1
    override def encode(value:     LocalTime):             Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): LocalTime       = resultSet.getTime(index)

  given Codec[LocalDate] with
    override def offset:                                   Int             = 1
    override def encode(value:     LocalDate):             Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): LocalDate       = resultSet.getDate(index)

  given Codec[LocalDateTime] with
    override def offset:                                   Int             = 1
    override def encode(value:     LocalDateTime):         Encoder.Encoded = Encoder.Encoded.success(List(value))
    override def decode(resultSet: ResultSet, index: Int): LocalDateTime   = resultSet.getTimestamp(index)

  given [A](using codec: Codec[Int]): Codec[Year] =
    codec.imap(Year.of)(_.getValue)

  given [A](using codec: Codec[String]): Codec[YearMonth] =
    codec.imap(YearMonth.parse)(_.toString)

  given [A](using codec: Codec[String]): Codec[BigInt] =
    codec.imap(str => if str == null then null else BigInt(str))(_.toString)

  given Codec[None.type] with
    override def offset:                                   Int             = 1
    override def encode(value:     None.type):             Encoder.Encoded = Encoder.Encoded.success(List(None))
    override def decode(resultSet: ResultSet, index: Int): None.type       = None

  given [A](using codec: Codec[A]): Codec[Option[A]] = codec.opt

  given [A, B](using ca: Codec[A], cb: Codec[B]): Codec[(A, B)] = ca product cb

  given [H, T <: Tuple](using dh: Codec[H], dt: Codec[T]): Codec[H *: T] =
    dh.product(dt).imap { case (h, t) => h *: t }(tuple => (tuple.head, tuple.tail))

  given [P <: Product](using mirror: Mirror.ProductOf[P], codec: Codec[mirror.MirroredElemTypes]): Codec[P] =
    codec.to[P]
