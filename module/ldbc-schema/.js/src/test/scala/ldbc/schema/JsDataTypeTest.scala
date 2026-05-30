/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.sql.Types

class JsDataTypeTest extends DataTypeTest:

  test("The query string generated from the Bit DataType model matches the specified one.") {
    val bitType = DataType.Bit[Byte](None, false, None)
    assertEquals(bitType.typeName, "BIT")
    assertEquals(bitType.length, None)
    assertEquals(bitType.sqlType, Types.BIT)
    assertEquals(bitType.isOptional, false)
    assertEquals(bitType.queryString, "BIT NOT NULL")
    assertEquals(bitType.toOption.isOptional, true)
    assertEquals(bitType.toOption.queryString, "BIT NULL")
    assertEquals(bitType.DEFAULT(1.toByte).queryString, "BIT NOT NULL DEFAULT 1")
    assertEquals(BIT[Byte](1).queryString, "BIT(1) NOT NULL")
    assertEquals(BIT[Byte](64).DEFAULT("byte".getBytes.head).queryString, "BIT(64) NOT NULL DEFAULT 98")
    assertEquals(BIT[Option[Short]](1).queryString, "BIT(1) NULL")
    assertEquals(BIT[Option[Short]](64).DEFAULT(None).queryString, "BIT(64) NULL DEFAULT NULL")
    assertEquals(BIT[Byte].queryString, "BIT NOT NULL")
    assertEquals(BIT[Byte].DEFAULT("byte".getBytes.head).queryString, "BIT NOT NULL DEFAULT 98")
    assertEquals(BIT[Option[Short]].queryString, "BIT NULL")
    assertEquals(BIT[Option[Short]].DEFAULT(None).queryString, "BIT NULL DEFAULT NULL")
  }

  test("The query string generated from the Tinyint DataType model matches the specified one.") {
    val tinyintType = DataType.Tinyint[Byte](None, false, false, false, None)
    assertEquals(tinyintType.typeName, "TINYINT")
    assertEquals(tinyintType.length, None)
    assertEquals(tinyintType.sqlType, Types.TINYINT)
    assertEquals(tinyintType.isOptional, false)
    assertEquals(tinyintType.queryString, "TINYINT NOT NULL")
    assertEquals(tinyintType.toOption.isOptional, true)
    assertEquals(tinyintType.toOption.queryString, "TINYINT NULL")
    assertEquals(tinyintType.DEFAULT(1.toByte).queryString, "TINYINT NOT NULL DEFAULT 1")
    assertEquals(tinyintType.UNSIGNED.queryString, "TINYINT UNSIGNED NOT NULL")
    assertEquals(tinyintType.ZEROFILL.queryString, "TINYINT ZEROFILL NOT NULL")
    assertEquals(tinyintType.UNSIGNED.ZEROFILL.queryString, "TINYINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(tinyintType.ZEROFILL.UNSIGNED.queryString, "TINYINT UNSIGNED ZEROFILL NOT NULL")
    assertEquals(TINYINT[Byte](1).queryString, "TINYINT(1) NOT NULL")
    assertEquals(TINYINT[Byte](1).UNSIGNED.queryString, "TINYINT(1) UNSIGNED NOT NULL")
    assertEquals(TINYINT[Byte](64).DEFAULT("byte".getBytes.head).queryString, "TINYINT(64) NOT NULL DEFAULT 98")
    assertEquals(
      TINYINT[Byte](64)
        .DEFAULT("byte".getBytes.head)
        .UNSIGNED
        .queryString,
      "TINYINT(64) UNSIGNED NOT NULL DEFAULT 98"
    )
    assertEquals(TINYINT[Option[Byte]](1).queryString, "TINYINT(1) NULL")
    assertEquals(TINYINT[Option[Byte]](1).UNSIGNED.queryString, "TINYINT(1) UNSIGNED NULL")
    assertEquals(TINYINT[Option[Byte]](64).DEFAULT(None).queryString, "TINYINT(64) NULL DEFAULT NULL")
    assertEquals(
      TINYINT[Option[Byte]](64)
        .DEFAULT("byte".getBytes.headOption)
        .queryString,
      "TINYINT(64) NULL DEFAULT 98"
    )
    assertEquals(TINYINT[Option[Byte]](64).DEFAULT(None).queryString, "TINYINT(64) NULL DEFAULT NULL")
    assertEquals(TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString, "TINYINT UNSIGNED NULL DEFAULT NULL")
    assertEquals(TINYINT[Byte].queryString, "TINYINT NOT NULL")
    assertEquals(TINYINT[Byte].UNSIGNED.queryString, "TINYINT UNSIGNED NOT NULL")
    assertEquals(TINYINT[Byte].DEFAULT("byte".getBytes.head).queryString, "TINYINT NOT NULL DEFAULT 98")
    assertEquals(
      TINYINT[Byte]
        .DEFAULT("byte".getBytes.head)
        .UNSIGNED
        .queryString,
      "TINYINT UNSIGNED NOT NULL DEFAULT 98"
    )
    assertEquals(TINYINT[Option[Byte]].queryString, "TINYINT NULL")
    assertEquals(TINYINT[Option[Byte]].UNSIGNED.queryString, "TINYINT UNSIGNED NULL")
    assertEquals(TINYINT[Option[Byte]].DEFAULT(None).queryString, "TINYINT NULL DEFAULT NULL")
    assertEquals(
      TINYINT[Option[Byte]]
        .DEFAULT("byte".getBytes.headOption)
        .queryString,
      "TINYINT NULL DEFAULT 98"
    )
    assertEquals(TINYINT[Option[Byte]].DEFAULT(None).queryString, "TINYINT NULL DEFAULT NULL")
    assertEquals(TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString, "TINYINT UNSIGNED NULL DEFAULT NULL")
  }

  test("The query string generated from the Float DataType model matches the specified one.") {
    val floatType = DataType.CFloat[Float](10, false, false, false, None)
    assertEquals(floatType.typeName, "FLOAT(10)")
    assertEquals(floatType.sqlType, Types.FLOAT)
    assertEquals(floatType.isOptional, false)
    assertEquals(floatType.queryString, "FLOAT(10) NOT NULL")
    assertEquals(floatType.toOption.isOptional, true)
    assertEquals(floatType.toOption.queryString, "FLOAT(10) NULL")
    assertEquals(floatType.DEFAULT(1.5f).queryString, "FLOAT(10) NOT NULL DEFAULT 1.5")
    assertEquals(floatType.UNSIGNED.queryString, "FLOAT(10) UNSIGNED NOT NULL")
    assertEquals(floatType.ZEROFILL.queryString, "FLOAT(10) ZEROFILL NOT NULL")
    assertEquals(floatType.UNSIGNED.ZEROFILL.queryString, "FLOAT(10) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(floatType.ZEROFILL.UNSIGNED.queryString, "FLOAT(10) UNSIGNED ZEROFILL NOT NULL")
    assertEquals(FLOAT[Float](0).queryString, "FLOAT(0) NOT NULL")
    assertEquals(FLOAT[Float](0).DEFAULT(1.2f).queryString, "FLOAT(0) NOT NULL DEFAULT 1.2000000476837158")
    assertEquals(FLOAT[Option[Float]](0).queryString, "FLOAT(0) NULL")
    assertEquals(FLOAT[Option[Float]](0).DEFAULT(None).queryString, "FLOAT(0) NULL DEFAULT NULL")
    assertEquals(FLOAT[Option[Float]](0).DEFAULT(Some(1.2f)).queryString, "FLOAT(0) NULL DEFAULT 1.2000000476837158")
  }
