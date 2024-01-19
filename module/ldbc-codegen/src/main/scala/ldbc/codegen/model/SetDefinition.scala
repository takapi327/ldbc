/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

/** A model to hold the results of parsing the SET syntax for variable assignments.
  *
  * @param variable
  *   Definition Variable Name
  * @param expr
  *   Variable value
  */
case class SetDefinition(
  variable: String,
  expr:     String
):

  /** SET syntax for variable assignment */
  val statement: String = s"SET $variable = $expr"
