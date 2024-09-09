/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import java.time.*

import ldbc.sql.PreparedStatement
import ldbc.dsl.codec.Encoder

/**
 * Trait for setting Scala and Java values to PreparedStatement.
 */
trait Parameter:

  /** Query parameters to be plugged into the Statement. */
  def parameter: String

object Parameter:

  case class Static(parameter: String) extends Parameter:
    override def toString: String = parameter

  trait Dynamic extends Parameter:

    /**
     * Methods for setting Scala and Java values to the specified position in PreparedStatement.
     *
     * @param statement
     *   An object that represents a precompiled SQL statement.
     * @param index
     *   the parameter value
     */
    def bind[F[_]](statement: PreparedStatement[F], index: Int): F[Unit]

  object Dynamic:

    def apply[A](value: A)(using encoder: Encoder[A]): Dynamic =
      new Dynamic:
        override def parameter: String = value.toString
        override def bind[F[_]](statement: PreparedStatement[F], index: Int): F[Unit] =
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

  given [A](using Encoder[A]): Conversion[A, Dynamic] with
    override def apply(value: A): Dynamic = Dynamic(value)
