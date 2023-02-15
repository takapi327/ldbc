/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.interpreter

import ldbc.core.Column

/** Type to convert type from Tuple to Tuple in Colum.
  *
  * @tparam Types
  *   Tuple Type
  * @tparam F
  *   The effect type
  */
type ColumnTuples[Types <: Tuple, F[_]] = Types match
  case t *: EmptyTuple => Column[F, t]
  case _               => Tuple.Map[Types, [T] =>> Column[F, T]]
