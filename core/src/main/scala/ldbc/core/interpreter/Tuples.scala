/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.interpreter

import scala.compiletime.ops.int.S

/** An object with methods and types that perform processing on one or more informational Tuples.
  */
object Tuples:

  /** Type for obtaining location information inside the Tuple of the specified type.
    */
  type IndexOf[T <: Tuple, E] <: Int = T match
    case E *: _  => 0
    case _ *: es => S[IndexOf[es, E]]
