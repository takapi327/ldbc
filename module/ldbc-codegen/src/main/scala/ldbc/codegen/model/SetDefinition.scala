/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
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
