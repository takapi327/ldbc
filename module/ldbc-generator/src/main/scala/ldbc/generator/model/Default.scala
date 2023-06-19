/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator.model

case class Default(
  value: String | Int,
  attribute: Option[String]
):

  def toCode(isOptional: Boolean): String =
    (value, isOptional) match
      case ("NULL", true) => ".DEFAULT(None)"
      case ("NULL", false) => throw new IllegalArgumentException("NULL cannot be set as the default value for non-null-allowed columns.")
      case ("CURRENT_TIMESTAMP", _) => s".DEFAULT_CURRENT_TIMESTAMP(${ attribute.contains("CURRENT_TIMESTAMP") })"
      case (_, false) => s".DEFAULT($value)"
      case (_, true) => s".DEFAULT(Some($value))"
