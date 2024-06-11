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
 * Trait for building Statements to be added.
 *
 * @tparam P
 *   Base trait for all products
 */
private[ldbc] trait Insert[P <: Product] extends SQL:
  self =>

  /** A model for generating queries from Table information. */
  def table: Table[P]

  /** Methods for constructing INSERT ... ON DUPLICATE KEY UPDATE statements. */
  def onDuplicateKeyUpdate[T](func: Table[P] => T)(using
    Tuples.IsColumn[T] =:= true
  ): DuplicateKeyUpdateInsert =
    val duplicateKeys = func(self.table) match
      case tuple: Tuple => tuple.toList.map(column => s"$column = new_${ table._name }.$column")
      case column       => List(s"$column = new_${ table._name }.$column")
    DuplicateKeyUpdateInsert(
      s"${ self.statement } AS new_${ table._name } ON DUPLICATE KEY UPDATE ${ duplicateKeys.mkString(", ") }",
      self.params
    )

object Insert:

  private[ldbc] case class Impl[P <: Product](table: Table[P], statement: String, params: List[Parameter.DynamicBinder])
    extends Insert[P]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      Impl(table, statement ++ sql.statement, params ++ sql.params)

/**
 * Insert trait that provides a method to update in case of duplicate keys.
 */
case class DuplicateKeyUpdateInsert(
  statement: String,
  params:    List[Parameter.DynamicBinder]
) extends SQL:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    DuplicateKeyUpdateInsert(statement ++ sql.statement, params ++ sql.params)

/**
 * A model for constructing INSERT statements that insert single values in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param tuple
 *   Tuple type value of the property with type parameter P.
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Tuple type of the property with type parameter P
 */
case class SingleInsert[P <: Product, T <: Tuple](
  table:  Table[P],
  tuple:  T,
  params: List[Parameter.DynamicBinder]
) extends Insert[P]:

  override def statement: String =
    s"INSERT INTO ${ table._name } (${ table.*.toList.mkString(", ") }) VALUES(${ tuple.toArray.map(_ => "?").mkString(", ") })"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    SingleInsert(table, tuple, params ++ sql.params)

/**
 * A model for constructing INSERT statements that insert multiple values in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param tuples
 *   Tuple type value of the property with type parameter P.
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Tuple type of the property with type parameter P
 */
case class MultiInsert[P <: Product, T <: Tuple](
  table:  Table[P],
  tuples: List[T],
  params: List[Parameter.DynamicBinder]
) extends Insert[P]:

  private val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")

  override def statement: String =
    s"INSERT INTO ${ table._name } (${ table.*.toList.mkString(", ") }) VALUES${ values.mkString(", ") }"

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    MultiInsert(table, tuples, params ++ sql.params)

/**
 * A model for constructing INSERT statements that insert values into specified columns in MySQL.
 *
 * @param query
 *   Trait for generating SQL table information.
 * @param columns
 *   List of columns into which values are to be inserted.
 * @param parameter
 *   Parameters of the value to be inserted
 * @tparam P
 *   Base trait for all products
 * @tparam T
 *   Tuple type of the property with type parameter P
 */
case class SelectInsert[P <: Product, T](
  table:     Table[P],
  columns:   T,
  parameter: Parameter.MapToTuple[Column.Extract[T]]
):

  private val columnStatement = columns match
    case v: Tuple => v.toArray.distinct.mkString(", ")
    case v        => v

  private val insertStatement: String =
    s"INSERT INTO ${ table._name } ($columnStatement)"

  def values(tuple: Column.Extract[T]): Insert[P] =
    Insert.Impl[P](
      table,
      s"$insertStatement VALUES(${ tuple.toArray.map(_ => "?").mkString(", ") })",
      tuple
        .zip(parameter)
        .toArray
        .map {
          case (value: Any, parameter: Any) =>
            Parameter.DynamicBinder[Any](value)(using parameter.asInstanceOf[Parameter[Any]])
        }
        .toList
    )

  def values(tuples: List[Column.Extract[T]]): Insert[P] =
    val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")
    Insert.Impl[P](
      table,
      s"$insertStatement VALUES${ values.mkString(", ") }",
      tuples.flatMap(_.zip(parameter).toArray.map {
        case (value: Any, parameter: Any) =>
          Parameter.DynamicBinder[Any](value)(using parameter.asInstanceOf[Parameter[Any]])
      })
    )
