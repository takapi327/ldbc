/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

import org.typelevel.otel4s.Attribute

import ldbc.connector.*

class OpenTelemetryAttributesTest extends FTestPlatform:

  test("dbSystemName should return mysql") {
    assertEquals(OpenTelemetryAttributes.dbSystemName, Attribute("db.system.name", "mysql"))
  }

  test("dbNamespace should return correct attribute") {
    val database = "test_db"
    assertEquals(OpenTelemetryAttributes.dbNamespace(database), Attribute("db.namespace", database))
  }

  test("dbCollectionName should return correct attribute") {
    val table = "users"
    assertEquals(OpenTelemetryAttributes.dbCollectionName(table), Attribute("db.collection.name", table))
  }

  test("dbOperationName should return correct attribute") {
    val operation = "SELECT"
    assertEquals(OpenTelemetryAttributes.dbOperationName(operation), Attribute("db.operation.name", operation))
  }

  test("dbQueryText should return correct attribute") {
    val query = "SELECT * FROM users WHERE id = 1"
    assertEquals(OpenTelemetryAttributes.dbQueryText(query), Attribute("db.query.text", query))
  }

  test("dbQuerySummary should return correct attribute") {
    val summary = "SELECT * FROM users"
    assertEquals(OpenTelemetryAttributes.dbQuerySummary(summary), Attribute("db.query.summary", summary))
  }

  test("serverAddress should return correct attribute") {
    val host = "localhost"
    assertEquals(OpenTelemetryAttributes.serverAddress(host), Attribute("server.address", host))
  }

  test("serverPort should return correct attribute") {
    val port = 3306
    assertEquals(OpenTelemetryAttributes.serverPort(port), Attribute("server.port", 3306L))
  }

  test("dbMysqlVersion should return correct attribute") {
    val version = "8.0.33"
    assertEquals(OpenTelemetryAttributes.dbMysqlVersion(version), Attribute("db.mysql.version", version))
  }

  test("dbMysqlThreadId should return correct attribute") {
    val threadId = 12345
    assertEquals(OpenTelemetryAttributes.dbMysqlThreadId(threadId), Attribute("db.mysql.thread_id", 12345L))
  }

  test("dbMysqlAuthPlugin should return correct attribute") {
    val plugin = "mysql_native_password"
    assertEquals(OpenTelemetryAttributes.dbMysqlAuthPlugin(plugin), Attribute("db.mysql.auth_plugin", plugin))
  }

  test("batchSize should return Some for size >= 2") {
    assertEquals(OpenTelemetryAttributes.batchSize(2L), Some(Attribute("db.operation.batch.size", 2L)))
    assertEquals(OpenTelemetryAttributes.batchSize(100L), Some(Attribute("db.operation.batch.size", 100L)))
  }

  test("batchSize should return None for size < 2") {
    assertEquals(OpenTelemetryAttributes.batchSize(1L), None)
    assertEquals(OpenTelemetryAttributes.batchSize(0L), None)
  }

  test("sanitizeSql should replace string literals") {
    val sql       = "SELECT * FROM users WHERE name = 'John Doe' AND age = 25"
    val sanitized = OpenTelemetryAttributes.sanitizeSql(sql)
    assertEquals(sanitized, "SELECT * FROM users WHERE name = '?' AND age = ?")
  }

  test("sanitizeSql should replace double-quoted identifiers") {
    val sql       = """SELECT "user_name" FROM "users" WHERE "id" = 1"""
    val sanitized = OpenTelemetryAttributes.sanitizeSql(sql)
    assertEquals(sanitized, """SELECT "?" FROM "?" WHERE "?" = ?""")
  }

  test("sanitizeSql should handle multiple string literals") {
    val sql       = "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')"
    val sanitized = OpenTelemetryAttributes.sanitizeSql(sql)
    assertEquals(sanitized, "INSERT INTO users (name, email) VALUES ('?', '?')")
  }

  test("sanitizeSql should handle empty strings") {
    val sql       = "SELECT * FROM users WHERE name = ''"
    val sanitized = OpenTelemetryAttributes.sanitizeSql(sql)
    assertEquals(sanitized, "SELECT * FROM users WHERE name = '?'")
  }

  test("sanitizeSql should handle mixed numbers") {
    val sql       = "SELECT * FROM users WHERE age BETWEEN 18 AND 65 AND score = 100.5"
    val sanitized = OpenTelemetryAttributes.sanitizeSql(sql)
    assertEquals(sanitized, "SELECT * FROM users WHERE age BETWEEN ? AND ? AND score = ?")
  }

  test("extractOperationName should extract SELECT") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("SELECT * FROM users"), "SELECT")
    assertEquals(OpenTelemetryAttributes.extractOperationName("  select * from users  "), "SELECT")
  }

  test("extractOperationName should extract INSERT") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("INSERT INTO users VALUES (1, 'John')"), "INSERT")
  }

  test("extractOperationName should extract UPDATE") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("UPDATE users SET name = 'John'"), "UPDATE")
  }

  test("extractOperationName should extract DELETE") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("DELETE FROM users WHERE id = 1"), "DELETE")
  }

  test("extractOperationName should extract CREATE") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("CREATE TABLE users (id INT)"), "CREATE")
  }

  test("extractOperationName should extract DROP") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("DROP TABLE users"), "DROP")
  }

  test("extractOperationName should extract ALTER") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("ALTER TABLE users ADD COLUMN age INT"), "ALTER")
  }

  test("extractOperationName should extract TRUNCATE") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("TRUNCATE TABLE users"), "TRUNCATE")
  }

  test("extractOperationName should extract SHOW") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("SHOW TABLES"), "SHOW")
  }

  test("extractOperationName should extract SET") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("SET @var = 1"), "SET")
  }

  test("extractOperationName should extract BEGIN") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("BEGIN"), "BEGIN")
  }

  test("extractOperationName should extract COMMIT") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("COMMIT"), "COMMIT")
  }

  test("extractOperationName should extract ROLLBACK") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("ROLLBACK"), "ROLLBACK")
  }

  test("extractOperationName should return OTHER for unknown operations") {
    assertEquals(OpenTelemetryAttributes.extractOperationName("EXPLAIN SELECT * FROM users"), "OTHER")
    assertEquals(OpenTelemetryAttributes.extractOperationName(""), "OTHER")
  }

  test("extractTableName should extract table from SELECT") {
    assertEquals(OpenTelemetryAttributes.extractTableName("SELECT * FROM users"), Some("USERS"))
    assertEquals(OpenTelemetryAttributes.extractTableName("SELECT * FROM users WHERE id = 1"), Some("USERS"))
    assertEquals(OpenTelemetryAttributes.extractTableName("select * from users u"), Some("USERS"))
  }

  test("extractTableName should extract table from INSERT") {
    assertEquals(OpenTelemetryAttributes.extractTableName("INSERT INTO users VALUES (1, 'John')"), Some("USERS"))
    assertEquals(OpenTelemetryAttributes.extractTableName("insert into users (name) values ('test')"), Some("USERS"))
  }

  test("extractTableName should extract table from UPDATE") {
    assertEquals(OpenTelemetryAttributes.extractTableName("UPDATE users SET name = 'John'"), Some("USERS"))
    assertEquals(OpenTelemetryAttributes.extractTableName("update users set age = 30"), Some("USERS"))
  }

  test("extractTableName should extract table from DELETE") {
    assertEquals(OpenTelemetryAttributes.extractTableName("DELETE FROM users WHERE id = 1"), Some("USERS"))
  }

  test("extractTableName should handle table names with schema") {
    assertEquals(OpenTelemetryAttributes.extractTableName("SELECT * FROM schema.users"), Some("SCHEMA.USERS"))
  }

  test("extractTableName should handle table names with backticks") {
    assertEquals(OpenTelemetryAttributes.extractTableName("SELECT * FROM `users`"), Some("`USERS`"))
    assertEquals(OpenTelemetryAttributes.extractTableName("SELECT * FROM `my schema`.`my table`"), Some("`MY SCHEMA`.`MY TABLE`"))
    assertEquals(OpenTelemetryAttributes.extractTableName("UPDATE `db`.`table` SET col = 1"), Some("`DB`.`TABLE`"))
  }

  test("extractTableName should return None for queries without tables") {
    assertEquals(OpenTelemetryAttributes.extractTableName("SELECT 1"), None)
    assertEquals(OpenTelemetryAttributes.extractTableName("SET @var = 1"), None)
    assertEquals(OpenTelemetryAttributes.extractTableName("BEGIN"), None)
  }

  test("extractTableName should handle multiple tables and return first one") {
    assertEquals(
      OpenTelemetryAttributes.extractTableName("SELECT * FROM users, orders WHERE users.id = orders.user_id"),
      Some("USERS")
    )
  }

  test("dbStoredProcedureName should return correct attribute") {
    val procName = "get_user_by_id"
    assertEquals(
      OpenTelemetryAttributes.dbStoredProcedureName(procName),
      Attribute("db.stored_procedure.name", procName)
    )
  }

  test("extractStoredProcedureName should extract procedure name from CALL statements") {
    assertEquals(OpenTelemetryAttributes.extractStoredProcedureName("CALL get_user_by_id(123)"), Some("GET_USER_BY_ID"))
    assertEquals(OpenTelemetryAttributes.extractStoredProcedureName("call my_procedure"), Some("MY_PROCEDURE"))
    assertEquals(
      OpenTelemetryAttributes.extractStoredProcedureName("CALL schema.procedure_name(?)"),
      Some("SCHEMA.PROCEDURE_NAME")
    )
  }

  test("extractStoredProcedureName should return None for non-CALL statements") {
    assertEquals(OpenTelemetryAttributes.extractStoredProcedureName("SELECT * FROM users"), None)
    assertEquals(OpenTelemetryAttributes.extractStoredProcedureName("INSERT INTO users VALUES (1)"), None)
  }
