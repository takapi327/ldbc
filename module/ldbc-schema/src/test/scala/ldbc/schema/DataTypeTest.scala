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
    assert(smallintType.toOption.queryString === "SMALLINT NULL")
    assert(smallintType.DEFAULT(1.toShort).queryString === "SMALLINT NOT NULL DEFAULT 1")
    assert(smallintType.UNSIGNED.queryString === "SMALLINT UNSIGNED NOT NULL")
    assert(SMALLINT[Short](0).queryString === "SMALLINT(0) NOT NULL")
    assert(SMALLINT[Short](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NOT NULL")
    assert(SMALLINT[Short](255).DEFAULT(1).queryString === "SMALLINT(255) NOT NULL DEFAULT 1")
    assert(SMALLINT[Short](255).DEFAULT(1).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(SMALLINT[Option[Short]](0).queryString === "SMALLINT(0) NULL")
    assert(SMALLINT[Option[Short]](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NULL")
    assert(SMALLINT[Option[Short]](255).DEFAULT(None).queryString === "SMALLINT(255) NULL DEFAULT NULL")
    assert(SMALLINT[Option[Short]](255).DEFAULT(Some(2)).queryString === "SMALLINT(255) NULL DEFAULT 2")
    assert(
      SMALLINT[Option[Short]](255).DEFAULT(None).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NULL DEFAULT NULL"
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
    assert(mediumintType.toOption.queryString === "MEDIUMINT NULL")
    assert(mediumintType.DEFAULT(1).queryString === "MEDIUMINT NOT NULL DEFAULT 1")
    assert(mediumintType.UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL")
    assert(MEDIUMINT[Int](0).queryString === "MEDIUMINT(0) NOT NULL")
    assert(MEDIUMINT[Int](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NOT NULL")
    assert(MEDIUMINT[Int](255).DEFAULT(1).queryString === "MEDIUMINT(255) NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int](255).DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Option[Int]](0).queryString === "MEDIUMINT(0) NULL")
    assert(MEDIUMINT[Option[Int]](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NULL")
    assert(MEDIUMINT[Option[Int]](255).DEFAULT(None).queryString === "MEDIUMINT(255) NULL DEFAULT NULL")
    assert(MEDIUMINT[Option[Int]](255).DEFAULT(Some(2)).queryString === "MEDIUMINT(255) NULL DEFAULT 2")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL DEFAULT NULL")
    assert(MEDIUMINT[Int].queryString === "MEDIUMINT NOT NULL")
    assert(MEDIUMINT[Int].UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL")
    assert(MEDIUMINT[Int].DEFAULT(1).queryString === "MEDIUMINT NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Int].DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL DEFAULT 1")
    assert(MEDIUMINT[Option[Int]].queryString === "MEDIUMINT NULL")
    assert(MEDIUMINT[Option[Int]].UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).queryString === "MEDIUMINT NULL DEFAULT NULL")
    assert(MEDIUMINT[Option[Int]].DEFAULT(Some(2)).queryString === "MEDIUMINT NULL DEFAULT 2")
    assert(MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL DEFAULT NULL")
  }

  it should "The query string generated from the Integer DataType model matches the specified one." in {
    val integerType = DataType.Integer[Int](None, false, false, false, None)
    assert(integerType.typeName === "INT")
    assert(integerType.length === None)
    assert(integerType.sqlType === Types.INTEGER)
    assert(integerType.isOptional === false)
    assert(integerType.queryString === "INT NOT NULL")
    assert(integerType.toOption.isOptional === true)
    assert(integerType.toOption.queryString === "INT NULL")
    assert(integerType.DEFAULT(1).queryString === "INT NOT NULL DEFAULT 1")
    assert(integerType.UNSIGNED.queryString === "INT UNSIGNED NOT NULL")
    assert(INT[Int](0).queryString === "INT(0) NOT NULL")
    assert(INT[Int](0).UNSIGNED.queryString === "INT(0) UNSIGNED NOT NULL")
    assert(INT[Int](255).DEFAULT(1).queryString === "INT(255) NOT NULL DEFAULT 1")
    assert(INT[Int](255).DEFAULT(1).UNSIGNED.queryString === "INT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(INT[Option[Int]](0).queryString === "INT(0) NULL")
    assert(INT[Option[Int]](0).UNSIGNED.queryString === "INT(0) UNSIGNED NULL")
    assert(INT[Option[Int]](255).DEFAULT(None).queryString === "INT(255) NULL DEFAULT NULL")
    assert(INT[Option[Int]](255).DEFAULT(Some(2)).queryString === "INT(255) NULL DEFAULT 2")
    assert(INT[Option[Int]](255).DEFAULT(None).UNSIGNED.queryString === "INT(255) UNSIGNED NULL DEFAULT NULL")
    assert(INT[Int].queryString === "INT NOT NULL")
    assert(INT[Int].UNSIGNED.queryString === "INT UNSIGNED NOT NULL")
    assert(INT[Int].DEFAULT(1).queryString === "INT NOT NULL DEFAULT 1")
    assert(INT[Int].DEFAULT(1).UNSIGNED.queryString === "INT UNSIGNED NOT NULL DEFAULT 1")
    assert(INT[Option[Int]].queryString === "INT NULL")
    assert(INT[Option[Int]].UNSIGNED.queryString === "INT UNSIGNED NULL")
    assert(INT[Option[Int]].DEFAULT(None).queryString === "INT NULL DEFAULT NULL")
    assert(INT[Option[Int]].DEFAULT(Some(2)).queryString === "INT NULL DEFAULT 2")
    assert(INT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "INT UNSIGNED NULL DEFAULT NULL")
  }

  it should "The query string generated from the Bigint DataType model matches the specified one." in {
    val bigintType = DataType.Bigint[Long](None, false, false, false, None)
    assert(bigintType.typeName === "BIGINT")
    assert(bigintType.length === None)
    assert(bigintType.sqlType === Types.BIGINT)
    assert(bigintType.isOptional === false)
    assert(bigintType.queryString === "BIGINT NOT NULL")
    assert(bigintType.toOption.isOptional === true)
    assert(bigintType.toOption.queryString === "BIGINT NULL")
    assert(bigintType.DEFAULT(1L).queryString === "BIGINT NOT NULL DEFAULT 1")
    assert(bigintType.UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL")
    assert(BIGINT[Long](0).queryString === "BIGINT(0) NOT NULL")
    assert(BIGINT[Long](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NOT NULL")
    assert(BIGINT[Long](255).DEFAULT(1).queryString === "BIGINT(255) NOT NULL DEFAULT 1")
    assert(BIGINT[Long](255).DEFAULT(1).UNSIGNED.queryString === "BIGINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(BIGINT[Option[Long]](0).queryString === "BIGINT(0) NULL")
    assert(BIGINT[Option[Long]](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NULL")
    assert(BIGINT[Option[Long]](255).DEFAULT(None).queryString === "BIGINT(255) NULL DEFAULT NULL")
    assert(BIGINT[Option[Long]](255).DEFAULT(Some(2)).queryString === "BIGINT(255) NULL DEFAULT 2")
    assert(BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString === "BIGINT UNSIGNED NULL DEFAULT NULL")
    assert(BIGINT[Long].queryString === "BIGINT NOT NULL")
    assert(BIGINT[Long].UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL")
    assert(BIGINT[Long].DEFAULT(1).queryString === "BIGINT NOT NULL DEFAULT 1")
    assert(BIGINT[Long].DEFAULT(1).UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL DEFAULT 1")
    assert(BIGINT[Option[Long]].queryString === "BIGINT NULL")
    assert(BIGINT[Option[Long]].UNSIGNED.queryString === "BIGINT UNSIGNED NULL")
    assert(BIGINT[Option[Long]].DEFAULT(None).queryString === "BIGINT NULL DEFAULT NULL")
    assert(BIGINT[Option[Long]].DEFAULT(Some(2)).queryString === "BIGINT NULL DEFAULT 2")
    assert(BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString === "BIGINT UNSIGNED NULL DEFAULT NULL")
  }

  it should "The query string generated from the Decimal DataType model matches the specified one." in {
    val decimalType = DataType.Decimal[BigDecimal](10, 5, false, false, false, None)
    assert(decimalType.typeName === "DECIMAL(10, 5)")
    assert(decimalType.sqlType === Types.DECIMAL)
    assert(decimalType.isOptional === false)
    assert(decimalType.queryString === "DECIMAL(10, 5) NOT NULL")
    assert(decimalType.toOption.isOptional === true)
    assert(decimalType.toOption.queryString === "DECIMAL(10, 5) NULL")
    assert(decimalType.DEFAULT(BigDecimal(1.5)).queryString === "DECIMAL(10, 5) NOT NULL DEFAULT '1.5'")
    assert(decimalType.UNSIGNED.queryString === "DECIMAL(10, 5) UNSIGNED NOT NULL")
    assert(DECIMAL[BigDecimal](10, 7).queryString === "DECIMAL(10, 7) NOT NULL")
    assert(
      DECIMAL[BigDecimal](10, 7)
        .DEFAULT(BigDecimal(10, 7))
        .queryString === "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'"
    )
    assert(DECIMAL[Option[BigDecimal]](10, 7).queryString === "DECIMAL(10, 7) NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7).DEFAULT(None).queryString === "DECIMAL(10, 7) NULL DEFAULT NULL")
    assert(
      DECIMAL[Option[BigDecimal]](10, 7)
        .DEFAULT(Some(BigDecimal(10, 7)))
        .queryString === "DECIMAL(10, 7) NULL DEFAULT '0.0000010'"
    )
  }

  it should "The query string generated from the Char DataType model matches the specified one." in {
    val charType = DataType.CChar[String](10, false, None, None, None)
    assert(charType.typeName === "CHAR(10)")
    assert(charType.sqlType === Types.CHAR)
    assert(charType.isOptional === false)
    assert(charType.queryString === "CHAR(10) NOT NULL")
    assert(charType.toOption.isOptional === true)
    assert(charType.toOption.queryString === "CHAR(10) NULL")
    assert(charType.DEFAULT("test").queryString === "CHAR(10) NOT NULL DEFAULT 'test'")
    assert(CHAR[String](0).queryString === "CHAR(0) NOT NULL")
    assert(CHAR[Option[String]](0).queryString === "CHAR(0) NULL")
    assert(CHAR[Option[String]](0).DEFAULT(None).queryString === "CHAR(0) NULL DEFAULT NULL")
    assert(CHAR[Option[String]](0).DEFAULT(Some("test")).queryString === "CHAR(0) NULL DEFAULT 'test'")
  }

  it should "The query string generated from the Varchar DataType model matches the specified one." in {
    val varcharType = DataType.Varchar[String](50, false, None, None, None)
    assert(varcharType.typeName === "VARCHAR(50)")
    assert(varcharType.sqlType === Types.VARCHAR)
    assert(varcharType.isOptional === false)
    assert(varcharType.queryString === "VARCHAR(50) NOT NULL")
    assert(varcharType.toOption.isOptional === true)
    assert(varcharType.toOption.queryString === "VARCHAR(50) NULL")
    assert(varcharType.DEFAULT("test").queryString === "VARCHAR(50) NOT NULL DEFAULT 'test'")
    assert(VARCHAR[String](0).queryString === "VARCHAR(0) NOT NULL")
    assert(VARCHAR[Option[String]](0).queryString === "VARCHAR(0) NULL")
    assert(VARCHAR[Option[String]](0).DEFAULT(None).queryString === "VARCHAR(0) NULL DEFAULT NULL")
    assert(
      VARCHAR[Option[String]](0)
        .DEFAULT(Some("test"))
        .queryString === "VARCHAR(0) NULL DEFAULT 'test'"
    )
  }

  it should "The query string generated from the Binary DataType model matches the specified one." in {
    val binaryType = DataType.Binary[Array[Byte]](10, false, None, None, None)
    assert(binaryType.typeName === "BINARY(10)")
    assert(binaryType.sqlType === Types.BINARY)
    assert(binaryType.isOptional === false)
    assert(binaryType.queryString === "BINARY(10) NOT NULL")
    assert(binaryType.toOption.isOptional === true)
    assert(binaryType.toOption.queryString === "BINARY(10) NULL")
    assert(BINARY[Array[Byte]](0).queryString === "BINARY(0) NOT NULL")
    assert(BINARY[Option[Array[Byte]]](0).queryString === "BINARY(0) NULL")
  }

  it should "The query string generated from the Varbinary DataType model matches the specified one." in {
    val varbinaryType = DataType.Varbinary[Array[Byte]](50, false, None, None, None)
    assert(varbinaryType.typeName === "VARBINARY(50)")
    assert(varbinaryType.sqlType === Types.VARBINARY)
    assert(varbinaryType.isOptional === false)
    assert(varbinaryType.queryString === "VARBINARY(50) NOT NULL")
    assert(varbinaryType.toOption.isOptional === true)
    assert(varbinaryType.toOption.queryString === "VARBINARY(50) NULL")
    assert(VARBINARY[Array[Byte]](0).queryString === "VARBINARY(0) NOT NULL")
    assert(VARBINARY[Option[Array[Byte]]](0).queryString === "VARBINARY(0) NULL")
  }

  it should "The query string generated from the Tinyblob DataType model matches the specified one." in {
    val tinyblobType = DataType.Tinyblob[Array[Byte]](false, None)
    assert(tinyblobType.typeName === "TINYBLOB")
    assert(tinyblobType.sqlType === Types.VARBINARY)
    assert(tinyblobType.isOptional === false)
    assert(tinyblobType.queryString === "TINYBLOB NOT NULL")
    assert(tinyblobType.toOption.isOptional === true)
    assert(tinyblobType.toOption.queryString === "TINYBLOB NULL")
    assert(TINYBLOB[Array[Byte]]().queryString === "TINYBLOB NOT NULL")
    assert(TINYBLOB[Option[Array[Byte]]]().queryString === "TINYBLOB NULL")
    assert(TINYBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "TINYBLOB NULL DEFAULT NULL")
  }

  it should "The query string generated from the Blob DataType model matches the specified one." in {
    val blobType = DataType.Blob[Array[Byte]](None, false, None)
    assert(blobType.typeName === "BLOB")
    assert(blobType.sqlType === Types.BLOB)
    assert(blobType.isOptional === false)
    assert(blobType.queryString === "BLOB NOT NULL")
    assert(blobType.toOption.isOptional === true)
    assert(blobType.toOption.queryString === "BLOB NULL")
    assert(BLOB[Array[Byte]](0).queryString === "BLOB(0) NOT NULL")
    assert(BLOB[Option[Array[Byte]]](0).queryString === "BLOB(0) NULL")
    assert(BLOB[Option[Array[Byte]]](0).DEFAULT(None).queryString === "BLOB(0) NULL DEFAULT NULL")
  }

  it should "The query string generated from the Mediumblob DataType model matches the specified one." in {
    val mediumblobType = DataType.Mediumblob[Array[Byte]](false, None)
    assert(mediumblobType.typeName === "MEDIUMBLOB")
    assert(mediumblobType.sqlType === Types.LONGVARBINARY)
    assert(mediumblobType.isOptional === false)
    assert(mediumblobType.queryString === "MEDIUMBLOB NOT NULL")
    assert(mediumblobType.toOption.isOptional === true)
    assert(mediumblobType.toOption.queryString === "MEDIUMBLOB NULL")
    assert(MEDIUMBLOB[Array[Byte]]().queryString === "MEDIUMBLOB NOT NULL")
    assert(MEDIUMBLOB[Option[Array[Byte]]]().queryString === "MEDIUMBLOB NULL")
    assert(MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "MEDIUMBLOB NULL DEFAULT NULL")
  }

  it should "The query string generated from the LongBlob DataType model matches the specified one." in {
    val longblobType = DataType.LongBlob[Array[Byte]](false, None)
    assert(longblobType.typeName === "LONGBLOB")
    assert(longblobType.sqlType === Types.LONGVARBINARY)
    assert(longblobType.isOptional === false)
    assert(longblobType.queryString === "LONGBLOB NOT NULL")
    assert(longblobType.toOption.isOptional === true)
    assert(longblobType.toOption.queryString === "LONGBLOB NULL")
    assert(LONGBLOB[Array[Byte]]().queryString === "LONGBLOB NOT NULL")
    assert(LONGBLOB[Option[Array[Byte]]]().queryString === "LONGBLOB NULL")
    assert(LONGBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "LONGBLOB NULL DEFAULT NULL")
  }

  it should "The query string generated from the TinyText DataType model matches the specified one." in {
    val tinytextType = DataType.TinyText[String](false, None, None, None)
    assert(tinytextType.typeName === "TINYTEXT")
    assert(tinytextType.sqlType === Types.VARCHAR)
    assert(tinytextType.isOptional === false)
    assert(tinytextType.queryString === "TINYTEXT NOT NULL")
    assert(tinytextType.toOption.isOptional === true)
    assert(tinytextType.toOption.queryString === "TINYTEXT NULL")
    assert(TINYTEXT[String]().queryString === "TINYTEXT NOT NULL")
    assert(TINYTEXT[Option[String]]().queryString === "TINYTEXT NULL")
    assert(TINYTEXT[Option[String]]().DEFAULT(None).queryString === "TINYTEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the Text DataType model matches the specified one." in {
    val textType = DataType.Text[String](false, None, None, None)
    assert(textType.typeName === "TEXT")
    assert(textType.sqlType === Types.LONGVARCHAR)
    assert(textType.isOptional === false)
    assert(textType.queryString === "TEXT NOT NULL")
    assert(textType.toOption.isOptional === true)
    assert(textType.toOption.queryString === "TEXT NULL")
    assert(TEXT[String]().queryString === "TEXT NOT NULL")
    assert(TEXT[Option[String]]().queryString === "TEXT NULL")
    assert(TEXT[Option[String]]().DEFAULT(None).queryString === "TEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the MediumText DataType model matches the specified one." in {
    val mediumtextType = DataType.MediumText[String](false, None, None, None)
    assert(mediumtextType.typeName === "MEDIUMTEXT")
    assert(mediumtextType.sqlType === Types.LONGVARCHAR)
    assert(mediumtextType.isOptional === false)
    assert(mediumtextType.queryString === "MEDIUMTEXT NOT NULL")
    assert(mediumtextType.toOption.isOptional === true)
    assert(mediumtextType.toOption.queryString === "MEDIUMTEXT NULL")
    assert(MEDIUMTEXT[String]().queryString === "MEDIUMTEXT NOT NULL")
    assert(MEDIUMTEXT[Option[String]]().queryString === "MEDIUMTEXT NULL")
    assert(MEDIUMTEXT[Option[String]]().DEFAULT(None).queryString === "MEDIUMTEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the LongText DataType model matches the specified one." in {
    val longtextType = DataType.LongText[String](false, None, None, None)
    assert(longtextType.typeName === "LONGTEXT")
    assert(longtextType.sqlType === Types.LONGVARCHAR)
    assert(longtextType.isOptional === false)
    assert(longtextType.queryString === "LONGTEXT NOT NULL")
    assert(longtextType.toOption.isOptional === true)
    assert(longtextType.toOption.queryString === "LONGTEXT NULL")
    assert(LONGTEXT[String]().queryString === "LONGTEXT NOT NULL")
    assert(LONGTEXT[Option[String]]().queryString === "LONGTEXT NULL")
    assert(LONGTEXT[Option[String]]().DEFAULT(None).queryString === "LONGTEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the Enum DataType model matches the specified one." in {
    enum Status:
      case Active, InActive

    val enumType = DataType.Enum[Status](List("Active", "InActive"), false, None, None, None)
    assert(enumType.typeName === "ENUM('Active','InActive')")
    assert(enumType.sqlType === Types.CHAR)
    assert(enumType.isOptional === false)
    assert(enumType.queryString === "ENUM('Active','InActive') NOT NULL")
    assert(enumType.toOption.isOptional === true)
    assert(enumType.toOption.queryString === "ENUM('Active','InActive') NULL")
    assert(enumType.DEFAULT(Status.Active).queryString === "ENUM('Active','InActive') NOT NULL DEFAULT 'Active'")
    assert(ENUM[Status].queryString === "ENUM('Active','InActive') NOT NULL")
  }

  it should "The query string generated from the Date DataType model matches the specified one." in {
    val dateType = DataType.Date[LocalDate](false, None)
    assert(dateType.typeName === "DATE")
    assert(dateType.sqlType === Types.DATE)
    assert(dateType.isOptional === false)
    assert(dateType.queryString === "DATE NOT NULL")
    assert(dateType.toOption.isOptional === true)
    assert(dateType.toOption.queryString === "DATE NULL")
    assert(dateType.DEFAULT(LocalDate.of(2023, 1, 1)).queryString === "DATE NOT NULL DEFAULT '2023-01-01'")
    assert(dateType.DEFAULT_CURRENT_DATE().queryString === "DATE NOT NULL DEFAULT (CURRENT_DATE)")
    assert(DATE[LocalDate].queryString === "DATE NOT NULL")
    assert(DATE[Option[LocalDate]].queryString === "DATE NULL")
    assert(DATE[Option[LocalDate]].DEFAULT(None).queryString === "DATE NULL DEFAULT NULL")
    assert(
      DATE[Option[LocalDate]]
        .DEFAULT(Some(LocalDate.of(2023, 2, 10)))
        .queryString === "DATE NULL DEFAULT '2023-02-10'"
    )
    assert(DATE[Option[LocalDate]].DEFAULT(0).queryString === "DATE NULL DEFAULT 0")
    assert(DATE[Option[LocalDate]].DEFAULT("2023-02-10").queryString === "DATE NULL DEFAULT '2023-02-10'")
    assert(DATE[Option[LocalDate]].DEFAULT_CURRENT_DATE().queryString === "DATE NULL DEFAULT (CURRENT_DATE)")
  }

  it should "The query string generated from the DateTime DataType model matches the specified one." in {
    val dateTimeType = DataType.DateTime[LocalDateTime](None, false, None)
    assert(dateTimeType.typeName === "DATETIME")
    assert(dateTimeType.sqlType === Types.TIMESTAMP)
    assert(dateTimeType.isOptional === false)
    assert(dateTimeType.queryString === "DATETIME NOT NULL")
    assert(dateTimeType.toOption.isOptional === true)
    assert(dateTimeType.toOption.queryString === "DATETIME NULL")
    assert(
      dateTimeType
        .DEFAULT(LocalDateTime.of(2023, 1, 1, 12, 0))
        .queryString === "DATETIME NOT NULL DEFAULT '2023-01-01T12:00'"
    )
    assert(dateTimeType.DEFAULT_CURRENT_TIMESTAMP().queryString === "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP")
    assert(
      dateTimeType
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString === "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assert(DATETIME[LocalDateTime].queryString === "DATETIME NOT NULL")
    assert(DATETIME[Option[LocalDateTime]].queryString === "DATETIME NULL")
    assert(DATETIME[Option[LocalDateTime]](6).queryString === "DATETIME(6) NULL")
    assert(DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL")
    assert(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
        .queryString === "DATETIME NULL DEFAULT '2023-02-10T10:00'"
    )
    assert(DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL")
    assert(DATETIME[Option[LocalDateTime]].DEFAULT(0).queryString === "DATETIME NULL DEFAULT 0")
    assert(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT("2023-02-10 10:00:00")
        .queryString === "DATETIME NULL DEFAULT '2023-02-10 10:00:00'"
    )
    assert(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP"
    )
    assert(
      DATETIME[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)"
    )
    assert(
      DATETIME[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assert(
      DATETIME[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
    )
  }

  it should "The query string generated from the TimeStamp DataType model matches the specified one." in {
    val timestampType = DataType.TimeStamp[LocalDateTime](None, false, None)
    assert(timestampType.typeName === "TIMESTAMP")
    assert(timestampType.sqlType === Types.TIMESTAMP)
    assert(timestampType.isOptional === false)
    assert(timestampType.queryString === "TIMESTAMP NOT NULL")
    assert(timestampType.toOption.isOptional === true)
    assert(timestampType.toOption.queryString === "TIMESTAMP NULL")
    assert(
      timestampType
        .DEFAULT(LocalDateTime.of(2023, 1, 1, 12, 0))
        .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-01-01T12:00'"
    )
    assert(timestampType.DEFAULT_CURRENT_TIMESTAMP().queryString === "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP")
    assert(
      timestampType
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString === "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assert(TIMESTAMP[LocalDateTime].queryString === "TIMESTAMP NOT NULL")
    assert(TIMESTAMP[Option[LocalDateTime]].queryString === "TIMESTAMP NULL")
    assert(TIMESTAMP[Option[LocalDateTime]](5).queryString === "TIMESTAMP(5) NULL")
    assert(TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL")
    assert(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
        .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10T10:00'"
    )
    assert(TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL")
    assert(TIMESTAMP[Option[LocalDateTime]].DEFAULT(0).queryString === "TIMESTAMP NULL DEFAULT 0")
    assert(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT("2023-02-10 10:00:00")
        .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10 10:00:00'"
    )
    assert(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"
    )
    assert(
      TIMESTAMP[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP()
        .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6)"
    )
    assert(
      TIMESTAMP[Option[LocalDateTime]]
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assert(
      TIMESTAMP[Option[LocalDateTime]](6)
        .DEFAULT_CURRENT_TIMESTAMP(true)
        .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
    )
  }

  it should "The query string generated from the Time DataType model matches the specified one." in {
    val timeType = DataType.Time[LocalTime](None, false, None)
    assert(timeType.typeName === "TIME")
    assert(timeType.sqlType === Types.TIME)
    assert(timeType.isOptional === false)
    assert(timeType.queryString === "TIME NOT NULL")
    assert(timeType.toOption.isOptional === true)
    assert(timeType.toOption.queryString === "TIME NULL")
    assert(timeType.DEFAULT(LocalTime.of(12, 30)).queryString === "TIME NOT NULL DEFAULT '12:30'")
    assert(TIME[LocalTime].queryString === "TIME NOT NULL")
    assert(TIME[LocalTime].DEFAULT(LocalTime.of(10, 0, 10)).queryString === "TIME NOT NULL DEFAULT '10:00:10'")
    assert(TIME[LocalTime].DEFAULT(0).queryString === "TIME NOT NULL DEFAULT 0")
    assert(TIME[LocalTime].DEFAULT("23:59:59").queryString === "TIME NOT NULL DEFAULT '23:59:59'")
    assert(TIME[Option[LocalTime]].queryString === "TIME NULL")
    assert(TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL")
    assert(
      TIME[Option[LocalTime]]
        .DEFAULT(Some(LocalTime.of(10, 0, 0)))
        .queryString === "TIME NULL DEFAULT '10:00'"
    )
    assert(TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL")
    assert(TIME[Option[LocalTime]].DEFAULT(0).queryString === "TIME NULL DEFAULT 0")
    assert(TIME[Option[LocalTime]].DEFAULT("23:59:59").queryString === "TIME NULL DEFAULT '23:59:59'")
  }

  it should "The query string generated from the Year DataType model matches the specified one." in {
    val yearType = DataType.Year[JYear](None, false, None)
    assert(yearType.typeName === "YEAR")
    assert(yearType.sqlType === Types.DATE)
    assert(yearType.isOptional === false)
    assert(yearType.queryString === "YEAR NOT NULL")
    assert(yearType.toOption.isOptional === true)
    assert(yearType.toOption.queryString === "YEAR NULL")
    assert(yearType.DEFAULT(JYear.of(2023)).queryString === "YEAR NOT NULL DEFAULT '2023'")
    assert(YEAR[JYear].queryString === "YEAR NOT NULL")
    assert(YEAR[Option[JYear]].queryString === "YEAR NULL")
    assert(YEAR[Option[JYear]].DEFAULT(None).queryString === "YEAR NULL DEFAULT NULL")
    assert(YEAR[Option[JYear]].DEFAULT(Some(JYear.of(2023))).queryString === "YEAR NULL DEFAULT '2023'")
    assert(YEAR[Option[JYear]].DEFAULT(0).queryString === "YEAR NULL DEFAULT 0")
    assert(YEAR[Option[JYear]].DEFAULT(2023).queryString === "YEAR NULL DEFAULT 2023")
  }

  it should "The query string generated from the Serial DataType model matches the specified one." in {
    val serialType = DataType.Alias.Serial[BigInt]()
    assert(serialType.typeName === "SERIAL")
    assert(serialType.sqlType === Types.BIGINT)
    assert(serialType.isOptional === false)
    assert(serialType.queryString === "SERIAL")
    assert(serialType.toOption.isOptional === true)
    assert(serialType.toOption.queryString === "SERIAL NULL")
    assert(SERIAL[BigInt].queryString === "SERIAL")
  }

  it should "The query string generated from the Boolean DataType model matches the specified one." in {
    val booleanType = DataType.Alias.Bool[Boolean](false, None)
    assert(booleanType.typeName === "BOOLEAN")
    assert(booleanType.sqlType === Types.BOOLEAN)
    assert(booleanType.isOptional === false)
    assert(booleanType.queryString === "BOOLEAN NOT NULL")
    assert(booleanType.toOption.isOptional === true)
    assert(booleanType.toOption.queryString === "BOOLEAN NULL")
    assert(booleanType.DEFAULT(true).queryString === "BOOLEAN NOT NULL DEFAULT true")
    assert(BOOLEAN[Boolean].queryString === "BOOLEAN NOT NULL")
    assert(BOOLEAN[Boolean].DEFAULT(true).queryString === "BOOLEAN NOT NULL DEFAULT true")
    assert(BOOLEAN[Boolean].DEFAULT(false).queryString === "BOOLEAN NOT NULL DEFAULT false")
    assert(BOOLEAN[Option[Boolean]].queryString === "BOOLEAN NULL")
    assert(BOOLEAN[Option[Boolean]].DEFAULT(None).queryString === "BOOLEAN NULL DEFAULT NULL")
    assert(BOOLEAN[Option[Boolean]].DEFAULT(Some(true)).queryString === "BOOLEAN NULL DEFAULT true")
  }
