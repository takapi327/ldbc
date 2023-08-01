/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import org.specs2.mutable.Specification

import cats.Id
import ldbc.core.*

object ColumnSyntaxTest extends Specification, ColumnSyntax[Id]:

  "ColumnSyntax Test" should {

    "The string of the expression syntax that constructs the match with the specified value matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 === 1L).statement === "id = ?" and (id1 === 1L).NOT.statement === "NOT id = ?" and
        (id2 === 1L).statement === "id = ?" and (id2 === 1L).NOT.statement === "NOT id = ?"
    }

    "The string constructed by the expression syntax that determines if it is greater than or equal to the specified value matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 >= 1L).statement === "id >= ?" and (id1 >= 1L).NOT.statement === "NOT id >= ?" and
        (id2 >= 1L).statement === "id >= ?" and (id2 >= 1L).NOT.statement === "NOT id >= ?"
    }

    "The string constructed by the expression syntax that determines whether the specified value is exceeded matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 > 1L).statement === "id > ?" and (id1 > 1L).NOT.statement === "NOT id > ?" and
        (id2 > 1L).statement === "id > ?" and (id2 > 1L).NOT.statement === "NOT id > ?"
    }

    "The string constructed by the expression syntax that determines if it is less than or equal to the specified value matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 <= 1L).statement === "id <= ?" and (id1 <= 1L).NOT.statement === "NOT id <= ?" and
        (id2 <= 1L).statement === "id <= ?" and (id2 <= 1L).NOT.statement === "NOT id <= ?"
    }

    "The string constructed by the expression syntax that determines if it is less than the specified value matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 < 1L).statement === "id < ?" and (id1 < 1L).NOT.statement === "NOT id < ?" and
        (id2 < 1L).statement === "id < ?" and (id2 < 1L).NOT.statement === "NOT id < ?"
    }

    "The string constructed by the expression syntax that determines whether the specified value match or not matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 <> 1L).statement === "id <> ?" and (id1 <> 1L).NOT.statement === "NOT id <> ?" and
        (id1 !== 1L).statement === "id != ?" and (id1 !== 1L).NOT.statement === "NOT id != ?" and
        (id2 <> 1L).statement === "id <> ?" and (id2 <> 1L).NOT.statement === "NOT id <> ?" and
        (id2 !== 1L).statement === "id != ?" and (id2 !== 1L).NOT.statement === "NOT id != ?"
    }

    "The string constructed by the expression syntax that determines whether it is a Boolean value that can be TRUE, FALSE, or UNKNOWN matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 IS "TRUE").statement === "id IS TRUE" and (id1 IS "FALSE").NOT.statement === "id IS NOT FALSE" and
        (id2 IS "TRUE").statement === "id IS TRUE" and (id2 IS "NULL").statement === "id IS NULL" and (id2 IS "UNKNOWN").NOT.statement === "id IS NOT UNKNOWN"
    }

    "NULL - The string constructed by the expression syntax to determine safe equivalence matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 <=> 1L).statement === "id <=> ?" and (id1 <=> 1L).NOT.statement === "NOT id <=> ?" and
        (id2 <=> 1L).statement === "id <=> ?" and (id2 <=> 1L).NOT.statement === "NOT id <=> ?"
    }

    "The string constructed by the expression syntax that determines whether it contains at least one of the specified values matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 IN (1L, 2L)).statement === "id IN (?, ?)" and (id1 IN (1L, 2L)).NOT.statement === "id NOT IN (?, ?)" and
        (id2 IN (1L, 2L)).statement === "id IN (?, ?)" and (id2 IN (1L, 2L, 3L)).NOT.statement === "id NOT IN (?, ?, ?)"
    }

    "The string constructed by the expression syntax that determines whether the value falls within the specified range matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 BETWEEN (1L, 10L)).statement === "id BETWEEN ? AND ?" and (id1 BETWEEN (1L, 10L)).NOT.statement === "id NOT BETWEEN ? AND ?" and
        (id2 BETWEEN (1L, 10L)).statement === "id BETWEEN ? AND ?" and (id2 BETWEEN (1L, 10L)).NOT.statement === "id NOT BETWEEN ? AND ?"
    }

    "The string constructed by the expression syntax that determines whether it contains a matching string matches the specified string." in {
      val name1 = column[String]("name", VARCHAR(255))
      val name2 = column[Option[String]]("name", VARCHAR(255))
      (name1 LIKE "ldbc").statement === "name LIKE ?" and (name1 LIKE "ldbc").NOT.statement === "NOT name LIKE ?" and
        (name2 LIKE "ldbc").statement === "name LIKE ?" and (name2 LIKE "ldbc").NOT.statement === "NOT name LIKE ?"
    }

    "The string constructed by the expression syntax that determines whether it contains a matching string matches the specified string." in {
      val name1 = column[String]("name", VARCHAR(255))
      val name2 = column[Option[String]]("name", VARCHAR(255))
      (name1 LIKE_ESCAPE ("T%", "$")).statement === "name LIKE ? ESCAPE ?" and (name1 LIKE_ESCAPE ("T%", "$")).NOT.statement === "NOT name LIKE ? ESCAPE ?" and
        (name2 LIKE_ESCAPE ("T%", "$")).statement === "name LIKE ? ESCAPE ?" and (name2 LIKE_ESCAPE ("T%", "$")).NOT.statement === "NOT name LIKE ? ESCAPE ?"
    }

    "The string constructed by the expression syntax that determines whether it matches the regular expression pattern matches the specified string." in {
      val name1 = column[String]("name", VARCHAR(255))
      val name2 = column[Option[String]]("name", VARCHAR(255))
      (name1 REGEXP "^[A-D]'").statement === "name REGEXP ?" and (name1 REGEXP "^[A-D]'").NOT.statement === "NOT name REGEXP ?" and
        (name2 REGEXP "^[A-D]'").statement === "name REGEXP ?" and (name2 REGEXP "^[A-D]'").NOT.statement === "NOT name REGEXP ?"
    }

    "The string constructed by the expression syntax that performs the integer division operation to determine if it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 DIV (5, 10)).statement === "id DIV ? = ?" and (id1 DIV (5, 10)).NOT.statement === "NOT id DIV ? = ?" and
        (id2 DIV (5, 10)).statement === "id DIV ? = ?" and (id2 DIV (5, 10)).NOT.statement === "NOT id DIV ? = ?"
    }

    "The string constructed by the expression syntax that performs the operation to find the remainder and determines whether it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 MOD (5, 0)).statement === "id MOD ? = ?" and (id1 MOD (5, 0)).NOT.statement === "NOT id MOD ? = ?" and
        (id1 % (5, 0)).statement === "id % ? = ?" and (id1 % (5, 0)).NOT.statement === "NOT id % ? = ?" and
        (id2 MOD (5, 0)).statement === "id MOD ? = ?" and (id2 MOD (5, 0)).NOT.statement === "NOT id MOD ? = ?" and
        (id2 % (5, 0)).statement === "id % ? = ?" and (id2 % (5, 0)).NOT.statement === "NOT id % ? = ?"
    }

    "The string constructed by the expression syntax that performs the bit XOR operation to determine if it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 MOD (5, 0)).statement === "id MOD ? = ?" and (id1 MOD (5, 0)).NOT.statement === "NOT id MOD ? = ?" and
        (id1 % (5, 0)).statement === "id % ? = ?" and (id1 % (5, 0)).NOT.statement === "NOT id % ? = ?" and
        (id2 MOD (5, 0)).statement === "id MOD ? = ?" and (id2 MOD (5, 0)).NOT.statement === "NOT id MOD ? = ?" and
        (id2 % (5, 0)).statement === "id % ? = ?" and (id2 % (5, 0)).NOT.statement === "NOT id % ? = ?"
    }

    "The string constructed by the expression syntax that performs the left shift operation to determine if it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 ^ 1L).statement === "id ^ ?" and (id1 ^ 1L).NOT.statement === "NOT id ^ ?" and
        (id2 ^ 1L).statement === "id ^ ?" and (id2 ^ 1L).NOT.statement === "NOT id ^ ?"
    }

    "The string constructed by the expression syntax that performs the right shift operation to determine if it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 >> 1L).statement === "id >> ?" and (id1 >> 1L).NOT.statement === "NOT id >> ?" and
        (id2 >> 1L).statement === "id >> ?" and (id2 >> 1L).NOT.statement === "NOT id >> ?"
    }

    "The string constructed by the expression syntax that performs addition operations to determine if they match matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      ((id1 ++ id1) < 1L).statement === "id + id < ?" and ((id1 ++ id1) < 1L).NOT.statement === "NOT id + id < ?" and
        ((id2 ++ id2) < 1L).statement === "id + id < ?" and ((id2 ++ id2) < 1L).NOT.statement === "NOT id + id < ?"
    }

    "The string constructed by the expression syntax that performs subtraction operations to determine if they match matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      ((id1 -- id1) < 1L).statement === "id - id < ?" and ((id1 -- id1) < 1L).NOT.statement === "NOT id - id < ?" and
        ((id2 -- id2) < 1L).statement === "id - id < ?" and ((id2 -- id2) < 1L).NOT.statement === "NOT id - id < ?"
    }

    "The string constructed by the expression syntax that performs the multiplication operation to determine if it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      ((id1 * id1) < 1L).statement === "id * id < ?" and ((id1 * id1) < 1L).NOT.statement === "NOT id * id < ?" and
        ((id2 * id2) < 1L).statement === "id * id < ?" and ((id2 * id2) < 1L).NOT.statement === "NOT id * id < ?"
    }

    "The string constructed by the expression syntax that performs the division operation and determines whether it matches matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      ((id1 / id1) < 1L).statement === "id / id < ?" and ((id1 / id1) < 1L).NOT.statement === "NOT id / id < ?" and
        ((id2 / id2) < 1L).statement === "id / id < ?" and ((id2 / id2) < 1L).NOT.statement === "NOT id / id < ?"
    }

    "The string constructed by the expression syntax, which performs bit inversion to determine if it matches, matches the specified string." in {
      val id1 = column[Long]("id", BIGINT)
      val id2 = column[Option[Long]]("id", BIGINT)
      (id1 ~ 1L).statement === "~id = ?" and (id1 ~ 1L).NOT.statement === "NOT ~id = ?" and
        (id2 ~ 1L).statement === "~id = ?" and (id2 ~ 1L).NOT.statement === "NOT ~id = ?"
    }

    "The query string of the combined expression matches the specified string." in {
      val id   = column[Long]("id", BIGINT)
      val name = column[String]("name", VARCHAR(255))
      val age  = column[Option[Int]]("age", INT)
      (id === 1L && name === "name" || age > 25).statement === "(id = ? AND name = ? OR age > ?)"
    }
  }
