/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import java.time.{ LocalDate, LocalDateTime, LocalTime, Year }

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.statement.Column

import ldbc.schema.DataType.*

class TableCreateStatementTest extends AnyFlatSpec:

  case class AllDataTypes(
    c1:  Option[Short],
    c2:  Option[Short],
    c3:  Option[Int],
    c4:  Option[Int],
    c5:  Long,
    c6:  Option[BigDecimal],
    c7:  Option[Float],
    c8:  Option[Double],
    c9:  Option[Byte],
    c10: Option[String],
    c11: Option[String],
    c12: Option[Array[Byte]],
    c13: Array[Byte],
    c14: Option[String],
    c15: Option[String],
    c16: Option[String],
    c17: String,
    c18: Array[Byte],
    c19: Option[Array[Byte]],
    c20: Option[Array[Byte]],
    c21: Option[Array[Byte]],
    c22: Option[LocalDate],
    c23: Option[LocalTime],
    c24: Option[LocalDateTime],
    c25: Option[LocalDateTime],
    c26: Option[Year]
  )

  class AllDataTypesTable extends Table[AllDataTypes]("all_data_types"):

    def c1:  Column[Option[Short]]         = smallint().unsigned.autoIncrement
    def c2:  Column[Option[Short]]         = smallint().default(Some(-1000))
    def c3:  Column[Option[Int]]           = mediumint().unsigned.default(Some(100000))
    def c4:  Column[Option[Int]]           = int().default(Some(42))
    def c5:  Column[Long]                  = bigint().default(9999999999L)
    def c6:  Column[Option[BigDecimal]]    = decimal(10, 2).default(Some(123.45))
    def c7:  Column[Option[Float]]         = float(10)
    def c8:  Column[Option[Double]]        = double(10).default(Some(2.71828))
    def c9:  Column[Option[Byte]]          = bit()
    def c10: Column[Option[String]]        = char(50).default(Some("FIXED"))
    def c11: Column[Option[String]]        = varchar(255).defaultNull
    def c12: Column[Option[Array[Byte]]]   = binary(10)
    def c13: Column[Array[Byte]]           = varbinary(255)
    def c14: Column[Option[String]]        = tinytext().charset(Character.utf8mb4)
    def c15: Column[Option[String]]        = text().collate(Collate.utf8mb4_unicode_ci)
    def c16: Column[Option[String]]        = mediumtext().charset(Character.utf8mb4).collate(Collate.utf8mb4_unicode_ci)
    def c17: Column[String]                = longtext().default("")
    def c18: Column[Array[Byte]]           = tinyblob()
    def c19: Column[Option[Array[Byte]]]   = blob().defaultNull
    def c20: Column[Option[Array[Byte]]]   = mediumblob()
    def c21: Column[Option[Array[Byte]]]   = longblob()
    def c22: Column[Option[LocalDate]]     = date().default(Some(LocalDate.of(2025, 2, 15)))
    def c23: Column[Option[LocalTime]]     = time().default(Some(LocalTime.of(12, 0, 0)))
    def c24: Column[Option[LocalDateTime]] = datetime().defaultCurrentTimestamp(false)
    def c25: Column[Option[LocalDateTime]] = timestamp().defaultCurrentTimestamp(true)
    def c26: Column[Option[Year]]          = year().default(Some(Year.of(2025)))

    override def keys: List[Key] = List(
      PRIMARY_KEY(c5),
      INDEX_KEY(c10)
    )

    override def * : Column[AllDataTypes] = (
      c1 *: c2 *: c3 *: c4 *: c5 *: c6 *: c7 *: c8 *: c9 *: c10 *: c11 *: c12 *: c13 *: c14 *: c15 *: c16 *: c17 *: c18 *: c19 *: c20 *: c21 *: c22 *: c23 *: c24 *: c25 *: c26
    ).to[AllDataTypes]

  private val allDataTypes = TableQuery[AllDataTypesTable]

  it should "The query string of the Column model generated with only label and DataType matches the specified string." in {
    assert(
      allDataTypes.table.c1.statement === "`c1` SMALLINT UNSIGNED NULL AUTO_INCREMENT"
    )
    assert(
      allDataTypes.table.c2.statement === "`c2` SMALLINT NULL DEFAULT -1000"
    )
    assert(
      allDataTypes.table.c3.statement === "`c3` MEDIUMINT UNSIGNED NULL DEFAULT 100000"
    )
    assert(
      allDataTypes.table.c4.statement === "`c4` INT NULL DEFAULT 42"
    )
    assert(
      allDataTypes.table.c5.statement === "`c5` BIGINT NOT NULL DEFAULT 9999999999"
    )
    assert(
      allDataTypes.table.c6.statement === "`c6` DECIMAL(10, 2) NULL DEFAULT '123.45'"
    )
    assert(
      allDataTypes.table.c7.statement === "`c7` FLOAT(10) NULL"
    )
    assert(
      allDataTypes.table.c8.statement === "`c8` FLOAT(10) NULL DEFAULT 2.71828"
    )
    assert(
      allDataTypes.table.c9.statement === "`c9` BIT NULL"
    )
    assert(
      allDataTypes.table.c10.statement === "`c10` CHAR(50) NULL DEFAULT 'FIXED'"
    )
    assert(
      allDataTypes.table.c11.statement === "`c11` VARCHAR(255) NULL DEFAULT NULL"
    )
    assert(
      allDataTypes.table.c12.statement === "`c12` BINARY(10) NULL"
    )
    assert(
      allDataTypes.table.c13.statement === "`c13` VARBINARY(255) NOT NULL"
    )
    assert(
      allDataTypes.table.c14.statement === "`c14` TINYTEXT CHARACTER SET utf8mb4 NULL"
    )
    assert(
      allDataTypes.table.c15.statement === "`c15` TEXT COLLATE utf8mb4_unicode_ci NULL"
    )
    assert(
      allDataTypes.table.c16.statement === "`c16` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL"
    )
    assert(
      allDataTypes.table.c17.statement === "`c17` LONGTEXT NOT NULL DEFAULT ''"
    )
    assert(
      allDataTypes.table.c18.statement === "`c18` TINYBLOB NOT NULL"
    )
    assert(
      allDataTypes.table.c19.statement === "`c19` BLOB NULL DEFAULT NULL"
    )
    assert(
      allDataTypes.table.c20.statement === "`c20` MEDIUMBLOB NULL"
    )
    assert(
      allDataTypes.table.c21.statement === "`c21` LONGBLOB NULL"
    )
    assert(
      allDataTypes.table.c22.statement === "`c22` DATE NULL DEFAULT '2025-02-15'"
    )
    assert(
      allDataTypes.table.c23.statement === "`c23` TIME NULL DEFAULT '12:00'"
    )
    assert(
      allDataTypes.table.c24.statement === "`c24` DATETIME NULL DEFAULT CURRENT_TIMESTAMP"
    )
    assert(
      allDataTypes.table.c25.statement === "`c25` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    assert(
      allDataTypes.table.c26.statement === "`c26` YEAR NULL DEFAULT '2025'"
    )
  }

  it should "The CREATE statement generated by Table matches the specified value." in {
    assert(
      allDataTypes.schema.createIfNotExists.statements.headOption.contains(
        """CREATE TABLE IF NOT EXISTS `all_data_types` (
          |  `c1` SMALLINT UNSIGNED NULL AUTO_INCREMENT,
          |  `c2` SMALLINT NULL DEFAULT -1000,
          |  `c3` MEDIUMINT UNSIGNED NULL DEFAULT 100000,
          |  `c4` INT NULL DEFAULT 42,
          |  `c5` BIGINT NOT NULL DEFAULT 9999999999,
          |  `c6` DECIMAL(10, 2) NULL DEFAULT '123.45',
          |  `c7` FLOAT(10) NULL,
          |  `c8` FLOAT(10) NULL DEFAULT 2.71828,
          |  `c9` BIT NULL,
          |  `c10` CHAR(50) NULL DEFAULT 'FIXED',
          |  `c11` VARCHAR(255) NULL DEFAULT NULL,
          |  `c12` BINARY(10) NULL,
          |  `c13` VARBINARY(255) NOT NULL,
          |  `c14` TINYTEXT CHARACTER SET utf8mb4 NULL,
          |  `c15` TEXT COLLATE utf8mb4_unicode_ci NULL,
          |  `c16` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
          |  `c17` LONGTEXT NOT NULL DEFAULT '',
          |  `c18` TINYBLOB NOT NULL,
          |  `c19` BLOB NULL DEFAULT NULL,
          |  `c20` MEDIUMBLOB NULL,
          |  `c21` LONGBLOB NULL,
          |  `c22` DATE NULL DEFAULT '2025-02-15',
          |  `c23` TIME NULL DEFAULT '12:00',
          |  `c24` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
          |  `c25` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          |  `c26` YEAR NULL DEFAULT '2025',
          |  PRIMARY KEY (`c5`),
          |  INDEX (`c10`)
          |)""".stripMargin
      )
    )
  }
