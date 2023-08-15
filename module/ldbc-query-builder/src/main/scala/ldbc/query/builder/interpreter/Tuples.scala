/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.interpreter

import ldbc.sql.ResultSetReader
import ldbc.query.builder.ColumnReader

object Tuples:

  type MapToColumn[T <: Tuple, F[_]] <: Tuple = T match
    case EmptyTuple      => EmptyTuple
    case h *: EmptyTuple => ColumnReader[F, h] *: EmptyTuple
    case h *: t          => ColumnReader[F, h] *: MapToColumn[t, F]

  type InverseColumnMap[F[_], T] <: Tuple = T match
    case EmptyTuple                       => EmptyTuple
    case ColumnReader[F, h]               => h *: EmptyTuple
    case ColumnReader[F, h] *: EmptyTuple => h *: EmptyTuple
    case ColumnReader[F, h] *: t          => h *: InverseColumnMap[F, t]

  type IsColumnReader[F[_], T] <: Boolean = T match
    case EmptyTuple                       => false
    case ColumnReader[F, h]               => true
    case ColumnReader[F, h] *: EmptyTuple => true
    case ColumnReader[F, h] *: t          => IsColumnReader[F, t]
    case _                                => false

  type ToColumn[F[_], T] = T match
    case Tuple => MapToColumn[T, F]
    case _     => ColumnReader[F, T]

  type MapToResultSetReader[F[_], T <: Tuple] <: Tuple = T match
    case EmptyTuple => EmptyTuple
    case h *: t => ResultSetReader[F, h] *: MapToResultSetReader[F, t]
