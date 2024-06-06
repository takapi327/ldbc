/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZonedDateTime }

import scala.compiletime.*

/**
 * Trait for setting Scala and Java values to PreparedStatement.
 *
 * @tparam T
 *   Scala and Java types available in PreparedStatement.
 */
trait Parameter[-T]:

  /**
   * Methods for setting Scala and Java values to the specified position in PreparedStatement.
   *
   * @param statement
   *   An object that represents a precompiled SQL statement.
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   * @tparam F
   *   The effect type
   */
  def bind[F[_]](statement: PreparedStatement[F], index: Int, value: T): F[Unit]

object Parameter:

  def convert[A, B](f: B => A)(using parameter: Parameter[A]): Parameter[B] = new Parameter[B]:
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: B): F[Unit] = 
      parameter.bind[F](statement, index, f(value))

  given Parameter[Boolean] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Boolean): F[Unit] =
      statement.setBoolean(index, value)

  given Parameter[Byte] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Byte): F[Unit] =
      statement.setByte(index, value)

  given Parameter[Int] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Int): F[Unit] =
      statement.setInt(index, value)

  given Parameter[Short] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Short): F[Unit] =
      statement.setShort(index, value)

  given Parameter[Long] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Long): F[Unit] =
      statement.setLong(index, value)

  given Parameter[Float] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Float): F[Unit] =
      statement.setFloat(index, value)

  given Parameter[Double] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Double): F[Unit] =
      statement.setDouble(index, value)

  given Parameter[BigDecimal] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: BigDecimal): F[Unit] =
      statement.setBigDecimal(index, value)

  given Parameter[String] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: String): F[Unit] =
      statement.setString(index, value)

  given Parameter[Array[Byte]] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Array[Byte]): F[Unit] =
      statement.setBytes(index, value)

  given Parameter[LocalDate] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: LocalDate): F[Unit] =
      statement.setDate(index, value)

  given Parameter[LocalTime] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: LocalTime): F[Unit] =
      statement.setTime(index, value)

  given Parameter[LocalDateTime] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: LocalDateTime): F[Unit] =
      statement.setTimestamp(index, value)

  given Parameter[Instant] =
    Parameter.convert(instant => LocalDateTime.ofInstant(instant, ZoneId.systemDefault()))

  given Parameter[ZonedDateTime] = Parameter.convert(_.toInstant)

  given Parameter[Object] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Object): F[Unit] =
      statement.setObject(index, value)

  given Parameter[Null] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Null): F[Unit] =
      statement.setObject(index, value)

  given Parameter[None.type] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: None.type): F[Unit] =
      statement.setObject(index, null)

  given [T](using parameter: Parameter[T], nullParameter: Parameter[Null]): Parameter[Option[T]] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Option[T]): F[Unit] =
      value match
        case Some(value) => parameter.bind(statement, index, value)
        case None        => nullParameter.bind(statement, index, null)

  given [T](using parameter: Parameter[String]): Parameter[List[T]] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: List[T]): F[Unit] =
      parameter.bind(statement, index, value.mkString(","))

  given [T](using parameter: Parameter[String]): Parameter[Seq[T]] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Seq[T]): F[Unit] =
      parameter.bind(statement, index, value.mkString(","))

  given [T](using parameter: Parameter[String]): Parameter[Set[T]] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Set[T]): F[Unit] =
      parameter.bind(statement, index, value.mkString(","))

  type MapToTuple[T] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => Parameter[h] *: EmptyTuple
    case h *: t          => Parameter[h] *: MapToTuple[t]

  inline def infer[T]: Parameter[T] =
    summonFrom[Parameter[T]] {
      case parameter: Parameter[T] => parameter
      case _                          => error("Parameter cannot be inferred")
    }

  inline def fold[T]: MapToTuple[T] =
    inline erasedValue[T] match
      case _: EmptyTuple        => EmptyTuple
      case _: (h *: EmptyTuple) => infer[h] *: EmptyTuple
      case _: (h *: t)          => infer[h] *: fold[t]
