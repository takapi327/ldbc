/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.interpreter

import scala.compiletime.ops.int.S

import ldbc.query.builder.*

/**
 * An object with methods and types that perform processing on one or more informational Tuples.
 */
object Tuples:

  /**
   * Type for obtaining location information inside the Tuple of the specified type.
   */
  type IndexOf[T <: Tuple, E] <: Int = T match
    case E *: _  => 0
    case _ *: es => S[IndexOf[es, E]]

  /**
   * Type to verify that a tuple of a given type consists only of the type wrapped in Column.
   */
  type IsColumn[T] <: Boolean = T match
    case EmptyTuple              => false
    case Column[t]               => true
    case Column[t] *: EmptyTuple => true
    case Column[t] *: ts         => IsColumn[ts]
    case _                       => false

  type MapToColumn[T <: Tuple] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => Column[h] *: EmptyTuple
    case h *: t          => Column[h] *: MapToColumn[t]

  type InverseColumnMap[T] <: Tuple = T match
    case EmptyTuple              => EmptyTuple
    case Column[h]               => h *: EmptyTuple
    case Column[h] *: EmptyTuple => h *: EmptyTuple
    case Column[h] *: t          => h *: InverseColumnMap[t]

  type IsTable[T] <: Boolean = T match
    case EmptyTuple             => false
    case Table[p]               => true
    case Table[p] *: EmptyTuple => true
    case Table[p] *: ts         => IsTable[ts]
    case _                      => false

  type IsTableOpt[T] <: Boolean = T match
    case EmptyTuple                  => false
    case MySQLTable[p]               => true
    case MySQLTable[p] *: EmptyTuple => true
    case MySQLTable[p] *: ts         => IsTableOpt[ts]
    case _                           => false

  type ToColumn[T] = T match
    case t *: EmptyTuple => Column[t]
    case t *: ts         => MapToColumn[t *: ts]
    case _               => Column[T]

  type ToTableOpt[T <: Tuple] <: Tuple = T match
    case MySQLTable[p] *: EmptyTuple => p match
      case Option[a] => TableOpt[a] *: EmptyTuple
      case _           => TableOpt[p] *: EmptyTuple
    case MySQLTable[p] *: ts         => p match
      case Option[a] => TableOpt[a] *: ToTableOpt[ts]
      case _           => TableOpt[p] *: ToTableOpt[ts]

  def toTableOpt[T <: Tuple](tuple: T)(using Tuples.IsTableOpt[T] =:= true): ToTableOpt[T] =
    val list: List[TableOpt[?]] = tuple.toList.map {
      case table: Table[p]       => TableOpt.Impl[p](table)
      case tableOpt: TableOpt[p] => tableOpt
    }
    Tuple.fromArray(list.toArray).asInstanceOf[ToTableOpt[T]]
