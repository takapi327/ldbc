/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.builder

import ldbc.codegen.formatter.Naming
import ldbc.codegen.model.DataType

import munit.CatsEffectSuite

class DataTypeCodeBuilderTest extends CatsEffectSuite:

  private def builder(scalaType: String) = DataTypeCodeBuilder(scalaType, Naming.PASCAL)

  test("BIT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.BIT(None)
    val dataType2 = DataType.BIT(Some(1))
    assertEquals(builder("Byte").build(dataType1), "BIT[Byte]")
    assertEquals(builder("Option[Byte]").build(dataType1), "BIT[Option[Byte]]")
    assertEquals(builder("Byte").build(dataType2), "BIT[Byte](1)")
    assertEquals(builder("Option[Byte]").build(dataType2), "BIT[Option[Byte]](1)")
  }

  test("TINYINT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.TINYINT(None, true, true)
    val dataType2 = DataType.TINYINT(Some(1), false, true)
    val dataType3 = DataType.TINYINT(None, true, false)
    val dataType4 = DataType.TINYINT(Some(1), false, false)
    assertEquals(builder("Short").build(dataType1), "TINYINT[Short].UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[Short]").build(dataType1), "TINYINT[Option[Short]].UNSIGNED.ZEROFILL")
    assertEquals(builder("Short").build(dataType2), "TINYINT[Short](1).ZEROFILL")
    assertEquals(builder("Option[Short]").build(dataType2), "TINYINT[Option[Short]](1).ZEROFILL")
    assertEquals(builder("Short").build(dataType3), "TINYINT[Short].UNSIGNED")
    assertEquals(builder("Option[Short]").build(dataType3), "TINYINT[Option[Short]].UNSIGNED")
    assertEquals(builder("Short").build(dataType4), "TINYINT[Short](1)")
    assertEquals(builder("Option[Short]").build(dataType4), "TINYINT[Option[Short]](1)")
  }

  test("SMALLINT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.SMALLINT(None, true, true)
    val dataType2 = DataType.SMALLINT(Some(1), false, true)
    val dataType3 = DataType.SMALLINT(None, true, false)
    val dataType4 = DataType.SMALLINT(Some(1), false, false)
    assertEquals(builder("Int").build(dataType1), "SMALLINT[Int].UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[Int]").build(dataType1), "SMALLINT[Option[Int]].UNSIGNED.ZEROFILL")
    assertEquals(builder("Int").build(dataType2), "SMALLINT[Int](1).ZEROFILL")
    assertEquals(builder("Option[Int]").build(dataType2), "SMALLINT[Option[Int]](1).ZEROFILL")
    assertEquals(builder("Int").build(dataType3), "SMALLINT[Int].UNSIGNED")
    assertEquals(builder("Option[Int]").build(dataType3), "SMALLINT[Option[Int]].UNSIGNED")
    assertEquals(builder("Int").build(dataType4), "SMALLINT[Int](1)")
    assertEquals(builder("Option[Int]").build(dataType4), "SMALLINT[Option[Int]](1)")
  }

  test("MEDIUMINT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.MEDIUMINT(None, true, true)
    val dataType2 = DataType.MEDIUMINT(Some(1), false, true)
    val dataType3 = DataType.MEDIUMINT(None, true, false)
    val dataType4 = DataType.MEDIUMINT(Some(1), false, false)
    assertEquals(builder("Int").build(dataType1), "MEDIUMINT[Int].UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[Int]").build(dataType1), "MEDIUMINT[Option[Int]].UNSIGNED.ZEROFILL")
    assertEquals(builder("Int").build(dataType2), "MEDIUMINT[Int](1).ZEROFILL")
    assertEquals(builder("Option[Int]").build(dataType2), "MEDIUMINT[Option[Int]](1).ZEROFILL")
    assertEquals(builder("Int").build(dataType3), "MEDIUMINT[Int].UNSIGNED")
    assertEquals(builder("Option[Int]").build(dataType3), "MEDIUMINT[Option[Int]].UNSIGNED")
    assertEquals(builder("Int").build(dataType4), "MEDIUMINT[Int](1)")
    assertEquals(builder("Option[Int]").build(dataType4), "MEDIUMINT[Option[Int]](1)")
  }

  test("INT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.INT(None, true, true)
    val dataType2 = DataType.INT(Some(1), false, true)
    val dataType3 = DataType.INT(None, true, false)
    val dataType4 = DataType.INT(Some(1), false, false)
    assertEquals(builder("Int").build(dataType1), "INT[Int].UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[Int]").build(dataType1), "INT[Option[Int]].UNSIGNED.ZEROFILL")
    assertEquals(builder("Int").build(dataType2), "INT[Int](1).ZEROFILL")
    assertEquals(builder("Option[Int]").build(dataType2), "INT[Option[Int]](1).ZEROFILL")
    assertEquals(builder("Int").build(dataType3), "INT[Int].UNSIGNED")
    assertEquals(builder("Option[Int]").build(dataType3), "INT[Option[Int]].UNSIGNED")
    assertEquals(builder("Int").build(dataType4), "INT[Int](1)")
    assertEquals(builder("Option[Int]").build(dataType4), "INT[Option[Int]](1)")
  }

  test("BIGINT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.BIGINT(None, true, true)
    val dataType2 = DataType.BIGINT(Some(1), false, true)
    val dataType3 = DataType.BIGINT(None, true, false)
    val dataType4 = DataType.BIGINT(Some(1), false, false)
    assertEquals(builder("BigInt").build(dataType1), "BIGINT[BigInt].UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[BigInt]").build(dataType1), "BIGINT[Option[BigInt]].UNSIGNED.ZEROFILL")
    assertEquals(builder("BigInt").build(dataType2), "BIGINT[BigInt](1).ZEROFILL")
    assertEquals(builder("Option[BigInt]").build(dataType2), "BIGINT[Option[BigInt]](1).ZEROFILL")
    assertEquals(builder("BigInt").build(dataType3), "BIGINT[BigInt].UNSIGNED")
    assertEquals(builder("Option[BigInt]").build(dataType3), "BIGINT[Option[BigInt]].UNSIGNED")
    assertEquals(builder("BigInt").build(dataType4), "BIGINT[BigInt](1)")
  }

  test("DECIMAL DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.DECIMAL(10, 0, true, true)
    val dataType2 = DataType.DECIMAL(10, 0, false, true)
    val dataType3 = DataType.DECIMAL(10, 0, true, false)
    val dataType4 = DataType.DECIMAL(10, 0, false, false)
    assertEquals(builder("BigDecimal").build(dataType1), "DECIMAL[BigDecimal](10, 0).UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[BigDecimal]").build(dataType1), "DECIMAL[Option[BigDecimal]](10, 0).UNSIGNED.ZEROFILL")
    assertEquals(builder("BigDecimal").build(dataType2), "DECIMAL[BigDecimal](10, 0).ZEROFILL")
    assertEquals(builder("Option[BigDecimal]").build(dataType2), "DECIMAL[Option[BigDecimal]](10, 0).ZEROFILL")
    assertEquals(builder("BigDecimal").build(dataType3), "DECIMAL[BigDecimal](10, 0).UNSIGNED")
    assertEquals(builder("Option[BigDecimal]").build(dataType3), "DECIMAL[Option[BigDecimal]](10, 0).UNSIGNED")
    assertEquals(builder("BigDecimal").build(dataType4), "DECIMAL[BigDecimal](10, 0)")
    assertEquals(builder("Option[BigDecimal]").build(dataType4), "DECIMAL[Option[BigDecimal]](10, 0)")
  }

  test("FLOAT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.FLOAT(10, true, true)
    val dataType2 = DataType.FLOAT(10, false, true)
    val dataType3 = DataType.FLOAT(10, true, false)
    val dataType4 = DataType.FLOAT(10, false, false)
    assertEquals(builder("Float").build(dataType1), "FLOAT[Float](10).UNSIGNED.ZEROFILL")
    assertEquals(builder("Option[Float]").build(dataType1), "FLOAT[Option[Float]](10).UNSIGNED.ZEROFILL")
    assertEquals(builder("Float").build(dataType2), "FLOAT[Float](10).ZEROFILL")
    assertEquals(builder("Option[Float]").build(dataType2), "FLOAT[Option[Float]](10).ZEROFILL")
    assertEquals(builder("Float").build(dataType3), "FLOAT[Float](10).UNSIGNED")
    assertEquals(builder("Option[Float]").build(dataType3), "FLOAT[Option[Float]](10).UNSIGNED")
    assertEquals(builder("Float").build(dataType4), "FLOAT[Float](10)")
    assertEquals(builder("Option[Float]").build(dataType4), "FLOAT[Option[Float]](10)")
  }

  test("CHAR DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.CHAR(255, Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.CHAR(255, Some("utf8mb4"), None)
    val dataType3 = DataType.CHAR(255, None, Some("utf8mb4_bin"))
    val dataType4 = DataType.CHAR(255, None, None)
    assertEquals(
      builder("String").build(dataType1),
      "CHAR[String](255).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[String]").build(dataType1),
      "CHAR[Option[String]](255).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType2), "CHAR[String](255).CHARACTER_SET(Character.utf8mb4)")
    assertEquals(
      builder("Option[String]").build(dataType2),
      "CHAR[Option[String]](255).CHARACTER_SET(Character.utf8mb4)"
    )
    assertEquals(builder("String").build(dataType3), "CHAR[String](255).COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("Option[String]").build(dataType3), "CHAR[Option[String]](255).COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("String").build(dataType4), "CHAR[String](255)")
    assertEquals(builder("Option[String]").build(dataType4), "CHAR[Option[String]](255)")
  }

  test("VARCHAR DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.VARCHAR(255, Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.VARCHAR(255, Some("utf8mb4"), None)
    val dataType3 = DataType.VARCHAR(255, None, Some("utf8mb4_bin"))
    val dataType4 = DataType.VARCHAR(255, None, None)
    assertEquals(
      builder("String").build(dataType1),
      "VARCHAR[String](255).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[String]").build(dataType1),
      "VARCHAR[Option[String]](255).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType2), "VARCHAR[String](255).CHARACTER_SET(Character.utf8mb4)")
    assertEquals(
      builder("Option[String]").build(dataType2),
      "VARCHAR[Option[String]](255).CHARACTER_SET(Character.utf8mb4)"
    )
    assertEquals(builder("String").build(dataType3), "VARCHAR[String](255).COLLATE(Collate.utf8mb4_bin)")
    assertEquals(
      builder("Option[String]").build(dataType3),
      "VARCHAR[Option[String]](255).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType4), "VARCHAR[String](255)")
    assertEquals(builder("Option[String]").build(dataType4), "VARCHAR[Option[String]](255)")
  }

  test("BINARY DataType construction to code string matches the specified string.") {
    val dataType = DataType.BINARY(255)
    assertEquals(builder("Array[Byte]").build(dataType), "BINARY[Array[Byte]](255)")
    assertEquals(builder("Option[Array[Byte]]").build(dataType), "BINARY[Option[Array[Byte]]](255)")
  }

  test("VARBINARY DataType construction to code string matches the specified string.") {
    val dataType = DataType.VARBINARY(255)
    assertEquals(builder("Array[Byte]").build(dataType), "VARBINARY[Array[Byte]](255)")
    assertEquals(builder("Option[Array[Byte]]").build(dataType), "VARBINARY[Option[Array[Byte]]](255)")
  }

  test("TINYBLOB DataType construction to code string matches the specified string.") {
    val dataType = DataType.TINYBLOB()
    assertEquals(builder("Array[Byte]").build(dataType), "TINYBLOB[Array[Byte]]()")
    assertEquals(builder("Option[Array[Byte]]").build(dataType), "TINYBLOB[Option[Array[Byte]]]()")
  }

  test("TINYTEXT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.TINYTEXT(Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.TINYTEXT(Some("utf8mb4"), None)
    val dataType3 = DataType.TINYTEXT(None, Some("utf8mb4_bin"))
    val dataType4 = DataType.TINYTEXT(None, None)
    assertEquals(
      builder("String").build(dataType1),
      "TINYTEXT[String]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[String]").build(dataType1),
      "TINYTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType2), "TINYTEXT[String]().CHARACTER_SET(Character.utf8mb4)")
    assertEquals(
      builder("Option[String]").build(dataType2),
      "TINYTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4)"
    )
    assertEquals(builder("String").build(dataType3), "TINYTEXT[String]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("Option[String]").build(dataType3), "TINYTEXT[Option[String]]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("String").build(dataType4), "TINYTEXT[String]()")
    assertEquals(builder("Option[String]").build(dataType4), "TINYTEXT[Option[String]]()")
  }

  test("ENUM DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.ENUM(List("Active", "InActive"), Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.ENUM(List("Active", "InActive"), Some("utf8mb4"), None)
    val dataType3 = DataType.ENUM(List("Active", "InActive"), None, Some("utf8mb4_bin"))
    val dataType4 = DataType.ENUM(List("Active", "InActive"), None, None)
    assertEquals(
      builder("Status").build(dataType1),
      "ENUM[Status](using Status).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[Status]").build(dataType1),
      "ENUM[Option[Status]](using Status).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("Status").build(dataType2), "ENUM[Status](using Status).CHARACTER_SET(Character.utf8mb4)")
    assertEquals(
      builder("Option[Status]").build(dataType2),
      "ENUM[Option[Status]](using Status).CHARACTER_SET(Character.utf8mb4)"
    )
    assertEquals(builder("Status").build(dataType3), "ENUM[Status](using Status).COLLATE(Collate.utf8mb4_bin)")
    assertEquals(
      builder("Option[Status]").build(dataType3),
      "ENUM[Option[Status]](using Status).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("Status").build(dataType4), "ENUM[Status](using Status)")
    assertEquals(builder("Option[Status]").build(dataType4), "ENUM[Option[Status]](using Status)")
  }

  test("BLOB DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.BLOB(Some(255))
    val dataType2 = DataType.BLOB(None)
    assertEquals(builder("Array[Byte]").build(dataType1), "BLOB[Array[Byte]](255)")
    assertEquals(builder("Option[Array[Byte]]").build(dataType1), "BLOB[Option[Array[Byte]]](255)")
    assertEquals(builder("Array[Byte]").build(dataType2), "BLOB[Array[Byte]]()")
    assertEquals(builder("Option[Array[Byte]]").build(dataType2), "BLOB[Option[Array[Byte]]]()")
  }

  test("TEXT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.TEXT(Some(255), Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.TEXT(None, Some("utf8mb4"), None)
    val dataType3 = DataType.TEXT(Some(255), None, Some("utf8mb4_bin"))
    val dataType4 = DataType.TEXT(None, None, None)
    assertEquals(
      builder("String").build(dataType1),
      "TEXT[String]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[String]").build(dataType1),
      "TEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType2), "TEXT[String]().CHARACTER_SET(Character.utf8mb4)")
    assertEquals(builder("Option[String]").build(dataType2), "TEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4)")
    assertEquals(builder("String").build(dataType3), "TEXT[String]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("Option[String]").build(dataType3), "TEXT[Option[String]]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("String").build(dataType4), "TEXT[String]()")
    assertEquals(builder("Option[String]").build(dataType4), "TEXT[Option[String]]()")
  }

  test("MEDIUMBLOB DataType construction to code string matches the specified string.") {
    val dataType = DataType.MEDIUMBLOB()
    assertEquals(builder("Array[Byte]").build(dataType), "MEDIUMBLOB[Array[Byte]]()")
    assertEquals(builder("Option[Array[Byte]]").build(dataType), "MEDIUMBLOB[Option[Array[Byte]]]()")
  }

  test("MEDIUMTEXT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.MEDIUMTEXT(Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.MEDIUMTEXT(Some("utf8mb4"), None)
    val dataType3 = DataType.MEDIUMTEXT(None, Some("utf8mb4_bin"))
    val dataType4 = DataType.MEDIUMTEXT(None, None)
    assertEquals(
      builder("String").build(dataType1),
      "MEDIUMTEXT[String]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[String]").build(dataType1),
      "MEDIUMTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType2), "MEDIUMTEXT[String]().CHARACTER_SET(Character.utf8mb4)")
    assertEquals(
      builder("Option[String]").build(dataType2),
      "MEDIUMTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4)"
    )
    assertEquals(builder("String").build(dataType3), "MEDIUMTEXT[String]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(
      builder("Option[String]").build(dataType3),
      "MEDIUMTEXT[Option[String]]().COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType4), "MEDIUMTEXT[String]()")
    assertEquals(builder("Option[String]").build(dataType4), "MEDIUMTEXT[Option[String]]()")
  }

  test("LONGBLOB DataType construction to code string matches the specified string.") {
    val dataType = DataType.LONGBLOB()
    assertEquals(builder("Array[Byte]").build(dataType), "LONGBLOB[Array[Byte]]()")
    assertEquals(builder("Option[Array[Byte]]").build(dataType), "LONGBLOB[Option[Array[Byte]]]()")
  }

  test("LONGTEXT DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.LONGTEXT(Some("utf8mb4"), Some("utf8mb4_bin"))
    val dataType2 = DataType.LONGTEXT(Some("utf8mb4"), None)
    val dataType3 = DataType.LONGTEXT(None, Some("utf8mb4_bin"))
    val dataType4 = DataType.LONGTEXT(None, None)
    assertEquals(
      builder("String").build(dataType1),
      "LONGTEXT[String]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(
      builder("Option[String]").build(dataType1),
      "LONGTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)"
    )
    assertEquals(builder("String").build(dataType2), "LONGTEXT[String]().CHARACTER_SET(Character.utf8mb4)")
    assertEquals(
      builder("Option[String]").build(dataType2),
      "LONGTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4)"
    )
    assertEquals(builder("String").build(dataType3), "LONGTEXT[String]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("Option[String]").build(dataType3), "LONGTEXT[Option[String]]().COLLATE(Collate.utf8mb4_bin)")
    assertEquals(builder("String").build(dataType4), "LONGTEXT[String]()")
    assertEquals(builder("Option[String]").build(dataType4), "LONGTEXT[Option[String]]()")
  }

  test("DATE DataType construction to code string matches the specified string.") {
    val dataType = DataType.DATE()
    assertEquals(builder("LocalDate").build(dataType), "DATE[LocalDate]")
    assertEquals(builder("Option[LocalDate]").build(dataType), "DATE[Option[LocalDate]]")
  }

  test("DATETIME DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.DATETIME(Some(6))
    val dataType2 = DataType.DATETIME(None)
    assertEquals(builder("LocalDateTime").build(dataType1), "DATETIME[LocalDateTime](6)")
    assertEquals(builder("Option[LocalDateTime]").build(dataType1), "DATETIME[Option[LocalDateTime]](6)")
    assertEquals(builder("LocalDateTime").build(dataType2), "DATETIME[LocalDateTime]")
    assertEquals(builder("Option[LocalDateTime]").build(dataType2), "DATETIME[Option[LocalDateTime]]")
  }

  test("TIMESTAMP DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.TIMESTAMP(Some(6))
    val dataType2 = DataType.TIMESTAMP(None)
    assertEquals(builder("LocalDateTime").build(dataType1), "TIMESTAMP[LocalDateTime](6)")
    assertEquals(builder("Option[LocalDateTime]").build(dataType1), "TIMESTAMP[Option[LocalDateTime]](6)")
    assertEquals(builder("LocalDateTime").build(dataType2), "TIMESTAMP[LocalDateTime]")
    assertEquals(builder("Option[LocalDateTime]").build(dataType2), "TIMESTAMP[Option[LocalDateTime]]")
  }

  test("TIME DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.TIME(Some(6))
    val dataType2 = DataType.TIME(None)
    assertEquals(builder("LocalTime").build(dataType1), "TIME[LocalTime](6)")
    assertEquals(builder("Option[LocalTime]").build(dataType1), "TIME[Option[LocalTime]](6)")
    assertEquals(builder("LocalTime").build(dataType2), "TIME[LocalTime]")
    assertEquals(builder("Option[LocalTime]").build(dataType2), "TIME[Option[LocalTime]]")
  }

  test("YEAR DataType construction to code string matches the specified string.") {
    val dataType1 = DataType.YEAR(Some(4))
    val dataType2 = DataType.YEAR(None)
    assertEquals(builder("Year").build(dataType1), "YEAR[Year](4)")
    assertEquals(builder("Option[Year]").build(dataType1), "YEAR[Option[Year]](4)")
    assertEquals(builder("Year").build(dataType2), "YEAR[Year]")
  }

  test("SERIAL DataType construction to code string matches the specified string.") {
    val dataType = DataType.SERIAL()
    assertEquals(builder("BigInt").build(dataType), "SERIAL[BigInt]")
    assertEquals(builder("Option[BigInt]").build(dataType), "SERIAL[BigInt]")
  }
