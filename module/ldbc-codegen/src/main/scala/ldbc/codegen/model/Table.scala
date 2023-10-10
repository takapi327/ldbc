/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.codegen.model

import ldbc.core.TableOption

object Table:

  case class CreateStatement(
    tableName:         String,
    columnDefinitions: List[ColumnDefinition],
    keyDefinitions:    List[Key],
    options:           Option[List[TableOption]]
  )

  case class DropStatement(tableName: String)
