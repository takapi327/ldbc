/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.interpreter

import scala.compiletime.ops.int.S

import ldbc.core.Column

/** An object with methods and types that perform processing on one or more informational Tuples.
  */
object Tuples:

  /** Type for obtaining location information inside the Tuple of the specified type.
    */
  type IndexOf[T <: Tuple, E] <: Int = T match
    case E *: _  => 0
    case _ *: es => S[IndexOf[es, E]]

  /**
   * Type to verify that a tuple of a given type consists only of the type wrapped in Column.
   */
  type IsColumn[T <: Tuple] <: Boolean = T match
    case EmptyTuple => false
    case Column[t] => true
    case Column[t] *: EmptyTuple => true
    case Column[t] *: ts => IsColumn[ts]
