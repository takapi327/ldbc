/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.codec.Codec

opaque type Token = String
object Token:
  def fromString(value: String):          Token  = value
  extension (token:     Token) def value: String = token

class ColumnTest extends AnyFlatSpec:

  given Codec[Token] = Codec[String].imap(Token.fromString)(_.value)

  private val id1    = Column.Impl[Long]("id")
  private val id2    = Column.Impl[Option[Long]]("id")
  private val name1  = Column.Impl[String]("name")
  private val name2  = Column.Impl[Option[String]]("name")
  private val token1 = Column.Impl[Token]("token")
  private val token2 = Column.Impl[Option[Token]]("token")

  it should "The string of the expression syntax that constructs the match with the specified value matches the specified string." in {
    assert((id1 === 1L).statement === "`id` = ?")
    assert((id1 === 1L).NOT.statement === "NOT `id` = ?")
    assert((id2 === 1L).statement === "`id` = ?")
    assert((id2 === 1L).NOT.statement === "NOT `id` = ?")
  }

  it should "The string constructed by the expression syntax that determines if it is greater than or equal to the specified value matches the specified string." in {
    assert((id1 >= 1L).statement === "`id` >= ?")
    assert((id1 >= 1L).NOT.statement === "NOT `id` >= ?")
    assert((id2 >= 1L).statement === "`id` >= ?")
    assert((id2 >= 1L).NOT.statement === "NOT `id` >= ?")
  }

  it should "The string constructed by the expression syntax that determines whether the specified value is exceeded matches the specified string." in {
    assert((id1 > 1L).statement === "`id` > ?")
    assert((id1 > 1L).NOT.statement === "NOT `id` > ?")
    assert((id2 > 1L).statement === "`id` > ?")
    assert((id2 > 1L).NOT.statement === "NOT `id` > ?")
  }

  it should "The string constructed by the expression syntax that determines if it is less than or equal to the specified value matches the specified string." in {
    assert((id1 <= 1L).statement === "`id` <= ?")
    assert((id1 <= 1L).NOT.statement === "NOT `id` <= ?")
    assert((id2 <= 1L).statement === "`id` <= ?")
    assert((id2 <= 1L).NOT.statement === "NOT `id` <= ?")
  }

  it should "The string constructed by the expression syntax that determines if it is less than the specified value matches the specified string." in {
    assert((id1 < 1L).statement === "`id` < ?")
    assert((id1 < 1L).NOT.statement === "NOT `id` < ?")
    assert((id2 < 1L).statement === "`id` < ?")
    assert((id2 < 1L).NOT.statement === "NOT `id` < ?")
  }

  it should "The string constructed by the expression syntax that determines whether the specified value match or not matches the specified string." in {
    assert((id1 <> 1L).statement === "`id` <> ?")
    assert((id1 <> 1L).NOT.statement === "NOT `id` <> ?")
    assert((id1 !== 1L).statement === "`id` != ?")
    assert((id1 !== 1L).NOT.statement === "NOT `id` != ?")
    assert((id2 <> 1L).statement === "`id` <> ?")
    assert((id2 <> 1L).NOT.statement === "NOT `id` <> ?")
    assert((id2 !== 1L).statement === "`id` != ?")
    assert((id2 !== 1L).NOT.statement === "NOT `id` != ?")
  }

  it should "The string constructed by the expression syntax that determines whether it is a Boolean value that can be TRUE, FALSE, or UNKNOWN matches the specified string." in {
    assert((id1 IS "TRUE").statement === "`id` IS TRUE")
    assert((id1 IS "FALSE").NOT.statement === "`id` IS NOT FALSE")
    assert((id2 IS "TRUE").statement === "`id` IS TRUE")
    assert((id2 IS "NULL").statement === "`id` IS NULL")
    assert((id2 IS "UNKNOWN").NOT.statement === "`id` IS NOT UNKNOWN")
  }

  it should "NULL - The string constructed by the expression syntax to determine safe equivalence matches the specified string." in {
    assert((id1 <=> 1L).statement === "`id` <=> ?")
    assert((id1 <=> 1L).NOT.statement === "NOT `id` <=> ?")
    assert((id2 <=> 1L).statement === "`id` <=> ?")
    assert((id2 <=> 1L).NOT.statement === "NOT `id` <=> ?")
  }

  it should "The string constructed by the expression syntax that determines whether it contains at least one of the specified values matches the specified string." in {
    assert((id1 IN (1L, 2L)).statement === "`id` IN (?, ?)")
    assert((id1 IN (1L, 2L)).NOT.statement === "`id` NOT IN (?, ?)")
    assert((id2 IN (1L, 2L)).statement === "`id` IN (?, ?)")
    assert((id2 IN (1L, 2L, 3L)).NOT.statement === "`id` NOT IN (?, ?, ?)")
  }

  it should "The string constructed by the expression syntax that determines whether the value falls within the specified range matches the specified string." in {
    assert((id1 BETWEEN (1L, 10L)).statement === "`id` BETWEEN ? AND ?")
    assert((id1 BETWEEN (1L, 10L)).NOT.statement === "`id` NOT BETWEEN ? AND ?")
    assert((id2 BETWEEN (1L, 10L)).statement === "`id` BETWEEN ? AND ?")
    assert((id2 BETWEEN (1L, 10L)).NOT.statement === "`id` NOT BETWEEN ? AND ?")
  }

  it should "The string constructed by the expression syntax that determines whether it contains a matching string matches the specified string." in {
    assert((name1 LIKE "ldbc").statement === "`name` LIKE ?")
    assert((name1 LIKE "ldbc").NOT.statement === "NOT `name` LIKE ?")
    assert((name2 LIKE "ldbc").statement === "`name` LIKE ?")
    assert((name2 LIKE "ldbc").NOT.statement === "NOT `name` LIKE ?")
    assert((name1 LIKE_ESCAPE ("T%", "$")).statement === "`name` LIKE ? ESCAPE ?")
    assert((name1 LIKE_ESCAPE ("T%", "$")).NOT.statement === "NOT `name` LIKE ? ESCAPE ?")
    assert((name2 LIKE_ESCAPE ("T%", "$")).statement === "`name` LIKE ? ESCAPE ?")
    assert((name2 LIKE_ESCAPE ("T%", "$")).NOT.statement === "NOT `name` LIKE ? ESCAPE ?")
  }

  it should "The string constructed by the expression syntax that determines whether it matches the regular expression pattern matches the specified string." in {
    assert((name1 REGEXP "^[A-D]'").statement === "`name` REGEXP ?")
    assert((name1 REGEXP "^[A-D]'").NOT.statement === "NOT `name` REGEXP ?")
    assert((name2 REGEXP "^[A-D]'").statement === "`name` REGEXP ?")
    assert((name2 REGEXP "^[A-D]'").NOT.statement === "NOT `name` REGEXP ?")
  }

  it should "The string constructed by the expression syntax that performs the integer division operation to determine if it matches matches the specified string." in {
    assert((id1 DIV (5L, 10L)).statement === "`id` DIV ? = ?")
    assert((id1 DIV (5L, 10L)).NOT.statement === "NOT `id` DIV ? = ?")
    assert((id2 DIV (5L, 10L)).statement === "`id` DIV ? = ?")
    assert((id2 DIV (5L, 10L)).NOT.statement === "NOT `id` DIV ? = ?")
  }

  it should "The string constructed by the expression syntax that performs the operation to find the remainder and determines whether it matches matches the specified string." in {
    assert((id1 MOD (5L, 0L)).statement === "`id` MOD ? = ?")
    assert((id1 MOD (5L, 0L)).NOT.statement === "NOT `id` MOD ? = ?")
    assert((id1 % (5L, 0L)).statement === "`id` % ? = ?")
    assert((id1 % (5L, 0L)).NOT.statement === "NOT `id` % ? = ?")
    assert((id2 MOD (5L, 0L)).statement === "`id` MOD ? = ?")
    assert((id2 MOD (5L, 0L)).NOT.statement === "NOT `id` MOD ? = ?")
    assert((id2 % (5L, 0L)).statement === "`id` % ? = ?")
    assert((id2 % (5L, 0L)).NOT.statement === "NOT `id` % ? = ?")
  }

  it should "The string constructed by the expression syntax that performs the bit XOR operation to determine if it matches matches the specified string." in {
    assert((id1 MOD (5L, 0L)).statement === "`id` MOD ? = ?")
    assert((id1 MOD (5L, 0L)).NOT.statement === "NOT `id` MOD ? = ?")
    assert((id1 % (5L, 0L)).statement === "`id` % ? = ?")
    assert((id1 % (5L, 0L)).NOT.statement === "NOT `id` % ? = ?")
    assert((id2 MOD (5L, 0L)).statement === "`id` MOD ? = ?")
    assert((id2 MOD (5L, 0L)).NOT.statement === "NOT `id` MOD ? = ?")
    assert((id2 % (5L, 0L)).statement === "`id` % ? = ?")
    assert((id2 % (5L, 0L)).NOT.statement === "NOT `id` % ? = ?")
  }

  it should "The string constructed by the expression syntax that performs the left shift operation to determine if it matches matches the specified string." in {
    assert((id1 ^ 1L).statement === "`id` ^ ?")
    assert((id1 ^ 1L).NOT.statement === "NOT `id` ^ ?")
    assert((id2 ^ 1L).statement === "`id` ^ ?")
    assert((id2 ^ 1L).NOT.statement === "NOT `id` ^ ?")
  }

  it should "The string constructed by the expression syntax that performs the right shift operation to determine if it matches matches the specified string." in {
    assert((id1 >> 1L).statement === "`id` >> ?")
    assert((id1 >> 1L).NOT.statement === "NOT `id` >> ?")
    assert((id2 >> 1L).statement === "`id` >> ?")
    assert((id2 >> 1L).NOT.statement === "NOT `id` >> ?")
  }

  it should "The string constructed by the expression syntax that performs addition operations to determine if they match matches the specified string." in {
    assert(((id1 ++ id1) < 1L).statement === "`id` + `id` < ?")
    assert(((id1 ++ id1) < 1L).NOT.statement === "NOT `id` + `id` < ?")
    assert(((id2 ++ id2) < 1L).statement === "`id` + `id` < ?")
    assert(((id2 ++ id2) < 1L).NOT.statement === "NOT `id` + `id` < ?")
  }

  it should "The string constructed by the expression syntax that performs subtraction operations to determine if they match matches the specified string." in {
    assert(((id1 -- id1) < 1L).statement === "`id` - `id` < ?")
    assert(((id1 -- id1) < 1L).NOT.statement === "NOT `id` - `id` < ?")
    assert(((id2 -- id2) < 1L).statement === "`id` - `id` < ?")
    assert(((id2 -- id2) < 1L).NOT.statement === "NOT `id` - `id` < ?")
  }

  it should "The string constructed by the expression syntax that performs the multiplication operation to determine if it matches matches the specified string." in {
    assert(((id1 * id1) < 1L).statement === "`id` * `id` < ?")
    assert(((id1 * id1) < 1L).NOT.statement === "NOT `id` * `id` < ?")
    assert(((id2 * id2) < 1L).statement === "`id` * `id` < ?")
    assert(((id2 * id2) < 1L).NOT.statement === "NOT `id` * `id` < ?")
  }

  it should "The string constructed by the expression syntax that performs the division operation and determines whether it matches matches the specified string." in {
    assert(((id1 / id1) < 1L).statement === "`id` / `id` < ?")
    assert(((id1 / id1) < 1L).NOT.statement === "NOT `id` / `id` < ?")
    assert(((id2 / id2) < 1L).statement === "`id` / `id` < ?")
    assert(((id2 / id2) < 1L).NOT.statement === "NOT `id` / `id` < ?")
  }

  it should "The string constructed by the expression syntax, which performs bit inversion to determine if it matches, matches the specified string." in {
    assert((id1 ~ 1L).statement === "~`id` = ?")
    assert((id1 ~ 1L).NOT.statement === "NOT ~`id` = ?")
    assert((id2 ~ 1L).statement === "~`id` = ?")
    assert((id2 ~ 1L).NOT.statement === "NOT ~`id` = ?")
  }

  it should "The query string of the combined expression matches the specified string." in {
    val age = Column.Impl[Option[Int]]("age")
    assert((id1 === 1L && name1 === "name" || age > 25).statement === "(`id` = ? AND `name` = ? OR `age` > ?)")
  }

  it should "Comparison operators can also be used when using Opaque Type Alias." in {
    assert((token1 === Token.fromString("token")).statement === "`token` = ?")
    assert((token1 === Token.fromString("token")).NOT.statement === "NOT `token` = ?")
    assert((token1 === Token.fromString("token")).statement === "`token` = ?")
    assert((token1 === Token.fromString("token")).NOT.statement === "NOT `token` = ?")
  }

  it should "Comparison operators can also be used when using Opaque Type Alias of Option type." in {
    assert((token2 === Token.fromString("token")).statement === "`token` = ?")
    assert((token2 === Token.fromString("token")).NOT.statement === "NOT `token` = ?")
    assert((token2 === Token.fromString("token")).statement === "`token` = ?")
    assert((token2 === Token.fromString("token")).NOT.statement === "NOT `token` = ?")
  }
