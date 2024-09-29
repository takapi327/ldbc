/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName
import ldbc.sql.ResultSet
import ldbc.dsl.{Parameter, SQL}
import ldbc.dsl.codec.Decoder
import ldbc.query.builder.*
import ldbc.query.builder.interpreter.Tuples
import ldbc.query.builder.interpreter.Tuples.InverseColumnMap

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
  private[ldbc] def main: Table[?]

  /** Tuple of the table that did the join. */
  private[ldbc] def joins: JOINS

  /** Tuple for building Select statements, etc. on joined tables. */
  private[ldbc] def selects: SELECTS

  /** Join's Statement List. */
  private[ldbc] def joinStatements: List[String]

  /** Statement of Join. */
  private[ldbc] def statement: String = s"FROM ${ main.label } ${ joinStatements.mkString(" ") }"

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
    val sub = other._alias.fold(other.as(other._name))(_ => other)
    Join.Impl(
      main,
      joins ++ Tuple(sub),
      selects ++ Tuple(sub),
      joinStatements :+ s"${ Join.JoinType.JOIN.statement } ${ other.label } ON ${ on(joins ++ Tuple(sub)).statement }"
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
  ): Join[Tuple.Concat[JOINS, Tuple1[Table[P]]], Tuple.Concat[SELECTS, Tuple1[TableOpt[P]]]] =
    val sub = other._alias.fold(other.as(other._name))(_ => other)
    Join.Impl(
      main,
      joins ++ Tuple(sub),
      selects ++ Tuple(TableOpt.Impl(sub)),
      joinStatements :+ s"${ Join.JoinType.LEFT_JOIN.statement } ${ other.label } ON ${ on(joins ++ Tuple(sub)).statement }"
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
    val sub = other._alias.fold(other.as(other._name))(_ => other)
    Join.Impl(
      main,
      joins ++ Tuple(sub),
      Tuples.toTableOpt[SELECTS](selects) ++ Tuple(sub),
      joinStatements :+ s"${ Join.JoinType.RIGHT_JOIN.statement } ${ other.label } ON ${ on(joins ++ Tuple(sub)).statement }"
    )

  def select[C](func: SELECTS => C)(using
    Tuples.IsColumn[C] =:= true
  ): Join.JoinSelect[SELECTS, C, Tuples.InverseColumnMap[C]] =
    val columns = func(selects)
    val decodes: Array[Decoder[?]] = columns match
      case v: Tuple =>
        v.toArray.map {
          case column: Column[t] => column.decoder
        }
      case v: Column[t] => Array(v.decoder)
    val decoder: Decoder[Tuples.InverseColumnMap[C]] = new Decoder[InverseColumnMap[C]](
      (resultSet: ResultSet, prefix: Option[String]) =>
        val results = decodes.map(_.decode(resultSet, None))
        Tuple.fromArray(results).asInstanceOf[Tuples.InverseColumnMap[C]]
    )
    Join.JoinSelect[SELECTS, C, Tuples.InverseColumnMap[C]](
      selects       = selects,
      fromStatement = statement,
      columns       = columns,
      params        = Nil,
      decoder       = decoder
    )

  def selectAll: Join.JoinSelect[SELECTS, Tuple, Table.Extract[SELECTS]] =
    val decoder = new Decoder[Table.Extract[SELECTS]](
      (resultSet: ResultSet, prefix: Option[String]) =>
        val results = selects.toArray.map {
          case table: Table[t]       => table.decoder.decode(resultSet, Some(table._name))
          case tableOpt: TableOpt[t] => tableOpt.decoder.decode(resultSet, Some(tableOpt._name))
        }
        Tuple.fromArray(results).asInstanceOf[Table.Extract[SELECTS]]
    )

    Join.JoinSelect[SELECTS, Tuple, Table.Extract[SELECTS]](
      selects       = selects,
      fromStatement = statement,
      columns = Tuple.fromArray(joins.toArray.flatMap {
        case table: Table[t] => table.*.toArray
      }),
      params  = Nil,
      decoder = decoder
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
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[T]
  ) extends Query[T],
            Limit.QueryProvider[T]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinOrderBy(statement ++ sql.statement, params ++ sql.params, decoder)

  private[ldbc] transparent trait JoinOrderByProvider[SELECTS <: Tuple, D]:
    self: Query[D] =>

    def selects: SELECTS

    def orderBy[A <: OrderBy.Order[?] | OrderBy.Order[?] *: NonEmptyTuple | Column[?]](
      func: SELECTS => A
    ): JoinOrderBy[D] =
      val order = func(selects) match
        case v: Tuple            => v.toList.mkString(", ")
        case v: OrderBy.Order[?] => v.statement
        case v: Column[?]        => v.alias.fold(v.name)(as => s"$as.${ v.name }")
      JoinOrderBy(
        statement = self.statement ++ s" ORDER BY $order",
        params    = self.params,
        decoder   = self.decoder
      )

  private[ldbc] case class JoinHaving[SELECTS <: Tuple, D](
    selects:   SELECTS,
    statement: String,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[D]
  ) extends Query[D],
            JoinOrderByProvider[SELECTS, D],
            Limit.QueryProvider[D]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinHaving(selects, statement ++ sql.statement, params ++ sql.params, decoder)

  private[ldbc] case class JoinGroupBy[SELECTS <: Tuple, C, D](
    selects:   SELECTS,
    statement: String,
    columns:   C,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[D]
  ) extends Query[D],
            JoinOrderByProvider[SELECTS, D],
            Limit.QueryProvider[D]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ sql.statement,
        columns   = columns,
        params    = params ++ sql.params,
        decoder   = decoder
      )

    def having[A](func: C => Expression): JoinHaving[SELECTS, D] =
      val expression = func(columns)
      JoinHaving(
        selects   = selects,
        statement = statement ++ s" HAVING ${ expression.statement }",
        params    = params ++ expression.parameter,
        decoder   = decoder
      )

  private[ldbc] case class JoinWhere[SELECTS <: Tuple, C, D](
    selects:   SELECTS,
    statement: String,
    columns:   C,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[D]
  ) extends Query[D],
            JoinOrderByProvider[SELECTS, D],
            Limit.QueryProvider[D]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinWhere(
        selects   = selects,
        statement = statement ++ sql.statement,
        columns   = columns,
        params    = params ++ sql.params,
        decoder   = decoder
      )

    private def union(label: String, expression: Expression): JoinWhere[SELECTS, C, D] =
      JoinWhere[SELECTS, C, D](
        selects   = selects,
        statement = statement ++ s" $label ${ expression.statement }",
        columns   = columns,
        params    = params ++ expression.parameter,
        decoder   = decoder
      )

    def and(func: SELECTS => Expression): JoinWhere[SELECTS, C, D] =
      union("AND", func(selects))

    def or(func: SELECTS => Expression): JoinWhere[SELECTS, C, D] =
      union("OR", func(selects))

    @targetName("OR")
    def ||(func: SELECTS => Expression): JoinWhere[SELECTS, C, D] =
      union("||", func(selects))

    def xor(func: SELECTS => Expression): JoinWhere[SELECTS, C, D] =
      union("XOR", func(selects))

    @targetName("AND")
    def &&(func: SELECTS => Expression): JoinWhere[SELECTS, C, D] =
      union("&&", func(selects))

    def groupBy[A](func: C => Column[A]): JoinGroupBy[SELECTS, C, D] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).name }",
        columns   = columns,
        params    = params,
        decoder   = decoder
      )

  private[ldbc] case class JoinSelect[SELECTS <: Tuple, C, D](
    selects:       SELECTS,
    fromStatement: String,
    columns:       C,
    params:        List[Parameter.Dynamic],
    decoder:       Decoder[D]
  ) extends Query[D],
            JoinOrderByProvider[SELECTS, D],
            Limit.QueryProvider[D]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      JoinSelect(
        selects       = selects,
        fromStatement = fromStatement,
        columns       = columns,
        params        = params ++ sql.params,
        decoder       = decoder
      )

    private val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v

    override def statement: String = s"SELECT $str $fromStatement"

    def where(func: SELECTS => Expression): JoinWhere[SELECTS, C, D] =
      val expression = func(selects)
      JoinWhere(
        selects   = selects,
        statement = statement ++ s" WHERE ${ expression.statement }",
        columns   = columns,
        params    = expression.parameter,
        decoder   = decoder
      )

    def groupBy[A](func: C => Column[A]): JoinGroupBy[SELECTS, C, D] =
      JoinGroupBy(
        selects   = selects,
        statement = statement ++ s" GROUP BY ${ func(columns).name }",
        columns   = columns,
        params    = params,
        decoder   = decoder
      )
