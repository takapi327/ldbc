/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReferenceTest extends AnyFlatSpec, Matchers:
  case class User(id: Long, name: String, age: Int)

  class UserTable extends Table[User]("user"):
    def id: Column[Long] = bigint("id").autoIncrement
    def name: Column[String] = varchar(255, "name")
    def age: Column[Int] = int("age")

    override def * = (id *: name *: age).to[User]

  val userTable = new UserTable

  it should "generate basic reference query string" in {
    val reference = Reference(userTable, userTable.id, None, None)
    reference.queryString should be("REFERENCES `user` (`id`)")
  }

  it should "generate reference with ON DELETE RESTRICT" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.RESTRICT)
    reference.queryString should be("REFERENCES `user` (`id`) ON DELETE RESTRICT")
  }

  it should "generate reference with ON DELETE CASCADE" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.CASCADE)
    reference.queryString should be("REFERENCES `user` (`id`) ON DELETE CASCADE")
  }

  it should "generate reference with ON DELETE SET NULL" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.SET_NULL)
    reference.queryString should be("REFERENCES `user` (`id`) ON DELETE SET NULL")
  }

  it should "generate reference with ON DELETE NO ACTION" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.NO_ACTION)
    reference.queryString should be("REFERENCES `user` (`id`) ON DELETE NO ACTION")
  }

  it should "generate reference with ON DELETE SET DEFAULT" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.SET_DEFAULT)
    reference.queryString should be("REFERENCES `user` (`id`) ON DELETE SET DEFAULT")
  }

  it should "generate reference with ON UPDATE RESTRICT" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.RESTRICT)
    reference.queryString should be("REFERENCES `user` (`id`) ON UPDATE RESTRICT")
  }

  it should "generate reference with ON UPDATE CASCADE" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.CASCADE)
    reference.queryString should be("REFERENCES `user` (`id`) ON UPDATE CASCADE")
  }

  it should "generate reference with ON UPDATE SET NULL" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.SET_NULL)
    reference.queryString should be("REFERENCES `user` (`id`) ON UPDATE SET NULL")
  }

  it should "generate reference with ON UPDATE NO ACTION" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.NO_ACTION)
    reference.queryString should be("REFERENCES `user` (`id`) ON UPDATE NO ACTION")
  }

  it should "generate reference with ON UPDATE SET DEFAULT" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onUpdate(Reference.ReferenceOption.SET_DEFAULT)
    reference.queryString should be("REFERENCES `user` (`id`) ON UPDATE SET DEFAULT")
  }

  it should "generate reference with both ON DELETE and ON UPDATE constraints" in {
    val reference = Reference(userTable, userTable.id, None, None)
      .onDelete(Reference.ReferenceOption.RESTRICT)
      .onUpdate(Reference.ReferenceOption.CASCADE)
    reference.queryString should be("REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE")
  }
