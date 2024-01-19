/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.core.interpreter

/** Type to convert type from Tuple to Tuple in Colum.
  *
  * @tparam Types
  *   Tuple Type
  * @tparam F
  *   Column Type
  */
type ColumnTuples[Types <: Tuple, F[_]] = Types match
  case t *: EmptyTuple => F[t]
  case _               => Tuple.Map[Types, F]
