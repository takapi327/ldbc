/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core

import org.scalatest.flatspec.AnyFlatSpec

class TableTest extends AnyFlatSpec:

  it should "Successful generation of Table" in {
    assertCompiles("""
      import ldbc.core.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[User] = Table[User]("user")(
        column("id", BIGINT(64)),
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

  it should "" in {

    import cats.data.NonEmptyList

    import ldbc.core.attribute.*
    import ldbc.core.syntax.given

    case class User(
      id:        Long,
      name:      String,
      age:       Option[Int],
      updatedAt: java.time.LocalDateTime,
      createdAt: java.time.LocalDateTime
    )

    case class Country(id: Long)

    val countryTable: Table[Country] = Table[Country]("country")(
      column("id", BIGINT(64), AutoInc(), Key.Primary())
    )

    val table: Table[User] = Table[User]("user")(
      column("id", BIGINT(64), AutoInc(), Key.Unique()),
      column("name", VARCHAR(255)),
      column("age", INT(255).DEFAULT_NULL),
      column("updated_at", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP()),
      column("created_at", TIMESTAMP.DEFAULT_CURRENT_TIMESTAMP(true))
    ).keys(table =>
      Seq(
        PrimaryKey(None, NonEmptyList.of(table.id, table.age), None),
        IndexKey(Some("index_age"), None, NonEmptyList.one(table.age), None),
        Constraint("fk_id", ForeignKey(None, NonEmptyList.one(table.id), Reference(countryTable)(countryTable.id)))
      )
    )

    val query = CreateTableQueryGenerator.build(table).querySting
    println(CreateTableQueryGenerator.build(countryTable).querySting)
    println(query)
    true
  }
