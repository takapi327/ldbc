/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Decoder
import ldbc.query.builder.*

/**
 * A trait for constructing WHERE statements in MySQL.
 *
 * @tparam P
 *   Base trait for all products
 */
trait Where[P <: Product]:

  type Self

  /** Trait for generating SQL table information. */
  def table: Table[P]

  /** SQL statement string */
  def statement: String

  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only. */
  def params: List[Parameter.Dynamic]

  /**
   * A method for combining WHERE statements.
   *
   * @param label
   *   A conjunctive expression to join WHERE statements together.
   * @param expression
   *   Trait for the syntax of expressions available in MySQL.
   */
  private[ldbc] def union(label: String, expression: Expression): Self

  def and(func: Table[P] => Expression): Self = union("AND", func(table))

  def or(func: Table[P] => Expression): Self = union("OR", func(table))

  @targetName("OR")
  def ||(func: Table[P] => Expression): Self = union("||", func(table))

  def xor(func: Table[P] => Expression): Self = union("XOR", func(table))

  @targetName("AND")
  def &&(func: Table[P] => Expression): Self = union("&&", func(table))

object Where:

  /**
   * A model for constructing read-only query WHERE statements in MySQL.
   * 
   * @param table
   *   Trait for generating SQL table information.
   * @param statement
   *   SQL statement string
   * @param columns
   *   Union-type column list
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
   * @param decoder
   *   Decoder for converting SQL data to Scala data
   * @tparam P
   *   Base trait for all products
   * @tparam C
   *   Union type of column
   * @tparam D
   *   Scala types to be converted by Decoder
   */
  case class Q[P <: Product, C, D](
    table:     Table[P],
    statement: String,
    columns:   C,
    params:    List[Parameter.Dynamic],
    decoder:   Decoder[D]
  ) extends Where[P],
            Query[D],
            OrderByProvider[P, D],
            Limit.QueryProvider[D]:

    override type Self = Q[P, C, D]

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      Q[P, C, D](table, statement ++ sql.statement, columns, params ++ sql.params, decoder)

    override private[ldbc] def union(label: String, expression: Expression): Q[P, C, D] =
      Q[P, C, D](
        table     = table,
        statement = statement ++ s" $label ${ expression.statement }",
        columns   = columns,
        params    = params ++ expression.parameter,
        decoder   = decoder
      )

    def groupBy[A](func: C => Column[A]): GroupBy[P, C, D] =
      GroupBy(
        table     = table,
        statement = statement ++ s" GROUP BY ${ func(columns).name }",
        columns   = columns,
        params    = params,
        decoder   = decoder
      )

  /**
   * A model for constructing write-only query WHERE statements in MySQL.
   * 
   * @param table
   *   Trait for generating SQL table information.
   * @param statement
   *   SQL statement string
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only.
   * @tparam P
   *   Base trait for all products
   */
  case class C[P <: Product](
    table:     Table[P],
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Where[P],
            Command,
            Limit.CommandProvider:

    override type Self = C[P]

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      C[P](table, statement ++ sql.statement, params ++ sql.params)

    override private[ldbc] def union(label: String, expression: Expression): C[P] =
      C[P](
        table     = table,
        statement = statement ++ s" $label ${ expression.statement }",
        params    = params ++ expression.parameter
      )
