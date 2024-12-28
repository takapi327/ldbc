/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import java.time.*

import cats.MonadThrow
import cats.syntax.all.*

import ldbc.sql.PreparedStatement
import ldbc.dsl.codec.Encoder

/**
 * Trait for setting Scala and Java values to PreparedStatement.
 */
trait Parameter:

  /** Query parameters to be plugged into the Statement. */
  def value: String

object Parameter:

  case class Static(value: String) extends Parameter:
    override def toString: String = value

  trait Dynamic extends Parameter:

    /**
     * Methods for setting Scala and Java values to the specified position in PreparedStatement.
     *
     * @param statement
     *   An object that represents a precompiled SQL statement.
     * @param index
     *   the parameter value
     */
    def bind[F[_]](statement: PreparedStatement[F], index: Int)(using ev: MonadThrow[F]): F[Unit]

  object Dynamic:

    def many[A](encoded: Encoder.Encoded): List[Dynamic] =
      encoded match
        case Encoder.Encoded.Success(list) =>
          list.map { v =>
            new Dynamic:
              override def value: String = v.toString
              override def bind[F[_]](statement: PreparedStatement[F], index: Int)(using ev: MonadThrow[F]): F[Unit] =
                v match
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
          }
        case Encoder.Encoded.Failure(errors) =>
          throw new IllegalArgumentException(errors.toList.mkString(", "))

    def apply[A](_value: A)(using encoder: Encoder[A]): Dynamic =
      new Dynamic:
        override def value: String = _value.toString
        override def bind[F[_]](statement: PreparedStatement[F], index: Int)(using ev: MonadThrow[F]): F[Unit] =
          encoder.encode(_value) match
            case Encoder.Encoded.Success(list) =>
              list.foldLeft(ev.unit) {
                case (acc, value) =>
                  acc.flatMap(_ =>
                    value match
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
                  )
              }
            case Encoder.Encoded.Failure(e) => ev.raiseError(new IllegalArgumentException(e.toList.mkString(", ")))

  given [A](using Encoder[A]): Conversion[A, Dynamic] with
    override def apply(value: A): Dynamic = Dynamic(value)
