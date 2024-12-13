/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec

class TableTest extends AnyFlatSpec:

  it should "Successful generation of Table" in {
    assertCompiles("""
      import ldbc.schema.*

      case class User(id: Long, name: String, age: Int)

      class UserTable extends Table[User]("user"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def name: Column[String] = column[String]("name", VARCHAR(255))
        def age: Column[Int] = column[Int]("age", INT)

        override def * = (id *: name *: age).to[User]
    """.stripMargin)
  }

  it should "Setting AUTO_INCREMENT to a non-numeric type results in a compile error" in {
    assertDoesNotCompile("""
      import ldbc.schema.*

      case class User(id: Long, name: String, age: Int)

      class UserTable extends Table[User]("user"):
        def id: Column[Long] = column[Long]("id", BIGINT)
        def name: Column[String] = column[String]("name", VARCHAR(255), AUTO_INCREMENT)
        def age: Column[Int] = column[Int]("age", INT)

        override def * = (id *: name *: age).to[User]
    """.stripMargin)
  }

  it should "Setting Collate to anything other than a string type results in a compile error." in {
    assertDoesNotCompile("""
      import ldbc.schema.*

      case class User(id: Long, name: String, age: Int)

      class UserTable extends Table[User]("user"):
        def id: Column[Long] = column[Long]("id", BIGINT, Collate.AsciiBin)
        def name: Column[String] = column[String]("name", VARCHAR(255))
        def age: Column[Int] = column[Int]("age", INT)

        override def * = (id *: name *: age).to[User]
    """.stripMargin)
  }

  it should "Foreign key constraints can be set between the same type without any problem." in {
    assertCompiles("""
      import ldbc.schema.*

      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long, test: String)

      class SubTestTable extends Table[SubTest]("sub_test"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def test: Column[String] = column[String]("test", VARCHAR(255))

        override def * = (id *: test).to[SubTest]

      val subTable = SubTestTable()

      class TestTable extends Table[Test]("test"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def subId: Column[Long] = column[Long]("sub_id", BIGINT)

        override def * = (id *: subId).to[Test]

        override def keys: List[Key] = List(
          PRIMARY_KEY(id),
          INDEX_KEY(subId),
          CONSTRAINT("fk_id", FOREIGN_KEY(subId, REFERENCE(subTable, subTable.id)))
        )
    """.stripMargin)
  }

  it should "Setting foreign key constraints between different types results in a compile error." in {
    assertDoesNotCompile("""
      import ldbc.schema.*

      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long, test: String)

      class SubTestTable extends Table[SubTest]("sub_test"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def test: Column[String] = column[String]("test", VARCHAR(255))

        override def * = (id *: test).to[SubTest]

      val subTable = SubTestTable()

      class TestTable extends Table[Test]("test"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def subId: Column[Long] = column[Long]("sub_id", BIGINT)

        override def * = (id *: subId).to[Test]

        override def keys: List[Key] = List(
          PRIMARY_KEY(id),
          INDEX_KEY(subId),
          CONSTRAINT("fk_id", FOREIGN_KEY(subId, REFERENCE(subTable, subTable.test)))
        )
    """.stripMargin)
  }

  it should "Compound foreign keys of the same type can be compiled without error." in {
    assertCompiles("""
      import ldbc.schema.*

      case class Test(id: Long, subId: Long, subCategory: Short)
      case class SubTest(id: Long, test: String, category: Short)

      class SubTestTable extends Table[SubTest]("sub_test"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def test: Column[String] = column[String]("test", VARCHAR(255))
        def category: Column[Short] = column[Short]("category", TINYINT)

        override def * = (id *: test *: category).to[SubTest]

      val subTable = SubTestTable()

      class TestTable extends Table[Test]("test"):
        def id: Column[Long] = column[Long]("id", BIGINT, AUTO_INCREMENT)
        def subId: Column[Long] = column[Long]("sub_id", BIGINT)
        def subCategory: Column[Short] = column[Short]("sub_category", TINYINT)

        override def * = (id *: subId *: subCategory).to[Test]

        override def keys: List[Key] = List(
          PRIMARY_KEY(id),
          INDEX_KEY(subId),
          CONSTRAINT("fk_id", FOREIGN_KEY(subId *: subCategory, REFERENCE(subTable, subTable.id *: subTable.category)))
        )
    """.stripMargin)
  }
