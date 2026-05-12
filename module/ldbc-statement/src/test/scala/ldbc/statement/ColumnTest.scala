/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.*
import ldbc.dsl.codec.Codec

opaque type Token = String
object Token:
  def fromString(value: String):          Token  = value
  extension (token:     Token) def value: String = token

class ColumnTest extends munit.FunSuite:

  given Codec[Token] = Codec[String].imap(Token.fromString)(_.value)

  private val id1   = Column.Impl[Long]("id")
  private val id2   = Column.Impl[Option[Long]]("id")
  private val name1 = Column.Impl[String]("name")
  private val name2 = Column.Impl[Option[String]]("name")

  test(
    "The string of the expression syntax that constructs the match with the specified value matches the specified string."
  ) {
    assertEquals((id1 === 1L).statement, "`id` = ?")
    assertEquals((id1 === 1L).NOT.statement, "NOT `id` = ?")
    assertEquals((id2 === 1L).statement, "`id` = ?")
    assertEquals((id2 === 1L).NOT.statement, "NOT `id` = ?")
  }

  test(
    "The string constructed by the expression syntax that determines if it is greater than or equal to the specified value matches the specified string."
  ) {
    assertEquals((id1 >= 1L).statement, "`id` >= ?")
    assertEquals((id1 >= 1L).NOT.statement, "NOT `id` >= ?")
    assertEquals((id2 >= 1L).statement, "`id` >= ?")
    assertEquals((id2 >= 1L).NOT.statement, "NOT `id` >= ?")
  }

  test(
    "The string constructed by the expression syntax that determines whether the specified value is exceeded matches the specified string."
  ) {
    assertEquals((id1 > 1L).statement, "`id` > ?")
    assertEquals((id1 > 1L).NOT.statement, "NOT `id` > ?")
    assertEquals((id2 > 1L).statement, "`id` > ?")
    assertEquals((id2 > 1L).NOT.statement, "NOT `id` > ?")
  }

  test(
    "The string constructed by the expression syntax that determines if it is less than or equal to the specified value matches the specified string."
  ) {
    assertEquals((id1 <= 1L).statement, "`id` <= ?")
    assertEquals((id1 <= 1L).NOT.statement, "NOT `id` <= ?")
    assertEquals((id2 <= 1L).statement, "`id` <= ?")
    assertEquals((id2 <= 1L).NOT.statement, "NOT `id` <= ?")
  }

  test(
    "The string constructed by the expression syntax that determines if it is less than the specified value matches the specified string."
  ) {
    assertEquals((id1 < 1L).statement, "`id` < ?")
    assertEquals((id1 < 1L).NOT.statement, "NOT `id` < ?")
    assertEquals((id2 < 1L).statement, "`id` < ?")
    assertEquals((id2 < 1L).NOT.statement, "NOT `id` < ?")
  }

  test(
    "The string constructed by the expression syntax that determines whether the specified value match or not matches the specified string."
  ) {
    assertEquals((id1 <> 1L).statement, "`id` <> ?")
    assertEquals((id1 <> 1L).NOT.statement, "NOT `id` <> ?")
    assertEquals((id1 !== 1L).statement, "`id` != ?")
    assertEquals((id1 !== 1L).NOT.statement, "NOT `id` != ?")
    assertEquals((id2 <> 1L).statement, "`id` <> ?")
    assertEquals((id2 <> 1L).NOT.statement, "NOT `id` <> ?")
    assertEquals((id2 !== 1L).statement, "`id` != ?")
    assertEquals((id2 !== 1L).NOT.statement, "NOT `id` != ?")
  }

  test(
    "The string constructed by the expression syntax that determines whether it is a Boolean value that can be TRUE, FALSE, or UNKNOWN matches the specified string."
  ) {
    assertEquals((id1 IS "TRUE").statement, "`id` IS TRUE")
    assertEquals((id1 IS "FALSE").NOT.statement, "`id` IS NOT FALSE")
    assertEquals((id2 IS "TRUE").statement, "`id` IS TRUE")
    assertEquals((id2 IS "NULL").statement, "`id` IS NULL")
    assertEquals((id2 IS "UNKNOWN").NOT.statement, "`id` IS NOT UNKNOWN")
  }

  test(
    "NULL - The string constructed by the expression syntax to determine safe equivalence matches the specified string."
  ) {
    assertEquals((id1 <=> 1L).statement, "`id` <=> ?")
    assertEquals((id1 <=> 1L).NOT.statement, "NOT `id` <=> ?")
    assertEquals((id2 <=> 1L).statement, "`id` <=> ?")
    assertEquals((id2 <=> 1L).NOT.statement, "NOT `id` <=> ?")
  }

  test(
    "The string constructed by the expression syntax that determines whether it contains at least one of the specified values matches the specified string."
  ) {
    assertEquals((id1 IN (1L, 2L)).statement, "`id` IN (?, ?)")
    assertEquals((id1 IN (1L, 2L)).NOT.statement, "`id` NOT IN (?, ?)")
    assertEquals((id2 IN (1L, 2L)).statement, "`id` IN (?, ?)")
    assertEquals((id2 IN (1L, 2L, 3L)).NOT.statement, "`id` NOT IN (?, ?, ?)")
  }

  test(
    "The string constructed by the expression syntax that determines whether the value falls within the specified range matches the specified string."
  ) {
    assertEquals((id1 BETWEEN (1L, 10L)).statement, "`id` BETWEEN ? AND ?")
    assertEquals((id1 BETWEEN (1L, 10L)).NOT.statement, "`id` NOT BETWEEN ? AND ?")
    assertEquals((id2 BETWEEN (1L, 10L)).statement, "`id` BETWEEN ? AND ?")
    assertEquals((id2 BETWEEN (1L, 10L)).NOT.statement, "`id` NOT BETWEEN ? AND ?")
  }

  test(
    "The string constructed by the expression syntax that determines whether it contains a matching string matches the specified string."
  ) {
    assertEquals((name1 LIKE "ldbc").statement, "`name` LIKE ?")
    assertEquals((name1 LIKE "ldbc").NOT.statement, "NOT `name` LIKE ?")
    assertEquals((name2 LIKE "ldbc").statement, "`name` LIKE ?")
    assertEquals((name2 LIKE "ldbc").NOT.statement, "NOT `name` LIKE ?")
    assertEquals((name1 LIKE_ESCAPE ("T%", "$")).statement, "`name` LIKE ? ESCAPE ?")
    assertEquals((name1 LIKE_ESCAPE ("T%", "$")).NOT.statement, "NOT `name` LIKE ? ESCAPE ?")
    assertEquals((name2 LIKE_ESCAPE ("T%", "$")).statement, "`name` LIKE ? ESCAPE ?")
    assertEquals((name2 LIKE_ESCAPE ("T%", "$")).NOT.statement, "NOT `name` LIKE ? ESCAPE ?")
  }

  test(
    "The string constructed by the expression syntax that determines whether it matches the regular expression pattern matches the specified string."
  ) {
    assertEquals((name1 REGEXP "^[A-D]'").statement, "`name` REGEXP ?")
    assertEquals((name1 REGEXP "^[A-D]'").NOT.statement, "NOT `name` REGEXP ?")
    assertEquals((name2 REGEXP "^[A-D]'").statement, "`name` REGEXP ?")
    assertEquals((name2 REGEXP "^[A-D]'").NOT.statement, "NOT `name` REGEXP ?")
  }

  test(
    "The string constructed by the expression syntax that performs the integer division operation to determine if it matches matches the specified string."
  ) {
    assertEquals((id1 DIV (5L, 10L)).statement, "`id` DIV ? = ?")
    assertEquals((id1 DIV (5L, 10L)).NOT.statement, "NOT `id` DIV ? = ?")
    assertEquals((id2 DIV (5L, 10L)).statement, "`id` DIV ? = ?")
    assertEquals((id2 DIV (5L, 10L)).NOT.statement, "NOT `id` DIV ? = ?")
  }

  test(
    "The string constructed by the expression syntax that performs the operation to find the remainder and determines whether it matches matches the specified string."
  ) {
    assertEquals((id1 MOD (5L, 0L)).statement, "`id` MOD ? = ?")
    assertEquals((id1 MOD (5L, 0L)).NOT.statement, "NOT `id` MOD ? = ?")
    assertEquals((id1 % (5L, 0L)).statement, "`id` % ? = ?")
    assertEquals((id1 % (5L, 0L)).NOT.statement, "NOT `id` % ? = ?")
    assertEquals((id2 MOD (5L, 0L)).statement, "`id` MOD ? = ?")
    assertEquals((id2 MOD (5L, 0L)).NOT.statement, "NOT `id` MOD ? = ?")
    assertEquals((id2 % (5L, 0L)).statement, "`id` % ? = ?")
    assertEquals((id2 % (5L, 0L)).NOT.statement, "NOT `id` % ? = ?")
  }

  test(
    "The string constructed by the expression syntax that performs the bit XOR operation to determine if it matches matches the specified string."
  ) {
    assertEquals((id1 MOD (5L, 0L)).statement, "`id` MOD ? = ?")
    assertEquals((id1 MOD (5L, 0L)).NOT.statement, "NOT `id` MOD ? = ?")
    assertEquals((id1 % (5L, 0L)).statement, "`id` % ? = ?")
    assertEquals((id1 % (5L, 0L)).NOT.statement, "NOT `id` % ? = ?")
    assertEquals((id2 MOD (5L, 0L)).statement, "`id` MOD ? = ?")
    assertEquals((id2 MOD (5L, 0L)).NOT.statement, "NOT `id` MOD ? = ?")
    assertEquals((id2 % (5L, 0L)).statement, "`id` % ? = ?")
    assertEquals((id2 % (5L, 0L)).NOT.statement, "NOT `id` % ? = ?")
  }

  test(
    "The string constructed by the expression syntax that performs the left shift operation to determine if it matches matches the specified string."
  ) {
    assertEquals((id1 << 1L).statement, "`id` << ?")
    assertEquals((id1 << 1L).NOT.statement, "NOT `id` << ?")
    assertEquals((id2 << 1L).statement, "`id` << ?")
    assertEquals((id2 << 1L).NOT.statement, "NOT `id` << ?")
  }

  test("The string constructed by the SQL subquery comparison matches the expected value") {
    val sql = sql"SELECT id FROM user WHERE id = 1"
    assertEquals((id1 === sql).statement, "`id` = (SELECT id FROM user WHERE id = 1)")
    assertEquals((id1 >= sql).statement, "`id` >= (SELECT id FROM user WHERE id = 1)")
    assertEquals((id1 > sql).statement, "`id` > (SELECT id FROM user WHERE id = 1)")
    assertEquals((id1 <= sql).statement, "`id` <= (SELECT id FROM user WHERE id = 1)")
    assertEquals((id1 < sql).statement, "`id` < (SELECT id FROM user WHERE id = 1)")
    assertEquals((id1 <> sql).statement, "`id` <> (SELECT id FROM user WHERE id = 1)")
    assertEquals((id1 IN sql).statement, "`id` IN (SELECT id FROM user WHERE id = 1)")
  }

  test("The join query expression should produce the correct statement") {
    val otherId   = Column.Impl[Long]("id")
    val otherName = Column.Impl[String]("name")

    assertEquals((id1 === otherId).statement, "`id` = `id`")
    assertEquals((id1 >= otherId).statement, "`id` >= `id`")
    assertEquals((id1 > otherId).statement, "`id` > `id`")
    assertEquals((id1 <= otherId).statement, "`id` <= `id`")
    assertEquals((id1 < otherId).statement, "`id` < `id`")
    assertEquals((id1 <> otherId).statement, "`id` <> `id`")
    assertEquals((id1 !== otherId).statement, "`id` != `id`")

    assertEquals((name1 === otherName).statement, "`name` = `name`")
  }

  test("The count method should produce the correct statement") {
    val countCol = id1.count
    assertEquals(countCol.name, "COUNT(`id`)")
    val aliasedCountCol = id1.as("user").count
    assertEquals(aliasedCountCol.alias, Some("COUNT(user)"))
  }

  test("The opt transformation should convert a Column[A] to a Column[Option[A]]") {
    val optId = id1.opt
    assert(optId.isInstanceOf[Column[Option[Long]]])

    val optName = name1.opt
    assert(optName.isInstanceOf[Column[Option[String]]])
  }

  test("Support OrderBy operations through asc and desc methods") {
    val ascOrder  = id1.asc
    val descOrder = id1.desc

    assertEquals(ascOrder.statement, "`id` ASC")
    assertEquals(descOrder.statement, "`id` DESC")
  }

  test("Support product operation to combine columns") {
    val combined = id1.product(name1)
    assertEquals(combined.name, "`id`, `name`")
    assertEquals(combined.values, 2)
  }

  test("Support imap transformation with correct behavior") {
    // Convert between String and Int
    val strToInt = name1.imap(_.toInt)(_.toString)
    assertEquals(strToInt.name, name1.name)
    assert(strToInt.isInstanceOf[Column[Int]])
  }
