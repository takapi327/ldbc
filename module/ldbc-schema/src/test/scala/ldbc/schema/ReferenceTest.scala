/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

class ReferenceTest extends munit.FunSuite:
  case class User(id: Long, name: String, age: Int)

  class UserTable extends Table[User]("user"):
    def id:   Column[Long]   = bigint("id").autoIncrement
    def name: Column[String] = varchar(255, "name")
    def age:  Column[Int]    = int("age")

    override def * = (id *: name *: age).to[User]

  val userTable = new UserTable

  test("generate basic reference query string") {
    val reference = Reference(userTable, userTable.id, None, None)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`)")
  }

  test("generate reference with ON DELETE RESTRICT") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.RESTRICT)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON DELETE RESTRICT")
  }

  test("generate reference with ON DELETE CASCADE") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.CASCADE)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON DELETE CASCADE")
  }

  test("generate reference with ON DELETE SET NULL") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.SET_NULL)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON DELETE SET NULL")
  }

  test("generate reference with ON DELETE NO ACTION") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.NO_ACTION)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON DELETE NO ACTION")
  }

  test("generate reference with ON DELETE SET DEFAULT") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.SET_DEFAULT)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON DELETE SET DEFAULT")
  }

  test("generate reference with ON UPDATE RESTRICT") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.RESTRICT)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON UPDATE RESTRICT")
  }

  test("generate reference with ON UPDATE CASCADE") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.CASCADE)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON UPDATE CASCADE")
  }

  test("generate reference with ON UPDATE SET NULL") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.SET_NULL)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON UPDATE SET NULL")
  }

  test("generate reference with ON UPDATE NO ACTION") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.NO_ACTION)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON UPDATE NO ACTION")
  }

  test("generate reference with ON UPDATE SET DEFAULT") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.SET_DEFAULT)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON UPDATE SET DEFAULT")
  }

  test("generate reference with both ON DELETE and ON UPDATE constraints") {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.RESTRICT)
      .onUpdate(Reference.ReferenceOption.CASCADE)
    assertEquals(reference.queryString, "REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE")
  }
