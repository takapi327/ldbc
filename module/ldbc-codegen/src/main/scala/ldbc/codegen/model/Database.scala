/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.codegen.model

object Database:
  case class CreateStatement(
    name:       String,
    charset:    Option[String],
    collate:    Option[String],
    encryption: Option["Y" | "N"]
  )

  case class DropStatement(name: String)

  case class UseStatement(name: String)
