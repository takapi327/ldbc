/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.codec

import java.time.*

import ldbc.sql.PreparedStatement

trait Encoder[A]:

  def encode(value: A): Encoder.SUPPORTED

object Encoder:

  type SUPPORTED = Boolean | Byte | Short | Int | Long | Float | Double | BigDecimal | String | Array[Byte] |
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

  given Encoder[Instant] with
    override def encode(value: Instant): LocalDateTime = LocalDateTime.ofInstant(value, ZoneId.systemDefault())

  given Encoder[ZonedDateTime] with
    override def encode(value: ZonedDateTime): LocalDateTime = value.toLocalDateTime

  given [A](using encoder: Encoder[A]): Encoder[Option[A]] with
    override def encode(value: Option[A]): Encoder.SUPPORTED =
      value match
        case Some(value) => encoder.encode(value)
        case None        => None

trait Parameter[F[_]]:
  def parameter:                                         String
  def bind(statement: PreparedStatement[F], index: Int): F[Unit]

object Parameter:

  def apply[F[_], A](value: A)(using encoder: Encoder[A]): Parameter[F] =
    new Parameter[F]:
      override def parameter: String = value.toString
      override def bind(statement: PreparedStatement[F], index: Int): F[Unit] =
        encoder.encode(value) match
          case value: Boolean       => statement.setBoolean(index, value)
          case value: Byte          => statement.setByte(index, value)
          case value: Short         => statement.setShort(index, value)
          case value: Int           => statement.setInt(index, value)
          case value: Long          => statement.setLong(index, value)
          case value: Float         => statement.setFloat(index, value)
          case value: Double        => statement.setDouble(index, value)
          case value: BigDecimal    => statement.setBigDecimal(index, value)
          case value: String        => statement.setString(index, value)
          case value: Array[Byte]   => statement.setBytes(index, value)
          case value: LocalTime     => statement.setTime(index, value)
          case value: LocalDate     => statement.setDate(index, value)
          case value: LocalDateTime => statement.setTimestamp(index, value)
          case None                 => statement.setNull(index, ldbc.sql.Types.NULL)

  given [F[_], A](using Encoder[A]): Conversion[A, Parameter[F]] with
    override def apply(value: A): Parameter[F] = Parameter(value)

trait Test[F[_]]:
  extension (sc: StringContext)
    def sql(args: Parameter[F]*): String =
      val query = sc.parts.iterator.mkString("?")
      val (expressions, parameters) = args.foldLeft((query, List.empty[Parameter[F]])) {
        case ((query, parameters), p) => (query, parameters :+ p)
      }
      expressions
  end extension

val io = new Test[cats.effect.IO] {}
