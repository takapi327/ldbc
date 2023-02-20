/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
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
