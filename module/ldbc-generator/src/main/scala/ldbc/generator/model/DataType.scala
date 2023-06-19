/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

import ldbc.core.JdbcType

enum ScalaType(val code: String):
  case Byte       extends ScalaType("Byte")
  case String     extends ScalaType("String")
  case Short      extends ScalaType("Short")
  case Int        extends ScalaType("Int")
  case Long       extends ScalaType("Long")
  case BigDecimal extends ScalaType("BigDecimal")
  case Float      extends ScalaType("Float")
  case BigInt     extends ScalaType("BigInt")
  case ArrayByte  extends ScalaType("Array[Byte]")
  case LocalDate  extends ScalaType("java.time.LocalDate")

trait DataType:

  val name: String

  val jdbcType: JdbcType

  val scalaType: ScalaType

  def toCode(typeParam: String): String

object DataType:

  def BIT(length: Int): DataType = new DataType:
    override val name:      String    = "BIT"
    override val jdbcType:  JdbcType  = JdbcType.Bit
    override val scalaType: ScalaType = ScalaType.Byte

    override def toCode(typeParam: String): String = s"$name[$typeParam]($length)"

  def TINYINT(length: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "TINYINT"
    override val jdbcType:  JdbcType  = JdbcType.TinyInt
    override val scalaType: ScalaType = if unsigned then ScalaType.Short else ScalaType.Byte

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($length).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($length).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($length).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($length)"

  def SMALLINT(length: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "SMALLINT"
    override val jdbcType:  JdbcType  = JdbcType.SmallInt
    override val scalaType: ScalaType = if unsigned then ScalaType.Int else ScalaType.Short

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($length).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($length).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($length).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($length)"

  def MEDIUMINT(length: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "MEDIUMINT"
    override val jdbcType:  JdbcType  = JdbcType.Integer
    override val scalaType: ScalaType = ScalaType.Int

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($length).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($length).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($length).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($length)"

  def INT(length: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "INT"
    override val jdbcType:  JdbcType  = JdbcType.Integer
    override val scalaType: ScalaType = if unsigned then ScalaType.Long else ScalaType.Int

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($length).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($length).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($length).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($length)"

  def BIGINT(length: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "BIGINT"
    override val jdbcType:  JdbcType  = JdbcType.BigInt
    override val scalaType: ScalaType = if unsigned then ScalaType.BigInt else ScalaType.Long

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($length).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($length).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($length).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($length)"

  def DECIMAL(accuracy: Int, scale: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "DECIMAL"
    override val jdbcType:  JdbcType  = JdbcType.Decimal
    override val scalaType: ScalaType = ScalaType.BigDecimal

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($accuracy, $scale).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($accuracy, $scale).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($accuracy, $scale).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($accuracy, $scale)"

  def FLOAT(accuracy: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:      String    = "FLOAT"
    override val jdbcType:  JdbcType  = JdbcType.Float
    override val scalaType: ScalaType = ScalaType.Float

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($accuracy).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($accuracy).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($accuracy).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($accuracy)"

  def CHAR(length: Int, character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:      String    = "CHAR"
    override val jdbcType:  JdbcType  = JdbcType.Char
    override val scalaType: ScalaType = ScalaType.String

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]($length).CHARACTER_SET(Character(\"$ch\").set(\"$co\"))"
        case (Some(ch), None)     => s"$name[$typeParam]($length).CHARACTER_SET(Character(\"$ch\"))"
        case (None, Some(co)) =>
          throw new IllegalArgumentException("It is not possible to set only COLLATE without setting Character.")
        case (None, None) => s"$name[$typeParam]($length)"

  def VARCHAR(length: Int, character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:      String    = "VARCHAR"
    override val jdbcType:  JdbcType  = JdbcType.VarChar
    override val scalaType: ScalaType = ScalaType.String

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]($length).CHARACTER_SET(Character(\"$ch\").set(\"$co\"))"
        case (Some(ch), None)     => s"$name[$typeParam]($length).CHARACTER_SET(Character(\"$ch\"))"
        case (None, Some(co)) =>
          throw new IllegalArgumentException("It is not possible to set only COLLATE without setting Character.")
        case (None, None) => s"$name[$typeParam]($length)"

  def BINARY(length: Int): DataType = new DataType:
    override val name:      String    = "BINARY"
    override val jdbcType:  JdbcType  = JdbcType.Binary
    override val scalaType: ScalaType = ScalaType.ArrayByte

    override def toCode(typeParam: String): String = s"$name[$typeParam]($length)"

  def VARBINARY(length: Int): DataType = new DataType:
    override val name:      String    = "VARBINARY"
    override val jdbcType:  JdbcType  = JdbcType.VarBinary
    override val scalaType: ScalaType = ScalaType.ArrayByte

    override def toCode(typeParam: String): String = s"$name[$typeParam]($length)"

  def TINYBLOB(): DataType = new DataType:
    override val name:      String    = "TINYBLOB"
    override val jdbcType:  JdbcType  = JdbcType.VarBinary
    override val scalaType: ScalaType = ScalaType.ArrayByte

    override def toCode(typeParam: String): String = s"$name[$typeParam]()"

  def TINYTEXT(character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:      String    = "TINYTEXT"
    override val jdbcType:  JdbcType  = JdbcType.VarChar
    override val scalaType: ScalaType = ScalaType.String

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\").set(\"$co\"))"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\"))"
        case (None, Some(co)) =>
          throw new IllegalArgumentException("It is not possible to set only COLLATE without setting Character.")
        case (None, None) => s"$name[$typeParam]()"

  def BLOB(length: Option[Int]): DataType = new DataType:
    override val name:      String    = "BLOB"
    override val jdbcType:  JdbcType  = JdbcType.Blob
    override val scalaType: ScalaType = ScalaType.ArrayByte

    override def toCode(typeParam: String): String = length.fold(s"$name[$typeParam]()")(n => s"$name[$typeParam]($n)")

  def TEXT(length: Option[Int], character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:      String    = "TEXT"
    override val jdbcType:  JdbcType  = JdbcType.LongVarChar
    override val scalaType: ScalaType = ScalaType.String

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\").set(\"$co\"))"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\"))"
        case (None, Some(co)) =>
          throw new IllegalArgumentException("It is not possible to set only COLLATE without setting Character.")
        case (None, None) => s"$name[$typeParam]()"

  def MEDIUMBLOB(): DataType = new DataType:
    override val name:      String    = "MEDIUMBLOB"
    override val jdbcType:  JdbcType  = JdbcType.LongVarBinary
    override val scalaType: ScalaType = ScalaType.ArrayByte

    override def toCode(typeParam: String): String = s"$name[$typeParam]()"

  def MEDIUMTEXT(character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:      String    = "MEDIUMTEXT"
    override val jdbcType:  JdbcType  = JdbcType.LongVarChar
    override val scalaType: ScalaType = ScalaType.String

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\").set(\"$co\"))"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\"))"
        case (None, Some(co)) =>
          throw new IllegalArgumentException("It is not possible to set only COLLATE without setting Character.")
        case (None, None) => s"$name[$typeParam]()"

  def LONGBLOB(): DataType = new DataType:
    override val name:      String    = "LONGBLOB"
    override val jdbcType:  JdbcType  = JdbcType.LongVarBinary
    override val scalaType: ScalaType = ScalaType.ArrayByte

    override def toCode(typeParam: String): String = s"$name[$typeParam]()"

  def LONGTEXT(character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:      String    = "LONGTEXT"
    override val jdbcType:  JdbcType  = JdbcType.LongVarChar
    override val scalaType: ScalaType = ScalaType.String

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\").set(\"$co\"))"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(Character(\"$ch\"))"
        case (None, Some(co)) =>
          throw new IllegalArgumentException("It is not possible to set only COLLATE without setting Character.")
        case (None, None) => s"$name[$typeParam]()"

  def DATE(): DataType = new DataType:
    override val name:      String    = "DATE"
    override val jdbcType:  JdbcType  = JdbcType.Date
    override val scalaType: ScalaType = ScalaType.LocalDate

    override def toCode(typeParam: String): String = s"$name[$typeParam]"
