/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.core.Column
import ldbc.dsl.Parameter
import ldbc.query.builder.TableQuery

/**
 * A model for constructing ORDER BY statements in MySQL.
 *
 * @param tableQuery
 *   Trait for generating SQL table information.
 * @param statement
 *   SQL statement string
 * @param columns
 *   Union-type column list
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] case class OrderBy[P <: Product, T](
  tableQuery: TableQuery[P],
  statement:  String,
  columns:    T,
  params:     Seq[Parameter.DynamicBinder]
) extends Query[T],
          LimitProvider[T]

object OrderBy:

  /**
   * Trait to indicate the order of the order.
   */
  trait Order:

    /** Sort Order Type */
    def name: String

    /** Trait for representing SQL Column */
    def column: Column[?]

    /** SQL query string */
    def statement: String = column.alias.fold(s"${ column.label } $name")(as => s"$as.${ column.label } $name")

    override def toString: String = statement

  case class Asc(column: Column[?]) extends Order:
    override def name: String = "ASC"
  case class Desc(column: Column[?]) extends Order:
    override def name: String = "DESC"

/**
 * Transparent Trait to provide orderBy method.
 *
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Union type of column
 */
private[ldbc] transparent trait OrderByProvider[P <: Product, T]:
  self: Query[T] =>

  /**
   * Trait for generating SQL table information.
   */
  def tableQuery: TableQuery[P]

  /**
   * A method for setting the ORDER BY condition in a statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def orderBy[A <: OrderBy.Order | OrderBy.Order *: NonEmptyTuple | Column[?]](
    func: TableQuery[P] => A
  ): OrderBy[P, T] =
    val order = func(tableQuery) match
      case v: Tuple         => v.toList.mkString(", ")
      case v: OrderBy.Order => v.statement
      case v: Column[?]     => v.alias.fold(v.label)(name => s"$name.${ v.label }")
    OrderBy(
      tableQuery = tableQuery,
      statement  = self.statement ++ s" ORDER BY $order",
      columns    = self.columns,
      params     = self.params
    )
