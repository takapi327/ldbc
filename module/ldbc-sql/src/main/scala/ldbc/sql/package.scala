/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
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
