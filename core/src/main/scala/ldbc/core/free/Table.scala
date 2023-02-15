/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.free

import ldbc.core.Key

/** Trait for generating SQL table information.
  */
private[ldbc] trait Table:

  /** Table name */
  private[ldbc] def name: String

  /** Table Key definitions */
  private[ldbc] def keyDefinitions: Seq[Key]
