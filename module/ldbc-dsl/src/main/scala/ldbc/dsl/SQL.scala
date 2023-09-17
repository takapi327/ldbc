/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import scala.annotation.targetName

import ldbc.sql.ParameterBinder

/** A model with a query string and parameters to be bound to the query string that is executed by PreparedStatement,
  * etc.
  *
  * @param statement
  *   an SQL statement that may contain one or more '?' IN parameter placeholders
  * @param params
  *   statement has '?' that the statement has.
  * @tparam F
  *   The effect type
  */
case class SQL[F[_]](statement: String, params: Seq[ParameterBinder[F]]):

  @targetName("combine")
  def ++(sql: SQL[F]): SQL[F] =
    SQL[F](statement ++ " " ++ sql.statement, params ++ sql.params)
