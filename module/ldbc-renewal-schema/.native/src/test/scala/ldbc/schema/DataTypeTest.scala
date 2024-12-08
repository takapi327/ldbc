/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.time.{ LocalDate, LocalDateTime, LocalTime, Year as JYear }

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.schema.DataType.*
import ldbc.schema.model.EnumDataType

class DataTypeTest extends AnyFlatSpec:

  it should "The query string generated from the Bit DataType model matches the specified one." in {
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
    assert(TINYINT[Byte](1).queryString === "TINYINT(1) NOT NULL")
    assert(TINYINT[Byte](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NOT NULL")
    assert(TINYINT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "TINYINT(64) NOT NULL DEFAULT '98'")
    assert(TINYINT[Byte](64)
      .DEFAULT("byte".getBytes.head)
      .UNSIGNED
      .queryString === "TINYINT(64) UNSIGNED NOT NULL DEFAULT '98'")
    assert(TINYINT[Option[Byte]](1).queryString === "TINYINT(1) NULL")
    assert(TINYINT[Option[Byte]](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NULL")
    assert(TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL")
    assert(TINYINT[Option[Byte]](64)
      .DEFAULT("byte".getBytes.headOption)
      .queryString === "TINYINT(64) NULL DEFAULT '98'")
    assert(TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL")
    assert(TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString === "TINYINT UNSIGNED NULL DEFAULT NULL")
    assert(TINYINT[Byte].queryString === "TINYINT NOT NULL")
    assert(TINYINT[Byte].UNSIGNED.queryString === "TINYINT UNSIGNED NOT NULL")
    assert(TINYINT[Byte].DEFAULT("byte".getBytes.head).queryString === "TINYINT NOT NULL DEFAULT '98'")
    assert(TINYINT[Byte]
      .DEFAULT("byte".getBytes.head)
      .UNSIGNED
      .queryString === "TINYINT UNSIGNED NOT NULL DEFAULT '98'")
    assert(TINYINT[Option[Byte]].queryString === "TINYINT NULL")
    assert(TINYINT[Option[Byte]].UNSIGNED.queryString === "TINYINT UNSIGNED NULL")
    assert(TINYINT[Option[Byte]].DEFAULT(None).queryString === "TINYINT NULL DEFAULT NULL")
    assert(TINYINT[Option[Byte]]
      .DEFAULT("byte".getBytes.headOption)
      .queryString === "TINYINT NULL DEFAULT '98'")
    assert(TINYINT[Option[Byte]].DEFAULT(None).queryString === "TINYINT NULL DEFAULT NULL")
    assert(TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString === "TINYINT UNSIGNED NULL DEFAULT NULL")
  }

  it should "The query string generated from the Smallint DataType model matches the specified one." in {
    assert(SMALLINT[Short](0).queryString === "SMALLINT(0) NOT NULL")
    assert(SMALLINT[Short](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NOT NULL")
    assert(SMALLINT[Short](255).DEFAULT(1).queryString === "SMALLINT(255) NOT NULL DEFAULT 1")
    assert(SMALLINT[Short](255).DEFAULT(1).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NOT NULL DEFAULT 1")
    assert(SMALLINT[Option[Short]](0).queryString === "SMALLINT(0) NULL")
    assert(SMALLINT[Option[Short]](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NULL")
    assert(SMALLINT[Option[Short]](255).DEFAULT(None).queryString === "SMALLINT(255) NULL DEFAULT NULL")
    assert(SMALLINT[Option[Short]](255).DEFAULT(Some(2)).queryString === "SMALLINT(255) NULL DEFAULT 2")
    assert(SMALLINT[Option[Short]](255).DEFAULT(None).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NULL DEFAULT NULL")
  }

  it should "The query string generated from the Mediumint DataType model matches the specified one." in {
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
    assert(DECIMAL[BigDecimal](10, 7).queryString === "DECIMAL(10, 7) NOT NULL")
    assert(DECIMAL[BigDecimal](10, 7)
      .DEFAULT(BigDecimal(10, 7))
      .queryString === "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'")
    assert(DECIMAL[Option[BigDecimal]](10, 7).queryString === "DECIMAL(10, 7) NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7).DEFAULT(None).queryString === "DECIMAL(10, 7) NULL DEFAULT NULL")
    assert(DECIMAL[Option[BigDecimal]](10, 7)
      .DEFAULT(Some(BigDecimal(10, 7)))
      .queryString === "DECIMAL(10, 7) NULL DEFAULT '0.0000010'")
  }

  it should "The query string generated from the Float DataType model matches the specified one." in {
    assert(FLOAT[Float](0).queryString === "FLOAT(0) NOT NULL")
    assert(FLOAT[Float](0).DEFAULT(1.2f).queryString === "FLOAT(0) NOT NULL DEFAULT 1.2000000476837158")
    assert(FLOAT[Option[Float]](0).queryString === "FLOAT(0) NULL")
    assert(FLOAT[Option[Float]](0).DEFAULT(None).queryString === "FLOAT(0) NULL DEFAULT NULL")
    assert(FLOAT[Option[Float]](0).DEFAULT(Some(1.2f)).queryString === "FLOAT(0) NULL DEFAULT 1.2000000476837158")
  }

  it should "The query string generated from the Char DataType model matches the specified one." in {
    assert(CHAR[String](0).queryString === "CHAR(0) NOT NULL")
    assert(CHAR[String](0).DEFAULT("test").queryString === "CHAR(0) NOT NULL DEFAULT 'test'")
    assert(CHAR[Option[String]](0).queryString === "CHAR(0) NULL")
    assert(CHAR[Option[String]](0).DEFAULT(None).queryString === "CHAR(0) NULL DEFAULT NULL")
    assert(CHAR[Option[String]](0).DEFAULT(Some("test")).queryString === "CHAR(0) NULL DEFAULT 'test'")
  }

  it should "The query string generated from the Varchar DataType model matches the specified one." in {
    assert(VARCHAR[String](0).queryString === "VARCHAR(0) NOT NULL")
    assert(VARCHAR[String](0).DEFAULT("test").queryString === "VARCHAR(0) NOT NULL DEFAULT 'test'")
    assert(VARCHAR[Option[String]](0).queryString === "VARCHAR(0) NULL")
    assert(VARCHAR[Option[String]](0).DEFAULT(None).queryString === "VARCHAR(0) NULL DEFAULT NULL")
    assert(VARCHAR[Option[String]](0)
      .DEFAULT(Some("test"))
      .queryString === "VARCHAR(0) NULL DEFAULT 'test'")
  }

  it should "The query string generated from the Binary DataType model matches the specified one." in {
    assert(BINARY[Array[Byte]](0).queryString === "BINARY(0) NOT NULL")
    assert(BINARY[Option[Array[Byte]]](0).queryString === "BINARY(0) NULL")
  }

  it should "The query string generated from the Varbinary DataType model matches the specified one." in {
    assert(VARBINARY[Array[Byte]](0).queryString === "VARBINARY(0) NOT NULL")
    assert(VARBINARY[Option[Array[Byte]]](0).queryString === "VARBINARY(0) NULL")
  }

  it should "The query string generated from the Tinyblob DataType model matches the specified one." in {
    assert(TINYBLOB[Array[Byte]]().queryString === "TINYBLOB NOT NULL")
    assert(TINYBLOB[Option[Array[Byte]]]().queryString === "TINYBLOB NULL")
    assert(TINYBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "TINYBLOB NULL DEFAULT NULL")
  }

  it should "The query string generated from the Blob DataType model matches the specified one." in {
    assert(BLOB[Array[Byte]](0).queryString === "BLOB(0) NOT NULL")
    assert(BLOB[Option[Array[Byte]]](0).queryString === "BLOB(0) NULL")
    assert(BLOB[Option[Array[Byte]]](0).DEFAULT(None).queryString === "BLOB(0) NULL DEFAULT NULL")
  }

  it should "The query string generated from the Mediumblob DataType model matches the specified one." in {
    assert(MEDIUMBLOB[Array[Byte]]().queryString === "MEDIUMBLOB NOT NULL")
    assert(MEDIUMBLOB[Option[Array[Byte]]]().queryString === "MEDIUMBLOB NULL")
    assert(MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "MEDIUMBLOB NULL DEFAULT NULL")
  }

  it should "The query string generated from the LongBlob DataType model matches the specified one." in {
    assert(LONGBLOB[Array[Byte]]().queryString === "LONGBLOB NOT NULL")
    assert(LONGBLOB[Option[Array[Byte]]]().queryString === "LONGBLOB NULL")
    assert(LONGBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "LONGBLOB NULL DEFAULT NULL")
  }

  it should "The query string generated from the TinyText DataType model matches the specified one." in {
    assert(TINYTEXT[String]().queryString === "TINYTEXT NOT NULL")
    assert(TINYTEXT[Option[String]]().queryString === "TINYTEXT NULL")
    assert(TINYTEXT[Option[String]]().DEFAULT(None).queryString === "TINYTEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the Text DataType model matches the specified one." in {
    assert(TEXT[String]().queryString === "TEXT NOT NULL")
    assert(TEXT[Option[String]]().queryString === "TEXT NULL")
    assert(TEXT[Option[String]]().DEFAULT(None).queryString === "TEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the MediumText DataType model matches the specified one." in {
    assert(MEDIUMTEXT[String]().queryString === "MEDIUMTEXT NOT NULL")
    assert(MEDIUMTEXT[Option[String]]().queryString === "MEDIUMTEXT NULL")
    assert(MEDIUMTEXT[Option[String]]().DEFAULT(None).queryString === "MEDIUMTEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the LongText DataType model matches the specified one." in {
    assert(LONGTEXT[String]().queryString === "LONGTEXT NOT NULL")
    assert(LONGTEXT[Option[String]]().queryString === "LONGTEXT NULL")
    assert(LONGTEXT[Option[String]]().DEFAULT(None).queryString === "LONGTEXT NULL DEFAULT NULL")
  }

  it should "The query string generated from the Enum DataType model matches the specified one." in {
    enum Status extends ldbc.schema.model.Enum:
      case Active, InActive
    object Status extends EnumDataType[Status]

    assert(ENUM[Status](using Status).queryString === "ENUM('Active','InActive') NOT NULL")
    assert(ENUM[Status](using Status)
      .DEFAULT(Status.Active)
      .queryString === "ENUM('Active','InActive') NOT NULL DEFAULT 'Active'")
    assert(ENUM[Option[Status]](using Status).queryString === "ENUM('Active','InActive') NULL")
    assert(ENUM[Option[Status]](using Status).DEFAULT(None).queryString === "ENUM('Active','InActive') NULL DEFAULT NULL")
  }

  it should "The query string generated from the Date DataType model matches the specified one." in {
    assert(DATE[LocalDate].queryString === "DATE NOT NULL")
    assert(DATE[LocalDate]
      .DEFAULT(LocalDate.of(2023, 2, 10))
      .queryString === "DATE NOT NULL DEFAULT '2023-02-10'")
    assert(DATE[LocalDate].DEFAULT(0).queryString === "DATE NOT NULL DEFAULT 0")
    assert(DATE[LocalDate].DEFAULT("2023-02-10").queryString === "DATE NOT NULL DEFAULT '2023-02-10'")
    assert(DATE[LocalDate].DEFAULT_CURRENT_DATE().queryString === "DATE NOT NULL DEFAULT (CURRENT_DATE)")
    assert(DATE[Option[LocalDate]].queryString === "DATE NULL")
    assert(DATE[Option[LocalDate]].DEFAULT(None).queryString === "DATE NULL DEFAULT NULL")
    assert(DATE[Option[LocalDate]]
      .DEFAULT(Some(LocalDate.of(2023, 2, 10)))
      .queryString === "DATE NULL DEFAULT '2023-02-10'")
    assert(DATE[Option[LocalDate]].DEFAULT(0).queryString === "DATE NULL DEFAULT 0")
    assert(DATE[Option[LocalDate]].DEFAULT("2023-02-10").queryString === "DATE NULL DEFAULT '2023-02-10'")
    assert(DATE[Option[LocalDate]].DEFAULT_CURRENT_DATE().queryString === "DATE NULL DEFAULT (CURRENT_DATE)")
  }

  it should "The query string generated from the DateTime DataType model matches the specified one." in {
    assert(DATETIME[LocalDateTime].queryString === "DATETIME NOT NULL")
    assert(DATETIME[LocalDateTime](6).queryString === "DATETIME(6) NOT NULL")
    assert(DATETIME[LocalDateTime]
      .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
      .queryString === "DATETIME NOT NULL DEFAULT '2023-02-10T10:00'")
    assert(DATETIME[LocalDateTime].DEFAULT(0).queryString === "DATETIME NOT NULL DEFAULT 0")
    assert(DATETIME[LocalDateTime]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "DATETIME NOT NULL DEFAULT '2023-02-10 10:00:00'")
    assert(DATETIME[Option[LocalDateTime]].queryString === "DATETIME NULL")
    assert(DATETIME[Option[LocalDateTime]](6).queryString === "DATETIME(6) NULL")
    assert(DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL")
    assert(DATETIME[Option[LocalDateTime]]
      .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
      .queryString === "DATETIME NULL DEFAULT '2023-02-10T10:00'")
    assert(DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL")
    assert(DATETIME[Option[LocalDateTime]].DEFAULT(0).queryString === "DATETIME NULL DEFAULT 0")
    assert(DATETIME[Option[LocalDateTime]]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "DATETIME NULL DEFAULT '2023-02-10 10:00:00'")
    assert(DATETIME[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP")
    assert(DATETIME[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)")
    assert(DATETIME[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    assert(DATETIME[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
  }

  it should "The query string generated from the TimeStamp DataType model matches the specified one." in {
    assert(TIMESTAMP[LocalDateTime].queryString === "TIMESTAMP NOT NULL")
    assert(TIMESTAMP[LocalDateTime](6).queryString === "TIMESTAMP(6) NOT NULL")
    assert(TIMESTAMP[LocalDateTime]
      .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
      .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-02-10T10:00'")
    assert(TIMESTAMP[LocalDateTime].DEFAULT(0).queryString === "TIMESTAMP NOT NULL DEFAULT 0")
    assert(TIMESTAMP[LocalDateTime]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-02-10 10:00:00'")
    assert(TIMESTAMP[Option[LocalDateTime]].queryString === "TIMESTAMP NULL")
    assert(TIMESTAMP[Option[LocalDateTime]](5).queryString === "TIMESTAMP(5) NULL")
    assert(TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL")
    assert(TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
      .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10T10:00'")
    assert(TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL")
    assert(TIMESTAMP[Option[LocalDateTime]].DEFAULT(0).queryString === "TIMESTAMP NULL DEFAULT 0")
    assert(TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10 10:00:00'")
    assert(TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP")
    assert(TIMESTAMP[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6)")
    assert(TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    assert(TIMESTAMP[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)")
  }

  it should "The query string generated from the Time DataType model matches the specified one." in {
    assert(TIME[LocalTime].queryString === "TIME NOT NULL")
    assert(TIME[LocalTime].DEFAULT(LocalTime.of(10, 0, 10)).queryString === "TIME NOT NULL DEFAULT '10:00:10'")
    assert(TIME[LocalTime].DEFAULT(0).queryString === "TIME NOT NULL DEFAULT 0")
    assert(TIME[LocalTime].DEFAULT("23:59:59").queryString === "TIME NOT NULL DEFAULT '23:59:59'")
    assert(TIME[Option[LocalTime]].queryString === "TIME NULL")
    assert(TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL")
    assert(TIME[Option[LocalTime]]
      .DEFAULT(Some(LocalTime.of(10, 0, 0)))
      .queryString === "TIME NULL DEFAULT '10:00'")
    assert(TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL")
    assert(TIME[Option[LocalTime]].DEFAULT(0).queryString === "TIME NULL DEFAULT 0")
    assert(TIME[Option[LocalTime]].DEFAULT("23:59:59").queryString === "TIME NULL DEFAULT '23:59:59'")
  }

  it should "The query string generated from the Year DataType model matches the specified one." in {
    assert(YEAR[JYear].queryString === "YEAR NOT NULL")
    assert(YEAR[JYear].DEFAULT(JYear.of(2023)).queryString === "YEAR NOT NULL DEFAULT '2023'")
    assert(YEAR[JYear].DEFAULT(0).queryString === "YEAR NOT NULL DEFAULT 0")
    assert(YEAR[JYear].DEFAULT(2023).queryString === "YEAR NOT NULL DEFAULT 2023")
    assert(YEAR[Option[JYear]].queryString === "YEAR NULL")
    assert(YEAR[Option[JYear]].DEFAULT(None).queryString === "YEAR NULL DEFAULT NULL")
    assert(YEAR[Option[JYear]].DEFAULT(Some(JYear.of(2023))).queryString === "YEAR NULL DEFAULT '2023'")
    assert(YEAR[Option[JYear]].DEFAULT(0).queryString === "YEAR NULL DEFAULT 0")
    assert(YEAR[Option[JYear]].DEFAULT(2023).queryString === "YEAR NULL DEFAULT 2023")
  }

  it should "The query string generated from the Serial DataType model matches the specified one." in {
    assert(SERIAL[BigInt].queryString === "BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE KEY")
  }

  it should "The query string generated from the Boolean DataType model matches the specified one." in {
    assert(BOOLEAN[Boolean].queryString === "BOOLEAN NOT NULL")
    assert(BOOLEAN[Boolean].DEFAULT(true).queryString === "BOOLEAN NOT NULL DEFAULT true")
    assert(BOOLEAN[Boolean].DEFAULT(false).queryString === "BOOLEAN NOT NULL DEFAULT false")
    assert(BOOLEAN[Option[Boolean]].queryString === "BOOLEAN NULL")
    assert(BOOLEAN[Option[Boolean]].DEFAULT(None).queryString === "BOOLEAN NULL DEFAULT NULL")
    assert(BOOLEAN[Option[Boolean]].DEFAULT(Some(true)).queryString === "BOOLEAN NULL DEFAULT true")
  }
