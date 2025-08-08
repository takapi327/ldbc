/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core

import org.scalatest.flatspec.AnyFlatSpec

class TableTest extends AnyFlatSpec:

  it should "Successful generation of Table" in {
    assertCompiles("""
      import ldbc.core.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[User] = Table[User]("user")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("name", VARCHAR(255)),
        column("age", INT(255))
      )
    """.stripMargin)
  }

  it should "If columns are passed in an order that does not match the type information of the properties of the specified case class, a compile error will occur." in {
    assertDoesNotCompile("""
      import ldbc.core.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[User] = Table[User]("user")(
        column("name", VARCHAR(255)),
        column("id", BIGINT(64)),
        column("age", INT(255))
      )
    """.stripMargin)
  }

  it should "Setting AUTO_INCREMENT to a non-numeric type results in a compile error" in {
    assertDoesNotCompile("""
      import ldbc.core.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[User] = Table[User]("user")(
        column("id", BIGINT(64)),
        column("name", VARCHAR(255), AUTO_INCREMENT),
        column("age", INT(255))
      )
    """.stripMargin)
  }

  it should "Setting Collate to anything other than a string type results in a compile error." in {
    assertDoesNotCompile("""
      import ldbc.core.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[User] = Table[User]("user")(
        column("id", BIGINT(64), Collate.AsciiBin),
        column("name", VARCHAR(255)),
        column("age", INT(255))
      )
    """.stripMargin)
  }

  it should "Foreign key constraints can be set between the same type without any problem." in {
    assertCompiles("""
      import ldbc.core.*

      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long, test: String)

      val subTable: Table[SubTest] = Table[SubTest]("sub_test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY),
        column("test", VARCHAR(255))
      )

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )
        .keySet(table => PRIMARY_KEY(table.id))
        .keySet(table => INDEX_KEY(table.subId))
        .keySet(table => CONSTRAINT("fk_id", FOREIGN_KEY(table.subId, REFERENCE(subTable, subTable.id))))
    """.stripMargin)
  }

  it should "Setting foreign key constraints between different types results in a compile error." in {
    assertDoesNotCompile("""
      import ldbc.core.*

      case class Test(id: Long, subId: Long)
      case class SubTest(id: Long, test: String)

      val subTable: Table[SubTest] = Table[SubTest]("sub_test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY),
        column("test", VARCHAR(255))
      )

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64))
      )
        .keySet(table => PRIMARY_KEY(table.id))
        .keySet(table => INDEX_KEY(table.subId))
        .keySet(table => CONSTRAINT("fk_id", FOREIGN_KEY(table.subId, REFERENCE(subTable, subTable.test))))
    """.stripMargin)
  }

  it should "Compound foreign keys of the same type can be compiled without error." in {
    assertCompiles("""
      import ldbc.core.*

      case class Test(id: Long, subId: Long, subCategory: Short)
      case class SubTest(id: Long, test: String, category: Short)

      val subTable: Table[SubTest] = Table[SubTest]("sub_test")(
        column("id", BIGINT(64), AUTO_INCREMENT, PRIMARY_KEY),
        column("test", VARCHAR(255)),
        column("category", TINYINT)
      )

      val table: Table[Test] = Table[Test]("test")(
        column("id", BIGINT(64), AUTO_INCREMENT),
        column("sub_id", BIGINT(64)),
        column("sub_category", TINYINT)
      )
        .keySets(table => Seq(
          PRIMARY_KEY(table.id),
          INDEX_KEY(table.subId),
          CONSTRAINT("fk_id", FOREIGN_KEY((table.subId, table.subCategory), REFERENCE(subTable, (subTable.id, subTable.category))))
        ))
    """.stripMargin)
  }

  it should "The type of the column accessed by selectDynamic matches the type of the specified column." in {
    case class User(id: Long, name: String, age: Int)

    val table: Table[User] = Table[User]("user")(
      Column("id", BIGINT(64)),
      Column("name", VARCHAR(255)),
      Column("age", INT(255))
    )

    table.id === Column("id", BIGINT(64))
  }

  it should "The column accessed by selectDynamic does not match the type of the specified column." in {
    case class User(id: Long, name: String, age: Int)

    val table: Table[User] = Table[User]("user")(
      column("id", BIGINT(64)),
      column("name", VARCHAR(255)),
      column("age", INT(255))
    )

    table.id !== Column("name", VARCHAR(255))
  }
