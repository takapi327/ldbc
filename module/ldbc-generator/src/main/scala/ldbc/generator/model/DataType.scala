/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

import ldbc.core.JdbcType

enum ScalaType:
  case Byte, String, Short, Int, Long, BigDecimal, Float

trait DataType:

  val jdbcType: JdbcType

  val scalaType: ScalaType

object DataType:

  case class Bit(length: Int) extends DataType:
    override val jdbcType:  JdbcType  = JdbcType.Bit
    override val scalaType: ScalaType = ScalaType.Byte

    override def toString: String = s"BIT[$scalaType]($length)"

  case class Tinyint(length: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType:  JdbcType  = JdbcType.TinyInt
    override val scalaType: ScalaType = ScalaType.Byte

    override def toString: String =
      if unsigned then s"TINYINT[$scalaType]($length).UNSIGNED"
      else s"TINYINT[$scalaType]($length)"

  case class Smallint(length: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType: JdbcType = JdbcType.SmallInt
    override val scalaType: ScalaType = ScalaType.Short

    override def toString: String =
      if unsigned then s"SMALLINT[$scalaType]($length).UNSIGNED"
      else s"SMALLINT[$scalaType]($length)"

  case class Mediumint(length: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType: JdbcType = JdbcType.Integer
    override val scalaType: ScalaType = ScalaType.Int

    override def toString: String =
      if unsigned then s"MEDIUMINT[$scalaType]($length).UNSIGNED"
      else s"MEDIUMINT[$scalaType]($length)"

  case class Integer(length: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType: JdbcType = JdbcType.Integer
    override val scalaType: ScalaType = ScalaType.Int

    override def toString: String =
      if unsigned then s"INT[$scalaType]($length).UNSIGNED"
      else s"INT[$scalaType]($length)"

  case class BigInt(length: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType:  JdbcType  = JdbcType.BigInt
    override val scalaType: ScalaType = ScalaType.Long

    override def toString: String =
      if unsigned then s"BIGINT[$scalaType]($length).UNSIGNED"
      else s"BIGINT[$scalaType]($length)"

  case class Decimal(accuracy: Int, scale: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType: JdbcType = JdbcType.Decimal
    override val scalaType: ScalaType = ScalaType.BigDecimal

    override def toString: String =
      if unsigned then s"DECIMAL[$scalaType]($accuracy, $scale).UNSIGNED"
      else s"DECIMAL[$scalaType]($accuracy, $scale)"

  case class CFloat(accuracy: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val jdbcType: JdbcType = JdbcType.Float
    override val scalaType: ScalaType = ScalaType.Float

    override def toString: String =
      if unsigned then s"FLOAT[$scalaType]($accuracy).UNSIGNED"
      else s"FLOAT[$scalaType]($accuracy)"
