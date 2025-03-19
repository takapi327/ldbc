/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import java.time.*

import cats.syntax.all.*
import cats.MonadThrow

import ldbc.sql.PreparedStatement

import ldbc.dsl.codec.Encoder
import ldbc.dsl.syntax.*

/**
 * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
 * might look something like this.
 *
 * example:
 * {{{
 *   import ldbc.dsl.*
 * }}}
 */
package object dsl extends HelperFunctionsSyntax:

  private[ldbc] trait ParamBinder:
    protected def paramBind[F[_]: MonadThrow](
      prepareStatement: PreparedStatement[F],
      params:           List[Parameter.Dynamic]
    ): F[Unit] =
      val encoded = params.foldLeft(MonadThrow[F].pure(List.empty[Encoder.Supported])) {
        case (acc, param) =>
          for
            acc$ <- acc
            value <- param match
                       case Parameter.Dynamic.Success(value) => MonadThrow[F].pure(value)
                       case Parameter.Dynamic.Failure(errors) =>
                         MonadThrow[F].raiseError(new IllegalArgumentException(errors.mkString(", ")))
          yield acc$ :+ value
      }
      encoded.flatMap(_.zipWithIndex.foldLeft(MonadThrow[F].unit) {
        case (acc, (value, index)) =>
          acc *> (value match
            case value: Boolean       => prepareStatement.setBoolean(index + 1, value)
            case value: Byte          => prepareStatement.setByte(index + 1, value)
            case value: Short         => prepareStatement.setShort(index + 1, value)
            case value: Int           => prepareStatement.setInt(index + 1, value)
            case value: Long          => prepareStatement.setLong(index + 1, value)
            case value: Float         => prepareStatement.setFloat(index + 1, value)
            case value: Double        => prepareStatement.setDouble(index + 1, value)
            case value: BigDecimal    => prepareStatement.setBigDecimal(index + 1, value)
            case value: String        => prepareStatement.setString(index + 1, value)
            case value: Array[Byte]   => prepareStatement.setBytes(index + 1, value)
            case value: LocalDate     => prepareStatement.setDate(index + 1, value)
            case value: LocalTime     => prepareStatement.setTime(index + 1, value)
            case value: LocalDateTime => prepareStatement.setTimestamp(index + 1, value)
            case None                 => prepareStatement.setNull(index + 1, ldbc.sql.Types.NULL))
      })
