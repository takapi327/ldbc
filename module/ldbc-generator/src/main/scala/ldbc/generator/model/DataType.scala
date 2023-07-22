/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.model

import ldbc.core.JdbcType

enum ScalaType(val code: String):
  case Byte                      extends ScalaType("Byte")
  case String                    extends ScalaType("String")
  case Short                     extends ScalaType("Short")
  case Int                       extends ScalaType("Int")
  case Long                      extends ScalaType("Long")
  case BigDecimal                extends ScalaType("BigDecimal")
  case Float                     extends ScalaType("Float")
  case Double                    extends ScalaType("Double")
  case BigInt                    extends ScalaType("BigInt")
  case ArrayByte                 extends ScalaType("Array[Byte]")
  case Instant                   extends ScalaType("java.time.Instant")
  case LocalDate                 extends ScalaType("java.time.LocalDate")
  case LocalDateTime             extends ScalaType("java.time.LocalDateTime")
  case LocalTime                 extends ScalaType("java.time.LocalTime")
  case OffsetTime                extends ScalaType("java.time.OffsetTime")
  case OffsetDateTime            extends ScalaType("java.time.OffsetDateTime")
  case ZonedDateTime             extends ScalaType("java.time.ZonedDateTime")
  case Year                      extends ScalaType("java.time.Year")
  case Enum(types: List[String]) extends ScalaType("Enum")

trait DataType:

  val name: String

  val jdbcType: JdbcType

  val scalaType: ScalaType

  val scalaTypes: Seq[ScalaType]

  def getTypeMatches(custom: String): String =
    scalaTypes.find(_.toString == custom).getOrElse(scalaType).code

  def propertyType(isOptional: Boolean): String =
    if isOptional then s"Option[${ scalaType.code }]" else scalaType.code

  def toCode(typeParam: String): String

object DataType:

  def BIT(length: Option[Int]): DataType = new DataType:
    override val name:      String    = "BIT"
    override val jdbcType:  JdbcType  = JdbcType.Bit
    override val scalaType: ScalaType = ScalaType.Byte
    override val scalaTypes: Seq[ScalaType] =
      Seq(ScalaType.Short, ScalaType.Int, ScalaType.Long)

    override def toCode(typeParam: String): String = length.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")

  def TINYINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "TINYINT"
    override val jdbcType:   JdbcType       = JdbcType.TinyInt
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Short else ScalaType.Byte
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Byte, ScalaType.Short)

    override def toCode(typeParam: String): String =
      val dataType = length.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")
      (unsigned, zerofill) match
        case (true, true)   => s"$dataType.UNSIGNED.ZEROFILL"
        case (true, false)  => s"$dataType.UNSIGNED"
        case (false, true)  => s"$dataType.ZEROFILL"
        case (false, false) => s"$dataType"

  def SMALLINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "SMALLINT"
    override val jdbcType:   JdbcType       = JdbcType.SmallInt
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Int else ScalaType.Short
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Short, ScalaType.Int)

    override def toCode(typeParam: String): String =
      val dataType = length.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")
      (unsigned, zerofill) match
        case (true, true)   => s"$dataType.UNSIGNED.ZEROFILL"
        case (true, false)  => s"$dataType.UNSIGNED"
        case (false, true)  => s"$dataType.ZEROFILL"
        case (false, false) => s"$dataType"

  def MEDIUMINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "MEDIUMINT"
    override val jdbcType:   JdbcType       = JdbcType.Integer
    override val scalaType:  ScalaType      = ScalaType.Int
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      val dataType = length.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")
      (unsigned, zerofill) match
        case (true, true)   => s"$dataType.UNSIGNED.ZEROFILL"
        case (true, false)  => s"$dataType.UNSIGNED"
        case (false, true)  => s"$dataType.ZEROFILL"
        case (false, false) => s"$dataType"

  def INT(length: Option[Int], unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "INT"
    override val jdbcType:   JdbcType       = JdbcType.Integer
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Long else ScalaType.Int
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Int, ScalaType.Long)

    override def toCode(typeParam: String): String =
      val dataType = length.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")
      (unsigned, zerofill) match
        case (true, true)   => s"$dataType.UNSIGNED.ZEROFILL"
        case (true, false)  => s"$dataType.UNSIGNED"
        case (false, true)  => s"$dataType.ZEROFILL"
        case (false, false) => s"$dataType"

  def BIGINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "BIGINT"
    override val jdbcType:   JdbcType       = JdbcType.BigInt
    override val scalaType:  ScalaType      = if unsigned then ScalaType.BigInt else ScalaType.Long
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Long, ScalaType.BigInt)

    override def toCode(typeParam: String): String =
      val dataType = length.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")
      (unsigned, zerofill) match
        case (true, true)   => s"$dataType.UNSIGNED.ZEROFILL"
        case (true, false)  => s"$dataType.UNSIGNED"
        case (false, true)  => s"$dataType.ZEROFILL"
        case (false, false) => s"$dataType"

  def DECIMAL(accuracy: Int, scale: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "DECIMAL"
    override val jdbcType:   JdbcType       = JdbcType.Decimal
    override val scalaType:  ScalaType      = ScalaType.BigDecimal
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($accuracy, $scale).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($accuracy, $scale).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($accuracy, $scale).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($accuracy, $scale)"

  def FLOAT(accuracy: Int, unsigned: Boolean, zerofill: Boolean): DataType = new DataType:
    override val name:       String         = "FLOAT"
    override val jdbcType:   JdbcType       = JdbcType.Float
    override val scalaType:  ScalaType      = ScalaType.Float
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Float, ScalaType.Double)

    override def toCode(typeParam: String): String =
      (unsigned, zerofill) match
        case (true, true)   => s"$name[$typeParam]($accuracy).UNSIGNED.ZEROFILL"
        case (true, false)  => s"$name[$typeParam]($accuracy).UNSIGNED"
        case (false, true)  => s"$name[$typeParam]($accuracy).ZEROFILL"
        case (false, false) => s"$name[$typeParam]($accuracy)"

  def CHAR(length: Int, character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:       String         = "CHAR"
    override val jdbcType:   JdbcType       = JdbcType.Char
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]($length).CHARACTER_SET(\"$ch\").COLLATE(\"$co\")"
        case (Some(ch), None)     => s"$name[$typeParam]($length).CHARACTER_SET(\"$ch\")"
        case (None, Some(co))     => s"$name[$typeParam]($length).COLLATE(\"$co\")"
        case (None, None)         => s"$name[$typeParam]($length)"

  def VARCHAR(length: Int, character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:       String         = "VARCHAR"
    override val jdbcType:   JdbcType       = JdbcType.VarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]($length).CHARACTER_SET(\"$ch\").COLLATE(\"$co\")"
        case (Some(ch), None)     => s"$name[$typeParam]($length).CHARACTER_SET(\"$ch\")"
        case (None, Some(co))     => s"$name[$typeParam]($length).COLLATE(\"$co\")"
        case (None, None)         => s"$name[$typeParam]($length)"

  def BINARY(length: Int): DataType = new DataType:
    override val name:       String         = "BINARY"
    override val jdbcType:   JdbcType       = JdbcType.Binary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = s"$name[$typeParam]($length)"

  def VARBINARY(length: Int): DataType = new DataType:
    override val name:       String         = "VARBINARY"
    override val jdbcType:   JdbcType       = JdbcType.VarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = s"$name[$typeParam]($length)"

  def TINYBLOB(): DataType = new DataType:
    override val name:       String         = "TINYBLOB"
    override val jdbcType:   JdbcType       = JdbcType.VarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = s"$name[$typeParam]()"

  def TINYTEXT(character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:       String         = "TINYTEXT"
    override val jdbcType:   JdbcType       = JdbcType.VarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(\"$ch\").COLLATE(\"$co\")"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(\"$ch\")"
        case (None, Some(co))     => s"$name[$typeParam]().COLLATE(\"$co\")"
        case (None, None)         => s"$name[$typeParam]()"

  def ENUM(types: List[String]): DataType = new DataType:
    override val name:       String         = "ENUM"
    override val jdbcType:   JdbcType       = JdbcType.Char
    override val scalaType:  ScalaType      = ScalaType.Enum(types)
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def getTypeMatches(custom: String): String =
      scalaTypes.find(_.toString == custom).map(_.code).getOrElse(custom)

    override def toCode(typeParam: String): String = s"$name[$typeParam]"

  def BLOB(length: Option[Int]): DataType = new DataType:
    override val name:       String         = "BLOB"
    override val jdbcType:   JdbcType       = JdbcType.Blob
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = length.fold(s"$name[$typeParam]()")(n => s"$name[$typeParam]($n)")

  def TEXT(length: Option[Int], character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:       String         = "TEXT"
    override val jdbcType:   JdbcType       = JdbcType.LongVarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(\"$ch\").COLLATE(\"$co\")"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(\"$ch\")"
        case (None, Some(co))     => s"$name[$typeParam]().COLLATE(\"$co\")"
        case (None, None)         => s"$name[$typeParam]()"

  def MEDIUMBLOB(): DataType = new DataType:
    override val name:       String         = "MEDIUMBLOB"
    override val jdbcType:   JdbcType       = JdbcType.LongVarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = s"$name[$typeParam]()"

  def MEDIUMTEXT(character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:       String         = "MEDIUMTEXT"
    override val jdbcType:   JdbcType       = JdbcType.LongVarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(\"$ch\").COLLATE(\"$co\")"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(\"$ch\")"
        case (None, Some(co))     => s"$name[$typeParam]().COLLATE(\"$co\")"
        case (None, None)         => s"$name[$typeParam]()"

  def LONGBLOB(): DataType = new DataType:
    override val name:       String         = "LONGBLOB"
    override val jdbcType:   JdbcType       = JdbcType.LongVarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = s"$name[$typeParam]()"

  def LONGTEXT(character: Option[String], collate: Option[String]): DataType = new DataType:
    override val name:       String         = "LONGTEXT"
    override val jdbcType:   JdbcType       = JdbcType.LongVarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String =
      (character, collate) match
        case (Some(ch), Some(co)) => s"$name[$typeParam]().CHARACTER_SET(\"$ch\").COLLATE(\"$co\")"
        case (Some(ch), None)     => s"$name[$typeParam]().CHARACTER_SET(\"$ch\")"
        case (None, Some(co))     => s"$name[$typeParam]().COLLATE(\"$co\")"
        case (None, None)         => s"$name[$typeParam]()"

  def DATE(): DataType = new DataType:
    override val name:       String         = "DATE"
    override val jdbcType:   JdbcType       = JdbcType.Date
    override val scalaType:  ScalaType      = ScalaType.LocalDate
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = s"$name[$typeParam]"

  def DATETIME(fsp: Option[Int]): DataType = new DataType:
    override val name:       String         = "DATETIME"
    override val jdbcType:   JdbcType       = JdbcType.Timestamp
    override val scalaType:  ScalaType      = ScalaType.LocalDateTime
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Instant, ScalaType.OffsetTime)

    override def toCode(typeParam: String): String = fsp.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")

  def TIMESTAMP(fsp: Option[Int]): DataType = new DataType:
    override val name:       String         = "TIMESTAMP"
    override val jdbcType:   JdbcType       = JdbcType.Timestamp
    override val scalaType:  ScalaType      = ScalaType.LocalDateTime
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Instant, ScalaType.OffsetDateTime, ScalaType.ZonedDateTime)

    override def toCode(typeParam: String): String = fsp.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")

  def TIME(fsp: Option[Int]): DataType = new DataType:
    override val name:       String         = "TIME"
    override val jdbcType:   JdbcType       = JdbcType.Time
    override val scalaType:  ScalaType      = ScalaType.LocalTime
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def toCode(typeParam: String): String = fsp.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")

  def YEAR(digit: Option[4]): DataType = new DataType:
    override val name:       String         = "YEAR"
    override val jdbcType:   JdbcType       = JdbcType.Date
    override val scalaType:  ScalaType      = ScalaType.Year
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Instant, ScalaType.LocalDate)

    override def toCode(typeParam: String): String = digit.fold(s"$name[$typeParam]")(n => s"$name[$typeParam]($n)")

  def SERIAL(): DataType = new DataType:
    override val name:       String         = "SERIAL"
    override val jdbcType:   JdbcType       = JdbcType.BigInt
    override val scalaType:  ScalaType      = ScalaType.BigInt
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def propertyType(isOptional: Boolean): String = scalaType.code

    override def toCode(typeParam: String): String = s"$name[$scalaType]"
