/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.annotation.targetName

import cats.syntax.all.*

import ldbc.dsl.*
import ldbc.dsl.codec.Encoder
import ldbc.query.builder.*
import ldbc.query.builder.interpreter.*

/**
 * Trait for building Statements to be added.
 *
 * @tparam P
 *   Base trait for all products
 */
private[ldbc] trait Insert[P <: Product] extends Command:
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

  case class Impl[P <: Product](table: Table[P], statement: String, params: List[Parameter.Dynamic])
    extends Insert[P]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(
        table     = table,
        statement = statement ++ sql.statement,
        params    = params ++ sql.params
      )

/**
 * Insert trait that provides a method to update in case of duplicate keys.
 */
private[ldbc] case class DuplicateKeyUpdateInsert(
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Command:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    DuplicateKeyUpdateInsert(statement ++ sql.statement, params ++ sql.params)

/**
 * A model for constructing INSERT statements that insert values into specified columns in MySQL.
 *
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
  encoder: Encoder.MapToTuple[Column.Extract[T]]
)(using Tuples.IsColumn[T] =:= true):

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
        .zip(encoder)
        .toArray
        .map {
          case (value: Any, encoder: Any) =>
            Parameter.Dynamic[Any](value)(using encoder.asInstanceOf[Encoder[Any]])
        }
        .toList
    )

  def values(tuples: List[Column.Extract[T]]): Insert[P] =
    val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")
    Insert.Impl[P](
      table,
      s"$insertStatement VALUES${ values.mkString(", ") }",
      tuples.flatMap(_.zip(encoder).toArray.map {
        case (value: Any, encoder: Any) =>
          Parameter.Dynamic[Any](value)(using encoder.asInstanceOf[Encoder[Any]])
      })
    )
