/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import org.specs2.mutable.Specification

import ldbc.core.*
import ldbc.core.syntax.{ *, given }

object TableQueryBuilderTest extends Specification:

  case class User(
    id:        Long,
    name:      String,
    age:       Option[Int],
    updatedAt: java.time.LocalDateTime,
    createdAt: java.time.LocalDateTime
  )

  "TableQueryBuilder Test" should {

    "The Create statement of the Table generated using TableQueryBuilder matches the specified value." in {

      val table: Table[User] = Table[User]("user")(
        column("id", BIGINT(64), AUTO_INCREMENT, UNIQUE_KEY),
        column("name", VARCHAR(255)),
        column("age", INT(255).DEFAULT_NULL),
        column("updated_at", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP(true)),
        column("created_at", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP())
      )

      TableQueryBuilder(table).querySting ===
        """
          |CREATE TABLE `user` (
          |  `id` BIGINT(64) NOT NULL AUTO_INCREMENT UNIQUE KEY,
          |  `name` VARCHAR(255) NOT NULL,
          |  `age` INT(255) NULL DEFAULT NULL,
          |  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          |  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
          |);
          |""".stripMargin
    }

    "If the column name is duplicated, an IllegalArgumentException is raised." in {
      val table: Table[User] = Table[User]("user")(
        column("id", BIGINT(64), AUTO_INCREMENT, UNIQUE_KEY),
        column("id", VARCHAR(255)),
        column("age", INT(255).DEFAULT_NULL),
        column("updated_at", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP(true)),
        column("created_at", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP())
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

      TableQueryBuilder(table).querySting ===
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

      TableQueryBuilder(table).querySting ===
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

      TableQueryBuilder(table).querySting ===
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

    "IllegalArgumentException is raised if the type of the column set in FOREIGN KEY does not match." in {
      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long, test: String)

      val subTable: Table[SubTest] = Table[SubTest]("sub_test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY),
        column("test", VARCHAR(64))
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
