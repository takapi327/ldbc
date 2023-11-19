/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.interpreter

import ldbc.query.builder.*
import ldbc.query.builder.statement.TableOpt

object Tuples:

  type MapToColumn[T <: Tuple, F[_]] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => ColumnQuery[F, h] *: EmptyTuple
    case h *: t          => ColumnQuery[F, h] *: MapToColumn[t, F]

  type InverseColumnMap[F[_], T] <: Tuple = T match
    case EmptyTuple                      => EmptyTuple
    case ColumnQuery[F, h]               => h *: EmptyTuple
    case ColumnQuery[F, h] *: EmptyTuple => h *: EmptyTuple
    case ColumnQuery[F, h] *: t          => h *: InverseColumnMap[F, t]

  type IsTableQuery[F[_], T] <: Boolean = T match
    case EmptyTuple => false
    case TableQuery[F, p] => true
    case TableQuery[F, p] *: EmptyTuple => true
    case TableQuery[F, p] *: ts => IsTableQuery[F, ts]
    case _ => false

  type IsTableOpt[F[_], T] <: Boolean = T match
    case EmptyTuple => false
    case TableOpt[F, p] => true
    case TableOpt[F, p] *: EmptyTuple => true
    case TableOpt[F, p] *: ts => IsTableQuery[F, ts]
    case _ => false

  type IsTableQueryOpt[F[_], T] <: Boolean = T match
    case EmptyTuple => false
    case TableQuery[F, p] => true
    case TableOpt[F, p] => true
    case TableQuery[F, p] *: EmptyTuple => true
    case TableOpt[F, p] *: EmptyTuple => true
    case TableQuery[F, p] *: ts => IsTableQueryOpt[F, ts]
    case TableOpt[F, p] *: ts => IsTableQueryOpt[F, ts]
    case _ => false

  type IsColumnQuery[F[_], T] <: Boolean = T match
    case EmptyTuple                      => false
    case ColumnQuery[F, h]               => true
    case ColumnQuery[F, h] *: EmptyTuple => true
    case ColumnQuery[F, h] *: t          => IsColumnQuery[F, t]
    case _                               => false

  type ToColumn[F[_], T] = T match
    case Tuple => MapToColumn[T, F]
    case _     => ColumnQuery[F, T]

  type ToTableOpt[F[_], T <: Tuple] <: Tuple = T match
    case TableQuery[F, t] *: EmptyTuple => TableOpt[F, t] *: EmptyTuple
    case TableOpt[F, t] *: EmptyTuple   => TableOpt[F, t] *: EmptyTuple
    case TableQuery[F, t] *: ts         => TableOpt[F, t] *: ToTableOpt[F, ts]
    case TableOpt[F, t] *: ts           => TableOpt[F, t] *: ToTableOpt[F, ts]

  def toTableOpt[F[_], T <: Tuple](tuple: T)(using IsTableQueryOpt[F, T] =:= true): ToTableOpt[F, T] =
    val list = tuple.toList.asInstanceOf[List[TableQuery[F, ?] | TableOpt[F, ?]]].map {
      case query: TableQuery[F, p] => TableOpt[F, p](query.table)
      case opt: TableOpt[F, p]     => opt
    }
    Tuple.fromArray(list.toArray).asInstanceOf[ToTableOpt[F, T]]
