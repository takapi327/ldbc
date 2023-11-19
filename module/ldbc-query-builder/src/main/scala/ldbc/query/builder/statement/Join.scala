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

/** Trait to build a Join.
  *
  * @tparam F
  *   The effect type
  * @tparam JOINS
  *   Tuple type of TableQuery used to perform the Join.
  * @tparam SELECTS
  *   Tuple type of TableQuery used to construct Select statements, etc.
  */
trait Join[F[_], JOINS <: Tuple, SELECTS <: Tuple]:
  self =>

  /** The table on which the Join is based. */
  def main: TableQuery[F, ?]

  /** Tuple of the table that did the join. */
  def joins: JOINS

  /** Tuple for building Select statements, etc. on joined tables. */
  def selects: SELECTS

  /** Join's Statement List. */
  def joinStatements: Seq[String]

  /** Statement of Join. */
  def statement: String = s"FROM ${ main.table._name } ${ joinStatements.mkString(" ") }"

  /** A method to perform a simple Join.
    *
    * @param other
    *   [[TableQuery]] to do a Join.
    * @param on
    *   Comparison function that performs a Join.
    * @tparam P
    *   Base trait for all products
    */
  def join[P <: Product](other: TableQuery[F, P])(
    on:                         Tuple.Concat[JOINS, Tuple1[TableQuery[F, P]]] => ExpressionSyntax[F]
  ): Join[F, Tuple.Concat[JOINS, Tuple1[TableQuery[F, P]]], Tuple.Concat[SELECTS, Tuple1[TableQuery[F, P]]]] =
    val joinTable: TableQuery[F, P] = TableQuery(
      other.table.alias.fold(other.table.as(other.table._name))(_ => other.table)
    )
    Join(
      main,
      joins ++ Tuple(joinTable),
      selects ++ Tuple(joinTable),
      joinStatements :+ s"${ Join.JoinType.JOIN.statement } ${ other.table._name } ON ${ on(joins ++ Tuple(joinTable)).statement }"
    )

  /** Method to perform Left Join.
    *
    * @param other
    *   [[TableQuery]] to do a Join.
    * @param on
    *   Comparison function that performs a Join.
    * @tparam P
    *   Base trait for all products
    */
  def leftJoin[P <: Product](other: TableQuery[F, P])(
    on:                             Tuple.Concat[JOINS, Tuple1[TableQuery[F, P]]] => ExpressionSyntax[F]
  ): Join[F, Tuple.Concat[JOINS, Tuple1[TableQuery[F, P]]], Tuple.Concat[SELECTS, Tuple1[TableOpt[F, P]]]] =
    val joinTable: TableQuery[F, P] = TableQuery(
      other.table.alias.fold(other.table.as(other.table._name))(_ => other.table)
    )
    Join(
      main,
      joins ++ Tuple(joinTable),
      selects ++ Tuple(TableOpt(joinTable.table)),
      joinStatements :+ s"${ Join.JoinType.LEFT_JOIN.statement } ${ other.table._name } ON ${ on(joins ++ Tuple(joinTable)).statement }"
    )

  /** Method to perform Right Join.
    *
    * @param other
    *   [[TableQuery]] to do a Join.
    * @param on
    *   Comparison function that performs a Join.
    * @tparam P
    *   Base trait for all products
    */
  def rightJoin[P <: Product](other: TableQuery[F, P])(
    on:                              Tuple.Concat[JOINS, Tuple1[TableQuery[F, P]]] => ExpressionSyntax[F]
  ): Join[F, Tuple.Concat[JOINS, Tuple1[TableQuery[F, P]]], Tuple.Concat[SELECTS, Tuple1[TableQuery[F, P]]]] =
    val joinTable: TableQuery[F, P] = TableQuery(
      other.table.alias.fold(other.table.as(other.table._name))(_ => other.table)
    )
    Join(
      main,
      joins ++ Tuple(joinTable),
      selects ++ Tuple(joinTable),
      joinStatements :+ s"${ Join.JoinType.LEFT_JOIN.statement } ${ other.table._name } ON ${ on(joins ++ Tuple(joinTable)).statement }"
    )

  def select[C](func: SELECTS => C)(using Tuples.IsColumnQuery[F, C] =:= true): Join.JoinSelect[F, SELECTS, C] =
    Join.JoinSelect[F, SELECTS, C](
      selects       = selects,
      fromStatement = statement,
      columns       = func(selects),
      params        = Nil
    )

object Join:

  enum JoinType(val statement: String):
    case JOIN       extends JoinType("JOIN")
    case LEFT_JOIN  extends JoinType("LEFT JOIN")
    case RIGHT_JOIN extends JoinType("RIGHT JOIN")

  def apply[F[_], JOINS <: Tuple, SELECTS <: Tuple](
    _main:         TableQuery[F, ?],
    joinQueries:   JOINS,
    selectQueries: SELECTS,
    statements:    Seq[String]
  ): Join[F, JOINS, SELECTS] =
    new Join[F, JOINS, SELECTS]:
      override def main:           TableQuery[F, ?] = _main
      override val joins:          JOINS            = joinQueries
      override def selects:        SELECTS          = selectQueries
      override val joinStatements: Seq[String]      = statements

  case class JoinSelect[F[_], SELECTS <: Tuple, T](
    selects:       SELECTS,
    fromStatement: String,
    columns:       T,
    params:        Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            JoinOrderByProvider[F, SELECTS, T],
            LimitProvider[F, T]:

    private val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    override def statement: String = s"SELECT $str $fromStatement"

    def where(func: SELECTS => ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      val expressionSyntax = func(selects)
      JoinWhere(
        selects   = selects,
        statement = statement ++ s" WHERE ${ expressionSyntax.statement }",
        columns   = columns,
        params    = expressionSyntax.parameter
      )

    def groupBy[A](func: T => Column[A]): JoinGroupBy[F, SELECTS, T] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).label }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinWhere[F[_], SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            JoinOrderByProvider[F, SELECTS, T],
            LimitProvider[F, T]:

    private def union(label: String, expressionSyntax: ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      JoinWhere[F, SELECTS, T](
        selects   = selects,
        statement = statement ++ s" $label ${ expressionSyntax.statement }",
        columns   = columns,
        params    = params ++ expressionSyntax.parameter
      )

    def and(func: SELECTS => ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      union("AND", func(selects))
    def or(func: SELECTS => ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      union("OR", func(selects))
    def ||(func: SELECTS => ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      union("||", func(selects))
    def xor(func: SELECTS => ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      union("XOR", func(selects))
    def &&(func: SELECTS => ExpressionSyntax[F]): JoinWhere[F, SELECTS, T] =
      union("&&", func(selects))

    def groupBy[A](func: T => Column[A]): JoinGroupBy[F, SELECTS, T] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).label }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinOrderBy[F[_], T](
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            LimitProvider[F, T]

  private[ldbc] transparent trait JoinOrderByProvider[F[_], SELECTS <: Tuple, T]:
    self: Query[F, T] =>

    def selects: SELECTS

    def orderBy[A <: OrderBy.Order | OrderBy.Order *: NonEmptyTuple | Column[?]](
      func: SELECTS => A
    ): JoinOrderBy[F, T] =
      val order = func(selects) match
        case v: Tuple         => v.toList.mkString(", ")
        case v: OrderBy.Order => v.statement
        case v: Column[?]     => v.alias.fold(v.label)(name => s"$name.${ v.label }")
      JoinOrderBy(
        statement = self.statement ++ s" ORDER BY $order",
        columns   = self.columns,
        params    = self.params
      )

  private[ldbc] case class JoinHaving[F[_], SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            JoinOrderByProvider[F, SELECTS, T],
            LimitProvider[F, T]

  private[ldbc] case class JoinGroupBy[F[_], SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    Seq[ParameterBinder[F]]
  ) extends Query[F, T],
            JoinOrderByProvider[F, SELECTS, T],
            LimitProvider[F, T]:

    def having[A](func: T => ExpressionSyntax[F]): JoinHaving[F, SELECTS, T] =
      val expressionSyntax = func(columns)
      JoinHaving(
        selects   = selects,
        statement = statement ++ s" HAVING ${ expressionSyntax.statement }",
        columns   = columns,
        params    = params ++ expressionSyntax.parameter
      )

case class TableOpt[F[_], P <: Product](table: Table[P]) extends Dynamic:

  transparent inline def selectDynamic[Tag <: Singleton](tag: Tag)(using
    mirror:                                                   Mirror.ProductOf[P],
    index: ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    reader: ResultSetReader[F, Option[
      Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ]]
  ): ColumnQuery[F, Option[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]] =
    val column = table.selectDynamic[Tag](tag)
    ColumnQuery[F, Option[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]](
      _label      = column.label,
      _dataType   = column.dataType.toOption,
      _attributes = Seq.empty,
      _alias      = column.alias,
      _reader     = reader
    )
