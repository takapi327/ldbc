/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.time.{ LocalTime, LocalDate, LocalDateTime }
import java.time.Year as JYear

import org.specs2.mutable.Specification

import ldbc.core.DataType.*
import ldbc.core.model.EnumDataType

object DataTypeTest extends Specification:

  "DataType Test" should {
    "The query string generated from the Bit DataType model matches the specified one." in {
      BIT[Byte](1).queryString === "BIT(1) NOT NULL" and
        BIT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "BIT(64) NOT NULL DEFAULT '98'" and
        BIT[Option[Short]](1).queryString === "BIT(1) NULL" and
        BIT[Option[Short]](64).DEFAULT(None).queryString === "BIT(64) NULL DEFAULT NULL"
    }

    "The query string generated from the Tinyint DataType model matches the specified one." in {
      TINYINT[Byte](1).queryString === "TINYINT(1) NOT NULL" and
        TINYINT[Byte](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NOT NULL" and
        TINYINT[Byte](64).DEFAULT("byte".getBytes.head).queryString === "TINYINT(64) NOT NULL DEFAULT '98'" and
        TINYINT[Byte](64)
          .DEFAULT("byte".getBytes.head)
          .UNSIGNED
          .queryString === "TINYINT(64) UNSIGNED NOT NULL DEFAULT '98'" and
        TINYINT[Option[Byte]](1).queryString === "TINYINT(1) NULL" and
        TINYINT[Option[Byte]](1).UNSIGNED.queryString === "TINYINT(1) UNSIGNED NULL" and
        TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL" and
        TINYINT[Option[Byte]](64)
          .DEFAULT("byte".getBytes.headOption)
          .queryString === "TINYINT(64) NULL DEFAULT '98'" and
        TINYINT[Option[Byte]](64).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL" and
        TINYINT[Option[Byte]](64).DEFAULT(None).UNSIGNED.queryString === "TINYINT(64) UNSIGNED NULL DEFAULT NULL"
    }

    "The query string generated from the Smallint DataType model matches the specified one." in {
      SMALLINT[Short](0).queryString === "SMALLINT(0) NOT NULL" and
        SMALLINT[Short](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NOT NULL" and
        SMALLINT[Short](255).DEFAULT(1).queryString === "SMALLINT(255) NOT NULL DEFAULT '1'" and
        SMALLINT[Short](255).DEFAULT(1).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NOT NULL DEFAULT '1'" and
        SMALLINT[Option[Short]](0).queryString === "SMALLINT(0) NULL" and
        SMALLINT[Option[Short]](0).UNSIGNED.queryString === "SMALLINT(0) UNSIGNED NULL" and
        SMALLINT[Option[Short]](255).DEFAULT(None).queryString === "SMALLINT(255) NULL DEFAULT NULL" and
        SMALLINT[Option[Short]](255).DEFAULT(Some(2)).queryString === "SMALLINT(255) NULL DEFAULT '2'" and
        SMALLINT[Option[Short]](255).DEFAULT(None).UNSIGNED.queryString === "SMALLINT(255) UNSIGNED NULL DEFAULT NULL"
    }

    "The query string generated from the Mediumint DataType model matches the specified one." in {
      MEDIUMINT[Int](0).queryString === "MEDIUMINT(0) NOT NULL" and
        MEDIUMINT[Int](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NOT NULL" and
        MEDIUMINT[Int](255).DEFAULT(1).queryString === "MEDIUMINT(255) NOT NULL DEFAULT '1'" and
        MEDIUMINT[Int](255).DEFAULT(1).UNSIGNED.queryString === "MEDIUMINT(255) UNSIGNED NOT NULL DEFAULT '1'" and
        MEDIUMINT[Option[Int]](0).queryString === "MEDIUMINT(0) NULL" and
        MEDIUMINT[Option[Int]](0).UNSIGNED.queryString === "MEDIUMINT(0) UNSIGNED NULL" and
        MEDIUMINT[Option[Int]](255).DEFAULT(None).queryString === "MEDIUMINT(255) NULL DEFAULT NULL" and
        MEDIUMINT[Option[Int]](255).DEFAULT(Some(2)).queryString === "MEDIUMINT(255) NULL DEFAULT '2'" and
        MEDIUMINT[Option[Int]](255).DEFAULT(None).UNSIGNED.queryString === "MEDIUMINT(255) UNSIGNED NULL DEFAULT NULL"
    }

    "The query string generated from the Integer DataType model matches the specified one." in {
      INT[Int](0).queryString === "INT(0) NOT NULL" and
        INT[Int](0).UNSIGNED.queryString === "INT(0) UNSIGNED NOT NULL" and
        INT[Int](255).DEFAULT(1).queryString === "INT(255) NOT NULL DEFAULT '1'" and
        INT[Int](255).DEFAULT(1).UNSIGNED.queryString === "INT(255) UNSIGNED NOT NULL DEFAULT '1'" and
        INT[Option[Int]](0).queryString === "INT(0) NULL" and
        INT[Option[Int]](0).UNSIGNED.queryString === "INT(0) UNSIGNED NULL" and
        INT[Option[Int]](255).DEFAULT(None).queryString === "INT(255) NULL DEFAULT NULL" and
        INT[Option[Int]](255).DEFAULT(Some(2)).queryString === "INT(255) NULL DEFAULT '2'" and
        INT[Option[Int]](255).DEFAULT(None).UNSIGNED.queryString === "INT(255) UNSIGNED NULL DEFAULT NULL"
    }

    "The query string generated from the Bigint DataType model matches the specified one." in {
      BIGINT[Long](0).queryString === "BIGINT(0) NOT NULL" and
        BIGINT[Long](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NOT NULL" and
        BIGINT[Long](255).DEFAULT(1).queryString === "BIGINT(255) NOT NULL DEFAULT '1'" and
        BIGINT[Long](255).DEFAULT(1).UNSIGNED.queryString === "BIGINT(255) UNSIGNED NOT NULL DEFAULT '1'" and
        BIGINT[Option[Long]](0).queryString === "BIGINT(0) NULL" and
        BIGINT[Option[Long]](0).UNSIGNED.queryString === "BIGINT(0) UNSIGNED NULL" and
        BIGINT[Option[Long]](255).DEFAULT(None).queryString === "BIGINT(255) NULL DEFAULT NULL" and
        BIGINT[Option[Long]](255).DEFAULT(Some(2)).queryString === "BIGINT(255) NULL DEFAULT '2'" and
        BIGINT[Option[Long]](255).DEFAULT(None).UNSIGNED.queryString === "BIGINT(255) UNSIGNED NULL DEFAULT NULL"
    }

    "The query string generated from the Decimal DataType model matches the specified one." in {
      DECIMAL[BigDecimal](10, 7).queryString === "DECIMAL(10, 7) NOT NULL" and
        DECIMAL[BigDecimal](10, 7)
          .DEFAULT(BigDecimal(10, 7))
          .queryString === "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'" and
        DECIMAL[Option[BigDecimal]](10, 7).queryString === "DECIMAL(10, 7) NULL" and
        DECIMAL[Option[BigDecimal]](10, 7).DEFAULT(None).queryString === "DECIMAL(10, 7) NULL DEFAULT NULL" and
        DECIMAL[Option[BigDecimal]](10, 7)
          .DEFAULT(Some(BigDecimal(10, 7)))
          .queryString === "DECIMAL(10, 7) NULL DEFAULT '0.0000010'"
    }

    "The query string generated from the Float DataType model matches the specified one." in {
      FLOAT[Float](0).queryString === "FLOAT(0) NOT NULL" and
        FLOAT[Float](0).DEFAULT(1.2f).queryString === "FLOAT(0) NOT NULL DEFAULT '1.2'" and
        FLOAT[Option[Float]](0).queryString === "FLOAT(0) NULL" and
        FLOAT[Option[Float]](0).DEFAULT(None).queryString === "FLOAT(0) NULL DEFAULT NULL" and
        FLOAT[Option[Float]](0).DEFAULT(Some(1.2f)).queryString === "FLOAT(0) NULL DEFAULT '1.2'"
    }

    "The query string generated from the Char DataType model matches the specified one." in {
      CHAR[String](0).queryString === "CHAR(0) NOT NULL" and
        CHAR[String](0).DEFAULT("test").queryString === "CHAR(0) NOT NULL DEFAULT 'test'" and
        CHAR[Option[String]](0).queryString === "CHAR(0) NULL" and
        CHAR[Option[String]](0).DEFAULT(None).queryString === "CHAR(0) NULL DEFAULT NULL" and
        CHAR[Option[String]](0).DEFAULT(Some("test")).queryString === "CHAR(0) NULL DEFAULT 'test'"
    }

    "The query string generated from the Varchar DataType model matches the specified one." in {
      VARCHAR[String](0).queryString === "VARCHAR(0) NOT NULL" and
        VARCHAR[String](0).DEFAULT("test").queryString === "VARCHAR(0) NOT NULL DEFAULT 'test'" and
        VARCHAR[Option[String]](0).queryString === "VARCHAR(0) NULL" and
        VARCHAR[Option[String]](0).DEFAULT(None).queryString === "VARCHAR(0) NULL DEFAULT NULL" and
        VARCHAR[Option[String]](0)
          .DEFAULT(Some("test"))
          .queryString === "VARCHAR(0) NULL DEFAULT 'test'"
    }

    "The query string generated from the Binary DataType model matches the specified one." in {
      BINARY[Array[Byte]](0).queryString === "BINARY(0) NOT NULL" and
        BINARY[Option[Array[Byte]]](0).queryString === "BINARY(0) NULL"
    }

    "The query string generated from the Varbinary DataType model matches the specified one." in {
      VARBINARY[Array[Byte]](0).queryString === "VARBINARY(0) NOT NULL" and
        VARBINARY[Option[Array[Byte]]](0).queryString === "VARBINARY(0) NULL"
    }

    "The query string generated from the Tinyblob DataType model matches the specified one." in {
      TINYBLOB[Array[Byte]]().queryString === "TINYBLOB NOT NULL" and
        TINYBLOB[Option[Array[Byte]]]().queryString === "TINYBLOB NULL" and
        TINYBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "TINYBLOB NULL DEFAULT NULL"
    }

    "The query string generated from the Blob DataType model matches the specified one." in {
      BLOB[Array[Byte]](0).queryString === "BLOB(0) NOT NULL" and
        BLOB[Option[Array[Byte]]](0).queryString === "BLOB(0) NULL" and
        BLOB[Option[Array[Byte]]](0).DEFAULT(None).queryString === "BLOB(0) NULL DEFAULT NULL"
    }

    "The query string generated from the Mediumblob DataType model matches the specified one." in {
      MEDIUMBLOB[Array[Byte]]().queryString === "MEDIUMBLOB NOT NULL" and
        MEDIUMBLOB[Option[Array[Byte]]]().queryString === "MEDIUMBLOB NULL" and
        MEDIUMBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "MEDIUMBLOB NULL DEFAULT NULL"
    }

    "The query string generated from the LongBlob DataType model matches the specified one." in {
      LONGBLOB[Array[Byte]]().queryString === "LONGBLOB NOT NULL" and
        LONGBLOB[Option[Array[Byte]]]().queryString === "LONGBLOB NULL" and
        LONGBLOB[Option[Array[Byte]]]().DEFAULT(None).queryString === "LONGBLOB NULL DEFAULT NULL"
    }

    "The query string generated from the TinyText DataType model matches the specified one." in {
      TINYTEXT[String]().queryString === "TINYTEXT NOT NULL" and
        TINYTEXT[Option[String]]().queryString === "TINYTEXT NULL" and
        TINYTEXT[Option[String]]().DEFAULT(None).queryString === "TINYTEXT NULL DEFAULT NULL"
    }

    "The query string generated from the Text DataType model matches the specified one." in {
      TEXT[String]().queryString === "TEXT NOT NULL" and
        TEXT[Option[String]]().queryString === "TEXT NULL" and
        TEXT[Option[String]]().DEFAULT(None).queryString === "TEXT NULL DEFAULT NULL"
    }

    "The query string generated from the MediumText DataType model matches the specified one." in {
      MEDIUMTEXT[String]().queryString === "MEDIUMTEXT NOT NULL" and
        MEDIUMTEXT[Option[String]]().queryString === "MEDIUMTEXT NULL" and
        MEDIUMTEXT[Option[String]]().DEFAULT(None).queryString === "MEDIUMTEXT NULL DEFAULT NULL"
    }

    "The query string generated from the LongText DataType model matches the specified one." in {
      LONGTEXT[String]().queryString === "LONGTEXT NOT NULL" and
        LONGTEXT[Option[String]]().queryString === "LONGTEXT NULL" and
        LONGTEXT[Option[String]]().DEFAULT(None).queryString === "LONGTEXT NULL DEFAULT NULL"
    }

    "The query string generated from the Enum DataType model matches the specified one." in {
      enum Status extends ldbc.core.model.Enum:
        case Active, InActive
      object Status extends EnumDataType[Status]

      ENUM[Status](using Status).queryString === "ENUM('Active','InActive') NOT NULL" and
        ENUM[Status](using Status)
          .DEFAULT(Status.Active)
          .queryString === "ENUM('Active','InActive') NOT NULL DEFAULT 'Active'" and
        ENUM[Option[Status]](using Status).queryString === "ENUM('Active','InActive') NULL" and
        ENUM[Option[Status]](using Status).DEFAULT(None).queryString === "ENUM('Active','InActive') NULL DEFAULT NULL"
    }

    "The query string generated from the Date DataType model matches the specified one." in {
      DATE[LocalDate].queryString === "DATE NOT NULL" and
        DATE[LocalDate]
          .DEFAULT(LocalDate.of(2023, 2, 10))
          .queryString === "DATE NOT NULL DEFAULT '2023-02-10'" and
        DATE[Option[LocalDate]].queryString === "DATE NULL" and
        DATE[Option[LocalDate]].DEFAULT(None).queryString === "DATE NULL DEFAULT NULL" and
        DATE[Option[LocalDate]]
          .DEFAULT(Some(LocalDate.of(2023, 2, 10)))
          .queryString === "DATE NULL DEFAULT '2023-02-10'"
    }

    "The query string generated from the DateTime DataType model matches the specified one." in {
      DATETIME[LocalDateTime].queryString === "DATETIME NOT NULL" and
        DATETIME[LocalDateTime](6).queryString === "DATETIME(6) NOT NULL" and
        DATETIME[LocalDateTime]
          .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
          .queryString === "DATETIME NOT NULL DEFAULT '2023-02-10T10:00'" and
        DATETIME[Option[LocalDateTime]].queryString === "DATETIME NULL" and
        DATETIME[Option[LocalDateTime]](6).queryString === "DATETIME(6) NULL" and
        DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL" and
        DATETIME[Option[LocalDateTime]]
          .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
          .queryString === "DATETIME NULL DEFAULT '2023-02-10T10:00'" and
        DATETIME[Option[LocalDateTime]].DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL" and
        DATETIME[Option[LocalDateTime]]
          .DEFAULT_CURRENT_TIMESTAMP()
          .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP" and
        DATETIME[Option[LocalDateTime]](6)
          .DEFAULT_CURRENT_TIMESTAMP()
          .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6)" and
        DATETIME[Option[LocalDateTime]]
          .DEFAULT_CURRENT_TIMESTAMP(true)
          .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" and
        DATETIME[Option[LocalDateTime]](6)
          .DEFAULT_CURRENT_TIMESTAMP(true)
          .queryString === "DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
    }

    "The query string generated from the TimeStamp DataType model matches the specified one." in {
      TIMESTAMP[LocalDateTime].queryString === "TIMESTAMP NOT NULL" and
        TIMESTAMP[LocalDateTime](6).queryString === "TIMESTAMP(6) NOT NULL" and
        TIMESTAMP[LocalDateTime]
          .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
          .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-02-10T10:00'" and
        TIMESTAMP[Option[LocalDateTime]].queryString === "TIMESTAMP NULL" and
        TIMESTAMP[Option[LocalDateTime]](5).queryString === "TIMESTAMP(5) NULL" and
        TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL" and
        TIMESTAMP[Option[LocalDateTime]]
          .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
          .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10T10:00'" and
        TIMESTAMP[Option[LocalDateTime]].DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL" and
        TIMESTAMP[Option[LocalDateTime]]
          .DEFAULT_CURRENT_TIMESTAMP()
          .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP" and
        TIMESTAMP[Option[LocalDateTime]](6)
          .DEFAULT_CURRENT_TIMESTAMP()
          .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6)" and
        TIMESTAMP[Option[LocalDateTime]]
          .DEFAULT_CURRENT_TIMESTAMP(true)
          .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" and
        TIMESTAMP[Option[LocalDateTime]](6)
          .DEFAULT_CURRENT_TIMESTAMP(true)
          .queryString === "TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)"
    }

    "The query string generated from the Time DataType model matches the specified one." in {
      TIME[LocalTime].queryString === "TIME NOT NULL" and
        TIME[LocalTime].DEFAULT(LocalTime.of(10, 0, 10)).queryString === "TIME NOT NULL DEFAULT '10:00:10'" and
        TIME[Option[LocalTime]].queryString === "TIME NULL" and
        TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL" and
        TIME[Option[LocalTime]]
          .DEFAULT(Some(LocalTime.of(10, 0, 0)))
          .queryString === "TIME NULL DEFAULT '10:00'" and
        TIME[Option[LocalTime]].DEFAULT(None).queryString === "TIME NULL DEFAULT NULL"
    }

    "The query string generated from the Year DataType model matches the specified one." in {
      YEAR[JYear].queryString === "YEAR NOT NULL" and
        YEAR[JYear].DEFAULT(JYear.of(2023)).queryString === "YEAR NOT NULL DEFAULT '2023'" and
        YEAR[Option[JYear]].queryString === "YEAR NULL" and
        YEAR[Option[JYear]].DEFAULT(None).queryString === "YEAR NULL DEFAULT NULL" and
        YEAR[Option[JYear]].DEFAULT(Some(JYear.of(2023))).queryString === "YEAR NULL DEFAULT '2023'"
    }

    "The query string generated from the Serial DataType model matches the specified one." in {
      SERIAL[BigInt].queryString === "BIGINT UNSIGNED NOT NULL"
    }

    "The query string generated from the Boolean DataType model matches the specified one." in {
      BOOLEAN[Boolean].queryString === "BOOLEAN NOT NULL" and
        BOOLEAN[Boolean].DEFAULT(true).queryString === "BOOLEAN NOT NULL DEFAULT true" and
        BOOLEAN[Boolean].DEFAULT(false).queryString === "BOOLEAN NOT NULL DEFAULT false" and
        BOOLEAN[Option[Boolean]].queryString === "BOOLEAN NULL" and
        BOOLEAN[Option[Boolean]].DEFAULT(None).queryString === "BOOLEAN NULL DEFAULT NULL" and
        BOOLEAN[Option[Boolean]].DEFAULT(Some(true)).queryString === "BOOLEAN NULL DEFAULT true"
    }
  }
