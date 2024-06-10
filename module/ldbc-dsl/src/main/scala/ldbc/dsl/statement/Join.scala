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
  def statement: String = s"FROM ${ main._name } ${ joinStatements.mkString(" ") }"

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
      joinStatements :+ s"${ Join.JoinType.JOIN.statement } ${ other._name } ON ${ on(joins ++ Tuple(other)).statement }"
    )

object Join:

  enum JoinType(val statement: String):
    case JOIN       extends JoinType("JOIN")
    case LEFT_JOIN  extends JoinType("LEFT JOIN")
    case RIGHT_JOIN extends JoinType("RIGHT JOIN")
    
  private[ldbc] case class Impl[JOINS <: Tuple, SELECTS <: Tuple](
    main: Table[?],
    joins: JOINS,
    selects: SELECTS,
    joinStatements: List[String]
  ) extends Join[JOINS, SELECTS]

  private[ldbc] case class JoinOrderBy(
                                           statement: String,
                                           params: List[Parameter.DynamicBinder]
                                         ) extends SQL, LimitProvider:
    
    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinOrderBy(statement ++ sql.statement, params ++ sql.params)

  private[ldbc] transparent trait JoinOrderByProvider[SELECTS <: Tuple]:
    self: SQL =>

    def selects: SELECTS

    def orderBy[A <: OrderBy.Order[?] | OrderBy.Order[?] *: NonEmptyTuple | Column[?]](
                                                                                  func: SELECTS => A
                                                                                ): JoinOrderBy =
      val order = func(selects) match
        case v: Tuple => v.toList.mkString(", ")
        case v: OrderBy.Order[?] => v.statement
        case v: Column[?] => v.alias.fold(v.name)(as => s"$as.${v.name}")
      JoinOrderBy(
        statement = self.statement ++ s" ORDER BY $order",
        params = self.params
      )

  private[ldbc] case class JoinHaving[SELECTS <: Tuple](
                                                         selects:   SELECTS,
                                                            statement: String,
                                                            params: List[Parameter.DynamicBinder]
                                                          ) extends SQL,
    JoinOrderByProvider[SELECTS],
    LimitProvider:
    
    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinHaving(selects, statement ++ sql.statement, params ++ sql.params)

  private[ldbc] case class JoinGroupBy[SELECTS <: Tuple, T](
                                                             selects:   SELECTS,
                                                             statement: String,
                                                             columns:   T,
                                                             params:    List[Parameter.DynamicBinder]
                                                           ) extends SQL,
    JoinOrderByProvider[SELECTS],
    LimitProvider:
    
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
