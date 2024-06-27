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
    BIT[Byte](1).queryString === "BIT(1) NOT NULL" &&
    BIT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "BIT(64) NOT NULL DEFAULT 98" &&
    BIT[Option[Short]](1).queryString === "BIT(1) NULL" &&
    BIT[Option[Short]](64).DEFAULT(None).queryString === "BIT(64) NULL DEFAULT NULL" &&
    BIT[Byte].queryString === "BIT NOT NULL" &&
    BIT[Byte].DEFAULT("byte".getBytes.head).queryString === "BIT NOT NULL DEFAULT 98" &&
    BIT[Option[Short]].queryString === "BIT NULL" &&
    BIT[Option[Short]].DEFAULT(None).queryString === "BIT NULL DEFAULT NULL"
  }

  it should "The query string generated from the Tinyint DataType model matches the specified one." in {
    TINYINT[Byte](1).queryString === "TINYINT(1) NOT NULL" &&
    TINYINT[Byte](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NOT NULL" &&
    TINYINT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "TINYINT(64) NOT NULL DEFAULT 98" &&
    TINYINT[Byte](64)
      .DEFAULT("byte".getBytes.head)
      .UNSIGNED
      .queryString === "TINYINT(64) UNSIGNED NOT NULL DEFAULT 98" &&
    TINYINT[Option[Byte]](1).queryString === "TINYINT(1) NULL" &&
    TINYINT[Option[Byte]](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NULL" &&
    TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL" &&
    TINYINT[Option[Byte]](64)
      .DEFAULT("byte".getBytes.headOption)
      .queryString === "TINYINT(64) NULL DEFAULT 98" &&
    TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL" &&
    TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString === "TINYINT UNSIGNED NULL DEFAULT NULL" &&
    TINYINT[Byte].queryString === "TINYINT NOT NULL" &&
    TINYINT[Byte].UNSIGNED.queryString === "TINYINT UNSIGNED NOT NULL" &&
    TINYINT[Byte].DEFAULT("byte".getBytes.head).queryString === "TINYINT NOT NULL DEFAULT 98" &&
    TINYINT[Byte]
      .DEFAULT("byte".getBytes.head)
      .UNSIGNED
      .queryString === "TINYINT UNSIGNED NOT NULL DEFAULT 98" &&
    TINYINT[Option[Byte]].queryString === "TINYINT NULL" &&
    TINYINT[Option[Byte]].UNSIGNED.queryString === "TINYINT UNSIGNED NULL" &&
    TINYINT[Option[Byte]].DEFAULT(None).queryString === "TINYINT NULL DEFAULT NULL" &&
    TINYINT[Option[Byte]]
      .DEFAULT("byte".getBytes.headOption)
      .queryString === "TINYINT NULL DEFAULT 98" &&
    TINYINT[Option[Byte]].DEFAULT(None).queryString === "TINYINT NULL DEFAULT NULL" &&
    TINYINT[Option[Byte]].DEFAULT(None).UNSIGNED.queryString === "TINYINT UNSIGNED NULL DEFAULT NULL"
  }

  it should "The query string generated from the Smallint DataType model matches the specified one." in {
    SMALLINT[Short](0).queryString === "SMALLINT(0) NOT NULL" &&
    SMALLINT[Short](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NOT NULL" &&
    SMALLINT[Short](255).DEFAULT(1).queryString === "SMALLINT(255) NOT NULL DEFAULT 1" &&
    SMALLINT[Short](255).DEFAULT(1).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NOT NULL DEFAULT 1" &&
    SMALLINT[Option[Short]](0).queryString === "SMALLINT(0) NULL" &&
    SMALLINT[Option[Short]](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NULL" &&
    SMALLINT[Option[Short]](255).DEFAULT(None).queryString === "SMALLINT(255) NULL DEFAULT NULL" &&
    SMALLINT[Option[Short]](255).DEFAULT(Some(2)).queryString === "SMALLINT(255) NULL DEFAULT 2" &&
    SMALLINT[Option[Short]](255).DEFAULT(None).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NULL DEFAULT NULL"
  }

  it should "The query string generated from the Mediumint DataType model matches the specified one." in {
    MEDIUMINT[Int](0).queryString === "MEDIUMINT(0) NOT NULL" &&
    MEDIUMINT[Int](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NOT NULL" &&
    MEDIUMINT[Int](255).DEFAULT(1).queryString === "MEDIUMINT(255) NOT NULL DEFAULT 1" &&
    MEDIUMINT[Int](255).DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT(255) UNSIGNED NOT NULL DEFAULT 1" &&
    MEDIUMINT[Option[Int]](0).queryString === "MEDIUMINT(0) NULL" &&
    MEDIUMINT[Option[Int]](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NULL" &&
    MEDIUMINT[Option[Int]](255).DEFAULT(None).queryString === "MEDIUMINT(255) NULL DEFAULT NULL" &&
    MEDIUMINT[Option[Int]](255).DEFAULT(Some(2)).queryString === "MEDIUMINT(255) NULL DEFAULT 2" &&
    MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL DEFAULT NULL" &&
    MEDIUMINT[Int].queryString === "MEDIUMINT NOT NULL" &&
    MEDIUMINT[Int].UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL" &&
    MEDIUMINT[Int].DEFAULT(1).queryString === "MEDIUMINT NOT NULL DEFAULT 1" &&
    MEDIUMINT[Int].DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NOT NULL DEFAULT 1" &&
    MEDIUMINT[Option[Int]].queryString === "MEDIUMINT NULL" &&
    MEDIUMINT[Option[Int]].UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL" &&
    MEDIUMINT[Option[Int]].DEFAULT(None).queryString === "MEDIUMINT NULL DEFAULT NULL" &&
    MEDIUMINT[Option[Int]].DEFAULT(Some(2)).queryString === "MEDIUMINT NULL DEFAULT 2" &&
    MEDIUMINT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT UNSIGNED NULL DEFAULT NULL"
  }

  it should "The query string generated from the Integer DataType model matches the specified one." in {
    INT[Int](0).queryString === "INT(0) NOT NULL" &&
    INT[Int](0).UNSIGNED.queryString === "INT(0) UNSIGNED NOT NULL" &&
    INT[Int](255).DEFAULT(1).queryString === "INT(255) NOT NULL DEFAULT 1" &&
    INT[Int](255).DEFAULT(1).UNSIGNED.queryString === "INT(255) UNSIGNED NOT NULL DEFAULT 1" &&
    INT[Option[Int]](0).queryString === "INT(0) NULL" &&
    INT[Option[Int]](0).UNSIGNED.queryString === "INT(0) UNSIGNED NULL" &&
    INT[Option[Int]](255).DEFAULT(None).queryString === "INT(255) NULL DEFAULT NULL" &&
    INT[Option[Int]](255).DEFAULT(Some(2)).queryString === "INT(255) NULL DEFAULT 2" &&
    INT[Option[Int]](255).DEFAULT(None).UNSIGNED.queryString === "INT(255) UNSIGNED NULL DEFAULT NULL" &&
    INT[Int].queryString === "INT NOT NULL" &&
    INT[Int].UNSIGNED.queryString === "INT UNSIGNED NOT NULL" &&
    INT[Int].DEFAULT(1).queryString === "INT NOT NULL DEFAULT 1" &&
    INT[Int].DEFAULT(1).UNSIGNED.queryString === "INT UNSIGNED NOT NULL DEFAULT 1" &&
    INT[Option[Int]].queryString === "INT NULL" &&
    INT[Option[Int]].UNSIGNED.queryString === "INT UNSIGNED NULL" &&
    INT[Option[Int]].DEFAULT(None).queryString === "INT NULL DEFAULT NULL" &&
    INT[Option[Int]].DEFAULT(Some(2)).queryString === "INT NULL DEFAULT 2" &&
    INT[Option[Int]].DEFAULT(None).UNSIGNED.queryString === "INT UNSIGNED NULL DEFAULT NULL"
  }

  it should "The query string generated from the Bigint DataType model matches the specified one." in {
    BIGINT[Long](0).queryString === "BIGINT(0) NOT NULL" &&
    BIGINT[Long](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NOT NULL" &&
    BIGINT[Long](255).DEFAULT(1).queryString === "BIGINT(255) NOT NULL DEFAULT 1" &&
    BIGINT[Long](255).DEFAULT(1).UNSIGNED.queryString === "BIGINT(255) UNSIGNED NOT NULL DEFAULT 1" &&
    BIGINT[Option[Long]](0).queryString === "BIGINT(0) NULL" &&
    BIGINT[Option[Long]](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NULL" &&
    BIGINT[Option[Long]](255).DEFAULT(None).queryString === "BIGINT(255) NULL DEFAULT NULL" &&
    BIGINT[Option[Long]](255).DEFAULT(Some(2)).queryString === "BIGINT(255) NULL DEFAULT 2" &&
    BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString === "BIGINT UNSIGNED NULL DEFAULT NULL" &&
    BIGINT[Long].queryString === "BIGINT NOT NULL" &&
    BIGINT[Long].UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL" &&
    BIGINT[Long].DEFAULT(1).queryString === "BIGINT NOT NULL DEFAULT 1" &&
    BIGINT[Long].DEFAULT(1).UNSIGNED.queryString === "BIGINT UNSIGNED NOT NULL DEFAULT 1" &&
    BIGINT[Option[Long]].queryString === "BIGINT NULL" &&
    BIGINT[Option[Long]].UNSIGNED.queryString === "BIGINT UNSIGNED NULL" &&
    BIGINT[Option[Long]].DEFAULT(None).queryString === "BIGINT NULL DEFAULT NULL" &&
    BIGINT[Option[Long]].DEFAULT(Some(2)).queryString === "BIGINT NULL DEFAULT 2" &&
    BIGINT[Option[Long]].DEFAULT(None).UNSIGNED.queryString === "BIGINT UNSIGNED NULL DEFAULT NULL"
  }

  it should "The query string generated from the Decimal DataType model matches the specified one." in {
    DECIMAL[BigDecimal](10, 7).queryString === "DECIMAL(10, 7) NOT NULL" &&
    DECIMAL[BigDecimal](10, 7)
      .DEFAULT(BigDecimal(10, 7))
      .queryString === "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'" &&
    DECIMAL[Option[BigDecimal]](10, 7).queryString === "DECIMAL(10, 7) NULL" &&
    DECIMAL[Option[BigDecimal]](10, 7).DEFAULT(None).queryString === "DECIMAL(10, 7) NULL DEFAULT NULL" &&
    DECIMAL[Option[BigDecimal]](10, 7)
      .DEFAULT(Some(BigDecimal(10, 7)))
      .queryString === "DECIMAL(10, 7) NULL DEFAULT '0.0000010'"
  }

  it should "The query string generated from the Float DataType model matches the specified one." in {
    FLOAT[Float](0).queryString === "FLOAT(0) NOT NULL" &&
    FLOAT[Float](0).DEFAULT(1.2f).queryString === "FLOAT(0) NOT NULL DEFAULT 1.2000000476837158" &&
    FLOAT[Option[Float]](0).queryString === "FLOAT(0) NULL" &&
    FLOAT[Option[Float]](0).DEFAULT(None).queryString === "FLOAT(0) NULL DEFAULT NULL" &&
    FLOAT[Option[Float]](0).DEFAULT(Some(1.2f)).queryString === "FLOAT(0) NULL DEFAULT 1.2000000476837158"
  }

  it should "The query string generated from the Char DataType model matches the specified one." in {
    CHAR[String](0).queryString === "CHAR(0) NOT NULL" &&
    CHAR[String](0).DEFAULT("test").queryString === "CHAR(0) NOT NULL DEFAULT 'test'" &&
    CHAR[Option[String]](0).queryString === "CHAR(0) NULL" &&
    CHAR[Option[String]](0).DEFAULT(None).queryString === "CHAR(0) NULL DEFAULT NULL" &&
    CHAR[Option[String]](0).DEFAULT(Some("test")).queryString === "CHAR(0) NULL DEFAULT 'test'"
  }

  it should "The query string generated from the Varchar DataType model matches the specified one." in {
    VARCHAR[String](0).queryString === "VARCHAR(0) NOT NULL" &&
    VARCHAR[String](0).DEFAULT("test").queryString === "VARCHAR(0) NOT NULL DEFAULT 'test'" &&
    VARCHAR[Option[String]](0).queryString === "VARCHAR(0) NULL" &&
    VARCHAR[Option[String]](0).DEFAULT(None).queryString === "VARCHAR(0) NULL DEFAULT NULL" &&
    VARCHAR[Option[String]](0)
      .DEFAULT(Some("test"))
      .queryString === "VARCHAR(0) NULL DEFAULT 'test'"
  }

  it should "The query string generated from the Binary DataType model matches the specified one." in {
    BINARY[Array[Byte]](0).queryString === "BINARY(0) NOT NULL" &&
    BINARY[Option[Array[Byte]]](0).queryString === "BINARY(0) NULL"
  }

  it should "The query string generated from the Varbinary DataType model matches the specified one." in {
    VARBINARY[Array[Byte]](0).queryString === "VARBINARY(0) NOT NULL" &&
    VARBINARY[Option[Array[Byte]]](0).queryString === "VARBINARY(0) NULL"
  }

  it should "The query string generated from the Tinyblob DataType model matches the specified one." in {
    TINYBLOB[Array[Byte]]().queryString === "TINYBLOB NOT NULL" &&
    TINYBLOB[Option[Array[Byte]]]().queryString === "TINYBLOB NULL" &&
    TINYBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "TINYBLOB NULL DEFAULT NULL"
  }

  it should "The query string generated from the Blob DataType model matches the specified one." in {
    BLOB[Array[Byte]](0).queryString === "BLOB(0) NOT NULL" &&
    BLOB[Option[Array[Byte]]](0).queryString === "BLOB(0) NULL" &&
    BLOB[Option[Array[Byte]]](0).DEFAULT(None).queryString === "BLOB(0) NULL DEFAULT NULL"
  }

  it should "The query string generated from the Mediumblob DataType model matches the specified one." in {
    MEDIUMBLOB[Array[Byte]]().queryString === "MEDIUMBLOB NOT NULL" &&
    MEDIUMBLOB[Option[Array[Byte]]]().queryString === "MEDIUMBLOB NULL" &&
    MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "MEDIUMBLOB NULL DEFAULT NULL"
  }

  it should "The query string generated from the LongBlob DataType model matches the specified one." in {
    LONGBLOB[Array[Byte]]().queryString === "LONGBLOB NOT NULL" &&
    LONGBLOB[Option[Array[Byte]]]().queryString === "LONGBLOB NULL" &&
    LONGBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "LONGBLOB NULL DEFAULT NULL"
  }

  it should "The query string generated from the TinyText DataType model matches the specified one." in {
    TINYTEXT[String]().queryString === "TINYTEXT NOT NULL" &&
    TINYTEXT[Option[String]]().queryString === "TINYTEXT NULL" &&
    TINYTEXT[Option[String]]().DEFAULT(None).queryString === "TINYTEXT NULL DEFAULT NULL"
  }

  it should "The query string generated from the Text DataType model matches the specified one." in {
    TEXT[String]().queryString === "TEXT NOT NULL" &&
    TEXT[Option[String]]().queryString === "TEXT NULL" &&
    TEXT[Option[String]]().DEFAULT(None).queryString === "TEXT NULL DEFAULT NULL"
  }

  it should "The query string generated from the MediumText DataType model matches the specified one." in {
    MEDIUMTEXT[String]().queryString === "MEDIUMTEXT NOT NULL" &&
    MEDIUMTEXT[Option[String]]().queryString === "MEDIUMTEXT NULL" &&
    MEDIUMTEXT[Option[String]]().DEFAULT(None).queryString === "MEDIUMTEXT NULL DEFAULT NULL"
  }

  it should "The query string generated from the LongText DataType model matches the specified one." in {
    LONGTEXT[String]().queryString === "LONGTEXT NOT NULL" &&
    LONGTEXT[Option[String]]().queryString === "LONGTEXT NULL" &&
    LONGTEXT[Option[String]]().DEFAULT(None).queryString === "LONGTEXT NULL DEFAULT NULL"
  }

  it should "The query string generated from the Enum DataType model matches the specified one." in {
    enum Status extends ldbc.schema.model.Enum:
      case Active, InActive
    object Status extends EnumDataType[Status]

    ENUM[Status](using Status).queryString === "ENUM('Active','InActive') NOT NULL" &&
    ENUM[Status](using Status)
      .DEFAULT(Status.Active)
      .queryString === "ENUM('Active','InActive') NOT NULL DEFAULT 'Active'" &&
    ENUM[Option[Status]](using Status).queryString === "ENUM('Active','InActive') NULL" &&
    ENUM[Option[Status]](using Status).DEFAULT(None).queryString === "ENUM('Active','InActive') NULL DEFAULT NULL"
  }

  it should "The query string generated from the Date DataType model matches the specified one." in {
    DATE[LocalDate].queryString === "DATE NOT NULL" &&
    DATE[LocalDate]
      .DEFAULT(LocalDate.of(2023, 2, 10))
      .queryString === "DATE NOT NULL DEFAULT '2023-02-10'" &&
    DATE[LocalDate].DEFAULT(0).queryString === "DATE NOT NULL DEFAULT 0" &&
    DATE[LocalDate].DEFAULT("2023-02-10").queryString === "DATE NOT NULL DEFAULT '2023-02-10'" &&
    DATE[LocalDate].DEFAULT_CURRENT_DATE().queryString === "DATE NOT NULL DEFAULT (CURRENT_DATE)" &&
    DATE[Option[LocalDate]].queryString === "DATE NULL" &&
    DATE[Option[LocalDate]].DEFAULT(None).queryString === "DATE NULL DEFAULT NULL" &&
    DATE[Option[LocalDate]]
      .DEFAULT(Some(LocalDate.of(2023, 2, 10)))
      .queryString === "DATE NULL DEFAULT '2023-02-10'" &&
    DATE[Option[LocalDate]].DEFAULT(0).queryString === "DATE NULL DEFAULT 0" &&
    DATE[Option[LocalDate]].DEFAULT("2023-02-10").queryString === "DATE NULL DEFAULT '2023-02-10'" &&
    DATE[Option[LocalDate]].DEFAULT_CURRENT_DATE().queryString === "DATE NULL DEFAULT (CURRENT_DATE)"
  }

  it should "The query string generated from the DateTime DataType model matches the specified one." in {
    DATETIME[LocalDateTime].queryString === "DATETIME NOT NULL" &&
    DATETIME[LocalDateTime](6).queryString === "DATETIME(6) NOT NULL" &&
    DATETIME[LocalDateTime]
      .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
      .queryString === "DATETIME NOT NULL DEFAULT '2023-02-10T10:00'" &&
    DATETIME[LocalDateTime].DEFAULT(0).queryString === "DATETIME NOT NULL DEFAULT 0" &&
    DATETIME[LocalDateTime]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "DATETIME NOT NULL DEFAULT '2023-02-10 10:00:00'" &&
    DATETIME[Option[LocalDateTime]].queryString === "DATETIME NULL" &&
    DATETIME[Option[LocalDateTime]](6).queryString === "DATETIME(6) NULL" &&
    DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL" &&
    DATETIME[Option[LocalDateTime]]
      .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
      .queryString === "DATETIME NULL DEFAULT '2023-02-10T10:00'" &&
    DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL" &&
    DATETIME[Option[LocalDateTime]].DEFAULT(0).queryString === "DATETIME NULL DEFAULT 0" &&
    DATETIME[Option[LocalDateTime]]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "DATETIME NULL DEFAULT '2023-02-10 10:00:00'" &&
    DATETIME[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP" &&
    DATETIME[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)" &&
    DATETIME[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" &&
    DATETIME[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
  }

  it should "The query string generated from the TimeStamp DataType model matches the specified one." in {
    TIMESTAMP[LocalDateTime].queryString === "TIMESTAMP NOT NULL" &&
    TIMESTAMP[LocalDateTime](6).queryString === "TIMESTAMP(6) NOT NULL" &&
    TIMESTAMP[LocalDateTime]
      .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
      .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-02-10T10:00'" &&
    TIMESTAMP[LocalDateTime].DEFAULT(0).queryString === "TIMESTAMP NOT NULL DEFAULT 0" &&
    TIMESTAMP[LocalDateTime]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-02-10 10:00:00'" &&
    TIMESTAMP[Option[LocalDateTime]].queryString === "TIMESTAMP NULL" &&
    TIMESTAMP[Option[LocalDateTime]](5).queryString === "TIMESTAMP(5) NULL" &&
    TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL" &&
    TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
      .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10T10:00'" &&
    TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL" &&
    TIMESTAMP[Option[LocalDateTime]].DEFAULT(0).queryString === "TIMESTAMP NULL DEFAULT 0" &&
    TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT("2023-02-10 10:00:00")
      .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10 10:00:00'" &&
    TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP" &&
    TIMESTAMP[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP()
      .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6)" &&
    TIMESTAMP[Option[LocalDateTime]]
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" &&
    TIMESTAMP[Option[LocalDateTime]](6)
      .DEFAULT_CURRENT_TIMESTAMP(true)
      .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
  }

  it should "The query string generated from the Time DataType model matches the specified one." in {
    TIME[LocalTime].queryString === "TIME NOT NULL" &&
    TIME[LocalTime].DEFAULT(LocalTime.of(10, 0, 10)).queryString === "TIME NOT NULL DEFAULT '10:00:10'" &&
    TIME[LocalTime].DEFAULT(0).queryString === "TIME NOT NULL DEFAULT 0" &&
    TIME[LocalTime].DEFAULT("23:59:59").queryString === "TIME NOT NULL DEFAULT '23:59:59'" &&
    TIME[Option[LocalTime]].queryString === "TIME NULL" &&
    TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL" &&
    TIME[Option[LocalTime]]
      .DEFAULT(Some(LocalTime.of(10, 0, 0)))
      .queryString === "TIME NULL DEFAULT '10:00'" &&
    TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL" &&
    TIME[Option[LocalTime]].DEFAULT(0).queryString === "TIME NULL DEFAULT 0" &&
    TIME[Option[LocalTime]].DEFAULT("23:59:59").queryString === "TIME NULL DEFAULT '23:59:59'"
  }

  it should "The query string generated from the Year DataType model matches the specified one." in {
    YEAR[JYear].queryString === "YEAR NOT NULL" &&
    YEAR[JYear].DEFAULT(JYear.of(2023)).queryString === "YEAR NOT NULL DEFAULT '2023'" &&
    YEAR[JYear].DEFAULT(0).queryString === "YEAR NOT NULL DEFAULT 0" &&
    YEAR[JYear].DEFAULT(2023).queryString === "YEAR NOT NULL DEFAULT 2023" &&
    YEAR[Option[JYear]].queryString === "YEAR NULL" &&
    YEAR[Option[JYear]].DEFAULT(None).queryString === "YEAR NULL DEFAULT NULL" &&
    YEAR[Option[JYear]].DEFAULT(Some(JYear.of(2023))).queryString === "YEAR NULL DEFAULT '2023'" &&
    YEAR[Option[JYear]].DEFAULT(0).queryString === "YEAR NULL DEFAULT 0" &&
    YEAR[Option[JYear]].DEFAULT(2023).queryString === "YEAR NULL DEFAULT 2023"
  }

  it should "The query string generated from the Serial DataType model matches the specified one." in {
    SERIAL[BigInt].queryString === "BIGINT UNSIGNED NOT NULL"
  }

  it should "The query string generated from the Boolean DataType model matches the specified one." in {
    BOOLEAN[Boolean].queryString === "BOOLEAN NOT NULL" &&
    BOOLEAN[Boolean].DEFAULT(true).queryString === "BOOLEAN NOT NULL DEFAULT true" &&
    BOOLEAN[Boolean].DEFAULT(false).queryString === "BOOLEAN NOT NULL DEFAULT false" &&
    BOOLEAN[Option[Boolean]].queryString === "BOOLEAN NULL" &&
    BOOLEAN[Option[Boolean]].DEFAULT(None).queryString === "BOOLEAN NULL DEFAULT NULL" &&
    BOOLEAN[Option[Boolean]].DEFAULT(Some(true)).queryString === "BOOLEAN NULL DEFAULT true"
  }
