/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.model

import munit.CatsEffectSuite

import ldbc.codegen.formatter.Naming

class KeyTest extends CatsEffectSuite:

  val camelCaseFormatter: Naming = Naming.CAMEL
  val snakeCaseFormatter: Naming = Naming.SNAKE
  val tableName = "user_profiles"

  test("Index.toCode should generate correct code") {
    val index = Key.Index(
      Some("idx_user_email"),
      Some(Key.IndexType("BTREE")),
      List("email", "created_at"),
      Some(Key.IndexOption.empty.setComment(CommentSet("Email index")))
    )

    val expected = """INDEX_KEY(Some("idx_user_email"), Some(Index.Type.BTREE), Some(Index.IndexOption(None, None, None, Some(Comment("Email index")), None, None)), email *: createdAt)"""
    assertEquals(index.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("Primary.toCode should generate correct code without index type") {
    val primary = Key.Primary(
      None,
      None,
      List("id"),
      None
    )

    val expected = "PRIMARY_KEY(id)"
    assertEquals(primary.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("Primary.toCode should generate correct code with index type and options") {
    val primary = Key.Primary(
      Some(Key.Constraint(Some("pk_users"))),
      Some(Key.IndexType("BTREE")),
      List("id"),
      Some(Key.IndexOption.empty.setSize(Key.KeyBlockSize(8)))
    )

    val expected = """CONSTRAINT("pk_users", PRIMARY_KEY(Index.Type.BTREE, Index.IndexOption(Some(8), None, None, None, None, None), id))"""
    assertEquals(primary.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("Unique.toCode should generate correct code") {
    val unique = Key.Unique(
      None,
      Some("uq_email"),
      Some(Key.IndexType("BTREE")),
      List("email"),
      None
    )

    val expected = """UNIQUE_KEY(Some("uq_email"), Some(Index.Type.BTREE), None, email)"""
    assertEquals(unique.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("Unique.toCode with constraint should generate correct code") {
    val unique = Key.Unique(
      Some(Key.Constraint(Some("uq_constraint"))),
      Some("uq_email"),
      Some(Key.IndexType("BTREE")),
      List("email"),
      None
    )

    val expected = """CONSTRAINT("uq_constraint", UNIQUE_KEY(Some("uq_email"), Some(Index.Type.BTREE), None, email))"""
    assertEquals(unique.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("Foreign.toCode should generate correct code") {
    val foreign = Key.Foreign(
      None,
      Some("fk_user_role"),
      List("role_id"),
      Key.Reference("roles", List("id"), None)
    )

    val expected = """FOREIGN_KEY(Some("fk_user_role"), user_profiles.roleId, REFERENCE(roles.table, roles.table.id))"""
    assertEquals(foreign.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("Foreign.toCode with ON DELETE and ON UPDATE should generate correct code") {
    val foreign = Key.Foreign(
      Some(Key.Constraint(Some("fk_constraint"))),
      Some("fk_user_role"),
      List("role_id"),
      Key.Reference("roles", List("id"), Some(List(Key.OnDelete("CASCADE"), Key.OnUpdate("SET NULL"))))
    )

    val expected = """CONSTRAINT("fk_constraint", FOREIGN_KEY(Some("fk_user_role"), user_profiles.roleId, REFERENCE(roles.table, roles.table.id).onDelete(CASCADE).onUpdate(SET NULL)))"""
    assertEquals(foreign.toCode(tableName, camelCaseFormatter, camelCaseFormatter), expected)
  }

  test("IndexOption.toCode should generate code with all options") {
    val option = Key.IndexOption.empty
      .setSize(Key.KeyBlockSize(4))
      .setIndexType(Key.IndexType("HASH"))
      .setWithParser(Key.WithParser("ngram"))
      .setComment(CommentSet("Test index"))
      .setEngineAttribute(Key.EngineAttribute("test-engine"))
      .setSecondaryEngineAttribute(Key.SecondaryEngineAttribute("secondary"))

    val expected = "Index.IndexOption(Some(4), Some(Index.Type.HASH), Some(ngram), Some(Comment(\"Test index\")), Some(test-engine), Some(secondary))"
    assertEquals(option.toCode, expected)
  }

  test("Reference.toCode should handle different ON clause combinations") {
    val refNoOn = Key.Reference("roles", List("id"), None)
    assertEquals(
      refNoOn.toCode(camelCaseFormatter, camelCaseFormatter),
      "REFERENCE(roles.table, roles.table.id)"
    )

    val refDeleteOnly = Key.Reference("roles", List("id"), Some(List(Key.OnDelete("CASCADE"))))
    assertEquals(
      refDeleteOnly.toCode(camelCaseFormatter, camelCaseFormatter),
      "REFERENCE(roles.table, roles.table.id).onDelete(CASCADE)"
    )

    val refUpdateOnly = Key.Reference("roles", List("id"), Some(List(Key.OnUpdate("SET NULL"))))
    assertEquals(
      refUpdateOnly.toCode(camelCaseFormatter, camelCaseFormatter),
      "REFERENCE(roles.table, roles.table.id).onUpdate(SET NULL)"
    )
  }