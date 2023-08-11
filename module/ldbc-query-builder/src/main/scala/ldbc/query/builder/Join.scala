/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.query.builder

import ldbc.core.*
import ldbc.sql.ParameterBinder

/** A model for constructing JOIN statements in MySQL.
 *
 * @param left
 *   The left-hand column where the join join will be performed.
 * @param right
 *   The right-hand column where the join join will be performed.
 * @tparam F
 *   The effect type
 * @tparam P1
 *   Base trait for all products
 * @tparam P2
 *   Base trait for all products
 */
case class Join[F[_], P1 <: Product, P2 <: Product](left: Table[P1], right: Table[P2]):
  def on(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): Join.On[F, P1, P2] =
    Join.On[F, P1, P2](left, right, func(left, right))

object Join:

  private[ldbc] case class On[F[_], P1 <: Product, P2 <: Product](
                                                                   left:       Table[P1],
                                                                   right:      Table[P2],
                                                                   expression: ExpressionSyntax[F]
                                                                 ):
    def select[T <: Tuple](func: (Table[P1], Table[P2]) => Tuple.Map[T, Column]): JoinSelect[F, P1, P2, Tuple.Map[T, Column]] =
      val leftTableName  = left.alias.fold(left._name)(name => s"${ left._name } AS $name")
      val rightTableName = right.alias.fold(left._name)(name => s"${ right._name } AS $name")
      val columns        = func(left, right)
      JoinSelect[F, P1, P2, Tuple.Map[T, Column]](
        left  = left,
        right = right,
        statement =
          s"SELECT ${ columns.toArray.mkString(", ") } FROM $leftTableName JOIN $rightTableName ON ${ expression.statement }",
        columns = columns,
        params  = expression.parameter
      )

  private[ldbc] case class JoinSelect[F[_], P1 <: Product, P2 <: Product, T <: Tuple](
                                                                                       left:      Table[P1],
                                                                                       right:     Table[P2],
                                                                                       statement: String,
                                                                                       columns:   T,
                                                                                       params:    Seq[ParameterBinder[F]]
                                                                                     ) extends Query[F, T],
    JoinOrderByProvider[F, P1, P2, T],
    LimitProvider[F, T]:

    def where(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      val expressionSyntax = func(left, right)
      JoinWhere(
        left      = left,
        right     = right,
        statement = statement ++ s" WHERE ${ expressionSyntax.statement }",
        columns   = columns,
        params    = expressionSyntax.parameter
      )

    def groupBy[A](func: T => Column[A]): JoinGroupBy[F, P1, P2, T] =
      JoinGroupBy(
        left      = left,
        right     = right,
        statement = statement ++ s" GROUP BY ${ func(columns).label }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinWhere[F[_], P1 <: Product, P2 <: Product, T <: Tuple](
                                                                                      left:      Table[P1],
                                                                                      right:     Table[P2],
                                                                                      statement: String,
                                                                                      columns:   T,
                                                                                      params:    Seq[ParameterBinder[F]]
                                                                                    ) extends Query[F, T],
    JoinOrderByProvider[F, P1, P2, T],
    LimitProvider[F, T]:

    private def union(label: String, expressionSyntax: ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      JoinWhere[F, P1, P2, T](
        left      = left,
        right     = right,
        statement = statement ++ s" $label ${ expressionSyntax.statement }",
        columns   = columns,
        params    = params ++ expressionSyntax.parameter
      )

    def and(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("AND", func(left, right))
    def or(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("OR", func(left, right))
    def ||(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("||", func(left, right))
    def xor(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("XOR", func(left, right))
    def &&(func: (Table[P1], Table[P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("&&", func(left, right))

    def groupBy[A](func: T => Column[A]): JoinGroupBy[F, P1, P2, T] =
      JoinGroupBy(
        left      = left,
        right     = right,
        statement = statement ++ s" GROUP BY ${ func(columns).label }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinOrderBy[F[_], P1 <: Product, P2 <: Product, T](
                                                                               left:      Table[P1],
                                                                               right:     Table[P2],
                                                                               statement: String,
                                                                               columns:   T,
                                                                               params:    Seq[ParameterBinder[F]]
                                                                             ) extends Query[F, T],
    LimitProvider[F, T]

  private[ldbc] transparent trait JoinOrderByProvider[F[_], P1 <: Product, P2 <: Product, T]:
    self: Query[F, T] =>

    def left:  Table[P1]
    def right: Table[P2]

    def orderBy[A <: OrderBy.Order | OrderBy.Order *: NonEmptyTuple | Column[?]](
                                                                                  func: (Table[P1], Table[P2]) => A
                                                                                ): JoinOrderBy[F, P1, P2, T] =
      val order = func(left, right) match
        case v: Tuple         => v.toList.mkString(", ")
        case v: OrderBy.Order => v.statement
        case v: Column[?]     => v.alias.fold(v.label)(name => s"$name.${ v.label }")
      JoinOrderBy(
        left      = left,
        right     = right,
        statement = self.statement ++ s" ORDER BY $order",
        columns   = self.columns,
        params    = self.params
      )

  private[ldbc] case class JoinHaving[F[_], P1 <: Product, P2 <: Product, T](
                                                                              left:      Table[P1],
                                                                              right:     Table[P2],
                                                                              statement: String,
                                                                              columns:   T,
                                                                              params:    Seq[ParameterBinder[F]]
                                                                            ) extends Query[F, T],
    JoinOrderByProvider[F, P1, P2, T],
    LimitProvider[F, T]

  private[ldbc] case class JoinGroupBy[F[_], P1 <: Product, P2 <: Product, T <: Tuple](
                                                                                        left:      Table[P1],
                                                                                        right:     Table[P2],
                                                                                        statement: String,
                                                                                        columns:   T,
                                                                                        params:    Seq[ParameterBinder[F]]
                                                                                      ) extends Query[F, T],
    JoinOrderByProvider[F, P1, P2, T],
    LimitProvider[F, T]:

    def having[A](func: T => ExpressionSyntax[F]): JoinHaving[F, P1, P2, T] =
      val expressionSyntax = func(columns)
      JoinHaving(
        left      = left,
        right     = right,
        statement = statement ++ s" HAVING ${ expressionSyntax.statement }",
        columns   = columns,
        params    = params ++ expressionSyntax.parameter
      )
