/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * Trait to allow values to be set in PreparedStatement with only index by generating them from Parameter.
 *
 * @tparam F
 *   The effect type
 */
trait ParameterBinder:

  /** Query parameters to be plugged into the Statement. */
  def parameter: Any

  /**
   * Methods for setting Scala and Java values to the specified position in PreparedStatement.
   *
   * @param statement
   *   An object that represents a precompiled SQL statement.
   * @param index
   *   the first parameter is 1, the second is 2, ...
   */
  def bind[F[_]](statement: PreparedStatement[F], index: Int): F[Unit]

object ParameterBinder:

  trait Static extends ParameterBinder:
    def value:              String
    override def parameter: Any = value

  def apply[T](value: T)(using param: Parameter[T]): ParameterBinder =
    new ParameterBinder:
      override def parameter: Any = value
      override def bind[F[_]](statement: PreparedStatement[F], index: Int): F[Unit] =
        param.bind(statement, index, value)

  given [T](using Parameter[T]): Conversion[T, ParameterBinder] with
    override def apply(x: T): ParameterBinder = ParameterBinder[T](x)
