/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.time.{ LocalDate, LocalDateTime, LocalTime, Year as JYear }

import ldbc.sql.Types

import ldbc.schema.DataType.*

trait DataTypeTest extends munit.FunSuite:

  test("The query string generated from the Smallint DataType model matches the specified one.") {
    val smallintType = DataType.Smallint[Short](None, false, false, false, None)
    assertEquals(smallintType.typeName, "SMALLINT")
    assertEquals(smallintType.length, None)
    assertEquals(smallintType.sqlType, Types.SMALLINT)
    assertEquals(smallintType.isOptional, false)
    assertEquals(smallintType.queryString, "SMALLINT NOT NULL")
    assertEquals(smallintType.toOption.isOptional, true)
    assertEquals(smallintType.toOption.sqlType, Types.SMALLINT)
    assertEquals(smallintType.toOption.queryString, "SMALLINT NULL")
    assertEquals(smallintType.DEFAULT(1.toShort).queryString, "SMALLINT NOT NULL DEFAULT 1")
    assertEquals(smallintType.UNSIGNED.queryString, "SMALLINT UNSIGNED NOT NULL")
    assertEquals(smallintType.ZEROFILL.queryString, "SMALLINT ZEROFILL NOT NULL")
    assertEquals(smallintType.UNSIGNED.ZEROFILL.queryString, "SMALLINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(smallintType.ZEROFILL.UNSIGNED.queryString, "SMALLINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(SMALLINT[Short](0).queryString, "SMALLINT(0) NOT NULL")
    assertEquals(SMALLINT[Short](0).UNSIGNED.queryString, "SMALLINT(0) UNSIGNED NOT NULL")
    assertEquals(SMALLINT[Short](0).ZEROFILL.queryString, "SMALLINT(0) ZEROFILL NOT NULL")
    assertEquals(SMALLINT[Short](0).UNSIGNED.ZEROFILL.queryString, "SMALLINT(0) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(SMALLINT[Short](255).DEFAULT(1).queryString, "SMALLINT(255) NOT NULL DEFAULT 1")
    assertEquals(SMALLINT[Short](255).DEFAULT(1).UNSIGNED.queryString, "SMALLINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(SMALLINT[Short](255).DEFAULT(1).ZEROFILL.queryString, "SMALLINT(255) ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(
      SMALLINT[Short](255)
        .DEFAULT(1)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "SMALLINT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assertEquals(SMALLINT[Option[Short]](0).queryString, "SMALLINT(0) NULL")
    assertEquals(SMALLINT[Option[Short]](0).UNSIGNED.queryString, "SMALLINT(0) UNSIGNED NULL")
    assertEquals(SMALLINT[Option[Short]](0).ZEROFILL.queryString, "SMALLINT(0) ZEROFILL NULL")
    assertEquals(SMALLINT[Option[Short]](0).UNSIGNED.ZEROFILL.queryString, "SMALLINT(0) UNSIGNED ZEROFILL NULL")
    assertEquals(SMALLINT[Option[Short]](255).DEFAULT(None).queryString, "SMALLINT(255) NULL DEFAULT NULL")
    assertEquals(SMALLINT[Option[Short]](255).DEFAULT(Some(2)).queryString, "SMALLINT(255) NULL DEFAULT 2")
    assertEquals(
      SMALLINT[Option[Short]](255).DEFAULT(None).UNSIGNED.queryString,
      "SMALLINT(255) UNSIGNED NULL DEFAULT NULL"
    )
    assertEquals(
      SMALLINT[Option[Short]](255).DEFAULT(None).ZEROFILL.queryString,
      "SMALLINT(255) ZEROFILL NULL DEFAULT NULL"
    )
    assertEquals(
      SMALLINT[Option[Short]](255)
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "SMALLINT(255) UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  test("The query string generated from the Mediumint DataType model matches the specified one.") {
    val mediumintType = DataType.Mediumint[Int](None, false, false, false, None)
    assertEquals(mediumintType.typeName, "MEDIUMINT")
    assertEquals(mediumintType.length, None)
    assertEquals(mediumintType.sqlType, Types.INTEGER)
    assertEquals(mediumintType.isOptional, false)
    assertEquals(mediumintType.queryString, "MEDIUMINT NOT NULL")
    assertEquals(mediumintType.toOption.isOptional, true)
    assertEquals(mediumintType.toOption.sqlType, Types.INTEGER)
    assertEquals(mediumintType.toOption.queryString, "MEDIUMINT NULL")
    assertEquals(mediumintType.DEFAULT(1).queryString, "MEDIUMINT NOT NULL DEFAULT 1")
    assertEquals(mediumintType.UNSIGNED.queryString, "MEDIUMINT UNSIGNED NOT NULL")
    assertEquals(mediumintType.ZEROFILL.queryString, "MEDIUMINT ZEROFILL NOT NULL")
    assertEquals(mediumintType.UNSIGNED.ZEROFILL.queryString, "MEDIUMINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(mediumintType.ZEROFILL.UNSIGNED.queryString, "MEDIUMINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(MEDIUMINT[Int](0).queryString, "MEDIUMINT(0) NOT NULL")
    assertEquals(MEDIUMINT[Int](0).UNSIGNED.queryString, "MEDIUMINT(0) UNSIGNED NOT NULL")
    assertEquals(MEDIUMINT[Int](0).ZEROFILL.queryString, "MEDIUMINT(0) ZEROFILL NOT NULL")
    assertEquals(MEDIUMINT[Int](0).UNSIGNED.ZEROFILL.queryString, "MEDIUMINT(0) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(MEDIUMINT[Int](255).DEFAULT(1).queryString, "MEDIUMINT(255) NOT NULL DEFAULT 1")
    assertEquals(MEDIUMINT[Int](255).DEFAULT(1).UNSIGNED.queryString, "MEDIUMINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(MEDIUMINT[Int](255).DEFAULT(1).ZEROFILL.queryString, "MEDIUMINT(255) ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(
      MEDIUMINT[Int](255)
        .DEFAULT(1)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "MEDIUMINT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assertEquals(MEDIUMINT[Option[Int]](0).queryString, "MEDIUMINT(0) NULL")
    assertEquals(MEDIUMINT[Option[Int]](0).UNSIGNED.queryString, "MEDIUMINT(0) UNSIGNED NULL")
    assertEquals(MEDIUMINT[Option[Int]](0).ZEROFILL.queryString, "MEDIUMINT(0) ZEROFILL NULL")
    assertEquals(MEDIUMINT[Option[Int]](0).UNSIGNED.ZEROFILL.queryString, "MEDIUMINT(0) UNSIGNED ZEROFILL NULL")
    assertEquals(MEDIUMINT[Option[Int]](255).DEFAULT(None).queryString, "MEDIUMINT(255) NULL DEFAULT NULL")
    assertEquals(MEDIUMINT[Option[Int]](255).DEFAULT(Some(2)).queryString, "MEDIUMINT(255) NULL DEFAULT 2")
    assertEquals(MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString, "MEDIUMINT UNSIGNED NULL DEFAULT NULL")
    assertEquals(MEDIUMINT[Option[Int]].DEFAULT(None).ZEROFILL.queryString, "MEDIUMINT ZEROFILL NULL DEFAULT NULL")
    assertEquals(
      MEDIUMINT[Option[Int]]
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "MEDIUMINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assertEquals(MEDIUMINT[Int].queryString, "MEDIUMINT NOT NULL")
    assertEquals(MEDIUMINT[Int].UNSIGNED.queryString, "MEDIUMINT UNSIGNED NOT NULL")
    assertEquals(MEDIUMINT[Int].ZEROFILL.queryString, "MEDIUMINT ZEROFILL NOT NULL")
    assertEquals(MEDIUMINT[Int].UNSIGNED.ZEROFILL.queryString, "MEDIUMINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(MEDIUMINT[Int].DEFAULT(1).queryString, "MEDIUMINT NOT NULL DEFAULT 1")
    assertEquals(MEDIUMINT[Int].DEFAULT(1).UNSIGNED.queryString, "MEDIUMINT UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(MEDIUMINT[Int].DEFAULT(1).ZEROFILL.queryString, "MEDIUMINT ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(
      MEDIUMINT[Int].DEFAULT(1).UNSIGNED.ZEROFILL.queryString,
      "MEDIUMINT UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assertEquals(MEDIUMINT[Option[Int]].queryString, "MEDIUMINT NULL")
    assertEquals(MEDIUMINT[Option[Int]].UNSIGNED.queryString, "MEDIUMINT UNSIGNED NULL")
    assertEquals(MEDIUMINT[Option[Int]].ZEROFILL.queryString, "MEDIUMINT ZEROFILL NULL")
    assertEquals(MEDIUMINT[Option[Int]].UNSIGNED.ZEROFILL.queryString, "MEDIUMINT UNSIGNED ZEROFILL NULL")
    assertEquals(MEDIUMINT[Option[Int]].DEFAULT(None).queryString, "MEDIUMINT NULL DEFAULT NULL")
    assertEquals(MEDIUMINT[Option[Int]].DEFAULT(Some(2)).queryString, "MEDIUMINT NULL DEFAULT 2")
    assertEquals(MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString, "MEDIUMINT UNSIGNED NULL DEFAULT NULL")
    assertEquals(MEDIUMINT[Option[Int]].DEFAULT(None).ZEROFILL.queryString, "MEDIUMINT ZEROFILL NULL DEFAULT NULL")
    assertEquals(
      MEDIUMINT[Option[Int]]
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "MEDIUMINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  test("The query string generated from the Integer DataType model matches the specified one.") {
    val integerType = DataType.Integer[Int](None, false, false, false, None)
    assertEquals(integerType.typeName, "INT")
    assertEquals(integerType.length, None)
    assertEquals(integerType.sqlType, Types.INTEGER)
    assertEquals(integerType.isOptional, false)
    assertEquals(integerType.queryString, "INT NOT NULL")
    assertEquals(integerType.toOption.isOptional, true)
    assertEquals(integerType.toOption.sqlType, Types.INTEGER)
    assertEquals(integerType.toOption.queryString, "INT NULL")
    assertEquals(integerType.DEFAULT(1).queryString, "INT NOT NULL DEFAULT 1")
    assertEquals(integerType.UNSIGNED.queryString, "INT UNSIGNED NOT NULL")
    assertEquals(integerType.ZEROFILL.queryString, "INT ZEROFILL NOT NULL")
    assertEquals(integerType.UNSIGNED.ZEROFILL.queryString, "INT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(integerType.ZEROFILL.UNSIGNED.queryString, "INT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(INT[Int](0).queryString, "INT(0) NOT NULL")
    assertEquals(INT[Int](0).UNSIGNED.queryString, "INT(0) UNSIGNED NOT NULL")
    assertEquals(INT[Int](0).ZEROFILL.queryString, "INT(0) ZEROFILL NOT NULL")
    assertEquals(INT[Int](0).UNSIGNED.ZEROFILL.queryString, "INT(0) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(INT[Int](255).DEFAULT(1).queryString, "INT(255) NOT NULL DEFAULT 1")
    assertEquals(INT[Int](255).DEFAULT(1).UNSIGNED.queryString, "INT(255) UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(INT[Int](255).DEFAULT(1).ZEROFILL.queryString, "INT(255) ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(
      INT[Int](255).DEFAULT(1).UNSIGNED.ZEROFILL.queryString,
      "INT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assertEquals(INT[Option[Int]](0).queryString, "INT(0) NULL")
    assertEquals(INT[Option[Int]](0).UNSIGNED.queryString, "INT(0) UNSIGNED NULL")
    assertEquals(INT[Option[Int]](0).ZEROFILL.queryString, "INT(0) ZEROFILL NULL")
    assertEquals(INT[Option[Int]](0).UNSIGNED.ZEROFILL.queryString, "INT(0) UNSIGNED ZEROFILL NULL")
    assertEquals(INT[Option[Int]](255).DEFAULT(None).queryString, "INT(255) NULL DEFAULT NULL")
    assertEquals(INT[Option[Int]](255).DEFAULT(Some(2)).queryString, "INT(255) NULL DEFAULT 2")
    assertEquals(INT[Option[Int]](255).DEFAULT(None).UNSIGNED.queryString, "INT(255) UNSIGNED NULL DEFAULT NULL")
    assertEquals(INT[Option[Int]](255).DEFAULT(None).ZEROFILL.queryString, "INT(255) ZEROFILL NULL DEFAULT NULL")
    assertEquals(
      INT[Option[Int]](255)
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "INT(255) UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assertEquals(INT[Int].queryString, "INT NOT NULL")
    assertEquals(INT[Int].UNSIGNED.queryString, "INT UNSIGNED NOT NULL")
    assertEquals(INT[Int].ZEROFILL.queryString, "INT ZEROFILL NOT NULL")
    assertEquals(INT[Int].UNSIGNED.ZEROFILL.queryString, "INT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(INT[Int].DEFAULT(1).queryString, "INT NOT NULL DEFAULT 1")
    assertEquals(INT[Int].DEFAULT(1).UNSIGNED.queryString, "INT UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(INT[Int].DEFAULT(1).ZEROFILL.queryString, "INT ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(INT[Int].DEFAULT(1).UNSIGNED.ZEROFILL.queryString, "INT UNSIGNED ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(INT[Option[Int]].queryString, "INT NULL")
    assertEquals(INT[Option[Int]].UNSIGNED.queryString, "INT UNSIGNED NULL")
    assertEquals(INT[Option[Int]].ZEROFILL.queryString, "INT ZEROFILL NULL")
    assertEquals(INT[Option[Int]].UNSIGNED.ZEROFILL.queryString, "INT UNSIGNED ZEROFILL NULL")
    assertEquals(INT[Option[Int]].DEFAULT(None).queryString, "INT NULL DEFAULT NULL")
    assertEquals(INT[Option[Int]].DEFAULT(Some(2)).queryString, "INT NULL DEFAULT 2")
    assertEquals(INT[Option[Int]].DEFAULT(None).UNSIGNED.queryString, "INT UNSIGNED NULL DEFAULT NULL")
    assertEquals(INT[Option[Int]].DEFAULT(None).ZEROFILL.queryString, "INT ZEROFILL NULL DEFAULT NULL")
    assertEquals(
      INT[Option[Int]].DEFAULT(None).UNSIGNED.ZEROFILL.queryString,
      "INT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  test("The query string generated from the Bigint DataType model matches the specified one.") {
    val bigintType = DataType.Bigint[Long](None, false, false, false, None)
    assertEquals(bigintType.typeName, "BIGINT")
    assertEquals(bigintType.length, None)
    assertEquals(bigintType.sqlType, Types.BIGINT)
    assertEquals(bigintType.isOptional, false)
    assertEquals(bigintType.queryString, "BIGINT NOT NULL")
    assertEquals(bigintType.toOption.isOptional, true)
    assertEquals(bigintType.toOption.sqlType, Types.BIGINT)
    assertEquals(bigintType.toOption.queryString, "BIGINT NULL")
    assertEquals(bigintType.DEFAULT(1L).queryString, "BIGINT NOT NULL DEFAULT 1")
    assertEquals(bigintType.UNSIGNED.queryString, "BIGINT UNSIGNED NOT NULL")
    assertEquals(bigintType.ZEROFILL.queryString, "BIGINT ZEROFILL NOT NULL")
    assertEquals(bigintType.UNSIGNED.ZEROFILL.queryString, "BIGINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(bigintType.ZEROFILL.UNSIGNED.queryString, "BIGINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(BIGINT[Long](0).queryString, "BIGINT(0) NOT NULL")
    assertEquals(BIGINT[Long](0).UNSIGNED.queryString, "BIGINT(0) UNSIGNED NOT NULL")
    assertEquals(BIGINT[Long](0).ZEROFILL.queryString, "BIGINT(0) ZEROFILL NOT NULL")
    assertEquals(BIGINT[Long](0).UNSIGNED.ZEROFILL.queryString, "BIGINT(0) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(BIGINT[Long](255).DEFAULT(1).queryString, "BIGINT(255) NOT NULL DEFAULT 1")
    assertEquals(BIGINT[Long](255).DEFAULT(1).UNSIGNED.queryString, "BIGINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(BIGINT[Long](255).DEFAULT(1).ZEROFILL.queryString, "BIGINT(255) ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(
      BIGINT[Long](255).DEFAULT(1).UNSIGNED.ZEROFILL.queryString,
      "BIGINT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assertEquals(BIGINT[Option[Long]](0).queryString, "BIGINT(0) NULL")
    assertEquals(BIGINT[Option[Long]](0).UNSIGNED.queryString, "BIGINT(0) UNSIGNED NULL")
    assertEquals(BIGINT[Option[Long]](0).ZEROFILL.queryString, "BIGINT(0) ZEROFILL NULL")
    assertEquals(BIGINT[Option[Long]](0).UNSIGNED.ZEROFILL.queryString, "BIGINT(0) UNSIGNED ZEROFILL NULL")
    assertEquals(BIGINT[Option[Long]](255).DEFAULT(None).queryString, "BIGINT(255) NULL DEFAULT NULL")
    assertEquals(BIGINT[Option[Long]](255).DEFAULT(Some(2)).queryString, "BIGINT(255) NULL DEFAULT 2")
    assertEquals(BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString, "BIGINT UNSIGNED NULL DEFAULT NULL")
    assertEquals(BIGINT[Option[Long]].DEFAULT(None).ZEROFILL.queryString, "BIGINT ZEROFILL NULL DEFAULT NULL")
    assertEquals(
      BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.ZEROFILL.queryString,
      "BIGINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assertEquals(BIGINT[Long].queryString, "BIGINT NOT NULL")
    assertEquals(BIGINT[Long].UNSIGNED.queryString, "BIGINT UNSIGNED NOT NULL")
    assertEquals(BIGINT[Long].ZEROFILL.queryString, "BIGINT ZEROFILL NOT NULL")
    assertEquals(BIGINT[Long].UNSIGNED.ZEROFILL.queryString, "BIGINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(BIGINT[Long].DEFAULT(1).queryString, "BIGINT NOT NULL DEFAULT 1")
    assertEquals(BIGINT[Long].DEFAULT(1).UNSIGNED.queryString, "BIGINT UNSIGNED NOT NULL DEFAULT 1")
    assertEquals(BIGINT[Long].DEFAULT(1).ZEROFILL.queryString, "BIGINT ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(BIGINT[Long].DEFAULT(1).UNSIGNED.ZEROFILL.queryString, "BIGINT UNSIGNED ZEROFILL NOT NULL DEFAULT 1")
    assertEquals(BIGINT[Option[Long]].queryString, "BIGINT NULL")
    assertEquals(BIGINT[Option[Long]].UNSIGNED.queryString, "BIGINT UNSIGNED NULL")
    assertEquals(BIGINT[Option[Long]].ZEROFILL.queryString, "BIGINT ZEROFILL NULL")
    assertEquals(BIGINT[Option[Long]].UNSIGNED.ZEROFILL.queryString, "BIGINT UNSIGNED ZEROFILL NULL")
    assertEquals(BIGINT[Option[Long]].DEFAULT(None).queryString, "BIGINT NULL DEFAULT NULL")
    assertEquals(BIGINT[Option[Long]].DEFAULT(Some(2)).queryString, "BIGINT NULL DEFAULT 2")
    assertEquals(BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString, "BIGINT UNSIGNED NULL DEFAULT NULL")
    assertEquals(BIGINT[Option[Long]].DEFAULT(None).ZEROFILL.queryString, "BIGINT ZEROFILL NULL DEFAULT NULL")
    assertEquals(
      BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.ZEROFILL.queryString,
      "BIGINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  test("The query string generated from the Decimal DataType model matches the specified one.") {
    val decimalType = DataType.Decimal[BigDecimal](10, 5, false, false, false, None)
    assertEquals(decimalType.typeName, "DECIMAL(10, 5)")
    assertEquals(decimalType.sqlType, Types.DECIMAL)
    assertEquals(decimalType.isOptional, false)
    assertEquals(decimalType.queryString, "DECIMAL(10, 5) NOT NULL")
    assertEquals(decimalType.toOption.isOptional, true)
    assertEquals(decimalType.toOption.sqlType, Types.DECIMAL)
    assertEquals(decimalType.toOption.queryString, "DECIMAL(10, 5) NULL")
    assertEquals(decimalType.DEFAULT(BigDecimal(1.5)).queryString, "DECIMAL(10, 5) NOT NULL DEFAULT '1.5'")
    assertEquals(decimalType.UNSIGNED.queryString, "DECIMAL(10, 5) UNSIGNED NOT NULL")
    assertEquals(decimalType.ZEROFILL.queryString, "DECIMAL(10, 5) ZEROFILL NOT NULL")
    assertEquals(decimalType.UNSIGNED.ZEROFILL.queryString, "DECIMAL(10, 5) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(decimalType.ZEROFILL.UNSIGNED.queryString, "DECIMAL(10, 5) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(DECIMAL[BigDecimal](10, 7).queryString, "DECIMAL(10, 7) NOT NULL")
    assertEquals(DECIMAL[BigDecimal](10, 7).UNSIGNED.queryString, "DECIMAL(10, 7) UNSIGNED NOT NULL")
    assertEquals(DECIMAL[BigDecimal](10, 7).ZEROFILL.queryString, "DECIMAL(10, 7) ZEROFILL NOT NULL")
    assertEquals(DECIMAL[BigDecimal](10, 7).UNSIGNED.ZEROFILL.queryString, "DECIMAL(10, 7) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .queryString,
      "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'"
    )
    assertEquals(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .UNSIGNED
        .queryString,
      "DECIMAL(10, 7) UNSIGNED NOT NULL DEFAULT '0.0000010'"
    )
    assertEquals(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .ZEROFILL
        .queryString,
      "DECIMAL(10, 7) ZEROFILL NOT NULL DEFAULT '0.0000010'"
    )
    assertEquals(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "DECIMAL(10, 7) UNSIGNED ZEROFILL NOT NULL DEFAULT '0.0000010'"
    )
    assertEquals(DECIMAL[Option[BigDecimal]](10, 7).queryString, "DECIMAL(10, 7) NULL")
    assertEquals(DECIMAL[Option[BigDecimal]](10, 7).UNSIGNED.queryString, "DECIMAL(10, 7) UNSIGNED NULL")
    assertEquals(DECIMAL[Option[BigDecimal]](10, 7).ZEROFILL.queryString, "DECIMAL(10, 7) ZEROFILL NULL")
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7).UNSIGNED.ZEROFILL.queryString,
      "DECIMAL(10, 7) UNSIGNED ZEROFILL NULL"
    )
    assertEquals(DECIMAL[Option[BigDecimal]](10, 7).DEFAULT(None).queryString, "DECIMAL(10, 7) NULL DEFAULT NULL")
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(None)
        .UNSIGNED
        .queryString,
      "DECIMAL(10, 7) UNSIGNED NULL DEFAULT NULL"
    )
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(None)
        .ZEROFILL
        .queryString,
      "DECIMAL(10, 7) ZEROFILL NULL DEFAULT NULL"
    )
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "DECIMAL(10, 7) UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .queryString,
      "DECIMAL(10, 7) NULL DEFAULT '0.0000010'"
    )
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .UNSIGNED
        .queryString,
      "DECIMAL(10, 7) UNSIGNED NULL DEFAULT '0.0000010'"
    )
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .ZEROFILL
        .queryString,
      "DECIMAL(10, 7) ZEROFILL NULL DEFAULT '0.0000010'"
    )
    assertEquals(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .UNSIGNED
        .ZEROFILL
        .queryString,
      "DECIMAL(10, 7) UNSIGNED ZEROFILL NULL DEFAULT '0.0000010'"
    )
  }

  test("The query string generated from the Char DataType model matches the specified one.") {
    val charType = DataType.CChar[String](10, false, None, None, None)
    assertEquals(charType.typeName, "CHAR(10)")
    assertEquals(charType.sqlType, Types.CHAR)
    assertEquals(charType.isOptional, false)
    assertEquals(charType.queryString, "CHAR(10) NOT NULL")
    assertEquals(charType.toOption.isOptional, true)
    assertEquals(charType.toOption.sqlType, Types.CHAR)
    assertEquals(charType.toOption.queryString, "CHAR(10) NULL")
    assertEquals(charType.DEFAULT("test").queryString, "CHAR(10) NOT NULL DEFAULT 'test'")
    assertEquals(CHAR[String](0).queryString, "CHAR(0) NOT NULL")
    assertEquals(CHAR[Option[String]](0).queryString, "CHAR(0) NULL")
    assertEquals(CHAR[Option[String]](0).DEFAULT(None).queryString, "CHAR(0) NULL DEFAULT NULL")
    assertEquals(CHAR[Option[String]](0).DEFAULT(Some("test")).queryString, "CHAR(0) NULL DEFAULT 'test'")
  }

  test(
    "The query string generated from the Char DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val charType    = DataType.CChar[String](10, false, None, None, None)
    val withCharSet = charType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "CHAR(10) CHARACTER SET utf8mb4 NOT NULL")
    assertEquals(withCharSet.toOption.queryString, "CHAR(10) CHARACTER SET utf8mb4 NULL")

    val withCollate = charType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "CHAR(10) COLLATE utf8mb4_bin NOT NULL")
    assertEquals(withCollate.toOption.queryString, "CHAR(10) COLLATE utf8mb4_bin NULL")

    val withBoth = charType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "CHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    assertEquals(withBoth.toOption.queryString, "CHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL")

    val withDefault = withBoth.DEFAULT("test")
    assertEquals(withDefault.queryString, "CHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'test'")

    val optionalWithBoth = CHAR[Option[String]](20).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(optionalWithBoth.queryString, "CHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")

    val optionalWithDefault = optionalWithBoth.DEFAULT(Some("value"))
    assertEquals(
      optionalWithDefault.queryString,
      "CHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'value'"
    )
  }

  test(
    "The query string generated from the Varchar DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val varcharType = DataType.Varchar[String](50, false, None, None, None)
    val withCharSet = varcharType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL")

    val withCollate = varcharType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "VARCHAR(50) COLLATE utf8mb4_bin NOT NULL")

    val withBoth = varcharType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")

    val withDefault = withBoth.DEFAULT("test")
    assertEquals(
      withDefault.queryString,
      "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'test'"
    )

    val optionalWithBoth =
      VARCHAR[Option[String]](100).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(optionalWithBoth.queryString, "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
  }

  test(
    "The query string generated from the Binary DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val binaryType  = DataType.Binary[Array[Byte]](10, false, None, None, None)
    val withCharSet = binaryType.CHARACTER_SET(Character.binary)
    assertEquals(withCharSet.queryString, "BINARY(10) CHARACTER SET binary NOT NULL")

    val withCollate = binaryType.COLLATE(Collate.binary)
    assertEquals(withCollate.queryString, "BINARY(10) COLLATE binary NOT NULL")

    val withBoth = binaryType.CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assertEquals(withBoth.queryString, "BINARY(10) CHARACTER SET binary COLLATE binary NOT NULL")

    val optionalWithBoth = BINARY[Option[Array[Byte]]](15).CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assertEquals(optionalWithBoth.queryString, "BINARY(15) CHARACTER SET binary COLLATE binary NULL")
  }

  test(
    "The query string generated from the Varbinary DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val varbinaryType = DataType.Varbinary[Array[Byte]](50, false, None, None, None)
    val withCharSet   = varbinaryType.CHARACTER_SET(Character.binary)
    assertEquals(withCharSet.queryString, "VARBINARY(50) CHARACTER SET binary NOT NULL")

    val withCollate = varbinaryType.COLLATE(Collate.binary)
    assertEquals(withCollate.queryString, "VARBINARY(50) COLLATE binary NOT NULL")

    val withBoth = varbinaryType.CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assertEquals(withBoth.queryString, "VARBINARY(50) CHARACTER SET binary COLLATE binary NOT NULL")

    val optionalWithBoth = VARBINARY[Option[Array[Byte]]](100).CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assertEquals(optionalWithBoth.queryString, "VARBINARY(100) CHARACTER SET binary COLLATE binary NULL")
  }

  test(
    "The query string generated from the TinyText DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val tinytextType = DataType.TinyText[String](false, None, None, None)
    val withCharSet  = tinytextType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "TINYTEXT CHARACTER SET utf8mb4 NOT NULL")

    val withCollate = tinytextType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "TINYTEXT COLLATE utf8mb4_bin NOT NULL")

    val withBoth = tinytextType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")

    val optionalWithBoth =
      TINYTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(optionalWithBoth.queryString, "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")

    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assertEquals(
      optionalWithDefault.queryString,
      "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL"
    )
  }

  test(
    "The query string generated from the Text DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val textType    = DataType.Text[String](false, None, None, None)
    val withCharSet = textType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "TEXT CHARACTER SET utf8mb4 NOT NULL")

    val withCollate = textType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "TEXT COLLATE utf8mb4_bin NOT NULL")

    val withBoth = textType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")

    val optionalWithBoth = TEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(optionalWithBoth.queryString, "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")

    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assertEquals(
      optionalWithDefault.queryString,
      "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL"
    )
  }

  test(
    "The query string generated from the MediumText DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val mediumtextType = DataType.MediumText[String](false, None, None, None)
    val withCharSet    = mediumtextType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "MEDIUMTEXT CHARACTER SET utf8mb4 NOT NULL")

    val withCollate = mediumtextType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "MEDIUMTEXT COLLATE utf8mb4_bin NOT NULL")

    val withBoth = mediumtextType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")

    val optionalWithBoth =
      MEDIUMTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(optionalWithBoth.queryString, "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")

    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assertEquals(
      optionalWithDefault.queryString,
      "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL"
    )
  }

  test(
    "The query string generated from the LongText DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    val longtextType = DataType.LongText[String](false, None, None, None)
    val withCharSet  = longtextType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "LONGTEXT CHARACTER SET utf8mb4 NOT NULL")

    val withCollate = longtextType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "LONGTEXT COLLATE utf8mb4_bin NOT NULL")

    val withBoth = longtextType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")

    val optionalWithBoth =
      LONGTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(optionalWithBoth.queryString, "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")

    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assertEquals(
      optionalWithDefault.queryString,
      "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL"
    )
  }

  test("The query string generated from the Enum DataType model matches the specified one.") {
    enum Status:
      case Active, InActive

    val enumType = DataType.Enum[Status](List("Active", "InActive"), false, None, None, None)
    assertEquals(enumType.typeName, "ENUM('Active','InActive')")
    assertEquals(enumType.sqlType, Types.CHAR)
    assertEquals(enumType.isOptional, false)
    assertEquals(enumType.queryString, "ENUM('Active','InActive') NOT NULL")
    assertEquals(enumType.toOption.isOptional, true)
    assertEquals(enumType.toOption.sqlType, Types.CHAR)
    assertEquals(enumType.toOption.queryString, "ENUM('Active','InActive') NULL")
    assertEquals(enumType.DEFAULT(Status.Active).queryString, "ENUM('Active','InActive') NOT NULL DEFAULT 'Active'")
    assertEquals(ENUM[Status].queryString, "ENUM('Active','InActive') NOT NULL")
    assertEquals(
      ENUM[Status]
        .DEFAULT(Status.Active)
        .queryString,
      "ENUM('Active','InActive') NOT NULL DEFAULT 'Active'"
    )
    assertEquals(ENUM[Option[Status]].queryString, "ENUM('Active','InActive') NULL")
    assertEquals(
      ENUM[Option[Status]].DEFAULT(None).queryString,
      "ENUM('Active','InActive') NULL DEFAULT NULL"
    )
  }

  test(
    "The query string generated from the Enum DataType model with CHARACTER_SET and COLLATE matches the specified one."
  ) {
    enum Status:
      case Active, InActive

    val enumType    = DataType.Enum[Status](List("Active", "InActive"), false, None, None, None)
    val withCharSet = enumType.CHARACTER_SET(Character.utf8mb4)
    assertEquals(withCharSet.queryString, "ENUM('Active','InActive') CHARACTER SET utf8mb4 NOT NULL")

    val withCollate = enumType.COLLATE(Collate.utf8mb4_bin)
    assertEquals(withCollate.queryString, "ENUM('Active','InActive') COLLATE utf8mb4_bin NOT NULL")

    val withBoth = enumType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assertEquals(withBoth.queryString, "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")

    val withDefault = withBoth.DEFAULT(Status.Active)
    assertEquals(
      withDefault.queryString,
      "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'Active'"
    )

    val optionalEnumType = DataType.Enum[Option[Status]](List("Active", "InActive"), true, None, None, None)
    val optionalWithBoth = optionalEnumType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assertEquals(
      optionalWithBoth.queryString,
      "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
    )

    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assertEquals(
      optionalWithDefault.queryString,
      "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL"
    )
  }

  test("The query string generated from the Date DataType model matches the specified one.") {
    val dateType = DataType.Date[LocalDate](false, None)
    assertEquals(dateType.typeName, "DATE")
    assertEquals(dateType.sqlType, Types.DATE)
    assertEquals(dateType.isOptional, false)
    assertEquals(dateType.queryString, "DATE NOT NULL")
    assertEquals(dateType.toOption.isOptional, true)
    assertEquals(dateType.toOption.sqlType, Types.DATE)
    assertEquals(dateType.toOption.queryString, "DATE NULL")
    assertEquals(dateType.DEFAULT(LocalDate.of(2023, 2, 10)).queryString, "DATE NOT NULL DEFAULT '2023-02-10'")
    assertEquals(dateType.DEFAULT_CURRENT_DATE().queryString, "DATE NOT NULL DEFAULT (CURRENT_DATE)")
    assertEquals(DATE[LocalDate].queryString, "DATE NOT NULL")
    assertEquals(
      DATE[LocalDate]
        .DEFAULT(LocalDate.of(2023, 2, 10))
        .queryString,
      "DATE NOT NULL DEFAULT '2023-02-10'"
    )
    assertEquals(DATE[LocalDate].DEFAULT(0).queryString, "DATE NOT NULL DEFAULT 0")
    assertEquals(DATE[LocalDate].DEFAULT("2023-02-10").queryString, "DATE NOT NULL DEFAULT '2023-02-10'")
    assertEquals(DATE[LocalDate].DEFAULT_CURRENT_DATE().queryString, "DATE NOT NULL DEFAULT (CURRENT_DATE)")
    assertEquals(DATE[Option[LocalDate]].queryString, "DATE NULL")
    assertEquals(DATE[Option[LocalDate]].DEFAULT(None).queryString, "DATE NULL DEFAULT NULL")
    assertEquals(
      DATE[Option[LocalDate]]
        .DEFAULT(Some(LocalDate.of(2023, 2, 10)))
        .queryString,
      "DATE NULL DEFAULT '2023-02-10'"
    )
    assertEquals(DATE[Option[LocalDate]].DEFAULT(0).queryString, "DATE NULL DEFAULT 0")
    assertEquals(DATE[Option[LocalDate]].DEFAULT("2023-02-10").queryString, "DATE NULL DEFAULT '2023-02-10'")
    assertEquals(DATE[Option[LocalDate]].DEFAULT_CURRENT_DATE().queryString, "DATE NULL DEFAULT (CURRENT_DATE)")
  }

  test("The query string generated from the DateTime DataType model matches the specified one.") {
    val dateTimeType = DataType.DateTime[LocalDateTime](None, false, None)
    assertEquals(dateTimeType.typeName, "DATETIME")
    assertEquals(dateTimeType.sqlType, Types.TIMESTAMP)
    assertEquals(dateTimeType.isOptional, false)
    assertEquals(dateTimeType.queryString, "DATETIME NOT NULL")
    assertEquals(dateTimeType.toOption.isOptional, true)
    assertEquals(dateTimeType.toOption.sqlType, Types.TIMESTAMP)
    assertEquals(dateTimeType.toOption.queryString, "DATETIME NULL")
    assertEquals(
      dateTimeType
        .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
        .queryString,
      "DATETIME NOT NULL DEFAULT '2023-02-10T10:00'"
    )
    assertEquals(dateTimeType.DEFAULT_CURRENT_TIMESTAMP().queryString, "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP")
    assertEquals(
      dateTimeType
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString,
      "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assertEquals(DATETIME[LocalDateTime].queryString, "DATETIME NOT NULL")
    assertEquals(DATETIME[LocalDateTime](6).queryString, "DATETIME(6) NOT NULL")
    assertEquals(
      DATETIME[LocalDateTime]
        .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
        .queryString,
      "DATETIME NOT NULL DEFAULT '2023-02-10T10:00'"
    )
    assertEquals(DATETIME[LocalDateTime].DEFAULT(0).queryString, "DATETIME NOT NULL DEFAULT 0")
    assertEquals(
      DATETIME[LocalDateTime]
        .DEFAULT("2023-02-10 10:00:00")
        .queryString,
      "DATETIME NOT NULL DEFAULT '2023-02-10 10:00:00'"
    )
    assertEquals(DATETIME[Option[LocalDateTime]].queryString, "DATETIME NULL")
    assertEquals(DATETIME[Option[LocalDateTime]](6).queryString, "DATETIME(6) NULL")
    assertEquals(DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString, "DATETIME NULL DEFAULT NULL")
    assertEquals(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
        .queryString,
      "DATETIME NULL DEFAULT '2023-02-10T10:00'"
    )
    assertEquals(DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString, "DATETIME NULL DEFAULT NULL")
    assertEquals(DATETIME[Option[LocalDateTime]].DEFAULT(0).queryString, "DATETIME NULL DEFAULT 0")
    assertEquals(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT("2023-02-10 10:00:00")
        .queryString,
      "DATETIME NULL DEFAULT '2023-02-10 10:00:00'"
    )
    assertEquals(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString,
      "DATETIME NULL DEFAULT CURRENT_TIMESTAMP"
    )
    assertEquals(
      DATETIME[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString,
      "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)"
    )
    assertEquals(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString,
      "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assertEquals(
      DATETIME[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString,
      "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
    )
  }

  test("The query string generated from the TimeStamp DataType model matches the specified one.") {
    val timeStampType = DataType.TimeStamp[LocalDateTime](None, false, None)
    assertEquals(timeStampType.typeName, "TIMESTAMP")
    assertEquals(timeStampType.sqlType, Types.TIMESTAMP)
    assertEquals(timeStampType.isOptional, false)
    assertEquals(timeStampType.queryString, "TIMESTAMP NOT NULL")
    assertEquals(timeStampType.toOption.isOptional, true)
    assertEquals(timeStampType.toOption.sqlType, Types.TIMESTAMP)
    assertEquals(timeStampType.toOption.queryString, "TIMESTAMP NULL")
    assertEquals(
      timeStampType
        .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
        .queryString,
      "TIMESTAMP NOT NULL DEFAULT '2023-02-10T10:00'"
    )
    assertEquals(timeStampType.DEFAULT_CURRENT_TIMESTAMP().queryString, "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    assertEquals(
      timeStampType
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString,
      "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assertEquals(TIMESTAMP[LocalDateTime].queryString, "TIMESTAMP NOT NULL")
    assertEquals(TIMESTAMP[LocalDateTime](6).queryString, "TIMESTAMP(6) NOT NULL")
    assertEquals(
      TIMESTAMP[LocalDateTime]
        .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
        .queryString,
      "TIMESTAMP NOT NULL DEFAULT '2023-02-10T10:00'"
    )
    assertEquals(TIMESTAMP[LocalDateTime].DEFAULT(0).queryString, "TIMESTAMP NOT NULL DEFAULT 0")
    assertEquals(
      TIMESTAMP[LocalDateTime]
        .DEFAULT("2023-02-10 10:00:00")
        .queryString,
      "TIMESTAMP NOT NULL DEFAULT '2023-02-10 10:00:00'"
    )
    assertEquals(TIMESTAMP[Option[LocalDateTime]].queryString, "TIMESTAMP NULL")
    assertEquals(TIMESTAMP[Option[LocalDateTime]](5).queryString, "TIMESTAMP(5) NULL")
    assertEquals(TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString, "TIMESTAMP NULL DEFAULT NULL")
    assertEquals(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
        .queryString,
      "TIMESTAMP NULL DEFAULT '2023-02-10T10:00'"
    )
    assertEquals(TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString, "TIMESTAMP NULL DEFAULT NULL")
    assertEquals(TIMESTAMP[Option[LocalDateTime]].DEFAULT(0).queryString, "TIMESTAMP NULL DEFAULT 0")
    assertEquals(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT("2023-02-10 10:00:00")
        .queryString,
      "TIMESTAMP NULL DEFAULT '2023-02-10 10:00:00'"
    )
    assertEquals(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString,
      "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"
    )
    assertEquals(
      TIMESTAMP[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString,
      "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6)"
    )
    assertEquals(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString,
      "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assertEquals(
      TIMESTAMP[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString,
      "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
    )
  }

  test("The query string generated from the Time DataType model matches the specified one.") {
    val timeType = DataType.Time[LocalTime](None, false, None)
    assertEquals(timeType.typeName, "TIME")
    assertEquals(timeType.sqlType, Types.TIME)
    assertEquals(timeType.isOptional, false)
    assertEquals(timeType.queryString, "TIME NOT NULL")
    assertEquals(timeType.toOption.isOptional, true)
    assertEquals(timeType.toOption.sqlType, Types.TIME)
    assertEquals(timeType.toOption.queryString, "TIME NULL")
    assertEquals(timeType.DEFAULT(LocalTime.of(10, 10, 0)).queryString, "TIME NOT NULL DEFAULT '10:10'")
    assertEquals(TIME[LocalTime].queryString, "TIME NOT NULL")
    assertEquals(TIME[LocalTime].DEFAULT(LocalTime.of(10, 0, 10)).queryString, "TIME NOT NULL DEFAULT '10:00:10'")
    assertEquals(TIME[LocalTime].DEFAULT(0).queryString, "TIME NOT NULL DEFAULT 0")
    assertEquals(TIME[LocalTime].DEFAULT("23:59:59").queryString, "TIME NOT NULL DEFAULT '23:59:59'")
    assertEquals(TIME[Option[LocalTime]].queryString, "TIME NULL")
    assertEquals(TIME[Option[LocalTime]].DEFAULT(None).queryString, "TIME NULL DEFAULT NULL")
    assertEquals(
      TIME[Option[LocalTime]]
        .DEFAULT(Some(LocalTime.of(10, 0, 0)))
        .queryString,
      "TIME NULL DEFAULT '10:00'"
    )
    assertEquals(TIME[Option[LocalTime]].DEFAULT(None).queryString, "TIME NULL DEFAULT NULL")
    assertEquals(TIME[Option[LocalTime]].DEFAULT(0).queryString, "TIME NULL DEFAULT 0")
    assertEquals(TIME[Option[LocalTime]].DEFAULT("23:59:59").queryString, "TIME NULL DEFAULT '23:59:59'")
  }

  test("The query string generated from the Year DataType model matches the specified one.") {
    val yearType = DataType.Year[JYear](None, false, None)
    assertEquals(yearType.typeName, "YEAR")
    assertEquals(yearType.sqlType, Types.DATE)
    assertEquals(yearType.isOptional, false)
    assertEquals(yearType.queryString, "YEAR NOT NULL")
    assertEquals(yearType.toOption.isOptional, true)
    assertEquals(yearType.toOption.sqlType, Types.DATE)
    assertEquals(yearType.toOption.queryString, "YEAR NULL")
    assertEquals(yearType.DEFAULT(JYear.of(2023)).queryString, "YEAR NOT NULL DEFAULT '2023'")
    assertEquals(YEAR[JYear].queryString, "YEAR NOT NULL")
    assertEquals(YEAR[JYear].DEFAULT(JYear.of(2023)).queryString, "YEAR NOT NULL DEFAULT '2023'")
    assertEquals(YEAR[JYear].DEFAULT(0).queryString, "YEAR NOT NULL DEFAULT 0")
    assertEquals(YEAR[JYear].DEFAULT(2023).queryString, "YEAR NOT NULL DEFAULT 2023")
    assertEquals(YEAR[Option[JYear]].queryString, "YEAR NULL")
    assertEquals(YEAR[Option[JYear]].DEFAULT(None).queryString, "YEAR NULL DEFAULT NULL")
    assertEquals(YEAR[Option[JYear]].DEFAULT(Some(JYear.of(2023))).queryString, "YEAR NULL DEFAULT '2023'")
    assertEquals(YEAR[Option[JYear]].DEFAULT(0).queryString, "YEAR NULL DEFAULT 0")
    assertEquals(YEAR[Option[JYear]].DEFAULT(2023).queryString, "YEAR NULL DEFAULT 2023")
  }

  test("The query string generated from the Serial DataType model matches the specified one.") {
    val serialType = DataType.Alias.Serial[BigInt]()
    assertEquals(serialType.typeName, "SERIAL")
    assertEquals(serialType.sqlType, Types.BIGINT)
    assertEquals(serialType.isOptional, false)
    assertEquals(serialType.queryString, "SERIAL")
    intercept[UnsupportedOperationException] { serialType.toOption }
    assertEquals(SERIAL[BigInt].queryString, "SERIAL")
  }

  test("The query string generated from the Boolean DataType model matches the specified one.") {
    val boolType = DataType.Alias.Bool[Boolean](false, None)
    assertEquals(boolType.typeName, "BOOLEAN")
    assertEquals(boolType.sqlType, Types.BOOLEAN)
    assertEquals(boolType.isOptional, false)
    assertEquals(boolType.queryString, "BOOLEAN NOT NULL")
    assertEquals(boolType.toOption.isOptional, true)
    assertEquals(boolType.toOption.sqlType, Types.BOOLEAN)
    assertEquals(boolType.toOption.queryString, "BOOLEAN NULL")
    assertEquals(boolType.DEFAULT(true).queryString, "BOOLEAN NOT NULL DEFAULT true")
    assertEquals(BOOLEAN[Boolean].queryString, "BOOLEAN NOT NULL")
    assertEquals(BOOLEAN[Boolean].DEFAULT(true).queryString, "BOOLEAN NOT NULL DEFAULT true")
    assertEquals(BOOLEAN[Boolean].DEFAULT(false).queryString, "BOOLEAN NOT NULL DEFAULT false")
    assertEquals(BOOLEAN[Option[Boolean]].queryString, "BOOLEAN NULL")
    assertEquals(BOOLEAN[Option[Boolean]].DEFAULT(None).queryString, "BOOLEAN NULL DEFAULT NULL")
    assertEquals(BOOLEAN[Option[Boolean]].DEFAULT(Some(true)).queryString, "BOOLEAN NULL DEFAULT true")
  }
