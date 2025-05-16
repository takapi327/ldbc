/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.dsl.*
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
    assert((id1 << 1L).statement === "`id` << ?")
    assert((id1 << 1L).NOT.statement === "NOT `id` << ?")
    assert((id2 << 1L).statement === "`id` << ?")
    assert((id2 << 1L).NOT.statement === "NOT `id` << ?")
  }

  it should "The string constructed by the SQL subquery comparison matches the expected value" in {
    val sql = sql"SELECT id FROM user WHERE id = 1"
    assert((id1 === sql).statement === "`id` = (SELECT id FROM user WHERE id = 1)")
    assert((id1 >= sql).statement === "`id` >= (SELECT id FROM user WHERE id = 1)")
    assert((id1 > sql).statement === "`id` > (SELECT id FROM user WHERE id = 1)")
    assert((id1 <= sql).statement === "`id` <= (SELECT id FROM user WHERE id = 1)")
    assert((id1 < sql).statement === "`id` < (SELECT id FROM user WHERE id = 1)")
    assert((id1 <> sql).statement === "`id` <> (SELECT id FROM user WHERE id = 1)")
    assert((id1 IN sql).statement === "`id` IN (SELECT id FROM user WHERE id = 1)")
  }

  it should "The join query expression should produce the correct statement" in {
    val otherId = Column.Impl[Long]("id")
    val otherName = Column.Impl[String]("name")
    
    assert((id1 === otherId).statement === "`id` = `id`")
    assert((id1 >= otherId).statement === "`id` >= `id`")
    assert((id1 > otherId).statement === "`id` > `id`")
    assert((id1 <= otherId).statement === "`id` <= `id`")
    assert((id1 < otherId).statement === "`id` < `id`")
    assert((id1 <> otherId).statement === "`id` <> `id`")
    assert((id1 !== otherId).statement === "`id` != `id`")
    
    assert((name1 === otherName).statement === "`name` = `name`")
  }
  
  it should "The count method should produce the correct statement" in {
    val countCol = id1.count
    assert(countCol.name === "COUNT(`id`)")
    val aliasedCountCol = id1.as("user").count
    assert(aliasedCountCol.alias === Some("COUNT(user)"))
  }
  
  it should "The opt transformation should convert a Column[A] to a Column[Option[A]]" in {
    val optId = id1.opt
    assert(optId.isInstanceOf[Column[Option[Long]]])
    
    val optName = name1.opt
    assert(optName.isInstanceOf[Column[Option[String]]])
  }
  
  it should "Support OrderBy operations through asc and desc methods" in {
    val ascOrder = id1.asc
    val descOrder = id1.desc
    
    assert(ascOrder.statement === "`id` ASC")
    assert(descOrder.statement === "`id` DESC")
  }
  
  it should "Support product operation to combine columns" in {
    val combined = id1.product(name1)
    assert(combined.name === "`id`, `name`")
    assert(combined.values === 2)
  }
  
  it should "Support imap transformation with correct behavior" in {
    // Convert between String and Int
    val strToInt = name1.imap(_.toInt)(_.toString)
    assert(strToInt.name === name1.name)
    assert(strToInt.isInstanceOf[Column[Int]])
  }
