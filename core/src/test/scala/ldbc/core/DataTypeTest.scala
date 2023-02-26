/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import java.time.{ LocalTime, LocalDate, LocalDateTime }
import java.time.Year as JYear

import org.specs2.mutable.Specification

import ldbc.core.DataType.*

object DataTypeTest extends Specification:

  "DataType Test" should {
    "The query string generated from the Bit DataType model matches the specified one." in {
      Bit[Byte](1, None).queryString === "BIT(1) NOT NULL" and
        Bit[Byte](64, None).DEFAULT("byte".getBytes.head).queryString === "BIT(64) NOT NULL DEFAULT '98'" and
        BitOpt[Option[Short]](1, None).queryString === "BIT(1) NULL" and
        BitOpt[Option[Short]](64, None).DEFAULT(None).queryString === "BIT(64) NULL DEFAULT NULL" and
        BitOpt[Option[Short]](64, None).DEFAULT_NULL.queryString === "BIT(64) NULL DEFAULT NULL"
    }

    "The query string generated from the Tinyint DataType model matches the specified one." in {
      Tinyint[Byte](1, None).queryString === "TINYINT(1) NOT NULL" and
        Tinyint[Byte](64, None).DEFAULT("byte".getBytes.head).queryString === "TINYINT(64) NOT NULL DEFAULT '98'" and
        TinyintOpt[Option[Byte]](1, None).queryString === "TINYINT(1) NULL" and
        TinyintOpt[Option[Byte]](64, None).DEFAULT(None).queryString === "TINYINT(64) NULL DEFAULT NULL" and
        TinyintOpt[Option[Byte]](64, None)
          .DEFAULT("byte".getBytes.headOption)
          .queryString === "TINYINT(64) NULL DEFAULT '98'" and
        TinyintOpt[Option[Byte]](64, None).DEFAULT_NULL.queryString === "TINYINT(64) NULL DEFAULT NULL"
    }

    "The query string generated from the Smallint DataType model matches the specified one." in {
      Smallint[Short](0, None).queryString === "SMALLINT(0) NOT NULL" and
        Smallint[Short](255, None).DEFAULT(1).queryString === "SMALLINT(255) NOT NULL DEFAULT '1'" and
        SmallintOpt[Option[Short]](0, None).queryString === "SMALLINT(0) NULL" and
        SmallintOpt[Option[Short]](255, None).DEFAULT(None).queryString === "SMALLINT(255) NULL DEFAULT NULL" and
        SmallintOpt[Option[Short]](255, None).DEFAULT(Some(2)).queryString === "SMALLINT(255) NULL DEFAULT '2'" and
        SmallintOpt[Option[Short]](255, None).DEFAULT_NULL.queryString === "SMALLINT(255) NULL DEFAULT NULL"
    }

    "The query string generated from the Mediumint DataType model matches the specified one." in {
      Mediumint[Int](0, None).queryString === "MEDIUMINT(0) NOT NULL" and
        Mediumint[Int](255, None).DEFAULT(1).queryString === "MEDIUMINT(255) NOT NULL DEFAULT '1'" and
        MediumintOpt[Option[Int]](0, None).queryString === "MEDIUMINT(0) NULL" and
        MediumintOpt[Option[Int]](255, None).DEFAULT(None).queryString === "MEDIUMINT(255) NULL DEFAULT NULL" and
        MediumintOpt[Option[Int]](255, None).DEFAULT(Some(2)).queryString === "MEDIUMINT(255) NULL DEFAULT '2'" and
        MediumintOpt[Option[Int]](255, None).DEFAULT_NULL.queryString === "MEDIUMINT(255) NULL DEFAULT NULL"
    }

    "The query string generated from the Integer DataType model matches the specified one." in {
      Integer[Int](0, None).queryString === "INT(0) NOT NULL" and
        Integer[Int](255, None).DEFAULT(1).queryString === "INT(255) NOT NULL DEFAULT '1'" and
        IntegerOpt[Option[Int]](0, None).queryString === "INT(0) NULL" and
        IntegerOpt[Option[Int]](255, None).DEFAULT(None).queryString === "INT(255) NULL DEFAULT NULL" and
        IntegerOpt[Option[Int]](255, None).DEFAULT(Some(2)).queryString === "INT(255) NULL DEFAULT '2'" and
        IntegerOpt[Option[Int]](255, None).DEFAULT_NULL.queryString === "INT(255) NULL DEFAULT NULL"
    }

    "The query string generated from the Bigint DataType model matches the specified one." in {
      Bigint[Long](0, None).queryString === "BIGINT(0) NOT NULL" and
        Bigint[Long](255, None).DEFAULT(1).queryString === "BIGINT(255) NOT NULL DEFAULT '1'" and
        BigintOpt[Option[Long]](0, None).queryString === "BIGINT(0) NULL" and
        BigintOpt[Option[Long]](255, None).DEFAULT(None).queryString === "BIGINT(255) NULL DEFAULT NULL" and
        BigintOpt[Option[Long]](255, None).DEFAULT(Some(2)).queryString === "BIGINT(255) NULL DEFAULT '2'" and
        BigintOpt[Option[Long]](255, None).DEFAULT_NULL.queryString === "BIGINT(255) NULL DEFAULT NULL"
    }

    "The query string generated from the Decimal DataType model matches the specified one." in {
      Decimal[BigDecimal](10, 7, None).queryString === "DECIMAL(10, 7) NOT NULL" and
        Decimal[BigDecimal](10, 7, None)
          .DEFAULT(BigDecimal(10, 7))
          .queryString === "DECIMAL(10, 7) NOT NULL DEFAULT '0.0000010'" and
        DecimalOpt[Option[BigDecimal]](10, 7, None).queryString === "DECIMAL(10, 7) NULL" and
        DecimalOpt[Option[BigDecimal]](10, 7, None).DEFAULT(None).queryString === "DECIMAL(10, 7) NULL DEFAULT NULL" and
        DecimalOpt[Option[BigDecimal]](10, 7, None)
          .DEFAULT(Some(BigDecimal(10, 7)))
          .queryString === "DECIMAL(10, 7) NULL DEFAULT '0.0000010'" and
        DecimalOpt[Option[BigDecimal]](10, 7, None).DEFAULT_NULL.queryString === "DECIMAL(10, 7) NULL DEFAULT NULL"
    }

    "The query string generated from the Float DataType model matches the specified one." in {
      CFloat[Float](0, None).queryString === "FLOAT(0) NOT NULL" and
        CFloat[Float](0, None).DEFAULT(1.2f).queryString === "FLOAT(0) NOT NULL DEFAULT '1.2'" and
        FloatOpt[Option[Float]](0, None).queryString === "FLOAT(0) NULL" and
        FloatOpt[Option[Float]](0, None).DEFAULT(None).queryString === "FLOAT(0) NULL DEFAULT NULL" and
        FloatOpt[Option[Float]](0, None).DEFAULT(Some(1.2f)).queryString === "FLOAT(0) NULL DEFAULT '1.2'" and
        FloatOpt[Option[Float]](0, None).DEFAULT_NULL.queryString === "FLOAT(0) NULL DEFAULT NULL"
    }

    "The query string generated from the Char DataType model matches the specified one." in {
      CChar[String](0, None, None).queryString === "CHAR(0) NOT NULL" and
        CChar[String](0, None, None).DEFAULT("test").queryString === "CHAR(0) NOT NULL DEFAULT 'test'" and
        CharOpt[Option[String]](0, None, None).queryString === "CHAR(0) NULL" and
        CharOpt[Option[String]](0, None, None).DEFAULT(None).queryString === "CHAR(0) NULL DEFAULT NULL" and
        CharOpt[Option[String]](0, None, None).DEFAULT(Some("test")).queryString === "CHAR(0) NULL DEFAULT 'test'" and
        CharOpt[Option[String]](0, None, None).DEFAULT_NULL.queryString === "CHAR(0) NULL DEFAULT NULL"
    }

    "The query string generated from the Varchar DataType model matches the specified one." in {
      Varchar[String](0, None, None).queryString === "VARCHAR(0) NOT NULL" and
        Varchar[String](0, None, None).DEFAULT("test").queryString === "VARCHAR(0) NOT NULL DEFAULT 'test'" and
        VarcharOpt[Option[String]](0, None, None).queryString === "VARCHAR(0) NULL" and
        VarcharOpt[Option[String]](0, None, None).DEFAULT(None).queryString === "VARCHAR(0) NULL DEFAULT NULL" and
        VarcharOpt[Option[String]](0, None, None)
          .DEFAULT(Some("test"))
          .queryString === "VARCHAR(0) NULL DEFAULT 'test'" and
        VarcharOpt[Option[String]](0, None, None).DEFAULT_NULL.queryString === "VARCHAR(0) NULL DEFAULT NULL"
    }

    "The query string generated from the Binary DataType model matches the specified one." in {
      Binary[Array[Byte]](0, None).queryString === "BINARY(0) NOT NULL" and
        BinaryOpt[Option[Array[Byte]]](0, None).queryString === "BINARY(0) NULL"
    }

    "The query string generated from the Varbinary DataType model matches the specified one." in {
      Varbinary[Array[Byte]](0, None).queryString === "VARBINARY(0) NOT NULL" and
        VarbinaryOpt[Option[Array[Byte]]](0, None).queryString === "VARBINARY(0) NULL"
    }

    "The query string generated from the Tinyblob DataType model matches the specified one." in {
      Tinyblob[Array[Byte]](None).queryString === "TINYBLOB NOT NULL" and
        TinyblobOpt[Option[Array[Byte]]](None).queryString === "TINYBLOB NULL"
    }

    "The query string generated from the Blob DataType model matches the specified one." in {
      Blob[Array[Byte]](0, None).queryString === "BLOB(0) NOT NULL" and
        BlobOpt[Option[Array[Byte]]](0, None).queryString === "BLOB(0) NULL"
    }

    "The query string generated from the Mediumblob DataType model matches the specified one." in {
      Mediumblob[Array[Byte]](None).queryString === "MEDIUMBLOB NOT NULL" and
        MediumblobOpt[Option[Array[Byte]]](None).queryString === "MEDIUMBLOB NULL"
    }

    "The query string generated from the LongBlob DataType model matches the specified one." in {
      LongBlob[Array[Byte]](None).queryString === "LONGBLOB NOT NULL" and
        LongBlobOpt[Option[Array[Byte]]](None).queryString === "LONGBLOB NULL"
    }

    "The query string generated from the TinyText DataType model matches the specified one." in {
      TinyText[String](None).queryString === "TINYTEXT NOT NULL" and
        TinyTextOpt[Option[String]](None).queryString === "TINYTEXT NULL"
    }

    "The query string generated from the Text DataType model matches the specified one." in {
      Text[String](None).queryString === "TEXT NOT NULL" and
        TextOpt[Option[String]](None).queryString === "TEXT NULL"
    }

    "The query string generated from the MediumText DataType model matches the specified one." in {
      MediumText[String](None).queryString === "MEDIUMTEXT NOT NULL" and
        MediumTextOpt[Option[String]](None).queryString === "MEDIUMTEXT NULL"
    }

    "The query string generated from the LongText DataType model matches the specified one." in {
      LongText[String](None).queryString === "LONGTEXT NOT NULL" and
        LongTextOpt[Option[String]](None).queryString === "LONGTEXT NULL"
    }

    "The query string generated from the Date DataType model matches the specified one." in {
      Date[LocalDate](None).queryString === "DATE NOT NULL" and
        Date[LocalDate](None)
          .DEFAULT(LocalDate.of(2023, 2, 10))
          .queryString === "DATE NOT NULL DEFAULT '2023-02-10'" and
        DateOpt[Option[LocalDate]](None).queryString === "DATE NULL" and
        DateOpt[Option[LocalDate]](None).DEFAULT(None).queryString === "DATE NULL DEFAULT NULL" and
        DateOpt[Option[LocalDate]](None)
          .DEFAULT(Some(LocalDate.of(2023, 2, 10)))
          .queryString === "DATE NULL DEFAULT '2023-02-10'" and
        DateOpt[Option[LocalDate]](None).DEFAULT_NULL.queryString === "DATE NULL DEFAULT NULL"
    }

    "The query string generated from the DateTime DataType model matches the specified one." in {
      DateTime[LocalDateTime](None).queryString === "DATETIME NOT NULL" and
        DateTime[LocalDateTime](None)
          .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
          .queryString === "DATETIME NOT NULL DEFAULT '2023-02-10T10:00'" and
        DateTimeOpt[Option[LocalDateTime]](None).queryString === "DATETIME NULL" and
        DateTimeOpt[Option[LocalDateTime]](None).DEFAULT(None).queryString === "DATETIME NULL DEFAULT NULL" and
        DateTimeOpt[Option[LocalDateTime]](None)
          .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
          .queryString === "DATETIME NULL DEFAULT '2023-02-10T10:00'" and
        DateTimeOpt[Option[LocalDateTime]](None).DEFAULT_NULL.queryString === "DATETIME NULL DEFAULT NULL" and
        DateTimeOpt[Option[LocalDateTime]](None)
          .DEFAULT_CURRENT_TIMESTAMP()
          .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP" and
        DateTimeOpt[Option[LocalDateTime]](None)
          .DEFAULT_CURRENT_TIMESTAMP(true)
          .queryString === "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    }

    "The query string generated from the TimeStamp DataType model matches the specified one." in {
      TimeStamp[LocalDateTime](None).queryString === "TIMESTAMP NOT NULL" and
        TimeStamp[LocalDateTime](None)
          .DEFAULT(LocalDateTime.of(2023, 2, 10, 10, 0))
          .queryString === "TIMESTAMP NOT NULL DEFAULT '2023-02-10T10:00'" and
        TimeStampOpt[Option[LocalDateTime]](None).queryString === "TIMESTAMP NULL" and
        TimeStampOpt[Option[LocalDateTime]](None).DEFAULT(None).queryString === "TIMESTAMP NULL DEFAULT NULL" and
        TimeStampOpt[Option[LocalDateTime]](None)
          .DEFAULT(Some(LocalDateTime.of(2023, 2, 10, 10, 0)))
          .queryString === "TIMESTAMP NULL DEFAULT '2023-02-10T10:00'" and
        TimeStampOpt[Option[LocalDateTime]](None).DEFAULT_NULL.queryString === "TIMESTAMP NULL DEFAULT NULL" and
        TimeStampOpt[Option[LocalDateTime]](None)
          .DEFAULT_CURRENT_TIMESTAMP()
          .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP" and
        TimeStampOpt[Option[LocalDateTime]](None)
          .DEFAULT_CURRENT_TIMESTAMP(true)
          .queryString === "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    }

    "The query string generated from the Time DataType model matches the specified one." in {
      Time[LocalTime](None).queryString === "TIME NOT NULL" and
        Time[LocalTime](None).DEFAULT(LocalTime.of(10, 0, 10)).queryString === "TIME NOT NULL DEFAULT '10:00:10'" and
        TimeOpt[Option[LocalTime]](None).queryString === "TIME NULL" and
        TimeOpt[Option[LocalTime]](None).DEFAULT(None).queryString === "TIME NULL DEFAULT NULL" and
        TimeOpt[Option[LocalTime]](None)
          .DEFAULT(Some(LocalTime.of(10, 0, 0)))
          .queryString === "TIME NULL DEFAULT '10:00'" and
        TimeOpt[Option[LocalTime]](None).DEFAULT_NULL.queryString === "TIME NULL DEFAULT NULL"
    }

    "The query string generated from the Year DataType model matches the specified one." in {
      Year[JYear](None).queryString === "YEAR NOT NULL" and
        Year[JYear](None).DEFAULT(JYear.of(2023)).queryString === "YEAR NOT NULL DEFAULT '2023'" and
        YearOpt[Option[JYear]](None).queryString === "YEAR NULL" and
        YearOpt[Option[JYear]](None).DEFAULT(None).queryString === "YEAR NULL DEFAULT NULL" and
        YearOpt[Option[JYear]](None).DEFAULT(Some(JYear.of(2023))).queryString === "YEAR NULL DEFAULT '2023'" and
        YearOpt[Option[JYear]](None).DEFAULT_NULL.queryString === "YEAR NULL DEFAULT NULL"
    }
  }
