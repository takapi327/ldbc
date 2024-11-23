/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.deriving.Mirror
import scala.compiletime.*

import ldbc.dsl.Parameter
import ldbc.dsl.codec.Encoder
import ldbc.statement.internal.QueryConcat

trait TableQuery[A, O]:

  type Entity = TableQuery.Extract[A]

  private[ldbc] def table: A

  private[ldbc] def column: Column[Entity]

  private[ldbc] def params: List[Parameter.Dynamic]

  def name: String

  def select[C](func: A => Column[C]): Select[A, C] =
    val columns = func(table)
    Select(table, columns, s"SELECT ${ columns.alias.getOrElse(columns.name) } FROM $name", params)

  def selectAll: Select[A, Entity] =
    Select(table, column, s"SELECT ${ column.alias.getOrElse(column.name) } FROM $name", params)

  private type ToTuple[T] <: Tuple = T match
    case h *: EmptyTuple => Tuple1[h]
    case h *: t          => h *: ToTuple[t]
    case Any             => Tuple1[T]

  inline def insert[C](func: A => Column[C], values: C): Insert[A] =
    val columns = func(table)
    val parameterBinders = (values match
      case h *: EmptyTuple => h *: EmptyTuple
      case h *: t          => h *: t *: EmptyTuple
      case h               => h *: EmptyTuple
    )
    .zip(Encoder.fold[ToTuple[C]])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    Insert.Impl(
      table     = table,
      statement = s"INSERT INTO $name ${ columns.insertStatement }",
      params    = params ++ parameterBinders
    )

  inline def insert(value: Entity)(using mirror: Mirror.Of[Entity]): Insert[A] =
    inline mirror match
      case s: Mirror.SumOf[Entity]     => error("Sum type is not supported.")
      case p: Mirror.ProductOf[Entity] => derivedProduct(value, p)

  private inline def derivedProduct[P](value: P, mirror: Mirror.ProductOf[P]): Insert[A] =
    val tuples = Tuple.fromProduct(value.asInstanceOf[Product]).asInstanceOf[mirror.MirroredElemTypes]
    val parameterBinders = tuples
      .zip(Encoder.fold[mirror.MirroredElemTypes])
      .toList
      .map {
        case (value, encoder) => Parameter.Dynamic(value)(using encoder.asInstanceOf[Encoder[Any]])
      }
    Insert.Impl(
      table     = table,
      statement = s"INSERT INTO $name ${ column.insertStatement }",
      params    = params ++ parameterBinders
    )

  def update: Update[A] = Update.Impl[A](table, s"UPDATE $name", params)

  /**
   * Method to construct a query to delete a table.
   */
  def delete: Delete[A] = Delete[A](table, s"DELETE FROM $name", params)

  /**
   * Method to construct a query to drop a table.
   */
  def dropTable: Command = Command.Pure(s"DROP TABLE $name", List.empty)

  /**
   * Method to construct a query to truncate a table.
   */
  def truncateTable: Command = Command.Pure(s"TRUNCATE TABLE $name", List.empty)

  def join[B, BO, AB, OO](other: TableQuery[B, BO])(using QueryConcat.Aux[A, B, AB], QueryConcat.Aux[O, BO, OO]): Join[A, B, AB, OO]

  def leftJoin[B, BO, OB, OO](other: TableQuery[B, BO])(using QueryConcat.Aux[A, BO, OB], QueryConcat.Aux[O, BO, OO]): Join[A, B, OB, OO]

  def rightJoin[B, BO, OB, OO](other: TableQuery[B, BO])(using
                                                     QueryConcat.Aux[O, B, OB],
                                                         QueryConcat.Aux[O, BO, OO]
  ): Join[A, B, OB, OO]

  private[ldbc] def toOption: TableQuery[A, O]

  private[ldbc] def asVector(): Vector[TableQuery[?, ?]]

object TableQuery:

  type Extract[T] = T match
    case AbstractTable[t]       => t
    case AbstractTable[t] *: tn => t *: Extract[tn]
