/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import ldbc.sql.PreparedStatement

/** Trait to allow values to be set in PreparedStatement with only index by generating them from Parameter.
  *
  * @tparam F
  *   The effect type
  */
trait ParameterBinder[F[_]]:

  /** Methods for setting Scala and Java values to the specified position in PreparedStatement.
    *
    * @param statement
    *   An object that represents a precompiled SQL statement.
    * @param index
    *   the first parameter is 1, the second is 2, ...
    */
  def bind(statement: PreparedStatement[F], index: Int): F[Unit]

object ParameterBinder:

  def apply[F[_], T](value: T)(using param: Parameter[F, T]): ParameterBinder[F] =
    new ParameterBinder[F]:
      override def bind(statement: PreparedStatement[F], index: Int): F[Unit] =
        param.bind(statement, index, value)

  given [F[_], T](using Parameter[F, T]): Conversion[T, ParameterBinder[F]] with
    override def apply(x: T): ParameterBinder[F] = ParameterBinder[F, T](x)
