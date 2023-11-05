/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.builder

import java.time.*
import java.time.Year as JYear

import org.specs2.mutable.Specification

import ldbc.core.*

object TableQueryBuilderTest extends Specification:

  case class AllDataTypeTest(
    p1:  Byte,
    p2:  Option[Byte],
    p3:  Byte,
    p4:  Option[Byte],
    p5:  Short,
    p6:  Option[Short],
    p7:  Int,
    p8:  Option[Int],
    p9:  Long,
    p10: Option[Long],
    p11: BigInt,
    p12: Option[BigInt],
    p13: Float,
    p14: Option[Float],
    p15: Double,
    p16: Option[Double],
    p17: BigDecimal,
    p18: Option[BigDecimal],
    p19: String,
    p20: Option[String],
    p21: Instant,
    p22: Option[Instant],
    p23: OffsetTime,
    p24: Option[OffsetTime],
    p25: LocalTime,
    p26: Option[LocalTime],
    p27: LocalDate,
    p28: Option[LocalDate],
    p29: LocalDateTime,
    p30: Option[LocalDateTime],
    p31: OffsetDateTime,
    p32: Option[OffsetDateTime],
    p33: ZonedDateTime,
    p34: Option[ZonedDateTime],
    p35: JYear,
    p36: Option[JYear]
  )

  "TableQueryBuilder Test" should {

    "The Create statement of the Table generated using TableQueryBuilder matches the specified value." in {

      val table: Table[AllDataTypeTest] = Table[AllDataTypeTest]("all_datatype_test")(
        column("p1", BIT(1)),
        column("p2", BIT(64).DEFAULT(None)),
        column("p3", TINYINT(0)),
        column("p4", TINYINT(255).DEFAULT(None)),
        column("p5", SMALLINT(0)),
        column("p6", SMALLINT(255).DEFAULT(None)),
        column("p7", MEDIUMINT(0)),
        column("p8", MEDIUMINT(255).DEFAULT(None)),
        column("p9", BIGINT(0)),
        column("p10", BIGINT(255).DEFAULT(None)),
        column("p11", BIGINT(0)),
        column("p12", BIGINT(255).DEFAULT(None)),
        column("p13", FLOAT(0)),
        column("p14", FLOAT(24).DEFAULT(None)),
        column("p15", DOUBLE(24)),
        column("p16", DOUBLE(53).DEFAULT(None)),
        column("p17", DECIMAL(10, 0)),
        column("p18", DECIMAL(10, 65).DEFAULT(None)),
        column("p19", VARCHAR(255)),
        column("p20", VARCHAR(255).DEFAULT(None)),
        column("p21", DATETIME),
        column("p22", DATETIME.DEFAULT(None)),
        column("p23", DATETIME),
        column("p24", DATETIME.DEFAULT(None)),
        column("p25", TIME),
        column("p26", TIME.DEFAULT(None)),
        column("p27", DATE),
        column("p28", DATE.DEFAULT(None)),
        column("p29", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP(true)),
        column("p30", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP()),
        column("p31", TIMESTAMP),
        column("p32", TIMESTAMP.DEFAULT(None)),
        column("p33", TIMESTAMP),
        column("p34", TIMESTAMP.DEFAULT(None)),
        column("p35", YEAR),
        column("p36", YEAR.DEFAULT(None))
      )

      TableQueryBuilder(table).createStatement ===
        """
          |CREATE TABLE `all_datatype_test` (
          |  `p1` BIT(1) NOT NULL,
          |  `p2` BIT(64) NULL DEFAULT NULL,
          |  `p3` TINYINT(0) NOT NULL,
          |  `p4` TINYINT(255) NULL DEFAULT NULL,
          |  `p5` SMALLINT(0) NOT NULL,
          |  `p6` SMALLINT(255) NULL DEFAULT NULL,
          |  `p7` MEDIUMINT(0) NOT NULL,
          |  `p8` MEDIUMINT(255) NULL DEFAULT NULL,
          |  `p9` BIGINT(0) NOT NULL,
          |  `p10` BIGINT(255) NULL DEFAULT NULL,
          |  `p11` BIGINT(0) NOT NULL,
          |  `p12` BIGINT(255) NULL DEFAULT NULL,
          |  `p13` FLOAT(0) NOT NULL,
          |  `p14` FLOAT(24) NULL DEFAULT NULL,
          |  `p15` FLOAT(24) NOT NULL,
          |  `p16` FLOAT(53) NULL DEFAULT NULL,
          |  `p17` DECIMAL(10, 0) NOT NULL,
          |  `p18` DECIMAL(10, 65) NULL DEFAULT NULL,
          |  `p19` VARCHAR(255) NOT NULL,
          |  `p20` VARCHAR(255) NULL DEFAULT NULL,
          |  `p21` DATETIME NOT NULL,
          |  `p22` DATETIME NULL DEFAULT NULL,
          |  `p23` DATETIME NOT NULL,
          |  `p24` DATETIME NULL DEFAULT NULL,
          |  `p25` TIME NOT NULL,
          |  `p26` TIME NULL DEFAULT NULL,
          |  `p27` DATE NOT NULL,
          |  `p28` DATE NULL DEFAULT NULL,
          |  `p29` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          |  `p30` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
          |  `p31` TIMESTAMP NOT NULL,
          |  `p32` TIMESTAMP NULL DEFAULT NULL,
          |  `p33` TIMESTAMP NOT NULL,
          |  `p34` TIMESTAMP NULL DEFAULT NULL,
          |  `p35` YEAR NOT NULL,
          |  `p36` YEAR NULL DEFAULT NULL
          |);
          |""".stripMargin
    }

    "If the column name is duplicated, an IllegalArgumentException is raised." in {
      val table: Table[AllDataTypeTest] = Table[AllDataTypeTest]("all_datatype_test")(
        column("p1", BIT(1)),
        column("p1", BIT(64).DEFAULT(None)),
        column("p3", TINYINT(0)),
        column("p4", TINYINT(255).DEFAULT(None)),
        column("p5", SMALLINT(0)),
        column("p6", SMALLINT(255).DEFAULT(None)),
        column("p7", MEDIUMINT(0)),
        column("p8", MEDIUMINT(255).DEFAULT(None)),
        column("p9", BIGINT(0)),
        column("p10", BIGINT(255).DEFAULT(None)),
        column("p11", BIGINT(0)),
        column("p12", BIGINT(255).DEFAULT(None)),
        column("p13", FLOAT(0)),
        column("p14", FLOAT(24).DEFAULT(None)),
        column("p15", DOUBLE(24)),
        column("p16", DOUBLE(53).DEFAULT(None)),
        column("p17", DECIMAL(10, 0)),
        column("p18", DECIMAL(10, 65).DEFAULT(None)),
        column("p19", VARCHAR(255)),
        column("p20", VARCHAR(255).DEFAULT(None)),
        column("p21", DATETIME),
        column("p22", DATETIME.DEFAULT(None)),
        column("p23", DATETIME),
        column("p24", DATETIME.DEFAULT(None)),
        column("p25", TIME),
        column("p26", TIME.DEFAULT(None)),
        column("p27", DATE),
        column("p28", DATE.DEFAULT(None)),
        column("p29", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP(true)),
        column("p30", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP()),
        column("p31", TIMESTAMP),
        column("p32", TIMESTAMP.DEFAULT(None)),
        column("p33", TIMESTAMP),
        column("p34", TIMESTAMP.DEFAULT(None)),
        column("p35", YEAR),
        column("p36", YEAR.DEFAULT(None))
      )

      TableQueryBuilder(table) must throwAn[IllegalArgumentException]
    }

    "If more than one AUTO_INCREMENT is set for a table, an IllegalArgumentException is raised." in {

      case class Test(id: Long, subId: Long)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT, UNIQUE_KEY),
        column("sub_id", BIGINT(64), AUTO_INCREMENT, UNIQUE_KEY)
      )

      TableQueryBuilder(table) must throwAn[IllegalArgumentException]
    }

    "If more than one PRIMARY_KEY is set for a table, an IllegalArgumentException is raised." in {

      case class Test(id: Long, subId: Long)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY),
        column("sub_id", BIGINT(64), PRIMARY_KEY)
      )

      TableQueryBuilder(table) must throwAn[IllegalArgumentException]
    }

    "If neither Primary Key nor Unique Key is set in AUTO_INCREMENT, IllegalArgumentException is raised." in {

      case class Test(id: Long, subId: Long)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )

      TableQueryBuilder(table) must throwAn[IllegalArgumentException]
    }

    "Query string matches the specified value when PRIMARY KEY is set to Table." in {
      case class Test(id: Long, subId: Long)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      ).keySet(table => PRIMARY_KEY(table.id))

      TableQueryBuilder(table).createStatement ===
        """
          |CREATE TABLE `test` (
          |  `id` BIGINT(64) NOT NULL AUTO_INCREMENT,
          |  `sub_id` BIGINT(64) NOT NULL,
          |  PRIMARY KEY (`id`)
          |);
          |""".stripMargin
    }

    "If two or more PRIMARY KEYs are set in a Table, an IllegalArgumentException is raised." in {
      case class Test(id: Long, subId: Long)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )
        .keySet(table => PRIMARY_KEY(table.id))
        .keySet(table => PRIMARY_KEY(table.subId))

      TableQueryBuilder(table) must throwAn[IllegalArgumentException]
    }

    "Query string matches the specified value when INDEX KEY is set to Table." in {
      case class Test(id: Long, subId: Long)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )
        .keySet(table => PRIMARY_KEY(table.id))
        .keySet(table => INDEX_KEY(table.subId))

      TableQueryBuilder(table).createStatement ===
        """
          |CREATE TABLE `test` (
          |  `id` BIGINT(64) NOT NULL AUTO_INCREMENT,
          |  `sub_id` BIGINT(64) NOT NULL,
          |  PRIMARY KEY (`id`),
          |  INDEX (`sub_id`)
          |);
          |""".stripMargin
    }

    "Query string matches the specified value when FOREIGN KEY is set to Table." in {
      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long)

      val subTable: Table[SubTest] = Table[SubTest]("sub_test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY)
      )

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )
        .keySet(table => PRIMARY_KEY(table.id))
        .keySet(table => INDEX_KEY(table.subId))
        .keySet(table => CONSTRAINT("fk_id", FOREIGN_KEY(table.subId, REFERENCE(subTable, subTable.id))))

      TableQueryBuilder(table).createStatement ===
        """
          |CREATE TABLE `test` (
          |  `id` BIGINT(64) NOT NULL AUTO_INCREMENT,
          |  `sub_id` BIGINT(64) NOT NULL,
          |  PRIMARY KEY (`id`),
          |  INDEX (`sub_id`),
          |  CONSTRAINT `fk_id` FOREIGN KEY (`sub_id`) REFERENCES `sub_test` (`id`)
          |);
          |""".stripMargin
    }

    "If the table option is set to Table, the query string will match the specified value." in {
      case class Test(id: Long, name: String)

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
        column("name", VARCHAR(255))
      )
        .setOptions(
          Seq(
            TableOption.Engine("InnoDB"),
            TableOption.Character("utf8mb4"),
            TableOption.Collate("utf8mb4_unicode_ci"),
            TableOption.Comment("test")
          )
        )

      TableQueryBuilder(table).createStatement ===
        """
          |CREATE TABLE `test` (
          |  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
          |  `name` VARCHAR(255) NOT NULL
          |) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 DEFAULT COLLATE=utf8mb4_unicode_ci COMMENT='test';
          |""".stripMargin
    }

    "If a column that is not a PRIMARY KEY is set as a FOREIGN KEY, an IllegalArgumentException will be thrown." in {
      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long, test: Long)

      val subTable: Table[SubTest] = Table[SubTest]("sub_test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY),
        column("test", BIGINT(64))
      )

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )
        .keySet(table => PRIMARY_KEY(table.id))
        .keySet(table => INDEX_KEY(table.subId))
        .keySet(table => CONSTRAINT("fk_id", FOREIGN_KEY(table.subId, REFERENCE(subTable, subTable.test))))

      TableQueryBuilder(table) must throwAn[IllegalArgumentException]
    }
  }
