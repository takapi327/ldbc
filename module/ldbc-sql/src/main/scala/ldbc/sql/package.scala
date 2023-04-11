/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc

import ldbc.core.{ Alias, DataTypes }

/** Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
  * might look something like this.
  *
  * example:
  * {{{
  *   import ldbc.sql.*
  * }}}
  */
package object sql extends Alias, DataTypes:

  type Table[P <: Product] = ldbc.core.Table[P]
  val Table: ldbc.core.Table.type = ldbc.core.Table

  type DataType[T] = ldbc.core.DataType[T]
  val DataType: ldbc.core.DataType.type = ldbc.core.DataType
