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
    case EmptyTuple                 => false
    case Table[p]                   => true
    case Table.Opt[p]               => true
    case Table[p] *: EmptyTuple     => true
    case Table.Opt[p] *: EmptyTuple => true
    case Table[p] *: ts             => IsTableOpt[ts]
    case Table.Opt[p] *: ts         => IsTableOpt[ts]
    case _                          => false

  type ToColumn[T] = T match
    case Tuple => MapToColumn[T]
    case _     => Column[T]

  type ToTableOpt[T <: Tuple] <: Tuple = T match
    case Table[t] *: EmptyTuple     => Table.Opt[t] *: EmptyTuple
    case Table.Opt[t] *: EmptyTuple => Table.Opt[t] *: EmptyTuple
    case Table[t] *: ts             => Table.Opt[t] *: ToTableOpt[ts]
    case Table.Opt[t] *: ts         => Table.Opt[t] *: ToTableOpt[ts]

  def toTableOpt[T <: Tuple](tuple: T)(using IsTableOpt[T] =:= true): ToTableOpt[T] =
    val list = tuple.toList.asInstanceOf[List[Table[?] | Table.Opt[?]]].map {
      case table: Table[p]   => Table.Opt[p](table.*)
      case opt: Table.Opt[p] => opt
    }
    Tuple.fromArray(list.toArray).asInstanceOf[ToTableOpt[T]]
