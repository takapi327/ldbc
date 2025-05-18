/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.sql.Types

class JvmDataTypeTest extends DataTypeTest:

  it should "The query string generated from the Bit DataType model matches the specified one." in {
    val bitType = DataType.Bit[Byte](None, false, None)
    assert(bitType.typeName === "BIT")
    assert(bitType.length === None)
    assert(bitType.sqlType === Types.BIT)
    assert(bitType.isOptional === false)
    assert(bitType.queryString === "BIT NOT NULL")
    assert(bitType.toOption.isOptional === true)
    assert(bitType.toOption.queryString === "BIT NULL")
    assert(bitType.DEFAULT(1.toByte).queryString === "BIT NOT NULL DEFAULT '1'")
    assert(BIT[Byte](1).queryString === "BIT(1) NOT NULL")
    assert(BIT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "BIT(64) NOT NULL DEFAULT '98'")
    assert(BIT[Option[Short]](1).queryString === "BIT(1) NULL")
    assert(BIT[Option[Short]](64).DEFAULT(None).queryString === "BIT(64) NULL DEFAULT NULL")
    assert(BIT[Byte].queryString === "BIT NOT NULL")
    assert(BIT[Byte].DEFAULT("byte".getBytes.head).queryString === "BIT NOT NULL DEFAULT '98'")
    assert(BIT[Option[Short]].queryString === "BIT NULL")
    assert(BIT[Option[Short]].DEFAULT(None).queryString === "BIT NULL DEFAULT NULL")
  }

  it should "The query string generated from the Tinyint DataType model matches the specified one." in {
    val tinyintType = DataType.Tinyint[Byte](None, false, false, false, None)
    assert(tinyintType.typeName === "TINYINT")
    assert(tinyintType.length === None)
    assert(tinyintType.sqlType === Types.TINYINT)
    assert(tinyintType.isOptional === false)
    assert(tinyintType.queryString === "TINYINT NOT NULL")
    assert(tinyintType.toOption.isOptional === true)
    assert(tinyintType.toOption.queryString === "TINYINT NULL")
    assert(tinyintType.DEFAULT(1.toByte).queryString === "TINYINT NOT NULL DEFAULT '1'")
    assert(tinyintType.UNSIGNED.queryString === "TINYINT UNSIGNED NOT NULL")
    assert(TINYINT[Byte](1).queryString === "TINYINT(1) NOT NULL")
    assert(TINYINT[Byte](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NOT NULL")
    assert(TINYINT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "TINYINT(64) NOT NULL DEFAULT '98'")
    assert(
      TINYINT[Byte](64)
        .DEFAULT("byte".getBytes.head)
        .UNSIGNED
        .queryString === "TINYINT(64) UNSIGNED NOT NULL DEFAULT '98'"
    )
    assert(TINYINT[Option[Byte]](1).queryString === "TINYINT(1) NULL")
    assert(TINYINT[Option[Byte]](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NULL")
    assert(TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL")
    assert(
      TINYINT[Option[Byte]](64)
        .DEFAULT("byte".getBytes.headOption)
        .queryString === "TINYINT(64) NULL DEFAULT '98'"
    )
    assert(TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL")
    assert(TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString === "TINYINT UNSIGNED NULL DEFAULT NULL")
    assert(TINYINT[Byte].queryString === "TINYINT NOT NULL")
    assert(TINYINT[Byte].UNSIGNED.queryString === "TINYINT UNSIGNED NOT NULL")
    assert(TINYINT[Byte].DEFAULT("byte".getBytes.head).queryString === "TINYINT NOT NULL DEFAULT '98'")
    assert(
      TINYINT[Byte]
        .DEFAULT("byte".getBytes.head)
        .UNSIGNED
        .queryString === "TINYINT UNSIGNED NOT NULL DEFAULT '98'"
    )
    assert(TINYINT[Option[Byte]].queryString === "TINYINT NULL")
    assert(TINYINT[Option[Byte]].UNSIGNED.queryString === "TINYINT UNSIGNED NULL")
    assert(TINYINT[Option[Byte]].DEFAULT(None).queryString === "TINYINT NULL DEFAULT NULL")
    assert(
      TINYINT[Option[Byte]]
        .DEFAULT("byte".getBytes.headOption)
        .queryString === "TINYINT NULL DEFAULT '98'"
    )
    assert(TINYINT[Option[Byte]].DEFAULT(None).queryString === "TINYINT NULL DEFAULT NULL")
    assert(TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString === "TINYINT UNSIGNED NULL DEFAULT NULL")
  }

  it should "The query string generated from the Float DataType model matches the specified one." in {
    val floatType = DataType.CFloat[Float](10, false, false, false, None)
    assert(floatType.typeName === "FLOAT(10)")
    assert(floatType.sqlType === Types.FLOAT)
    assert(floatType.isOptional === false)
    assert(floatType.queryString === "FLOAT(10) NOT NULL")
    assert(floatType.toOption.isOptional === true)
    assert(floatType.toOption.queryString === "FLOAT(10) NULL")
    assert(floatType.DEFAULT(1.5f).queryString === "FLOAT(10) NOT NULL DEFAULT 1.5")
    assert(floatType.UNSIGNED.queryString === "FLOAT(10) UNSIGNED NOT NULL")
    assert(FLOAT[Float](0).queryString === "FLOAT(0) NOT NULL")
    assert(FLOAT[Float](0).DEFAULT(1.2f).queryString === "FLOAT(0) NOT NULL DEFAULT 1.2")
    assert(FLOAT[Option[Float]](0).queryString === "FLOAT(0) NULL")
    assert(FLOAT[Option[Float]](0).DEFAULT(None).queryString === "FLOAT(0) NULL DEFAULT NULL")
    assert(FLOAT[Option[Float]](0).DEFAULT(Some(1.2f)).queryString === "FLOAT(0) NULL DEFAULT 1.2")
  }
