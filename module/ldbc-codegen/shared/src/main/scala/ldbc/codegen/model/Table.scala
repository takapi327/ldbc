/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

object Table:

  case class CreateStatement(
    tableName:         String,
    columnDefinitions: List[ColumnDefinition],
    keyDefinitions:    List[Key],
    options:           Option[List[TableOption]]
  )

  case class DropStatement(tableName: String)
