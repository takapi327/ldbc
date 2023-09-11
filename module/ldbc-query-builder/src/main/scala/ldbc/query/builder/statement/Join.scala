/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.core.*
import ldbc.core.interpreter.Tuples as CoreTuples
import ldbc.sql.*
import ldbc.query.builder.interpreter.Tuples
import ldbc.query.builder.{ TableQuery, ColumnQuery }

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
class Join[F[_], P1 <: Product, P2 <: Product](
  left:  TableQuery[F, P1],
  right: TableQuery[F, P2]
):
  def on(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): Join.On[F, P1, P2] =
    Join.On[F, P1, P2](left, right, func(left, right))

  def left(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): Join.LeftOn[F, P1, P2] =
    Join.LeftOn[F, P1, P2](left, right, func(left, right))

  def right(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): Join.RightOn[F, P1, P2] =
    Join.RightOn[F, P1, P2](left, right, func(left, right))

object Join:

  enum JoinType(val statement: String):
    case JOIN       extends JoinType("JOIN")
    case LEFT_JOIN  extends JoinType("LEFT JOIN")
    case RIGHT_JOIN extends JoinType("RIGHT JOIN")

  private[ldbc] transparent trait JoinOn[F[_], P1 <: Product, P2 <: Product]:
    def left:       TableQuery[F, P1]
    def right:      TableQuery[F, P2]
    def expression: ExpressionSyntax[F]

    def joinType: JoinType

    //private val leftTableName  = left.table.alias.fold(left.table._name)(name => s"${ left.table._name } AS $name")
    //private val rightTableName = right.table.alias.fold(left.table._name)(name => s"${ right.table._name } AS $name")
    private val leftTableName  = s"${left.table._name} AS ${left.alias.alias.getOrElse("")}"
    private val rightTableName = s"${right.table._name} AS ${right.alias.alias.getOrElse("")}"

    protected val fromStatement =
      s"FROM $leftTableName ${ joinType.statement } $rightTableName ON ${ expression.statement }"

  private[ldbc] case class On[F[_], P1 <: Product, P2 <: Product](
                                                                   left: TableQuery[F, P1],
                                                                   right: TableQuery[F, P2],
    expression: ExpressionSyntax[F]
  ) extends JoinOn[F, P1, P2]:

    override def joinType: JoinType = JoinType.JOIN

    def select[T <: Tuple](
      func: (TableQuery[F, P1], TableQuery[F, P2]) => Tuples.ToColumn[F, T]
    ): JoinSelect[F, P1, P2, Tuples.ToColumn[F, T]] =
      val columns   = func(left, right)
      val statement = s"SELECT ${ columns.toArray.mkString(", ") } $fromStatement"
      JoinSelect[F, P1, P2, Tuples.ToColumn[F, T]](
        left      = left,
        right     = right,
        statement = statement,
        columns   = columns,
        params    = expression.parameter
      )

  private[ldbc] case class LeftOn[F[_], P1 <: Product, P2 <: Product](
                                                                       left: TableQuery[F, P1],
                                                                       right: TableQuery[F, P2],
    expression: ExpressionSyntax[F]
  ) extends JoinOn[F, P1, P2]:

    override def joinType: JoinType = JoinType.LEFT_JOIN

    def select[T <: Tuple](
      func: (TableQuery[F, P1], TableOpt[F, P2]) => Tuples.ToColumn[F, T]
    ): Join.JoinSelect[F, P1, P2, Tuples.ToColumn[F, T]] =
      val columns   = func(left, TableOpt(right.table))
      val statement = s"SELECT ${ columns.toArray.mkString(", ") } $fromStatement"
      Join.JoinSelect[F, P1, P2, Tuples.ToColumn[F, T]](
        left      = left,
        right     = right,
        statement = statement,
        columns   = columns,
        params    = expression.parameter
      )

  private[ldbc] case class RightOn[F[_], P1 <: Product, P2 <: Product](
                                                                        left: TableQuery[F, P1],
                                                                        right: TableQuery[F, P2],
    expression: ExpressionSyntax[F]
  ) extends JoinOn[F, P1, P2]:

    override def joinType: JoinType = JoinType.RIGHT_JOIN

    def select[T <: Tuple](
      func: (TableOpt[F, P1], TableQuery[F, P2]) => Tuples.ToColumn[F, T]
    ): Join.JoinSelect[F, P1, P2, Tuples.ToColumn[F, T]] =
      val columns   = func(TableOpt(left.table), right)
      val statement = s"SELECT ${ columns.toArray.mkString(", ") } $fromStatement"
      Join.JoinSelect[F, P1, P2, Tuples.ToColumn[F, T]](
        left      = left,
        right     = right,
        statement = statement,
        columns   = columns,
        params    = expression.parameter
      )

  private[ldbc] case class JoinSelect[F[_], P1 <: Product, P2 <: Product, T <: Tuple](
                                                                                       left: TableQuery[F, P1],
                                                                                       right: TableQuery[F, P2],
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            JoinOrderByProvider[F, P1, P2, T],
            LimitProvider[F, T]:

    def where(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
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
                                                                                      left: TableQuery[F, P1],
                                                                                      right: TableQuery[F, P2],
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

    def and(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("AND", func(left, right))
    def or(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("OR", func(left, right))
    def ||(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("||", func(left, right))
    def xor(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
      union("XOR", func(left, right))
    def &&(func: (TableQuery[F, P1], TableQuery[F, P2]) => ExpressionSyntax[F]): JoinWhere[F, P1, P2, T] =
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
                                                                               left: TableQuery[F, P1],
                                                                               right: TableQuery[F, P2],
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            LimitProvider[F, T]

  private[ldbc] transparent trait JoinOrderByProvider[F[_], P1 <: Product, P2 <: Product, T]:
    self: Query[F, T] =>

    def left:  TableQuery[F, P1]
    def right: TableQuery[F, P2]

    def orderBy[A <: OrderBy.Order | OrderBy.Order *: NonEmptyTuple | Column[?]](
      func: (TableQuery[F, P1], TableQuery[F, P2]) => A
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
                                                                              left: TableQuery[F, P1],
                                                                              right: TableQuery[F, P2],
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            JoinOrderByProvider[F, P1, P2, T],
            LimitProvider[F, T]

  private[ldbc] case class JoinGroupBy[F[_], P1 <: Product, P2 <: Product, T](
                                                                               left: TableQuery[F, P1],
                                                                               right: TableQuery[F, P2],
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

case class TableOpt[F[_], P <: Product](table: Table[P]) extends Dynamic:

  def selectDynamic[Tag <: Singleton](tag: Tag)(using
    mirror:                                Mirror.ProductOf[P],
    index:                                 ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    reader: ResultSetReader[F, Option[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
  ): ColumnQuery[F, Option[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]] =
    val column = table.selectDynamic[Tag](tag)
    ColumnQuery[F, Option[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]](
      label = column.label,
      dataType = column.dataType.toOption,
      attributes = Seq.empty,
      _alias = column.alias,
      reader = reader
    )
