/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.sql.{ Table, ParameterBinder }

/** A model for constructing UPDATE statements in MySQL.
  *
  * @param table
  *   Trait for generating SQL table information.
  * @tparam F
  *   The effect type
  * @tparam P
  *   Base trait for all products
  */
case class Delete[F[_], P <: Product](
  table: Table[P]
) extends Command[F],
          Command.LimitProvider[F]:

  override def params: Seq[ParameterBinder[F]] = Seq.empty

  override def statement: String = s"DELETE ${ table._name }"

  /** A method for setting the WHERE condition in a DELETE statement.
   *
   * @param func
   * Function to construct an expression using the columns that Table has.
   */
  def where(func: Table[P] => ExpressionSyntax[F]): Command.Where[F] =
    val expressionSyntax = func(table)
    Command.Where[F](
      _statement = statement,
      expressionSyntax = expressionSyntax,
      params = params ++ expressionSyntax.parameter
    )
