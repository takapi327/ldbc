/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sbt

trait Format

object Format {
  case object CAMEL  extends Format
  case object PASCAL extends Format
  case object SNAKE  extends Format
  case object KEBAB  extends Format
}
