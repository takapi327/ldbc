/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.data

import scala.collection.immutable.SortedMap

import ldbc.mysql.FTestPlatform

/**
 * Unit tests for building batched statements.
 *
 * Regression guard for the batch INSERT SQL-injection finding: the rows of a batch used to be cut
 * out of the already bound statement by splitting on the word `VALUES`, so a bound value containing
 * that word moved the cut into the middle of its string literal. The split points must come from the
 * unbound statement only, as in MySQL Connector/J.
 */
class QueryRendererBatchTest extends FTestPlatform:

  private def params(values: Parameter*): SortedMap[Int, Parameter] =
    SortedMap(values.zipWithIndex.map { case (value, index) => (index + 1) -> value }*)

  private def rewrite(sql: String, rows: SortedMap[Int, Parameter]*): String =
    val template = QueryRenderer.multiValuesTemplate(sql).getOrElse(fail(s"not rewritable: $sql"))
    template.head + rows.map(QueryRenderer.buildBatch(sql, _, noBackslashEscapes = false)).mkString(",") + template.tail

  test("a bound value containing VALUES stays inside its string literal") {
    val sql     = "INSERT INTO t (a,b) VALUES (?,?)"
    val payload = "VALUES (1,(SELECT password FROM users LIMIT 1))#"
    assertEquals(
      rewrite(sql, params(Parameter.string(payload), Parameter.int(2)), params(Parameter.string("x"), Parameter.int(3))),
      "INSERT INTO t (a,b) VALUES ('VALUES (1,(SELECT password FROM users LIMIT 1))#',2),('x',3)"
    )
  }

  test("a bound value that tries to close the row is escaped and stays a single literal") {
    val sql = "INSERT INTO t (a) VALUES (?)"
    assertEquals(
      rewrite(sql, params(Parameter.string("'),(1);DROP TABLE t;#"))),
      "INSERT INTO t (a) VALUES ('\\'),(1);DROP TABLE t;#')"
    )
  }

  test("the VALUES keyword is matched case-insensitively") {
    assertEquals(
      rewrite("insert into t (a) values (?)", params(Parameter.int(1)), params(Parameter.int(2))),
      "insert into t (a) values (1),(2)"
    )
  }

  test("VALUES inside quoted text, comments and identifiers is not the clause") {
    val sql = "INSERT INTO `values` /* VALUES (x) */ (a, b) VALUES ('VALUES (?)', ?)"
    assertEquals(
      rewrite(sql, params(Parameter.int(1)), params(Parameter.int(2))),
      "INSERT INTO `values` /* VALUES (x) */ (a, b) VALUES ('VALUES (?)', 1),('VALUES (?)', 2)"
    )
  }

  test("a table named value is not taken for the VALUES clause") {
    assertEquals(
      rewrite("INSERT INTO value (a) VALUE (?)", params(Parameter.int(1)), params(Parameter.int(2))),
      "INSERT INTO value (a) VALUE (1),(2)"
    )
  }

  test("a statement with several rows is rewritten as a whole") {
    val sql = "INSERT INTO t (a) VALUES (?), ('VALUES'), (?)"
    assertEquals(
      rewrite(sql, params(Parameter.int(1), Parameter.int(2)), params(Parameter.int(3), Parameter.int(4))),
      "INSERT INTO t (a) VALUES (1), ('VALUES'), (2),(3), ('VALUES'), (4)"
    )
  }

  test("ON DUPLICATE KEY UPDATE with the VALUES() function is kept after the rows") {
    val sql = "INSERT INTO t (a,b) VALUES (?,?) ON DUPLICATE KEY UPDATE b = VALUES(b)"
    assertEquals(
      rewrite(sql, params(Parameter.int(1), Parameter.int(2)), params(Parameter.int(3), Parameter.int(4))),
      "INSERT INTO t (a,b) VALUES (1,2),(3,4) ON DUPLICATE KEY UPDATE b = VALUES(b)"
    )
  }

  test("a statement that cannot be rewritten has no template") {
    List(
      "INSERT INTO t (a) SELECT ? FROM dual",
      "INSERT INTO t (a) VALUES (?) ON DUPLICATE KEY UPDATE a = ?",
      "INSERT INTO t (a) VALUES (?), 1",
      "INSERT INTO t (a) VALUES (1)",
      "INSERT /*!50000 IGNORE */ INTO t (a) VALUES (?)",
      "INSERT INTO t (a) VALUES (?); DELETE FROM t",
      "INSERT INTO t (a) VALUES ('C:\\', ?)",
      "UPDATE t SET a = ? WHERE id = 1"
    ).foreach { sql =>
      assertEquals(QueryRenderer.multiValuesTemplate(sql), None, sql)
    }
  }

  test("an INSERT that cannot be rewritten is built as a complete statement") {
    val sql = "INSERT INTO t (a) VALUES (?) ON DUPLICATE KEY UPDATE a = ?"
    assertEquals(
      QueryRenderer.buildBatch(sql, params(Parameter.string("VALUES"), Parameter.int(2)), noBackslashEscapes = false),
      "INSERT INTO t (a) VALUES ('VALUES') ON DUPLICATE KEY UPDATE a = 2"
    )
  }

  test("a question mark inside quoted text is not a placeholder") {
    val sql = "UPDATE t SET a = 'why?' WHERE id = ?"
    assertEquals(
      QueryRenderer.buildBatch(sql, params(Parameter.int(1)), noBackslashEscapes = false),
      "UPDATE t SET a = 'why?' WHERE id = 1"
    )
  }

  test("the placeholder count must match the parameter count") {
    intercept[IllegalArgumentException] {
      QueryRenderer.buildBatch("INSERT INTO t (a, b) VALUES (?, ?)", params(Parameter.int(1)), noBackslashEscapes = false)
    }
  }

  test("an unterminated quoted string is rejected") {
    intercept[IllegalArgumentException] {
      QueryRenderer.buildBatch("INSERT INTO t (a) VALUES ('?)", params(Parameter.int(1)), noBackslashEscapes = false)
    }
  }
