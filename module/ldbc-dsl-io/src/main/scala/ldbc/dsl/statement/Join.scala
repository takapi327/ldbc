/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.statement

import ldbc.core.*
import ldbc.dsl.ParameterBinder

/**
 * A model for constructing JOIN statements in MySQL.
 *
 * @param left
 * The left-hand column where the join join will be performed.
 * @param right
 * The right-hand column where the join join will be performed.
 * @tparam F
 * The effect type
 * @tparam P1
 * Base trait for all products
 * @tparam P2
 * Base trait for all products
 */
case class Join[F[_], P1 <: Product, P2 <: Product](left: Table[P1], right: Table[P2]):
  def on(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): Join.On[F, P1, P2] =
    Join.On[F, P1, P2](left, right, func(left, right))

object Join:

  private[ldbc] case class On[F[_], P1 <: Product, P2 <: Product](left: Table[P1], right: Table[P2], expression: ExpressionSyntax[F]):
    def select[T <: Tuple](func: (Table[P1], Table[P2]) => T): JoinSelect[F, P1, P2, T] =
      val leftTableName = left.alias.fold(left._name)(name => s"${ left._name } AS $name")
      val rightTableName = right.alias.fold(left._name)(name => s"${ right._name } AS $name")
      val columns = func(left, right)
      JoinSelect[F, P1, P2, T](
        left = left,
        right = right,
        statement = s"SELECT ${ columns.toArray.mkString(", ") } FROM $leftTableName JOIN $rightTableName ON ${ expression.statement }",
        columns = columns,
        params = expression.parameter
      )

  private[ldbc] case class JoinSelect[F[_], P1 <: Product, P2 <: Product, T <: Tuple](
    left: Table[P1],
    right: Table[P2],
    statement: String,
    columns: T,
    params: Seq[ParameterBinder[F]]
  ) extends Query[F, T]:

    def where(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      val expressionSyntax = func(left, right)
      JoinWhere(
        statement = statement ++ s" WHERE ${ expressionSyntax.statement }",
        columns = columns,
        params = expressionSyntax.parameter
      )

  private[ldbc] case class JoinWhere[F[_], P1 <: Product, P2 <: Product, T <: Tuple](
    statement: String,
    columns: T,
    params: Seq[ParameterBinder[F]]
  ) extends Query[F, T]
