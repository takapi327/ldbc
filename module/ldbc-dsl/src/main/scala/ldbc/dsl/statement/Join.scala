/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.statement

import scala.annotation.targetName

import ldbc.sql.Parameter

import ldbc.dsl.*
import ldbc.dsl.interpreter.*

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
  def main: Table[?]

  /** Tuple of the table that did the join. */
  def joins: JOINS

  /** Tuple for building Select statements, etc. on joined tables. */
  def selects: SELECTS

  /** Join's Statement List. */
  def joinStatements: List[String]

  /** Statement of Join. */
  def statement: String = s"FROM ${ main.label } ${ joinStatements.mkString(" ") }"

  /**
   * A method to perform a simple Join.
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam P
   *   Base trait for all products
   */
  def join[P <: Product](other: Table[P])(
    on: Tuple.Concat[JOINS, Tuple1[Table[P]]] => Expression
  )(using
    Tuples.IsTableOpt[SELECTS] =:= true
  ): Join[Tuple.Concat[JOINS, Tuple1[Table[P]]], Tuple.Concat[SELECTS, Tuple1[Table[P]]]] =
    Join.Impl(
      main,
      joins ++ Tuple(other),
      selects ++ Tuple(other),
      joinStatements :+ s"${ Join.JoinType.JOIN.statement } ${ other.label } ON ${ on(joins ++ Tuple(other)).statement }"
    )

  /**
   * Method to perform Left Join.
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam P
   *   Base trait for all products
   */
  def leftJoin[P <: Product](other: Table[P])(
    on: Tuple.Concat[JOINS, Tuple1[Table[P]]] => Expression
  )(using
    Tuples.IsTableOpt[SELECTS] =:= true
  ): Join[Tuple.Concat[JOINS, Tuple1[Table[P]]], Tuple.Concat[SELECTS, Tuple1[Table.Opt[P]]]] =
    Join.Impl(
      main,
      joins ++ Tuple(other),
      selects ++ Tuple(Table.Opt(other.*)),
      joinStatements :+ s"${ Join.JoinType.LEFT_JOIN.statement } ${ other.label } ON ${ on(joins ++ Tuple(other)).statement }"
    )

  /**
   * Method to perform Right Join.
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam P
   *   Base trait for all products
   */
  def rightJoin[P <: Product](other: Table[P])(
    on: Tuple.Concat[JOINS, Tuple1[Table[P]]] => Expression
  )(using
    Tuples.IsTableOpt[SELECTS] =:= true
  ): Join[Tuple.Concat[JOINS, Tuple1[Table[P]]], Tuple.Concat[Tuples.ToTableOpt[SELECTS], Tuple1[Table[P]]]] =
    Join.Impl(
      main,
      joins ++ Tuple(other),
      Tuples.toTableOpt[SELECTS](selects) ++ Tuple(other),
      joinStatements :+ s"${ Join.JoinType.RIGHT_JOIN.statement } ${ other.label } ON ${ on(joins ++ Tuple(other)).statement }"
    )

  def select[C](func: SELECTS => C)(using Tuples.IsColumn[C] =:= true): Join.JoinSelect[SELECTS, C] =
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

  private[ldbc] case class Impl[JOINS <: Tuple, SELECTS <: Tuple](
    main:           Table[?],
    joins:          JOINS,
    selects:        SELECTS,
    joinStatements: List[String]
  ) extends Join[JOINS, SELECTS]

  private[ldbc] case class JoinOrderBy[T](
    statement: String,
    params:    List[Parameter.DynamicBinder]
  ) extends Query.Provider[T],
            LimitProvider[T]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinOrderBy(statement ++ sql.statement, params ++ sql.params)

  private[ldbc] transparent trait JoinOrderByProvider[SELECTS <: Tuple]:
    self: SQL =>

    def selects: SELECTS

    def orderBy[A <: OrderBy.Order[?] | OrderBy.Order[?] *: NonEmptyTuple | Column[?]](
      func: SELECTS => A
    ): JoinOrderBy[SELECTS] =
      val order = func(selects) match
        case v: Tuple            => v.toList.mkString(", ")
        case v: OrderBy.Order[?] => v.statement
        case v: Column[?]        => v.alias.fold(v.name)(as => s"$as.${ v.name }")
      JoinOrderBy(
        statement = self.statement ++ s" ORDER BY $order",
        params    = self.params
      )

  private[ldbc] case class JoinHaving[SELECTS <: Tuple](
    selects:   SELECTS,
    statement: String,
    params:    List[Parameter.DynamicBinder]
  ) extends Query.Provider[SELECTS],
            JoinOrderByProvider[SELECTS],
            LimitProvider[SELECTS]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinHaving(selects, statement ++ sql.statement, params ++ sql.params)

  private[ldbc] case class JoinGroupBy[SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    List[Parameter.DynamicBinder]
  ) extends Query.Provider[T],
            JoinOrderByProvider[SELECTS],
            LimitProvider[T]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ sql.statement,
        columns   = columns,
        params    = params ++ sql.params
      )

    def having[A](func: T => Expression): JoinHaving[SELECTS] =
      val expression = func(columns)
      JoinHaving(
        selects   = selects,
        statement = statement ++ s" HAVING ${ expression.statement }",
        params    = params ++ expression.parameter
      )

  private[ldbc] case class JoinWhere[SELECTS <: Tuple, T](
    selects:   SELECTS,
    statement: String,
    columns:   T,
    params:    List[Parameter.DynamicBinder]
  ) extends Query.Provider[T],
            JoinOrderByProvider[SELECTS],
            LimitProvider[T]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinWhere(
        selects   = selects,
        statement = statement ++ sql.statement,
        columns   = columns,
        params    = params ++ sql.params
      )

    private def union(label: String, expression: Expression): JoinWhere[SELECTS, T] =
      JoinWhere[SELECTS, T](
        selects   = selects,
        statement = statement ++ s" $label ${ expression.statement }",
        columns   = columns,
        params    = params ++ expression.parameter
      )

    def and(func: SELECTS => Expression): JoinWhere[SELECTS, T] =
      union("AND", func(selects))

    def or(func: SELECTS => Expression): JoinWhere[SELECTS, T] =
      union("OR", func(selects))

    @targetName("OR")
    def ||(func: SELECTS => Expression): JoinWhere[SELECTS, T] =
      union("||", func(selects))

    def xor(func: SELECTS => Expression): JoinWhere[SELECTS, T] =
      union("XOR", func(selects))

    @targetName("AND")
    def &&(func: SELECTS => Expression): JoinWhere[SELECTS, T] =
      union("&&", func(selects))

    def groupBy[A](func: T => Column[A]): JoinGroupBy[SELECTS, T] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).name }",
        columns   = columns,
        params    = params
      )

  private[ldbc] case class JoinSelect[SELECTS <: Tuple, T](
    selects:       SELECTS,
    fromStatement: String,
    columns:       T,
    params:        List[Parameter.DynamicBinder]
  ) extends Query.Provider[T],
            JoinOrderByProvider[SELECTS],
            LimitProvider[T]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinSelect(
        selects       = selects,
        fromStatement = fromStatement,
        columns       = columns,
        params        = params ++ sql.params
      )

    private val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v

    override def statement: String = s"SELECT $str $fromStatement"

    def where(func: SELECTS => Expression): JoinWhere[SELECTS, T] =
      val expression = func(selects)
      JoinWhere(
        selects   = selects,
        statement = statement ++ s" WHERE ${ expression.statement }",
        columns   = columns,
        params    = expression.parameter
      )

    def groupBy[A](func: T => Column[A]): JoinGroupBy[SELECTS, T] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).name }",
        columns   = columns,
        params    = params
      )
