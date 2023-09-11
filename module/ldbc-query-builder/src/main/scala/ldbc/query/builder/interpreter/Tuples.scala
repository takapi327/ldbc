/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.interpreter

import ldbc.query.builder.ColumnQuery

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

  type IsColumnQuery[F[_], T] <: Boolean = T match
    case EmptyTuple                      => false
    case ColumnQuery[F, h]               => true
    case ColumnQuery[F, h] *: EmptyTuple => true
    case ColumnQuery[F, h] *: t          => IsColumnQuery[F, t]
    case _                               => false

  type ToColumn[F[_], T] = T match
    case Tuple => MapToColumn[T, F]
    case _     => ColumnQuery[F, T]
