/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.core.interpreter

/** A type function to pull a type parameter it has from a type with one type parameter. */
type Extract[T] = T match
  case Option[t] => Extract[t]
  case Array[t] => Extract[t]
  case List[t] => Extract[t]
  case Seq[t] => Extract[t]
  case Set[t] => Extract[t]
  case _ => T
