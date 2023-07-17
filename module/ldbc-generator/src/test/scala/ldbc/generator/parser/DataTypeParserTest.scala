/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.generator.parser

import org.scalatest.flatspec.AnyFlatSpec

class DataTypeParserTest extends AnyFlatSpec, DataTypeParser:

  override def fileName: String = "test.sql"

  it should "BIT data type parsing test succeeds." in {
    assert(parseAll(bitType, "bit").successful)
    assert(parseAll(bitType, "Bit(1)").successful)
    assert(parseAll(bitType, "BIT(64)").successful)
  }

  it should "BIT data type parsing test fails." in {
    assert(!parseAll(bitType, "failed").successful)
    assert(!parseAll(bitType, "Bit(0)").successful)
    assert(!parseAll(bitType, "BIT(65)").successful)
  }

  it should "TINYINT data type parsing test succeeds." in {
    assert(parseAll(tinyintType, "tinyint").successful)
    assert(parseAll(tinyintType, "Tinyint").successful)
    assert(parseAll(tinyintType, "Tinyint(1)").successful)
    assert(parseAll(tinyintType, "TINYINT(255)").successful)
    assert(parseAll(tinyintType, "TINYINT(255) UNSIGNED").successful)
    assert(parseAll(tinyintType, "TINYINT(255) ZEROFILL").successful)
    assert(parseAll(tinyintType, "TINYINT(255) UNSIGNED ZEROFILL").successful)
  }

  it should "TINYINT data type parsing test fails." in {
    assert(!parseAll(tinyintType, "failed").successful)
    assert(!parseAll(tinyintType, "Tinyint(0)").successful)
    assert(!parseAll(tinyintType, "TINYINT(256)").successful)
    assert(!parseAll(tinyintType, "TINYINT(255) failed").successful)
    assert(!parseAll(tinyintType, "TINYINT(255) ZEROFILL UNSIGNED").successful)
  }

  it should "SMALLINT data type parsing test succeeds." in {
    assert(parseAll(smallintType, "smallint").successful)
    assert(parseAll(smallintType, "Smallint").successful)
    assert(parseAll(smallintType, "Smallint(1)").successful)
    assert(parseAll(smallintType, "SMALLINT(255)").successful)
    assert(parseAll(smallintType, "SMALLINT(255) UNSIGNED").successful)
    assert(parseAll(smallintType, "SMALLINT(255) ZEROFILL").successful)
    assert(parseAll(smallintType, "SMALLINT(255) UNSIGNED ZEROFILL").successful)
  }

  it should "SMALLINT data type parsing test fails." in {
    assert(!parseAll(smallintType, "failed").successful)
    assert(!parseAll(smallintType, "Smallint(0)").successful)
    assert(!parseAll(smallintType, "SMALLINT(256)").successful)
    assert(!parseAll(smallintType, "SMALLINT(255) failed").successful)
    assert(!parseAll(smallintType, "SMALLINT(255) ZEROFILL UNSIGNED").successful)
  }

  it should "MEDIUMINT data type parsing test succeeds." in {
    assert(parseAll(mediumintType, "mediumint").successful)
    assert(parseAll(mediumintType, "Mediumint").successful)
    assert(parseAll(mediumintType, "Mediumint(1)").successful)
    assert(parseAll(mediumintType, "MEDIUMINT(255)").successful)
    assert(parseAll(mediumintType, "MEDIUMINT(255) UNSIGNED").successful)
    assert(parseAll(mediumintType, "MEDIUMINT(255) ZEROFILL").successful)
    assert(parseAll(mediumintType, "MEDIUMINT(255) UNSIGNED ZEROFILL").successful)
  }

  it should "MEDIUMINT data type parsing test fails." in {
    assert(!parseAll(mediumintType, "failed").successful)
    assert(!parseAll(mediumintType, "Mediumint(0)").successful)
    assert(!parseAll(mediumintType, "MEDIUMINT(256)").successful)
    assert(!parseAll(mediumintType, "MEDIUMINT(255) failed").successful)
    assert(!parseAll(mediumintType, "MEDIUMINT(255) ZEROFILL UNSIGNED").successful)
  }

  it should "INT data type parsing test succeeds." in {
    assert(parseAll(intType, "int").successful)
    assert(parseAll(intType, "Int(1)").successful)
    assert(parseAll(intType, "INT(255)").successful)
    assert(parseAll(intType, "INT(255) UNSIGNED").successful)
    assert(parseAll(intType, "INT(255) ZEROFILL").successful)
    assert(parseAll(intType, "INT(255) UNSIGNED ZEROFILL").successful)
    assert(parseAll(intType, "integer").successful)
    assert(parseAll(intType, "Integer(1)").successful)
    assert(parseAll(intType, "INTEGER(255)").successful)
    assert(parseAll(intType, "INTEGER(255) UNSIGNED").successful)
    assert(parseAll(intType, "INTEGER(255) ZEROFILL").successful)
    assert(parseAll(intType, "INTEGER(255) UNSIGNED ZEROFILL").successful)
  }

  it should "INT data type parsing test fails." in {
    assert(!parseAll(intType, "failed").successful)
    assert(!parseAll(intType, "Int(0)").successful)
    assert(!parseAll(intType, "INT(256)").successful)
    assert(!parseAll(intType, "INT(255) failed").successful)
    assert(!parseAll(intType, "INT(255) ZEROFILL UNSIGNED").successful)
    assert(!parseAll(intType, "Integer(0)").successful)
    assert(!parseAll(intType, "INTEGER(256)").successful)
    assert(!parseAll(intType, "INTEGER(255) failed").successful)
    assert(!parseAll(intType, "INTEGER(255) ZEROFILL UNSIGNED").successful)
  }

  it should "BIGINT data type parsing test succeeds." in {
    assert(parseAll(bigIntType, "bigint").successful)
    assert(parseAll(bigIntType, "Bigint(1)").successful)
    assert(parseAll(bigIntType, "BIGINT(255)").successful)
    assert(parseAll(bigIntType, "BIGINT(255) UNSIGNED").successful)
    assert(parseAll(bigIntType, "BIGINT(255) ZEROFILL").successful)
    assert(parseAll(bigIntType, "BIGINT(255) UNSIGNED ZEROFILL").successful)
  }

  it should "BIGINT data type parsing test fails." in {
    assert(!parseAll(bigIntType, "failed").successful)
    assert(!parseAll(bigIntType, "Bigint(0)").successful)
    assert(!parseAll(bigIntType, "BIGINT(256)").successful)
    assert(!parseAll(bigIntType, "BIGINT(255) failed").successful)
    assert(!parseAll(bigIntType, "BIGINT(255) ZEROFILL UNSIGNED").successful)
  }

  it should "DECIMAL data type parsing test succeeds." in {
    assert(parseAll(decimalType, "decimal").successful)
    assert(parseAll(decimalType, "Decimal(1)").successful)
    assert(parseAll(decimalType, "DECIMAL(65, 5)").successful)
    assert(parseAll(decimalType, "DECIMAL(10, 30) UNSIGNED").successful)
    assert(parseAll(decimalType, "DECIMAL(0, 0) ZEROFILL").successful)
    assert(parseAll(decimalType, "DECIMAL(65, 30) UNSIGNED ZEROFILL").successful)
    assert(parseAll(decimalType, "dec").successful)
    assert(parseAll(decimalType, "Dec(1)").successful)
    assert(parseAll(decimalType, "DEC(65, 5)").successful)
    assert(parseAll(decimalType, "DEC(10, 30) UNSIGNED").successful)
    assert(parseAll(decimalType, "DEC(0, 0) ZEROFILL").successful)
    assert(parseAll(decimalType, "DEC(65, 30) UNSIGNED ZEROFILL").successful)
  }

  it should "DECIMAL data type parsing test fails." in {
    assert(!parseAll(decimalType, "failed").successful)
    assert(!parseAll(decimalType, "Decimal(-1)").successful)
    assert(!parseAll(decimalType, "DECIMAL(66, 0)").successful)
    assert(!parseAll(decimalType, "DECIMAL(10, 5) failed").successful)
    assert(!parseAll(decimalType, "DECIMAL(0, 31) UNSIGNED").successful)
    assert(!parseAll(decimalType, "DECIMAL(10, 5) ZEROFILL UNSIGNED").successful)
    assert(!parseAll(decimalType, "Dec(-1)").successful)
    assert(!parseAll(decimalType, "DEC(66, 0)").successful)
    assert(!parseAll(decimalType, "DEC(10, 5) failed").successful)
    assert(!parseAll(decimalType, "DEC(0, 31) UNSIGNED").successful)
    assert(!parseAll(decimalType, "DEC(10, 5) ZEROFILL UNSIGNED").successful)
  }

  it should "FLOAT data type parsing test succeeds." in {
    assert(parseAll(floatType, "float(0)").successful)
    assert(parseAll(floatType, "Float(24)").successful)
    assert(parseAll(floatType, "FLOAT(10) UNSIGNED").successful)
    assert(parseAll(floatType, "FLOAT(10) ZEROFILL").successful)
    assert(parseAll(floatType, "FLOAT(10) UNSIGNED ZEROFILL").successful)
  }

  it should "FLOAT data type parsing test fails." in {
    assert(!parseAll(floatType, "failed").successful)
    assert(!parseAll(floatType, "float").successful)
    assert(!parseAll(floatType, "Float(-1)").successful)
    assert(!parseAll(floatType, "Float(25)").successful)
    assert(!parseAll(floatType, "FLOAT(10) failed").successful)
    assert(!parseAll(floatType, "FLOAT(10) ZEROFILL UNSIGNED").successful)
  }

  it should "DOUBLE data type parsing test succeeds." in {
    assert(parseAll(doubleType, "double").successful)
    assert(parseAll(doubleType, "Double(24, 24)").successful)
    assert(parseAll(doubleType, "DOUBLE(53, 53)").successful)
    assert(parseAll(doubleType, "Double(24, 24) UNSIGNED").successful)
    assert(parseAll(doubleType, "DOUBLE(53, 53) ZEROFILL").successful)
    assert(parseAll(doubleType, "DOUBLE UNSIGNED ZEROFILL").successful)
    assert(parseAll(doubleType, "real").successful)
    assert(parseAll(doubleType, "Real(24, 24)").successful)
    assert(parseAll(doubleType, "REAL(53, 53)").successful)
    assert(parseAll(doubleType, "REAL(24, 24) UNSIGNED").successful)
    assert(parseAll(doubleType, "REAL(53, 53) ZEROFILL").successful)
    assert(parseAll(doubleType, "REAL UNSIGNED ZEROFILL").successful)
  }

  it should "DOUBLE data type parsing test fails." in {
    assert(!parseAll(doubleType, "failed").successful)
    assert(!parseAll(doubleType, "double(23, 24)").successful)
    assert(!parseAll(doubleType, "Double(24, 23)").successful)
    assert(!parseAll(doubleType, "DOUBLE(54, 54)").successful)
    assert(!parseAll(doubleType, "Double(24, 24) failed").successful)
    assert(!parseAll(doubleType, "DOUBLE ZEROFILL UNSIGNED").successful)
    assert(!parseAll(doubleType, "real(23, 24)").successful)
    assert(!parseAll(doubleType, "Real(24, 23)").successful)
    assert(!parseAll(doubleType, "REAL(54, 54)").successful)
    assert(!parseAll(doubleType, "REAL(24, 24) failed").successful)
    assert(!parseAll(doubleType, "REAL ZEROFILL UNSIGNED").successful)
  }

  it should "CHAR data type parsing test succeeds." in {
    assert(parseAll(charType, "char").successful)
    assert(parseAll(charType, "Char(0)").successful)
    assert(parseAll(charType, "CHAR(255)").successful)
    assert(parseAll(charType, "NATIONAL CHAR").successful)
    assert(parseAll(charType, "CHAR(0) CHARACTER SET utf8mb4").successful)
    assert(parseAll(charType, "CHAR(0) CHARACTER SET = utf8mb4").successful)
    assert(parseAll(charType, "CHAR(0) CHARSET utf8mb4").successful)
    assert(parseAll(charType, "CHAR(0) CHARSET=utf8mb4").successful)
    assert(parseAll(charType, "CHAR(255) COLLATE utf8mb4_bin").successful)
    assert(parseAll(charType, "CHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(charType, "CHAR(255) CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(charType, "CHAR(255) CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
    assert(parseAll(charType, "NATIONAL CHAR CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(charType, "character").successful)
    assert(parseAll(charType, "Character(0)").successful)
    assert(parseAll(charType, "CHARACTER(255)").successful)
    assert(parseAll(charType, "NATIONAL CHARACTER").successful)
    assert(parseAll(charType, "CHARACTER(0) CHARACTER SET utf8mb4").successful)
    assert(parseAll(charType, "CHARACTER(0) CHARACTER SET = utf8mb4").successful)
    assert(parseAll(charType, "CHARACTER(0) CHARSET utf8mb4").successful)
    assert(parseAll(charType, "CHARACTER(0) CHARSET=utf8mb4").successful)
    assert(parseAll(charType, "CHARACTER(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(charType, "CHARACTER(255) CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(charType, "CHARACTER(255) CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
    assert(parseAll(charType, "NATIONAL CHARACTER CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
  }

  it should "CHAR data type parsing test fails." in {
    assert(!parseAll(charType, "failed").successful)
    assert(!parseAll(charType, "failed Char").successful)
    assert(!parseAll(charType, "CHAR(-1)").successful)
    assert(!parseAll(charType, "CHAR(256)").successful)
    assert(!parseAll(charType, "CHARACTER(0) CHARACTER SET").successful)
    assert(!parseAll(charType, "CHAR(0) CHARACTER utf8mb4").successful)
    assert(!parseAll(charType, "CHARACTER(255) CHARACTER SET utf8mb4 COLLATE").successful)
  }

  it should "VARCHAR data type parsing test succeeds." in {
    assert(parseAll(varcharType, "Varchar(0)").successful)
    assert(parseAll(varcharType, "VARCHAR(255)").successful)
    assert(parseAll(varcharType, "VARCHAR(0) CHARACTER SET utf8mb4").successful)
    assert(parseAll(varcharType, "VARCHAR(0) CHARACTER SET = utf8mb4").successful)
    assert(parseAll(varcharType, "VARCHAR(0) CHARSET utf8mb4").successful)
    assert(parseAll(varcharType, "VARCHAR(0) CHARSET=utf8mb4").successful)
    assert(parseAll(varcharType, "VARCHAR(255) COLLATE utf8mb4_bin").successful)
    assert(parseAll(varcharType, "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(varcharType, "VARCHAR(65535) CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(varcharType, "VARCHAR(65535) CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
    assert(parseAll(varcharType, "NATIONAL VARCHAR(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
  }

  it should "VARCHAR data type parsing test fails." in {
    assert(!parseAll(varcharType, "failed").successful)
    assert(!parseAll(varcharType, "failed Varchar").successful)
    assert(!parseAll(varcharType, "NATIONAL VARCHAR").successful)
    assert(!parseAll(varcharType, "VARCHAR(-1)").successful)
    assert(!parseAll(varcharType, "VARCHAR(65536)").successful)
    assert(!parseAll(varcharType, "VARCHAR(0) CHARACTER SET").successful)
    assert(!parseAll(varcharType, "VARCHAR(0) CHARACTER utf8mb4").successful)
    assert(!parseAll(varcharType, "VARCHAR(255) CHARACTER SET utf8mb4 COLLATE").successful)
    assert(!parseAll(varcharType, "NATIONAL VARCHAR CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
  }

  it should "BINARY data type parsing test succeeds." in {
    assert(parseAll(binaryType, "binary").successful)
    assert(parseAll(binaryType, "Binary(0)").successful)
    assert(parseAll(binaryType, "BINARY(255)").successful)
  }

  it should "BINARY data type parsing test fails." in {
    assert(!parseAll(binaryType, "failed").successful)
    assert(!parseAll(binaryType, "binary(-1)").successful)
    assert(!parseAll(binaryType, "Binary(256)").successful)
  }

  it should "VARBINARY data type parsing test succeeds." in {
    assert(parseAll(varbinaryType, "Varbinary(0)").successful)
    assert(parseAll(varbinaryType, "VARBINARY(255)").successful)
  }

  it should "VARBINARY data type parsing test fails." in {
    assert(!parseAll(varbinaryType, "failed").successful)
    assert(!parseAll(varbinaryType, "varbinary").successful)
    assert(!parseAll(varbinaryType, "varbinary(-1)").successful)
  }

  it should "TINYBLOB data type parsing test succeeds." in {
    assert(parseAll(tinyblobType, "tinyblob").successful)
    assert(parseAll(tinyblobType, "Tinyblob").successful)
    assert(parseAll(tinyblobType, "TINYBLOB").successful)
  }

  it should "TINYBLOB data type parsing test fails." in {
    assert(!parseAll(tinyblobType, "failed").successful)
    assert(!parseAll(tinyblobType, "tinyblob(1)").successful)
  }

  it should "TINYTEXT data type parsing test succeeds." in {
    assert(parseAll(tinytextType, "tinytext").successful)
    assert(parseAll(tinytextType, "Tinytext").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARACTER SET utf8mb4").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARACTER SET = utf8mb4").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARSET utf8mb4").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARSET=utf8mb4").successful)
    assert(parseAll(tinytextType, "TINYTEXT COLLATE utf8mb4_bin").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(tinytextType, "TINYTEXT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
  }

  it should "TINYTEXT data type parsing test fails." in {
    assert(!parseAll(tinytextType, "failed").successful)
    assert(!parseAll(tinytextType, "Tinytext(-1)").successful)
    assert(!parseAll(tinytextType, "TINYTEXT CHARACTER utf8mb4").successful)
  }

  it should "BLOB data type parsing test succeeds." in {
    assert(parseAll(blobType, "blob").successful)
    assert(parseAll(blobType, "Blob(0)").successful)
    assert(parseAll(blobType, "BLOB(255)").successful)
  }

  it should "BLOB data type parsing test fails." in {
    assert(!parseAll(blobType, "failed").successful)
    assert(!parseAll(blobType, "Blob(-1)").successful)
  }

  it should "TEXT data type parsing test succeeds." in {
    assert(parseAll(textType, "text").successful)
    assert(parseAll(textType, "Text(0)").successful)
    assert(parseAll(textType, "TEXT(255)").successful)
    assert(parseAll(textType, "TEXT(0) CHARACTER SET utf8mb4").successful)
    assert(parseAll(textType, "TEXT(0) CHARACTER SET = utf8mb4").successful)
    assert(parseAll(textType, "TEXT(0) CHARSET utf8mb4").successful)
    assert(parseAll(textType, "TEXT(0) CHARSET=utf8mb4").successful)
    assert(parseAll(textType, "TEXT(255) COLLATE utf8mb4_bin").successful)
    assert(parseAll(textType, "TEXT(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(textType, "TEXT(255) CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(textType, "TEXT(255) CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
  }

  it should "TEXT data type parsing test fails." in {
    assert(!parseAll(textType, "failed").successful)
    assert(!parseAll(textType, "text(-1)").successful)
    assert(!parseAll(textType, "Text(256)").successful)
    assert(!parseAll(textType, "TEXT(0) CHARACTER utf8mb4").successful)
  }

  it should "MEDIUMBLOB data type parsing test succeeds." in {
    assert(parseAll(mediumblobType, "mediumblob").successful)
    assert(parseAll(mediumblobType, "Mediumblob").successful)
    assert(parseAll(mediumblobType, "MEDIUMBLOB").successful)
  }

  it should "MEDIUMBLOB data type parsing test fails." in {
    assert(!parseAll(mediumblobType, "failed").successful)
    assert(!parseAll(mediumblobType, "mediumblob(1)").successful)
  }

  it should "MEDIUMTEXT data type parsing test succeeds." in {
    assert(parseAll(mediumtextType, "mediumtext").successful)
    assert(parseAll(mediumtextType, "Mediumtext").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARACTER SET utf8mb4").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARACTER SET = utf8mb4").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARSET utf8mb4").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARSET=utf8mb4").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT COLLATE utf8mb4_bin").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(mediumtextType, "MEDIUMTEXT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
  }

  it should "MEDIUMTEXT data type parsing test fails." in {
    assert(!parseAll(mediumtextType, "failed").successful)
    assert(!parseAll(mediumtextType, "mediumtext(-1)").successful)
    assert(!parseAll(mediumtextType, "MEDIUMTEXT CHARACTER utf8mb4").successful)
  }

  it should "LONGBLOB data type parsing test succeeds." in {
    assert(parseAll(longblobType, "longblob").successful)
    assert(parseAll(longblobType, "Longblob").successful)
    assert(parseAll(longblobType, "LONGBLOB").successful)
  }

  it should "LONGBLOB data type parsing test fails." in {
    assert(!parseAll(longblobType, "failed").successful)
    assert(!parseAll(longblobType, "longblob(1)").successful)
  }

  it should "LONGTEXT data type parsing test succeeds." in {
    assert(parseAll(longtextType, "longtext").successful)
    assert(parseAll(longtextType, "Longtext").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARACTER SET utf8mb4").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARACTER SET = utf8mb4").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARSET utf8mb4").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARSET=utf8mb4").successful)
    assert(parseAll(longtextType, "LONGTEXT COLLATE utf8mb4_bin").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARACTER SET utf8mb4 COLLATE = utf8mb4_bin").successful)
    assert(parseAll(longtextType, "LONGTEXT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_bin").successful)
  }

  it should "LONGTEXT data type parsing test fails." in {
    assert(!parseAll(longtextType, "failed").successful)
    assert(!parseAll(longtextType, "longtext(-1)").successful)
    assert(!parseAll(longtextType, "LONGTEXT CHARACTER utf8mb4").successful)
  }

  it should "ENUM data type parsing test succeeds." in {
    assert(parseAll(enumType, "ENUM('Active', 'InActive')").successful)
    assert(parseAll(enumType, "ENUM('Active')").successful)
  }

  it should "ENUM data type parsing test fails." in {
    assert(!parseAll(enumType, "failed").successful)
    assert(!parseAll(enumType, "ENUM").successful)
    assert(!parseAll(enumType, "ENUM()").successful)
    assert(!parseAll(enumType, "ENUM(Active)").successful)
  }

  it should "DATE data type parsing test succeeds." in {
    assert(parseAll(dateType, "date").successful)
    assert(parseAll(dateType, "Date").successful)
    assert(parseAll(dateType, "DATE").successful)
  }

  it should "DATE data type parsing test fails." in {
    assert(!parseAll(dateType, "failed").successful)
    assert(!parseAll(dateType, "DATE(1)").successful)
  }

  it should "DATETIME data type parsing test succeeds." in {
    assert(parseAll(datetimeType, "datetime").successful)
    assert(parseAll(datetimeType, "Datetime(0)").successful)
    assert(parseAll(datetimeType, "DATETIME(6)").successful)
  }

  it should "DATETIME data type parsing test fails." in {
    assert(!parseAll(datetimeType, "failed").successful)
    assert(!parseAll(datetimeType, "Datetime(-1)").successful)
    assert(!parseAll(datetimeType, "DATETIME(7)").successful)
  }

  it should "TIMESTAMP data type parsing test succeeds." in {
    assert(parseAll(timestampType, "timestamp").successful)
    assert(parseAll(timestampType, "Timestamp(0)").successful)
    assert(parseAll(timestampType, "TIMESTAMP(6)").successful)
  }

  it should "TIMESTAMP data type parsing test fails." in {
    assert(!parseAll(timestampType, "failed").successful)
    assert(!parseAll(timestampType, "Timestamp(-1)").successful)
    assert(!parseAll(timestampType, "TIMESTAMP(7)").successful)
  }

  it should "TIME data type parsing test succeeds." in {
    assert(parseAll(timeType, "time").successful)
    assert(parseAll(timeType, "Time(0)").successful)
    assert(parseAll(timeType, "TIME(6)").successful)
  }

  it should "TIME data type parsing test fails." in {
    assert(!parseAll(timeType, "failed").successful)
    assert(!parseAll(timeType, "Time(-1)").successful)
    assert(!parseAll(timeType, "TIME(7)").successful)
  }

  it should "YEAR data type parsing test succeeds." in {
    assert(parseAll(yearType, "year").successful)
    assert(parseAll(yearType, "YEAR(4)").successful)
  }

  it should "YEAR data type parsing test fails." in {
    assert(!parseAll(yearType, "failed").successful)
    assert(!parseAll(yearType, "YEAR(0)").successful)
  }

  it should "SERIAL data type parsing test succeeds." in {
    assert(parseAll(serialType, "SERIAL").successful)
  }

  it should "SERIAL data type parsing test fails." in {
    assert(!parseAll(serialType, "failed").successful)
    assert(!parseAll(serialType, "SERIAL(0)").successful)
  }
