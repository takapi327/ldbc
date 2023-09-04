/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.sql.{ Parameter, ParameterBinder }

/** Trait for building Statements to be added, updated, and deleted.
  *
  * @tparam F
  *   The effect type
  */
private[ldbc] trait Command[F[_]]:

  /** SQL statement string
    */
  def statement: String

  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
    * only.
    */
  def params: Seq[ParameterBinder[F]]

object Command:

  /** A model for constructing WHERE statements in MySQL.
    *
    * @param _statement
    *   SQL statement string
    * @param expressionSyntax
    *   Trait for the syntax of expressions available in MySQL.
    * @param params
    *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
    *   only.
    * @tparam F
    *   The effect type
    */
  case class Where[F[_]](
    _statement:       String,
    expressionSyntax: ExpressionSyntax[F],
    params:           Seq[ParameterBinder[F]]
  ) extends Command[F],
            LimitProvider[F]:

    override def statement: String = _statement ++ s" WHERE ${ expressionSyntax.statement }"

  /** @param _statement
    *   SQL statement string
    * @param params
    *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
    *   only.
    * @tparam F
    *   The effect type
    */
  case class Limit[F[_]](
    _statement: String,
    params:     Seq[ParameterBinder[F]]
  ) extends Command[F]:

    override def statement: String = _statement ++ " LIMIT ?"

  /** Transparent Trait to provide limit method.
    *
    * @tparam F
    *   The effect type
    */
  private[ldbc] transparent trait LimitProvider[F[_]]:
    self: Command[F] =>

    /** A method for setting the LIMIT condition in a statement.
      *
      * @param length
      *   Upper limit to be updated
      */
    def limit(length: Long): Parameter[F, Long] ?=> Limit[F] =
      Limit[F](
        _statement = statement,
        params     = params :+ ParameterBinder(length)
      )
