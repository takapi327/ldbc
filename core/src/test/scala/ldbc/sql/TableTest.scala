/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.sql

import org.scalatest.flatspec.AnyFlatSpec

import cats.Id

import ldbc.sql.{ Column, Table }
import ldbc.sql.DataTypes.*

class TableTest extends AnyFlatSpec:

  it should "Successful generation of Table" in {
    assertCompiles("""
      import cats.Id

      import ldbc.sql.{ Column, Table }
      import ldbc.sql.DataTypes.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[Id, User] = Table[Id, User]("user")(
        Column("id", BIGINT(64)),
        Column("name", VARCHAR(255)),
        Column("age", INT(255))
      )
    """.stripMargin)
  }

  it should "If columns are passed in an order that does not match the type information of the properties of the specified case class, a compile error will occur." in {
    assertDoesNotCompile("""
      import cats.Id

      import ldbc.sql.{ Column, Table }
      import ldbc.sql.DataTypes.*

      case class User(id: Long, name: String, age: Int)

      val table: Table[Id, User] = Table[Id, User]("user")(
        Column("name", VARCHAR(255)),
        Column("id", BIGINT(64)),
        Column("age", INT(255))
      )
    """.stripMargin)
  }

  it should "The type of the column accessed by selectDynamic matches the type of the specified column." in {
    case class User(id: Long, name: String, age: Int)

    val table: Table[Id, User] = Table[Id, User]("user")(
      Column("id", BIGINT(64)),
      Column("name", VARCHAR(255)),
      Column("age", INT(255))
    )

    table.id === Column("id", BIGINT(64))
  }

  it should "The column accessed by selectDynamic does not match the type of the specified column." in {
    case class User(id: Long, name: String, age: Int)

    val table: Table[Id, User] = Table[Id, User]("user")(
      Column("id", BIGINT(64)),
      Column("name", VARCHAR(255)),
      Column("age", INT(255))
    )

    table.id !== Column("name", VARCHAR(255))
  }
