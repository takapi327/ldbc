/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.interpreter

import ldbc.query.builder.*
import ldbc.query.builder.statement.TableOpt

object Tuples:

  type MapToColumn[T <: Tuple] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => ColumnQuery[h] *: EmptyTuple
    case h *: t          => ColumnQuery[h] *: MapToColumn[t]

  type InverseColumnMap[T] <: Tuple = T match
    case EmptyTuple                   => EmptyTuple
    case ColumnQuery[h]               => h *: EmptyTuple
    case ColumnQuery[h] *: EmptyTuple => h *: EmptyTuple
    case ColumnQuery[h] *: t          => h *: InverseColumnMap[t]

  type IsTableQuery[T] <: Boolean = T match
    case EmptyTuple                  => false
    case TableQuery[p]               => true
    case TableQuery[p] *: EmptyTuple => true
    case TableQuery[p] *: ts         => IsTableQuery[ts]
    case _                           => false

  type IsTableOpt[T] <: Boolean = T match
    case EmptyTuple                => false
    case TableOpt[p]               => true
    case TableOpt[p] *: EmptyTuple => true
    case TableOpt[p] *: ts         => IsTableQuery[ts]
    case _                         => false

  type IsTableQueryOpt[T] <: Boolean = T match
    case EmptyTuple                  => false
    case TableQuery[p]               => true
    case TableOpt[p]                 => true
    case TableQuery[p] *: EmptyTuple => true
    case TableOpt[p] *: EmptyTuple   => true
    case TableQuery[p] *: ts         => IsTableQueryOpt[ts]
    case TableOpt[p] *: ts           => IsTableQueryOpt[ts]
    case _                           => false

  type IsColumnQuery[T] <: Boolean = T match
    case EmptyTuple                   => false
    case ColumnQuery[h]               => true
    case ColumnQuery[h] *: EmptyTuple => true
    case ColumnQuery[h] *: t          => IsColumnQuery[t]
    case _                            => false

  type ToColumn[T] = T match
    case Tuple => MapToColumn[T]
    case _     => ColumnQuery[T]

  type ToTableOpt[T <: Tuple] <: Tuple = T match
    case TableQuery[t] *: EmptyTuple => TableOpt[t] *: EmptyTuple
    case TableOpt[t] *: EmptyTuple   => TableOpt[t] *: EmptyTuple
    case TableQuery[t] *: ts         => TableOpt[t] *: ToTableOpt[ts]
    case TableOpt[t] *: ts           => TableOpt[t] *: ToTableOpt[ts]

  def toTableOpt[T <: Tuple](tuple: T)(using IsTableQueryOpt[T] =:= true): ToTableOpt[T] =
    val list = tuple.toList.asInstanceOf[List[TableQuery[?] | TableOpt[?]]].map {
      case query: TableQuery[p] => TableOpt[p](query.table)
      case opt: TableOpt[p]     => opt
    }
    Tuple.fromArray(list.toArray).asInstanceOf[ToTableOpt[T]]
