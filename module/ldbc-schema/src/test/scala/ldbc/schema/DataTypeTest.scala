/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.time.{ LocalDate, LocalDateTime, LocalTime, Year as JYear }

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.sql.Types

import ldbc.schema.DataType.*

trait DataTypeTest extends AnyFlatSpec:

  it should "The query string generated from the Smallint DataType model matches the specified one." in {
    val smallintType = DataType.Smallint[Short](None, false, false, false, None)
    assert(smallintType.typeName === "SMALLINT")
    assert(smallintType.length === None)
    assert(smallintType.sqlType === Types.SMALLINT)
    assert(smallintType.isOptional === false)
    assert(smallintType.queryString === "SMALLINT NOT NULL")
    assert(smallintType.toOption.isOptional === true)
    assert(smallintType.toOption.sqlType === Types.SMALLINT)
    assert(smallintType.toOption.queryString === "SMALLINT NULL")
    assert(smallintType.DEFAULT(1.toShort).queryString === "SMALLINT NOT NULL DEFAULT 1")
    assert(smallintType.UNSIGNED.queryString === "SMALLINT UNSIGNED NOT NULL")
    assert(smallintType.ZEROFILL.queryString === "SMALLINT ZEROFILL NOT NULL")
    assert(smallintType.UNSIGNED.ZEROFILL.queryString === "SMALLINT UNSIGNED ZEROFILL NOT NULL")
    assert(smallintType.ZEROFILL.UNSIGNED.queryString === "SMALLINT UNSIGNED ZEROFILL NOT NULL")
    assert(SMALLINT[Short](0).queryString === "SMALLINT(0) NOT NULL")
    assert(SMALLINT[Short](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NOT NULL")
    assert(SMALLINT[Short](0).ZEROFILL.queryString === "SMALLINT(0) ZEROFILL NOT NULL")
    assert(SMALLINT[Short](0).UNSIGNED.ZEROFILL.queryString === "SMALLINT(0) UNSIGNED ZEROFILL NOT NULL")
    assert(SMALLINT[Short](255).DEFAULT(1).queryString === "SMALLINT(255) NOT NULL DEFAULT 1")
    assert(SMALLINT[Short](255).DEFAULT(1).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(SMALLINT[Short](255).DEFAULT(1).ZEROFILL.queryString === "SMALLINT(255) ZEROFILL NOT NULL DEFAULT 1")
    assert(
      SMALLINT[Short](255)
        .DEFAULT(1)
        .UNSIGNED
        .ZEROFILL
        .queryString === "SMALLINT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assert(SMALLINT[Option[Short]](0).queryString === "SMALLINT(0) NULL")
    assert(SMALLINT[Option[Short]](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NULL")
    assert(SMALLINT[Option[Short]](0).ZEROFILL.queryString === "SMALLINT(0) ZEROFILL NULL")
    assert(SMALLINT[Option[Short]](0).UNSIGNED.ZEROFILL.queryString === "SMALLINT(0) UNSIGNED ZEROFILL NULL")
    assert(SMALLINT[Option[Short]](255).DEFAULT(None).queryString === "SMALLINT(255) NULL DEFAULT NULL")
    assert(SMALLINT[Option[Short]](255).DEFAULT(Some(2)).queryString === "SMALLINT(255) NULL DEFAULT 2")
    assert(
      SMALLINT[Option[Short]](255).DEFAULT(None).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NULL DEFAULT NULL"
    )
    assert(
      SMALLINT[Option[Short]](255).DEFAULT(None).ZEROFILL.queryString === "SMALLINT(255) ZEROFILL NULL DEFAULT NULL"
    )
    assert(
      SMALLINT[Option[Short]](255)
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString === "SMALLINT(255) UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  it should "The query string generated from the Mediumint DataType model matches the specified one." in {
    val mediumintType = DataType.Mediumint[Int](None, false, false, false, None)
    assert(mediumintType.typeName === "MEDIUMINT")
    assert(mediumintType.length === None)
    assert(mediumintType.sqlType === Types.INTEGER)
    assert(mediumintType.isOptional === false)
    assert(mediumintType.queryString === "MEDIUMINT NOT NULL")
    assert(mediumintType.toOption.isOptional === true)
    assert(mediumintType.toOption.sqlType === Types.INTEGER)
    assert(mediumintType.toOption.queryString === "MEDIUMINT NULL")
    assert(mediumintType.DEFAULT(1).queryString === "MEDIUMINT NOT NULL DEFAULT 1")
    assert(mediumintType.UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL")
    assert(mediumintType.ZEROFILL.queryString === "MEDIUMINT ZEROFILL NOT NULL")
    assert(mediumintType.UNSIGNED.ZEROFILL.queryString === "MEDIUMINT UNSIGNED ZEROFILL NOT NULL")
    assert(mediumintType.ZEROFILL.UNSIGNED.queryString === "MEDIUMINT UNSIGNED ZEROFILL NOT NULL")
    assert(MEDIUMINT[Int](0).queryString === "MEDIUMINT(0) NOT NULL")
    assert(MEDIUMINT[Int](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NOT NULL")
    assert(MEDIUMINT[Int](0).ZEROFILL.queryString === "MEDIUMINT(0) ZEROFILL NOT NULL")
    assert(MEDIUMINT[Int](0).UNSIGNED.ZEROFILL.queryString === "MEDIUMINT(0) UNSIGNED ZEROFILL NOT NULL")
    assert(MEDIUMINT[Int](255).DEFAULT(1).queryString === "MEDIUMINT(255) NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int](255).DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int](255).DEFAULT(1).ZEROFILL.queryString === "MEDIUMINT(255) ZEROFILL NOT NULL DEFAULT 1")
    assert(
      MEDIUMINT[Int](255)
        .DEFAULT(1)
        .UNSIGNED
        .ZEROFILL
        .queryString === "MEDIUMINT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assert(MEDIUMINT[Option[Int]](0).queryString === "MEDIUMINT(0) NULL")
    assert(MEDIUMINT[Option[Int]](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NULL")
    assert(MEDIUMINT[Option[Int]](0).ZEROFILL.queryString === "MEDIUMINT(0) ZEROFILL NULL")
    assert(MEDIUMINT[Option[Int]](0).UNSIGNED.ZEROFILL.queryString === "MEDIUMINT(0) UNSIGNED ZEROFILL NULL")
    assert(MEDIUMINT[Option[Int]](255).DEFAULT(None).queryString === "MEDIUMINT(255) NULL DEFAULT NULL")
    assert(MEDIUMINT[Option[Int]](255).DEFAULT(Some(2)).queryString === "MEDIUMINT(255) NULL DEFAULT 2")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL DEFAULT NULL")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).ZEROFILL.queryString === "MEDIUMINT ZEROFILL NULL DEFAULT NULL")
    assert(
      MEDIUMINT[Option[Int]]
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString === "MEDIUMINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assert(MEDIUMINT[Int].queryString === "MEDIUMINT NOT NULL")
    assert(MEDIUMINT[Int].UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL")
    assert(MEDIUMINT[Int].ZEROFILL.queryString === "MEDIUMINT ZEROFILL NOT NULL")
    assert(MEDIUMINT[Int].UNSIGNED.ZEROFILL.queryString === "MEDIUMINT UNSIGNED ZEROFILL NOT NULL")
    assert(MEDIUMINT[Int].DEFAULT(1).queryString === "MEDIUMINT NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int].DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int].DEFAULT(1).ZEROFILL.queryString === "MEDIUMINT ZEROFILL NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int].DEFAULT(1).UNSIGNED.ZEROFILL.queryString === "MEDIUMINT UNSIGNED ZEROFILL NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Option[Int]].queryString === "MEDIUMINT NULL")
    assert(MEDIUMINT[Option[Int]].UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL")
    assert(MEDIUMINT[Option[Int]].ZEROFILL.queryString === "MEDIUMINT ZEROFILL NULL")
    assert(MEDIUMINT[Option[Int]].UNSIGNED.ZEROFILL.queryString === "MEDIUMINT UNSIGNED ZEROFILL NULL")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).queryString === "MEDIUMINT NULL DEFAULT NULL")
    assert(MEDIUMINT[Option[Int]].DEFAULT(Some(2)).queryString === "MEDIUMINT NULL DEFAULT 2")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL DEFAULT NULL")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).ZEROFILL.queryString === "MEDIUMINT ZEROFILL NULL DEFAULT NULL")
    assert(
      MEDIUMINT[Option[Int]]
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString === "MEDIUMINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  it should "The query string generated from the Integer DataType model matches the specified one." in {
    val integerType = DataType.Integer[Int](None, false, false, false, None)
    assert(integerType.typeName === "INT")
    assert(integerType.length === None)
    assert(integerType.sqlType === Types.INTEGER)
    assert(integerType.isOptional === false)
    assert(integerType.queryString === "INT NOT NULL")
    assert(integerType.toOption.isOptional === true)
    assert(integerType.toOption.sqlType === Types.INTEGER)
    assert(integerType.toOption.queryString === "INT NULL")
    assert(integerType.DEFAULT(1).queryString === "INT NOT NULL DEFAULT 1")
    assert(integerType.UNSIGNED.queryString === "INT UNSIGNED NOT NULL")
    assert(integerType.ZEROFILL.queryString === "INT ZEROFILL NOT NULL")
    assert(integerType.UNSIGNED.ZEROFILL.queryString === "INT UNSIGNED ZEROFILL NOT NULL")
    assert(integerType.ZEROFILL.UNSIGNED.queryString === "INT UNSIGNED ZEROFILL NOT NULL")
    assert(INT[Int](0).queryString === "INT(0) NOT NULL")
    assert(INT[Int](0).UNSIGNED.queryString === "INT(0) UNSIGNED NOT NULL")
    assert(INT[Int](0).ZEROFILL.queryString === "INT(0) ZEROFILL NOT NULL")
    assert(INT[Int](0).UNSIGNED.ZEROFILL.queryString === "INT(0) UNSIGNED ZEROFILL NOT NULL")
    assert(INT[Int](255).DEFAULT(1).queryString === "INT(255) NOT NULL DEFAULT 1")
    assert(INT[Int](255).DEFAULT(1).UNSIGNED.queryString === "INT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(INT[Int](255).DEFAULT(1).ZEROFILL.queryString === "INT(255) ZEROFILL NOT NULL DEFAULT 1")
    assert(INT[Int](255).DEFAULT(1).UNSIGNED.ZEROFILL.queryString === "INT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1")
    assert(INT[Option[Int]](0).queryString === "INT(0) NULL")
    assert(INT[Option[Int]](0).UNSIGNED.queryString === "INT(0) UNSIGNED NULL")
    assert(INT[Option[Int]](0).ZEROFILL.queryString === "INT(0) ZEROFILL NULL")
    assert(INT[Option[Int]](0).UNSIGNED.ZEROFILL.queryString === "INT(0) UNSIGNED ZEROFILL NULL")
    assert(INT[Option[Int]](255).DEFAULT(None).queryString === "INT(255) NULL DEFAULT NULL")
    assert(INT[Option[Int]](255).DEFAULT(Some(2)).queryString === "INT(255) NULL DEFAULT 2")
    assert(INT[Option[Int]](255).DEFAULT(None).UNSIGNED.queryString === "INT(255) UNSIGNED NULL DEFAULT NULL")
    assert(INT[Option[Int]](255).DEFAULT(None).ZEROFILL.queryString === "INT(255) ZEROFILL NULL DEFAULT NULL")
    assert(
      INT[Option[Int]](255)
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString === "INT(255) UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assert(INT[Int].queryString === "INT NOT NULL")
    assert(INT[Int].UNSIGNED.queryString === "INT UNSIGNED NOT NULL")
    assert(INT[Int].ZEROFILL.queryString === "INT ZEROFILL NOT NULL")
    assert(INT[Int].UNSIGNED.ZEROFILL.queryString === "INT UNSIGNED ZEROFILL NOT NULL")
    assert(INT[Int].DEFAULT(1).queryString === "INT NOT NULL DEFAULT 1")
    assert(INT[Int].DEFAULT(1).UNSIGNED.queryString === "INT UNSIGNED NOT NULL DEFAULT 1")
    assert(INT[Int].DEFAULT(1).ZEROFILL.queryString === "INT ZEROFILL NOT NULL DEFAULT 1")
    assert(INT[Int].DEFAULT(1).UNSIGNED.ZEROFILL.queryString === "INT UNSIGNED ZEROFILL NOT NULL DEFAULT 1")
    assert(INT[Option[Int]].queryString === "INT NULL")
    assert(INT[Option[Int]].UNSIGNED.queryString === "INT UNSIGNED NULL")
    assert(INT[Option[Int]].ZEROFILL.queryString === "INT ZEROFILL NULL")
    assert(INT[Option[Int]].UNSIGNED.ZEROFILL.queryString === "INT UNSIGNED ZEROFILL NULL")
    assert(INT[Option[Int]].DEFAULT(None).queryString === "INT NULL DEFAULT NULL")
    assert(INT[Option[Int]].DEFAULT(Some(2)).queryString === "INT NULL DEFAULT 2")
    assert(INT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "INT UNSIGNED NULL DEFAULT NULL")
    assert(INT[Option[Int]].DEFAULT(None).ZEROFILL.queryString === "INT ZEROFILL NULL DEFAULT NULL")
    assert(INT[Option[Int]].DEFAULT(None).UNSIGNED.ZEROFILL.queryString === "INT UNSIGNED ZEROFILL NULL DEFAULT NULL")
  }

  it should "The query string generated from the Bigint DataType model matches the specified one." in {
    val bigintType = DataType.Bigint[Long](None, false, false, false, None)
    assert(bigintType.typeName === "BIGINT")
    assert(bigintType.length === None)
    assert(bigintType.sqlType === Types.BIGINT)
    assert(bigintType.isOptional === false)
    assert(bigintType.queryString === "BIGINT NOT NULL")
    assert(bigintType.toOption.isOptional === true)
    assert(bigintType.toOption.sqlType === Types.BIGINT)
    assert(bigintType.toOption.queryString === "BIGINT NULL")
    assert(bigintType.DEFAULT(1L).queryString === "BIGINT NOT NULL DEFAULT 1")
    assert(bigintType.UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL")
    assert(bigintType.ZEROFILL.queryString === "BIGINT ZEROFILL NOT NULL")
    assert(bigintType.UNSIGNED.ZEROFILL.queryString === "BIGINT UNSIGNED ZEROFILL NOT NULL")
    assert(bigintType.ZEROFILL.UNSIGNED.queryString === "BIGINT UNSIGNED ZEROFILL NOT NULL")
    assert(BIGINT[Long](0).queryString === "BIGINT(0) NOT NULL")
    assert(BIGINT[Long](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NOT NULL")
    assert(BIGINT[Long](0).ZEROFILL.queryString === "BIGINT(0) ZEROFILL NOT NULL")
    assert(BIGINT[Long](0).UNSIGNED.ZEROFILL.queryString === "BIGINT(0) UNSIGNED ZEROFILL NOT NULL")
    assert(BIGINT[Long](255).DEFAULT(1).queryString === "BIGINT(255) NOT NULL DEFAULT 1")
    assert(BIGINT[Long](255).DEFAULT(1).UNSIGNED.queryString === "BIGINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(BIGINT[Long](255).DEFAULT(1).ZEROFILL.queryString === "BIGINT(255) ZEROFILL NOT NULL DEFAULT 1")
    assert(
      BIGINT[Long](255).DEFAULT(1).UNSIGNED.ZEROFILL.queryString === "BIGINT(255) UNSIGNED ZEROFILL NOT NULL DEFAULT 1"
    )
    assert(BIGINT[Option[Long]](0).queryString === "BIGINT(0) NULL")
    assert(BIGINT[Option[Long]](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NULL")
    assert(BIGINT[Option[Long]](0).ZEROFILL.queryString === "BIGINT(0) ZEROFILL NULL")
    assert(BIGINT[Option[Long]](0).UNSIGNED.ZEROFILL.queryString === "BIGINT(0) UNSIGNED ZEROFILL NULL")
    assert(BIGINT[Option[Long]](255).DEFAULT(None).queryString === "BIGINT(255) NULL DEFAULT NULL")
    assert(BIGINT[Option[Long]](255).DEFAULT(Some(2)).queryString === "BIGINT(255) NULL DEFAULT 2")
    assert(BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString === "BIGINT UNSIGNED NULL DEFAULT NULL")
    assert(BIGINT[Option[Long]].DEFAULT(None).ZEROFILL.queryString === "BIGINT ZEROFILL NULL DEFAULT NULL")
    assert(
      BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.ZEROFILL.queryString === "BIGINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assert(BIGINT[Long].queryString === "BIGINT NOT NULL")
    assert(BIGINT[Long].UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL")
    assert(BIGINT[Long].ZEROFILL.queryString === "BIGINT ZEROFILL NOT NULL")
    assert(BIGINT[Long].UNSIGNED.ZEROFILL.queryString === "BIGINT UNSIGNED ZEROFILL NOT NULL")
    assert(BIGINT[Long].DEFAULT(1).queryString === "BIGINT NOT NULL DEFAULT 1")
    assert(BIGINT[Long].DEFAULT(1).UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL DEFAULT 1")
    assert(BIGINT[Long].DEFAULT(1).ZEROFILL.queryString === "BIGINT ZEROFILL NOT NULL DEFAULT 1")
    assert(BIGINT[Long].DEFAULT(1).UNSIGNED.ZEROFILL.queryString === "BIGINT UNSIGNED ZEROFILL NOT NULL DEFAULT 1")
    assert(BIGINT[Option[Long]].queryString === "BIGINT NULL")
    assert(BIGINT[Option[Long]].UNSIGNED.queryString === "BIGINT UNSIGNED NULL")
    assert(BIGINT[Option[Long]].ZEROFILL.queryString === "BIGINT ZEROFILL NULL")
    assert(BIGINT[Option[Long]].UNSIGNED.ZEROFILL.queryString === "BIGINT UNSIGNED ZEROFILL NULL")
    assert(BIGINT[Option[Long]].DEFAULT(None).queryString === "BIGINT NULL DEFAULT NULL")
    assert(BIGINT[Option[Long]].DEFAULT(Some(2)).queryString === "BIGINT NULL DEFAULT 2")
    assert(BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString === "BIGINT UNSIGNED NULL DEFAULT NULL")
    assert(BIGINT[Option[Long]].DEFAULT(None).ZEROFILL.queryString === "BIGINT ZEROFILL NULL DEFAULT NULL")
    assert(
      BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.ZEROFILL.queryString === "BIGINT UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
  }

  it should "The query string generated from the Decimal DataType model matches the specified one." in {
    val decimalType = DataType.Decimal[BigDecimal](10, 5, false, false, false, None)
    assert(decimalType.typeName === "DECIMAL(10, 5)")
    assert(decimalType.sqlType === Types.DECIMAL)
    assert(decimalType.isOptional === false)
    assert(decimalType.queryString === "DECIMAL(10, 5) NOT NULL")
    assert(decimalType.toOption.isOptional === true)
    assert(decimalType.toOption.sqlType === Types.DECIMAL)
    assert(decimalType.toOption.queryString === "DECIMAL(10, 5) NULL")
    assert(decimalType.DEFAULT(BigDecimal(1.5)).queryString === "DECIMAL(10, 5) NOT NULL DEFAULT '1.5'")
    assert(decimalType.UNSIGNED.queryString === "DECIMAL(10, 5) UNSIGNED NOT NULL")
    assert(decimalType.ZEROFILL.queryString === "DECIMAL(10, 5) ZEROFILL NOT NULL")
    assert(decimalType.UNSIGNED.ZEROFILL.queryString === "DECIMAL(10, 5) UNSIGNED ZEROFILL NOT NULL")
    assert(decimalType.ZEROFILL.UNSIGNED.queryString === "DECIMAL(10, 5) UNSIGNED ZEROFILL NOT NULL")
    assert(DECIMAL[BigDecimal](10, 7).queryString === "DECIMAL(10, 7) NOT NULL")
    assert(DECIMAL[BigDecimal](10, 7).UNSIGNED.queryString === "DECIMAL(10, 7) UNSIGNED NOT NULL")
    assert(DECIMAL[BigDecimal](10, 7).ZEROFILL.queryString === "DECIMAL(10, 7) ZEROFILL NOT NULL")
    assert(DECIMAL[BigDecimal](10, 7).UNSIGNED.ZEROFILL.queryString === "DECIMAL(10, 7) UNSIGNED ZEROFILL NOT NULL")
    assert(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .queryString === "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'"
    )
    assert(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .UNSIGNED
        .queryString === "DECIMAL(10, 7) UNSIGNED NOT NULL DEFAULT '0.0000010'"
    )
    assert(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .ZEROFILL
        .queryString === "DECIMAL(10, 7) ZEROFILL NOT NULL DEFAULT '0.0000010'"
    )
    assert(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .UNSIGNED
        .ZEROFILL
        .queryString === "DECIMAL(10, 7) UNSIGNED ZEROFILL NOT NULL DEFAULT '0.0000010'"
    )
    assert(DECIMAL[Option[BigDecimal]](10, 7).queryString === "DECIMAL(10, 7) NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7).UNSIGNED.queryString === "DECIMAL(10, 7) UNSIGNED NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7).ZEROFILL.queryString === "DECIMAL(10, 7) ZEROFILL NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7).UNSIGNED.ZEROFILL.queryString === "DECIMAL(10, 7) UNSIGNED ZEROFILL NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7).DEFAULT(None).queryString === "DECIMAL(10, 7) NULL DEFAULT NULL")
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(None)
        .UNSIGNED
        .queryString === "DECIMAL(10, 7) UNSIGNED NULL DEFAULT NULL"
    )
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(None)
        .ZEROFILL
        .queryString === "DECIMAL(10, 7) ZEROFILL NULL DEFAULT NULL"
    )
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(None)
        .UNSIGNED
        .ZEROFILL
        .queryString === "DECIMAL(10, 7) UNSIGNED ZEROFILL NULL DEFAULT NULL"
    )
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .queryString === "DECIMAL(10, 7) NULL DEFAULT '0.0000010'"
    )
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .UNSIGNED
        .queryString === "DECIMAL(10, 7) UNSIGNED NULL DEFAULT '0.0000010'"
    )
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .ZEROFILL
        .queryString === "DECIMAL(10, 7) ZEROFILL NULL DEFAULT '0.0000010'"
    )
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .UNSIGNED
        .ZEROFILL
        .queryString === "DECIMAL(10, 7) UNSIGNED ZEROFILL NULL DEFAULT '0.0000010'"
    )
  }

  it should "The query string generated from the Char DataType model matches the specified one." in {
    val charType = DataType.CChar[String](10, false, None, None, None)
    assert(charType.typeName === "CHAR(10)")
    assert(charType.sqlType === Types.CHAR)
    assert(charType.isOptional === false)
    assert(charType.queryString === "CHAR(10) NOT NULL")
    assert(charType.toOption.isOptional === true)
    assert(charType.toOption.sqlType === Types.CHAR)
    assert(charType.toOption.queryString === "CHAR(10) NULL")
    assert(charType.DEFAULT("test").queryString === "CHAR(10) NOT NULL DEFAULT 'test'")
    assert(CHAR[String](0).queryString === "CHAR(0) NOT NULL")
    assert(CHAR[Option[String]](0).queryString === "CHAR(0) NULL")
    assert(CHAR[Option[String]](0).DEFAULT(None).queryString === "CHAR(0) NULL DEFAULT NULL")
    assert(CHAR[Option[String]](0).DEFAULT(Some("test")).queryString === "CHAR(0) NULL DEFAULT 'test'")
  }

  it should "The query string generated from the Char DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val charType = DataType.CChar[String](10, false, None, None, None)
    val withCharSet = charType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "CHAR(10) CHARACTER SET utf8mb4 NOT NULL")
    assert(withCharSet.toOption.queryString === "CHAR(10) CHARACTER SET utf8mb4 NULL")
    
    val withCollate = charType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "CHAR(10) COLLATE utf8mb4_bin NOT NULL")
    assert(withCollate.toOption.queryString === "CHAR(10) COLLATE utf8mb4_bin NULL")
    
    val withBoth = charType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "CHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    assert(withBoth.toOption.queryString === "CHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL")
    
    val withDefault = withBoth.DEFAULT("test")
    assert(withDefault.queryString === "CHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'test'")
    
    val optionalWithBoth = CHAR[Option[String]](20).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "CHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
    
    val optionalWithDefault = optionalWithBoth.DEFAULT(Some("value"))
    assert(optionalWithDefault.queryString === "CHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'value'")
  }

  it should "The query string generated from the Varchar DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val varcharType = DataType.Varchar[String](50, false, None, None, None)
    val withCharSet = varcharType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "VARCHAR(50) CHARACTER SET utf8mb4 NOT NULL")
    
    val withCollate = varcharType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "VARCHAR(50) COLLATE utf8mb4_bin NOT NULL")
    
    val withBoth = varcharType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    
    val withDefault = withBoth.DEFAULT("test")
    assert(withDefault.queryString === "VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'test'")
    
    val optionalWithBoth = VARCHAR[Option[String]](100).CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
  }

  it should "The query string generated from the Binary DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val binaryType = DataType.Binary[Array[Byte]](10, false, None, None, None)
    val withCharSet = binaryType.CHARACTER_SET(Character.binary)
    assert(withCharSet.queryString === "BINARY(10) CHARACTER SET binary NOT NULL")
    
    val withCollate = binaryType.COLLATE(Collate.binary)
    assert(withCollate.queryString === "BINARY(10) COLLATE binary NOT NULL")
    
    val withBoth = binaryType.CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assert(withBoth.queryString === "BINARY(10) CHARACTER SET binary COLLATE binary NOT NULL")
    
    val optionalWithBoth = BINARY[Option[Array[Byte]]](15).CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assert(optionalWithBoth.queryString === "BINARY(15) CHARACTER SET binary COLLATE binary NULL")
  }

  it should "The query string generated from the Varbinary DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val varbinaryType = DataType.Varbinary[Array[Byte]](50, false, None, None, None)
    val withCharSet = varbinaryType.CHARACTER_SET(Character.binary)
    assert(withCharSet.queryString === "VARBINARY(50) CHARACTER SET binary NOT NULL")
    
    val withCollate = varbinaryType.COLLATE(Collate.binary)
    assert(withCollate.queryString === "VARBINARY(50) COLLATE binary NOT NULL")
    
    val withBoth = varbinaryType.CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assert(withBoth.queryString === "VARBINARY(50) CHARACTER SET binary COLLATE binary NOT NULL")
    
    val optionalWithBoth = VARBINARY[Option[Array[Byte]]](100).CHARACTER_SET(Character.binary).COLLATE(Collate.binary)
    assert(optionalWithBoth.queryString === "VARBINARY(100) CHARACTER SET binary COLLATE binary NULL")
  }

  it should "The query string generated from the TinyText DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val tinytextType = DataType.TinyText[String](false, None, None, None)
    val withCharSet = tinytextType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "TINYTEXT CHARACTER SET utf8mb4 NOT NULL")
    
    val withCollate = tinytextType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "TINYTEXT COLLATE utf8mb4_bin NOT NULL")
    
    val withBoth = tinytextType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    
    val optionalWithBoth = TINYTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
    
    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assert(optionalWithDefault.queryString === "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL")
  }

  it should "The query string generated from the Text DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val textType = DataType.Text[String](false, None, None, None)
    val withCharSet = textType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "TEXT CHARACTER SET utf8mb4 NOT NULL")
    
    val withCollate = textType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "TEXT COLLATE utf8mb4_bin NOT NULL")
    
    val withBoth = textType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    
    val optionalWithBoth = TEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
    
    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assert(optionalWithDefault.queryString === "TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL")
  }

  it should "The query string generated from the MediumText DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val mediumtextType = DataType.MediumText[String](false, None, None, None)
    val withCharSet = mediumtextType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "MEDIUMTEXT CHARACTER SET utf8mb4 NOT NULL")
    
    val withCollate = mediumtextType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "MEDIUMTEXT COLLATE utf8mb4_bin NOT NULL")
    
    val withBoth = mediumtextType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    
    val optionalWithBoth = MEDIUMTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
    
    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assert(optionalWithDefault.queryString === "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL")
  }

  it should "The query string generated from the LongText DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    val longtextType = DataType.LongText[String](false, None, None, None)
    val withCharSet = longtextType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "LONGTEXT CHARACTER SET utf8mb4 NOT NULL")
    
    val withCollate = longtextType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "LONGTEXT COLLATE utf8mb4_bin NOT NULL")
    
    val withBoth = longtextType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    
    val optionalWithBoth = LONGTEXT[Option[String]]().CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
    
    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assert(optionalWithDefault.queryString === "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL")
  }

  it should "The query string generated from the Enum DataType model with CHARACTER_SET and COLLATE matches the specified one." in {
    enum Status:
      case Active, InActive

    val enumType = DataType.Enum[Status](List("Active", "InActive"), false, None, None, None)
    val withCharSet = enumType.CHARACTER_SET(Character.utf8mb4)
    assert(withCharSet.queryString === "ENUM('Active','InActive') CHARACTER SET utf8mb4 NOT NULL")
    
    val withCollate = enumType.COLLATE(Collate.utf8mb4_bin)
    assert(withCollate.queryString === "ENUM('Active','InActive') COLLATE utf8mb4_bin NOT NULL")
    
    val withBoth = enumType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_bin)
    assert(withBoth.queryString === "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL")
    
    val withDefault = withBoth.DEFAULT(Status.Active)
    assert(withDefault.queryString === "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT 'Active'")
    
    val optionalEnumType = DataType.Enum[Option[Status]](List("Active", "InActive"), true, None, None, None)
    val optionalWithBoth = optionalEnumType.CHARACTER_SET(Character.utf8mb4).COLLATE(Collate.utf8mb4_general_ci)
    assert(optionalWithBoth.queryString === "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL")
    
    val optionalWithDefault = optionalWithBoth.DEFAULT(None)
    assert(optionalWithDefault.queryString === "ENUM('Active','InActive') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL")
  }
