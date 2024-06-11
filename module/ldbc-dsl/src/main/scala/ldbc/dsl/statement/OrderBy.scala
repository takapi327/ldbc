/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.statement

import scala.annotation.targetName

import ldbc.sql.Parameter

import ldbc.dsl.*

/**
 * A model for constructing ORDER BY statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param query
 *   Query string
 * @param order
 *   Order query string
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 */
private[ldbc] case class OrderBy[P <: Product](
  table: Table[P],
  query:  String,
  order:  String,
  params:     List[Parameter.DynamicBinder]
) extends SQL, LimitProvider:

  override def statement: String = query ++ s" ORDER BY $order"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    OrderBy[P](table, query ++ sql.statement, order, params ++ sql.params)

object OrderBy:
  
  enum Order[T](val name: String, val column: Column[T]):
    case Asc(v: Column[T]) extends Order[T]("ASC", v)
    case Desc(v: Column[T]) extends Order[T]("DESC", v)
    
    val statement: String = column.alias.fold(s"${ column.name } $name")(as => s"$as.${ column.name } $name")

/**
 * Transparent Trait to provide orderBy method.
 *
 * @tparam P
 *   Base trait for all products
 */
private[ldbc] transparent trait OrderByProvider[P <: Product]:
  self: SQL =>
  
  /** Trait for generating SQL table information. */
  def table: Table[P]

  /**
   * A method for setting the ORDER BY condition in a statement.
   */
  def orderBy[T <: OrderBy.Order[?] | OrderBy.Order[?] *: NonEmptyTuple | Column[?]](func: Table[P] => T): OrderBy[P] =
    val order = func(table) match
      case tuple: Tuple => tuple.toList.mkString(", ")
      case order: OrderBy.Order[?] => order.statement
      case column: Column[?] => column.toString
    OrderBy(
      table   = table,
      query   = statement,
      order   = order,
      params  = params
    )
