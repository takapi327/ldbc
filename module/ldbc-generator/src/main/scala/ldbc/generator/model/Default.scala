/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

/** Trait for setting SQL Default values
 */
trait Default:

  def toCode(isOptional: Boolean): String

object Default:

  /** Model to be used when a value matching the DataType type is set.
   *
   * @param value
   * Value set as the default value for DataType
   */
  case class Value(value: String | Int) extends Default:
    override def toCode(isOptional: Boolean): String =
      if isOptional then s".DEFAULT(Some($value))"
      else s".DEFAULT($value)"

  /** Object for setting NULL as the Default value when the SQL DataType is NULL-allowed.
   */
  object Null extends Default:
    override def toCode(isOptional: Boolean): String =
      if isOptional then ".DEFAULT(None)"
      else throw new IllegalArgumentException("NULL cannot be set as the default value for non-null-allowed columns.")

  /** Model for setting TimeStamp-specific Default values.
   *
   * @param onUpdate
   * Value to determine whether to set additional information
   */
  case class CurrentTimestamp(onUpdate: Boolean) extends Default:
    override def toCode(isOptional: Boolean): String =
      if onUpdate then ".DEFAULT_CURRENT_TIMESTAMP(true)"
      else ".DEFAULT_CURRENT_TIMESTAMP(false)"
