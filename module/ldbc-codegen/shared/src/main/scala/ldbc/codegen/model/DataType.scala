/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

import ldbc.sql.Types

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
  case Boolean                   extends ScalaType("Boolean")
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

  val sqlType: Int

  val scalaType: ScalaType

  val scalaTypes: Seq[ScalaType]

  def getTypeMatches(custom: String): String =
    scalaTypes.find(_.toString == custom).getOrElse(scalaType).code

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
    override val sqlType:   Int       = Types.BIT
    override val scalaType: ScalaType = ScalaType.Byte
    override val scalaTypes: Seq[ScalaType] =
      Seq(ScalaType.Short, ScalaType.Int, ScalaType.Long)

  case class TINYINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "TINYINT"
    override val sqlType:    Int            = Types.TINYINT
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Short else ScalaType.Byte
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Byte, ScalaType.Short)

  case class SMALLINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "SMALLINT"
    override val sqlType:    Int            = Types.SMALLINT
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Int else ScalaType.Short
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Short, ScalaType.Int)

  case class MEDIUMINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "MEDIUMINT"
    override val sqlType:    Int            = Types.INTEGER
    override val scalaType:  ScalaType      = ScalaType.Int
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class INT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "INT"
    override val sqlType:    Int            = Types.INTEGER
    override val scalaType:  ScalaType      = if unsigned then ScalaType.Long else ScalaType.Int
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Int, ScalaType.Long)

  case class BIGINT(length: Option[Int], unsigned: Boolean, zerofill: Boolean) extends NumberDataType:
    override val name:       String         = "BIGINT"
    override val sqlType:    Int            = Types.BIGINT
    override val scalaType:  ScalaType      = if unsigned then ScalaType.BigInt else ScalaType.Long
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Long, ScalaType.BigInt)

  case class DECIMAL(accuracy: Int, scale: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val name:       String         = "DECIMAL"
    override val sqlType:    Int            = Types.DECIMAL
    override val scalaType:  ScalaType      = ScalaType.BigDecimal
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class FLOAT(accuracy: Int, unsigned: Boolean, zerofill: Boolean) extends DataType:
    override val name:       String         = "FLOAT"
    override val sqlType:    Int            = Types.FLOAT
    override val scalaType:  ScalaType      = ScalaType.Float
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Float, ScalaType.Double)

  case class CHAR(length: Int, character: Option[String], collate: Option[String]) extends StringDataType:
    override val name:       String         = "CHAR"
    override val sqlType:    Int            = Types.CHAR
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class VARCHAR(length: Int, character: Option[String], collate: Option[String]) extends StringDataType:
    override val name:       String         = "VARCHAR"
    override val sqlType:    Int            = Types.VARCHAR
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class BINARY(length: Int) extends DataType:
    override val name:       String         = "BINARY"
    override val sqlType:    Int            = Types.BINARY
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class VARBINARY(length: Int) extends DataType:
    override val name:       String         = "VARBINARY"
    override val sqlType:    Int            = Types.VARBINARY
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class TINYBLOB() extends DataType:
    override val name:       String         = "TINYBLOB"
    override val sqlType:    Int            = Types.VARBINARY
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class TINYTEXT(character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "TINYTEXT"
    override val sqlType:    Int            = Types.VARCHAR
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class ENUM(types: List[String], character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "ENUM"
    override val sqlType:    Int            = Types.CHAR
    override val scalaType:  ScalaType      = ScalaType.Enum(types)
    override val scalaTypes: Seq[ScalaType] = Seq.empty

    override def getTypeMatches(custom: String): String =
      scalaTypes.find(_.toString == custom).map(_.code).getOrElse(custom)

  case class BLOB(length: Option[Int]) extends DataType:
    override val name:       String         = "BLOB"
    override val sqlType:    Int            = Types.BLOB
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class TEXT(length: Option[Int], character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "TEXT"
    override val sqlType:    Int            = Types.LONGVARCHAR
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class MEDIUMBLOB() extends DataType:
    override val name:       String         = "MEDIUMBLOB"
    override val sqlType:    Int            = Types.LONGVARBINARY
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class MEDIUMTEXT(character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "MEDIUMTEXT"
    override val sqlType:    Int            = Types.LONGVARCHAR
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class LONGBLOB() extends DataType:
    override val name:       String         = "LONGBLOB"
    override val sqlType:    Int            = Types.LONGVARBINARY
    override val scalaType:  ScalaType      = ScalaType.ArrayByte
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class LONGTEXT(character: Option[String], collate: Option[String]) extends DataType:
    override val name:       String         = "LONGTEXT"
    override val sqlType:    Int            = Types.LONGVARCHAR
    override val scalaType:  ScalaType      = ScalaType.String
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class DATE() extends DataType:
    override val name:       String         = "DATE"
    override val sqlType:    Int            = Types.DATE
    override val scalaType:  ScalaType      = ScalaType.LocalDate
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.String)

  case class DATETIME(fsp: Option[Int]) extends DataType:
    override val name:       String         = "DATETIME"
    override val sqlType:    Int            = Types.TIMESTAMP
    override val scalaType:  ScalaType      = ScalaType.LocalDateTime
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.String, ScalaType.Instant, ScalaType.OffsetTime)

  case class TIMESTAMP(fsp: Option[Int]) extends DataType:
    override val name:      String    = "TIMESTAMP"
    override val sqlType:   Int       = Types.TIMESTAMP
    override val scalaType: ScalaType = ScalaType.LocalDateTime
    override val scalaTypes: Seq[ScalaType] =
      Seq(ScalaType.String, ScalaType.Instant, ScalaType.OffsetDateTime, ScalaType.ZonedDateTime)

  case class TIME(fsp: Option[Int]) extends DataType:
    override val name:       String         = "TIME"
    override val sqlType:    Int            = Types.TIME
    override val scalaType:  ScalaType      = ScalaType.LocalTime
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.String)

  case class YEAR(digit: Option[4]) extends DataType:
    override val name:       String         = "YEAR"
    override val sqlType:    Int            = Types.DATE
    override val scalaType:  ScalaType      = ScalaType.Year
    override val scalaTypes: Seq[ScalaType] = Seq(ScalaType.Int, ScalaType.Instant, ScalaType.LocalDate)

  case class SERIAL() extends DataType:
    override val name:       String         = "SERIAL"
    override val sqlType:    Int            = Types.BIGINT
    override val scalaType:  ScalaType      = ScalaType.BigInt
    override val scalaTypes: Seq[ScalaType] = Seq.empty

  case class BOOLEAN() extends DataType:
    override val name:       String         = "BOOLEAN"
    override val sqlType:    Int            = Types.BOOLEAN
    override val scalaType:  ScalaType      = ScalaType.Boolean
    override val scalaTypes: Seq[ScalaType] = Seq.empty
