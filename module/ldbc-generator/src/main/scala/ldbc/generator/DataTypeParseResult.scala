/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator

import ldbc.core.JdbcType

enum ScalaType:
  case Byte, String, Short, Int, Long

trait DataTypeParseResult:

  val jdbcType: JdbcType

  val scalaType: ScalaType

object DataTypeParseResult:

  case class Bit(length: Int) extends DataTypeParseResult:
    override val jdbcType: JdbcType = JdbcType.Bit
    override val scalaType: ScalaType = ScalaType.Byte

    override def toString: String = s"BIT[$scalaType]($length)"

  case class Tinyint(length: Int, unsigned: Boolean, zerofill: Boolean) extends DataTypeParseResult:
    override val jdbcType: JdbcType = JdbcType.TinyInt
    override val scalaType: ScalaType = ScalaType.Byte

    override def toString: String =
      if unsigned then s"TINYINT[$scalaType]($length).UNSIGNED" 
      else s"TINYINT[$scalaType]($length)"
