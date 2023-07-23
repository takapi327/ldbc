/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.generator.builder

import org.specs2.mutable.Specification

import ldbc.generator.formatter.Naming
import ldbc.generator.model.DataType

object DataTypeCodeBuilderTest extends Specification:

  private def builder(scalaType: String) = DataTypeCodeBuilder(scalaType, Naming.PASCAL)

  "Testing the DataTypeCodeBuilder" should {
    "BIT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.BIT(None)
      val dataType2 = DataType.BIT(Some(1))
      builder("Byte").build(dataType1) === "BIT[Byte]" and
        builder("Option[Byte]").build(dataType1) === "BIT[Option[Byte]]" and
        builder("Byte").build(dataType2) === "BIT[Byte](1)" and
        builder("Option[Byte]").build(dataType2) === "BIT[Option[Byte]](1)"
    }

    "TINYINT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.TINYINT(None, true, true)
      val dataType2 = DataType.TINYINT(Some(1), false, true)
      val dataType3 = DataType.TINYINT(None, true, false)
      val dataType4 = DataType.TINYINT(Some(1), false, false)
      builder("Short").build(dataType1) === "TINYINT[Short].UNSIGNED.ZEROFILL" and
        builder("Option[Short]").build(dataType1) === "TINYINT[Option[Short]].UNSIGNED.ZEROFILL" and
        builder("Short").build(dataType2) === "TINYINT[Short](1).ZEROFILL" and
        builder("Option[Short]").build(dataType2) === "TINYINT[Option[Short]](1).ZEROFILL" and
        builder("Short").build(dataType3) === "TINYINT[Short].UNSIGNED" and
        builder("Option[Short]").build(dataType3) === "TINYINT[Option[Short]].UNSIGNED" and
        builder("Short").build(dataType4) === "TINYINT[Short](1)" and
        builder("Option[Short]").build(dataType4) === "TINYINT[Option[Short]](1)"
    }

    "SMALLINT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.SMALLINT(None, true, true)
      val dataType2 = DataType.SMALLINT(Some(1), false, true)
      val dataType3 = DataType.SMALLINT(None, true, false)
      val dataType4 = DataType.SMALLINT(Some(1), false, false)
      builder("Int").build(dataType1) === "SMALLINT[Int].UNSIGNED.ZEROFILL" and
        builder("Option[Int]").build(dataType1) === "SMALLINT[Option[Int]].UNSIGNED.ZEROFILL" and
        builder("Int").build(dataType2) === "SMALLINT[Int](1).ZEROFILL" and
        builder("Option[Int]").build(dataType2) === "SMALLINT[Option[Int]](1).ZEROFILL" and
        builder("Int").build(dataType3) === "SMALLINT[Int].UNSIGNED" and
        builder("Option[Int]").build(dataType3) === "SMALLINT[Option[Int]].UNSIGNED" and
        builder("Int").build(dataType4) === "SMALLINT[Int](1)" and
        builder("Option[Int]").build(dataType4) === "SMALLINT[Option[Int]](1)"
    }

    "MEDIUMINT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.MEDIUMINT(None, true, true)
      val dataType2 = DataType.MEDIUMINT(Some(1), false, true)
      val dataType3 = DataType.MEDIUMINT(None, true, false)
      val dataType4 = DataType.MEDIUMINT(Some(1), false, false)
      builder("Int").build(dataType1) === "MEDIUMINT[Int].UNSIGNED.ZEROFILL" and
        builder("Option[Int]").build(dataType1) === "MEDIUMINT[Option[Int]].UNSIGNED.ZEROFILL" and
        builder("Int").build(dataType2) === "MEDIUMINT[Int](1).ZEROFILL" and
        builder("Option[Int]").build(dataType2) === "MEDIUMINT[Option[Int]](1).ZEROFILL" and
        builder("Int").build(dataType3) === "MEDIUMINT[Int].UNSIGNED" and
        builder("Option[Int]").build(dataType3) === "MEDIUMINT[Option[Int]].UNSIGNED" and
        builder("Int").build(dataType4) === "MEDIUMINT[Int](1)" and
        builder("Option[Int]").build(dataType4) === "MEDIUMINT[Option[Int]](1)"
    }

    "INT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.INT(None, true, true)
      val dataType2 = DataType.INT(Some(1), false, true)
      val dataType3 = DataType.INT(None, true, false)
      val dataType4 = DataType.INT(Some(1), false, false)
      builder("Int").build(dataType1) === "INT[Int].UNSIGNED.ZEROFILL" and
        builder("Option[Int]").build(dataType1) === "INT[Option[Int]].UNSIGNED.ZEROFILL" and
        builder("Int").build(dataType2) === "INT[Int](1).ZEROFILL" and
        builder("Option[Int]").build(dataType2) === "INT[Option[Int]](1).ZEROFILL" and
        builder("Int").build(dataType3) === "INT[Int].UNSIGNED" and
        builder("Option[Int]").build(dataType3) === "INT[Option[Int]].UNSIGNED" and
        builder("Int").build(dataType4) === "INT[Int](1)" and
        builder("Option[Int]").build(dataType4) === "INT[Option[Int]](1)"
    }

    "BIGINT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.BIGINT(None, true, true)
      val dataType2 = DataType.BIGINT(Some(1), false, true)
      val dataType3 = DataType.BIGINT(None, true, false)
      val dataType4 = DataType.BIGINT(Some(1), false, false)
      builder("BigInt").build(dataType1) === "BIGINT[BigInt].UNSIGNED.ZEROFILL" and
        builder("Option[BigInt]").build(dataType1) === "BIGINT[Option[BigInt]].UNSIGNED.ZEROFILL" and
        builder("BigInt").build(dataType2) === "BIGINT[BigInt](1).ZEROFILL" and
        builder("Option[BigInt]").build(dataType2) === "BIGINT[Option[BigInt]](1).ZEROFILL" and
        builder("BigInt").build(dataType3) === "BIGINT[BigInt].UNSIGNED" and
        builder("Option[BigInt]").build(dataType3) === "BIGINT[Option[BigInt]].UNSIGNED" and
        builder("BigInt").build(dataType4) === "BIGINT[BigInt](1)" and
        builder("Option[BigInt]").build(dataType4) === "BIGINT[Option[BigInt]](1)"
    }

    "DECIMAL DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.DECIMAL(10, 0, true, true)
      val dataType2 = DataType.DECIMAL(10, 0, false, true)
      val dataType3 = DataType.DECIMAL(10, 0, true, false)
      val dataType4 = DataType.DECIMAL(10, 0, false, false)
      builder("BigDecimal").build(dataType1) === "DECIMAL[BigDecimal](10, 0).UNSIGNED.ZEROFILL" and
        builder("Option[BigDecimal]").build(dataType1) === "DECIMAL[Option[BigDecimal]](10, 0).UNSIGNED.ZEROFILL" and
        builder("BigDecimal").build(dataType2) === "DECIMAL[BigDecimal](10, 0).ZEROFILL" and
        builder("Option[BigDecimal]").build(dataType2) === "DECIMAL[Option[BigDecimal]](10, 0).ZEROFILL" and
        builder("BigDecimal").build(dataType3) === "DECIMAL[BigDecimal](10, 0).UNSIGNED" and
        builder("Option[BigDecimal]").build(dataType3) === "DECIMAL[Option[BigDecimal]](10, 0).UNSIGNED" and
        builder("BigDecimal").build(dataType4) === "DECIMAL[BigDecimal](10, 0)" and
        builder("Option[BigDecimal]").build(dataType4) === "DECIMAL[Option[BigDecimal]](10, 0)"
    }

    "FLOAT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.FLOAT(10, true, true)
      val dataType2 = DataType.FLOAT(10, false, true)
      val dataType3 = DataType.FLOAT(10, true, false)
      val dataType4 = DataType.FLOAT(10, false, false)
      builder("Float").build(dataType1) === "FLOAT[Float](10).UNSIGNED.ZEROFILL" and
        builder("Option[Float]").build(dataType1) === "FLOAT[Option[Float]](10).UNSIGNED.ZEROFILL" and
        builder("Float").build(dataType2) === "FLOAT[Float](10).ZEROFILL" and
        builder("Option[Float]").build(dataType2) === "FLOAT[Option[Float]](10).ZEROFILL" and
        builder("Float").build(dataType3) === "FLOAT[Float](10).UNSIGNED" and
        builder("Option[Float]").build(dataType3) === "FLOAT[Option[Float]](10).UNSIGNED" and
        builder("Float").build(dataType4) === "FLOAT[Float](10)" and
        builder("Option[Float]").build(dataType4) === "FLOAT[Option[Float]](10)"
    }

    "CHAR DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.CHAR(255, Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.CHAR(255, Some("utf8mb4"), None)
      val dataType3 = DataType.CHAR(255, None, Some("utf8mb4_bin"))
      val dataType4 = DataType.CHAR(255, None, None)
      builder("String").build(dataType1) === "CHAR[String](255).CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType1) === "CHAR[Option[String]](255).CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType2) === "CHAR[String](255).CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[String]").build(dataType2) === "CHAR[Option[String]](255).CHARACTER_SET(\"utf8mb4\")" and
        builder("String").build(dataType3) === "CHAR[String](255).COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType3) === "CHAR[Option[String]](255).COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType4) === "CHAR[String](255)" and
        builder("Option[String]").build(dataType4) === "CHAR[Option[String]](255)"
    }

    "VARCHAR DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.VARCHAR(255, Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.VARCHAR(255, Some("utf8mb4"), None)
      val dataType3 = DataType.VARCHAR(255, None, Some("utf8mb4_bin"))
      val dataType4 = DataType.VARCHAR(255, None, None)
      builder("String").build(dataType1) === "VARCHAR[String](255).CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType1) === "VARCHAR[Option[String]](255).CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType2) === "VARCHAR[String](255).CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[String]").build(dataType2) === "VARCHAR[Option[String]](255).CHARACTER_SET(\"utf8mb4\")" and
        builder("String").build(dataType3) === "VARCHAR[String](255).COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType3) === "VARCHAR[Option[String]](255).COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType4) === "VARCHAR[String](255)" and
        builder("Option[String]").build(dataType4) === "VARCHAR[Option[String]](255)"
    }

    "BINARY DataType construction to code string matches the specified string." in {
      val dataType = DataType.BINARY(255)
      builder("String").build(dataType) === "BINARY[String](255)" and
        builder("Option[String]").build(dataType) === "BINARY[Option[String]](255)"
    }

    "VARBINARY DataType construction to code string matches the specified string." in {
      val dataType = DataType.VARBINARY(255)
      builder("Array[Byte]").build(dataType) === "VARBINARY[Array[Byte]](255)" and
        builder("Option[Array[Byte]]").build(dataType) === "VARBINARY[Option[Array[Byte]]](255)"
    }

    "TINYBLOB DataType construction to code string matches the specified string." in {
      val dataType = DataType.TINYBLOB()
      builder("Array[Byte]").build(dataType) === "TINYBLOB[Array[Byte]]()" and
        builder("Option[Array[Byte]]").build(dataType) === "TINYBLOB[Option[Array[Byte]]]()"
    }

    "TINYTEXT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.TINYTEXT(Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.TINYTEXT(Some("utf8mb4"), None)
      val dataType3 = DataType.TINYTEXT(None, Some("utf8mb4_bin"))
      val dataType4 = DataType.TINYTEXT(None, None)
      builder("String").build(dataType1) === "TINYTEXT[String]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType1) === "TINYTEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType2) === "TINYTEXT[String]().CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[String]").build(dataType2) === "TINYTEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\")" and
        builder("String").build(dataType3) === "TINYTEXT[String]().COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType3) === "TINYTEXT[Option[String]]().COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType4) === "TINYTEXT[String]()" and
        builder("Option[String]").build(dataType4) === "TINYTEXT[Option[String]]()"
    }

    "ENUM DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.ENUM(List("Active", "InActive"), Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.ENUM(List("Active", "InActive"), Some("utf8mb4"), None)
      val dataType3 = DataType.ENUM(List("Active", "InActive"), None, Some("utf8mb4_bin"))
      val dataType4 = DataType.ENUM(List("Active", "InActive"), None, None)
      builder("Status").build(dataType1) === "ENUM[Status](using Status).CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[Status]").build(dataType1) === "ENUM[Option[Status]](using Status).CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Status").build(dataType2) === "ENUM[Status](using Status).CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[Status]").build(dataType2) === "ENUM[Option[Status]](using Status).CHARACTER_SET(\"utf8mb4\")" and
        builder("Status").build(dataType3) === "ENUM[Status](using Status).COLLATE(\"utf8mb4_bin\")" and
        builder("Option[Status]").build(dataType3) === "ENUM[Option[Status]](using Status).COLLATE(\"utf8mb4_bin\")" and
        builder("Status").build(dataType4) === "ENUM[Status](using Status)" and
        builder("Option[Status]").build(dataType4) === "ENUM[Option[Status]](using Status)"
    }

    "BLOB DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.BLOB(Some(255))
      val dataType2 = DataType.BLOB(None)
      builder("Array[Byte]").build(dataType1) === "BLOB[Array[Byte]](255)" and
        builder("Option[Array[Byte]]").build(dataType1) === "BLOB[Option[Array[Byte]]](255)" and
        builder("Array[Byte]").build(dataType2) === "BLOB[Array[Byte]]()" and
        builder("Option[Array[Byte]]").build(dataType2) === "BLOB[Option[Array[Byte]]]()"
    }

    "TEXT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.TEXT(Some(255), Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.TEXT(None, Some("utf8mb4"), None)
      val dataType3 = DataType.TEXT(Some(255), None, Some("utf8mb4_bin"))
      val dataType4 = DataType.TEXT(None, None, None)
      builder("String").build(dataType1) === "TEXT[String]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType1) === "TEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType2) === "TEXT[String]().CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[String]").build(dataType2) === "TEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\")" and
        builder("String").build(dataType3) === "TEXT[String]().COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType3) === "TEXT[Option[String]]().COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType4) === "TEXT[String]()" and
        builder("Option[String]").build(dataType4) === "TEXT[Option[String]]()"
    }

    "MEDIUMBLOB DataType construction to code string matches the specified string." in {
      val dataType = DataType.MEDIUMBLOB()
      builder("Array[Byte]").build(dataType) === "MEDIUMBLOB[Array[Byte]]()" and
        builder("Option[Array[Byte]]").build(dataType) === "MEDIUMBLOB[Option[Array[Byte]]]()"
    }

    "MEDIUMTEXT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.MEDIUMTEXT(Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.MEDIUMTEXT(Some("utf8mb4"), None)
      val dataType3 = DataType.MEDIUMTEXT(None, Some("utf8mb4_bin"))
      val dataType4 = DataType.MEDIUMTEXT(None, None)
      builder("String").build(dataType1) === "MEDIUMTEXT[String]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType1) === "MEDIUMTEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType2) === "MEDIUMTEXT[String]().CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[String]").build(dataType2) === "MEDIUMTEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\")" and
        builder("String").build(dataType3) === "MEDIUMTEXT[String]().COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType3) === "MEDIUMTEXT[Option[String]]().COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType4) === "MEDIUMTEXT[String]()" and
        builder("Option[String]").build(dataType4) === "MEDIUMTEXT[Option[String]]()"
    }

    "LONGBLOB DataType construction to code string matches the specified string." in {
      val dataType = DataType.LONGBLOB()
      builder("Array[Byte]").build(dataType) === "LONGBLOB[Array[Byte]]()" and
        builder("Option[Array[Byte]]").build(dataType) === "LONGBLOB[Option[Array[Byte]]]()"
    }

    "LONGTEXT DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.LONGTEXT(Some("utf8mb4"), Some("utf8mb4_bin"))
      val dataType2 = DataType.LONGTEXT(Some("utf8mb4"), None)
      val dataType3 = DataType.LONGTEXT(None, Some("utf8mb4_bin"))
      val dataType4 = DataType.LONGTEXT(None, None)
      builder("String").build(dataType1) === "LONGTEXT[String]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType1) === "LONGTEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\").COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType2) === "LONGTEXT[String]().CHARACTER_SET(\"utf8mb4\")" and
        builder("Option[String]").build(dataType2) === "LONGTEXT[Option[String]]().CHARACTER_SET(\"utf8mb4\")" and
        builder("String").build(dataType3) === "LONGTEXT[String]().COLLATE(\"utf8mb4_bin\")" and
        builder("Option[String]").build(dataType3) === "LONGTEXT[Option[String]]().COLLATE(\"utf8mb4_bin\")" and
        builder("String").build(dataType4) === "LONGTEXT[String]()" and
        builder("Option[String]").build(dataType4) === "LONGTEXT[Option[String]]()"
    }

    "DATE DataType construction to code string matches the specified string." in {
      val dataType = DataType.DATE()
      builder("LocalDate").build(dataType) === "DATE[LocalDate]" and
        builder("Option[LocalDate]").build(dataType) === "DATE[Option[LocalDate]]"
    }

    "DATETIME DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.DATETIME(Some(6))
      val dataType2 = DataType.DATETIME(None)
      builder("LocalDateTime").build(dataType1) === "DATETIME[LocalDateTime](6)" and
        builder("Option[LocalDateTime]").build(dataType1) === "DATETIME[Option[LocalDateTime]](6)" and
        builder("LocalDateTime").build(dataType2) === "DATETIME[LocalDateTime]" and
        builder("Option[LocalDateTime]").build(dataType2) === "DATETIME[Option[LocalDateTime]]"
    }

    "TIMESTAMP DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.TIMESTAMP(Some(6))
      val dataType2 = DataType.TIMESTAMP(None)
      builder("LocalDateTime").build(dataType1) === "TIMESTAMP[LocalDateTime](6)" and
        builder("Option[LocalDateTime]").build(dataType1) === "TIMESTAMP[Option[LocalDateTime]](6)" and
        builder("LocalDateTime").build(dataType2) === "TIMESTAMP[LocalDateTime]" and
        builder("Option[LocalDateTime]").build(dataType2) === "TIMESTAMP[Option[LocalDateTime]]"
    }

    "TIME DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.TIME(Some(6))
      val dataType2 = DataType.TIME(None)
      builder("LocalTime").build(dataType1) === "TIME[LocalTime](6)" and
        builder("Option[LocalTime]").build(dataType1) === "TIME[Option[LocalTime]](6)" and
        builder("LocalTime").build(dataType2) === "TIME[LocalTime]" and
        builder("Option[LocalTime]").build(dataType2) === "TIME[Option[LocalTime]]"
    }

    "YEAR DataType construction to code string matches the specified string." in {
      val dataType1 = DataType.YEAR(Some(4))
      val dataType2 = DataType.YEAR(None)
      builder("Year").build(dataType1) === "YEAR[Year](4)" and
        builder("Option[Year]").build(dataType1) === "YEAR[Option[Year]](4)" and
        builder("Year").build(dataType2) === "YEAR[Year]" and
        builder("Option[Year]").build(dataType2) === "YEAR[Option[Year]]"
    }

    "SERIAL DataType construction to code string matches the specified string." in {
      val dataType = DataType.SERIAL()
      builder("BigInt").build(dataType) === "SERIAL[BigInt]" and
        builder("Option[BigInt]").build(dataType) === "SERIAL[BigInt]"
    }
  }
