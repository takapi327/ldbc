/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.core.*
import ldbc.core.interpreter.{ ExtractOption, Tuples as CoreTuples }
import ldbc.sql.*
import ldbc.query.builder.interpreter.Tuples
import ldbc.query.builder.{ TableQuery, ColumnQuery }

/**
 * Trait to build a Join.
 *
 * @tparam JOINS
 *   Tuple type of TableQuery used to perform the Join.
 * @tparam SELECTS
 *   Tuple type of TableQuery used to construct Select statements, etc.
 */
trait Join[JOINS <: Tuple, SELECTS <: Tuple]:
  self =>

  /** The table on which the Join is based. */
  def main: TableQuery[?]

  /** Tuple of the table that did the join. */
  def joins: JOINS

  /** Tuple for building Select statements, etc. on joined tables. */
  def selects: SELECTS

  /** Join's Statement List. */
  def joinStatements: Seq[String]

  /** Statement of Join. */
  def statement: String = s"FROM ${ main.table._name } ${ joinStatements.mkString(" ") }"

  /**
   * A method to perform a simple Join.
   *
   * @param other
   *   [[TableQuery]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam P
   *   Base trait for all products
   */
  def join[P <: Product](other: TableQuery[P])(
    on: Tuple.Concat[JOINS, Tuple1[TableQuery[P]]] => ExpressionSyntax
  )(using
    Tuples.IsTableQueryOpt[SELECTS] =:= true
  ): Join[Tuple.Concat[JOINS, Tuple1[TableQuery[P]]], Tuple.Concat[SELECTS, Tuple1[TableQuery[P]]]] =
    val joinTable: TableQuery[P] = TableQuery(
      other.table.alias.fold(other.table.as(other.table._name))(_ => other.table)
    )
    Join(
      main,
      joins ++ Tuple(joinTable),
      selects ++ Tuple(joinTable),
      joinStatements :+ s"${ Join.JoinType.JOIN.statement } ${ other.table._name } ON ${ on(joins ++ Tuple(joinTable)).statement }"
    )

  /**
   * Method to perform Left Join.
   *
   * @param other
   *   [[TableQuery]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam P
   *   Base trait for all products
   */
  def leftJoin[P <: Product](other: TableQuery[P])(
    on: Tuple.Concat[JOINS, Tuple1[TableQuery[P]]] => ExpressionSyntax
  )(using
    Tuples.IsTableQueryOpt[SELECTS] =:= true
  ): Join[Tuple.Concat[JOINS, Tuple1[TableQuery[P]]], Tuple.Concat[SELECTS, Tuple1[TableOpt[P]]]] =
    val joinTable: TableQuery[P] = TableQuery(
      other.table.alias.fold(other.table.as(other.table._name))(_ => other.table)
    )
    Join(
      main,
      joins ++ Tuple(joinTable),
      selects ++ Tuple(TableOpt(joinTable.table)),
      joinStatements :+ s"${ Join.JoinType.LEFT_JOIN.statement } ${ other.table._name } ON ${ on(joins ++ Tuple(joinTable)).statement }"
    )

  /**
   * Method to perform Right Join.
   *
   * @param other
   *   [[TableQuery]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam P
   *   Base trait for all products
   */
  def rightJoin[P <: Product](other: TableQuery[P])(
    on: Tuple.Concat[JOINS, Tuple1[TableQuery[P]]] => ExpressionSyntax
  )(using
    Tuples.IsTableQueryOpt[SELECTS] =:= true
  ): Join[Tuple.Concat[JOINS, Tuple1[TableQuery[P]]], Tuple.Concat[Tuples.ToTableOpt[SELECTS], Tuple1[
    TableQuery[P]
  ]]] =
    val joinTable: TableQuery[P] = TableQuery(
      other.table.alias.fold(other.table.as(other.table._name))(_ => other.table)
    )
    Join(
      main,
      joins ++ Tuple(joinTable),
      Tuples.toTableOpt[SELECTS](selects) ++ Tuple(joinTable),
      joinStatements :+ s"${ Join.JoinType.RIGHT_JOIN.statement } ${ other.table._name } ON ${ on(joins ++ Tuple(joinTable)).statement }"
    )

  def select[C](func: SELECTS => C)(using Tuples.IsColumnQuery[C] =:= true): Join.JoinSelect[SELECTS, C] =
    Join.JoinSelect[SELECTS, C](
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

  private[ldbc] def apply[JOINS <: Tuple, SELECTS <: Tuple](
    _main:         TableQuery[?],
    joinQueries:   JOINS,
    selectQueries: SELECTS,
    statements:    Seq[String]
  ): Join[JOINS, SELECTS] =
    new Join[JOINS, SELECTS]:
      override def main:           TableQuery[?] = _main
      override val joins:          JOINS            = joinQueries
      override def selects:        SELECTS          = selectQueries
      override val joinStatements: Seq[String]      = statements

  private[ldbc] case class JoinSelect[SELECTS <: Tuple, T](
    selects:       SELECTS,
    fromStatement: String,
    columns:       T,
    params:        Seq[Parameter.DynamicBinder]
  ) extends Query[T],
            JoinOrderByProvider[SELECTS, T],
            LimitProvider[T]:

    private val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    override def statement: String = s"SELECT $str $fromStatement"

    def where(func: SELECTS => ExpressionSyntax): JoinWhere[SELECTS, T] =
      val expressionSyntax = func(selects)
      JoinWhere(
        selects   = selects,
        statement = statement ++ s" WHERE ${ expressionSyntax.statement }",
        columns   = columns,
        params    = expressionSyntax.parameter
      )

    def groupBy[A](func: T => Column[A]): JoinGroupBy[SELECTS, T] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).label }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinWhere[SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    Seq[Parameter.DynamicBinder]
  ) extends Query[T],
            JoinOrderByProvider[SELECTS, T],
            LimitProvider[T]:

    private def union(label: String, expressionSyntax: ExpressionSyntax): JoinWhere[SELECTS, T] =
      JoinWhere[SELECTS, T](
        selects   = selects,
        statement = statement ++ s" $label ${ expressionSyntax.statement }",
        columns   = columns,
        params    = params ++ expressionSyntax.parameter
      )

    def and(func: SELECTS => ExpressionSyntax): JoinWhere[SELECTS, T] =
      union("AND", func(selects))
    def or(func: SELECTS => ExpressionSyntax): JoinWhere[SELECTS, T] =
      union("OR", func(selects))
    def ||(func: SELECTS => ExpressionSyntax): JoinWhere[SELECTS, T] =
      union("||", func(selects))
    def xor(func: SELECTS => ExpressionSyntax): JoinWhere[SELECTS, T] =
      union("XOR", func(selects))
    def &&(func: SELECTS => ExpressionSyntax): JoinWhere[SELECTS, T] =
      union("&&", func(selects))

    def groupBy[A](func: T => Column[A]): JoinGroupBy[SELECTS, T] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).label }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinOrderBy[T](
    statement: String,
    columns:   T,
    params:    Seq[Parameter.DynamicBinder]
  ) extends Query[T],
            LimitProvider[T]

  private[ldbc] transparent trait JoinOrderByProvider[SELECTS <: Tuple, T]:
    self: Query[T] =>

    def selects: SELECTS

    def orderBy[A <: OrderBy.Order | OrderBy.Order *: NonEmptyTuple | Column[?]](
      func: SELECTS => A
    ): JoinOrderBy[T] =
      val order = func(selects) match
        case v: Tuple         => v.toList.mkString(", ")
        case v: OrderBy.Order => v.statement
        case v: Column[?]     => v.alias.fold(v.label)(name => s"$name.${ v.label }")
      JoinOrderBy(
        statement = self.statement ++ s" ORDER BY $order",
        columns   = self.columns,
        params    = self.params
      )

  private[ldbc] case class JoinHaving[SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    Seq[Parameter.DynamicBinder]
  ) extends Query[T],
            JoinOrderByProvider[SELECTS, T],
            LimitProvider[T]

  private[ldbc] case class JoinGroupBy[SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    Seq[Parameter.DynamicBinder]
  ) extends Query[T],
            JoinOrderByProvider[SELECTS, T],
            LimitProvider[T]:

    def having[A](func: T => ExpressionSyntax): JoinHaving[SELECTS, T] =
      val expressionSyntax = func(columns)
      JoinHaving(
        selects   = selects,
        statement = statement ++ s" HAVING ${ expressionSyntax.statement }",
        columns   = columns,
        params    = params ++ expressionSyntax.parameter
      )

case class TableOpt[P <: Product](table: Table[P]) extends Dynamic:

  transparent inline def selectDynamic[Tag <: Singleton](tag: Tag)(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): ColumnQuery[Option[
    ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
  ]] =
    val column = table.selectDynamic[Tag](tag)
    ColumnQuery[Option[
      ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
    ]](
      _label      = column.label,
      _dataType   = column.dataType.toOption,
      _attributes = Seq.empty,
      _alias      = column.alias
    )
