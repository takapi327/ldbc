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

object DataType:

  trait NumberDataType extends DataType:

    def length: Option[Int]

    def unsigned: Boolean

    def zerofill: Boolean

  trait StringDataType extends DataType:

    def length: Int

    def character: Option[String]

    def collate: Option[String]

  case class BIT(length: Option[Int]) extends DataType:
    override val name:      String    = "BIT"
    override val jdbcType:  JdbcType  = JdbcType.Bit
    override val scalaType: ScalaType = ScalaType.Byte
    override val scalaTypes: Seq[ScalaType] =
      Seq(ScalaType.Short, ScalaType.Int, ScalaType.Long)

  case class TINYINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "TINYINT"
    override val jdbcType:   JdbcType       = JdbcType.TinyInt
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Short else ScalaType.Byte
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Byte, ScalaType.Short)

  case class SMALLINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "SMALLINT"
    override val jdbcType:   JdbcType       = JdbcType.SmallInt
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Int else ScalaType.Short
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Short, ScalaType.Int)

  case class MEDIUMINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "MEDIUMINT"
    override val jdbcType:   JdbcType       = JdbcType.Integer
    override val scalaType:  ScalaType      = ScalaType.Int
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class INT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "INT"
    override val jdbcType:   JdbcType       = JdbcType.Integer
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Long else ScalaType.Int
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Int, ScalaType.Long)

  case class BIGINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "BIGINT"
    override val jdbcType:   JdbcType       = JdbcType.BigInt
    override val scalaType:  ScalaType      = if unsigned then ScalaType.BigInt else ScalaType.Long
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Long, ScalaType.BigInt)


  case class DECIMAL(accuracy: Int, scale: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val name:       String         = "DECIMAL"
    override val jdbcType:   JdbcType       = JdbcType.Decimal
    override val scalaType:  ScalaType      = ScalaType.BigDecimal
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class FLOAT(accuracy: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val name:       String         = "FLOAT"
    override val jdbcType:   JdbcType       = JdbcType.Float
    override val scalaType:  ScalaType      = ScalaType.Float
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Float, ScalaType.Double)


  case class CHAR(length: Int, character: Option[String], collate: Option[String]) extends StringDataType:
    override val name:       String         = "CHAR"
    override val jdbcType:   JdbcType       = JdbcType.Char
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class VARCHAR(length: Int, character: Option[String], collate: Option[String]) extends StringDataType:
    override val name:       String         = "VARCHAR"
    override val jdbcType:   JdbcType       = JdbcType.VarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class BINARY(length: Int) extends DataType:
    override val name:       String         = "BINARY"
    override val jdbcType:   JdbcType       = JdbcType.Binary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class VARBINARY(length: Int) extends DataType:
    override val name:       String         = "VARBINARY"
    override val jdbcType:   JdbcType       = JdbcType.VarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class TINYBLOB() extends DataType:
    override val name:       String         = "TINYBLOB"
    override val jdbcType:   JdbcType       = JdbcType.VarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class TINYTEXT(character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "TINYTEXT"
    override val jdbcType:   JdbcType       = JdbcType.VarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class ENUM(types: List[String], character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "ENUM"
    override val jdbcType:   JdbcType       = JdbcType.Char
    override val scalaType:  ScalaType      = ScalaType.Enum(types)
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def getTypeMatches(custom: String): String =
      scalaTypes.find(_.toString == custom).map(_.code).getOrElse(custom)

  case class BLOB(length: Option[Int]) extends DataType:
    override val name:       String         = "BLOB"
    override val jdbcType:   JdbcType       = JdbcType.Blob
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class TEXT(length: Option[Int], character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "TEXT"
    override val jdbcType:   JdbcType       = JdbcType.LongVarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class MEDIUMBLOB() extends DataType:
    override val name:       String         = "MEDIUMBLOB"
    override val jdbcType:   JdbcType       = JdbcType.LongVarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class MEDIUMTEXT(character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "MEDIUMTEXT"
    override val jdbcType:   JdbcType       = JdbcType.LongVarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class LONGBLOB() extends DataType:
    override val name:       String         = "LONGBLOB"
    override val jdbcType:   JdbcType       = JdbcType.LongVarBinary
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class LONGTEXT(character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "LONGTEXT"
    override val jdbcType:   JdbcType       = JdbcType.LongVarChar
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class DATE() extends DataType:
    override val name:       String         = "DATE"
    override val jdbcType:   JdbcType       = JdbcType.Date
    override val scalaType:  ScalaType      = ScalaType.LocalDate
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class DATETIME(fsp: Option[Int]) extends DataType:
    override val name:       String         = "DATETIME"
    override val jdbcType:   JdbcType       = JdbcType.Timestamp
    override val scalaType:  ScalaType      = ScalaType.LocalDateTime
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Instant, ScalaType.OffsetTime)

  case class TIMESTAMP(fsp: Option[Int]) extends DataType:
    override val name:       String         = "TIMESTAMP"
    override val jdbcType:   JdbcType       = JdbcType.Timestamp
    override val scalaType:  ScalaType      = ScalaType.LocalDateTime
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Instant, ScalaType.OffsetDateTime, ScalaType.ZonedDateTime)

  case class TIME(fsp: Option[Int]) extends DataType:
    override val name:       String         = "TIME"
    override val jdbcType:   JdbcType       = JdbcType.Time
    override val scalaType:  ScalaType      = ScalaType.LocalTime
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class YEAR(digit: Option[4]) extends DataType:
    override val name:       String         = "YEAR"
    override val jdbcType:   JdbcType       = JdbcType.Date
    override val scalaType:  ScalaType      = ScalaType.Year
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Instant, ScalaType.LocalDate)

  case class SERIAL() extends DataType:
    override val name:       String         = "SERIAL"
    override val jdbcType:   JdbcType       = JdbcType.BigInt
    override val scalaType:  ScalaType      = ScalaType.BigInt
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def propertyType(isOptional: Boolean): String = scalaType.code
