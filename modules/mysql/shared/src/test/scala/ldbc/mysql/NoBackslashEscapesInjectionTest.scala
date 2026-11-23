/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.syntax.*
import ldbc.net.SSL
import ldbc.telemetry.Tracer

/**
 * Verification test for the security finding: the client-side prepared-statement path
 * (`useServerPrepStmts = false`, the default) escapes string parameters with backslash escaping only
 * (`'` -> `\'`) and never consults the server `sql_mode`. When the server runs with
 * `NO_BACKSLASH_ESCAPES`, backslash is a literal character, so `\'` does NOT neutralize the quote and
 * a string parameter can break out of its literal — a SQL injection.
 *
 * This test drives a real MySQL session, switches it to `NO_BACKSLASH_ESCAPES`, and binds an injection
 * payload through the client PreparedStatement. The query is written so that it returns a row ONLY if
 * the payload escapes its string literal. Secure behaviour = no row. If the finding is real, the test
 * FAILS because the injection makes the predicate always true.
 */
class NoBackslashEscapesInjectionTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  private val connection = Connection[Fx](
    host     = TestConfig.host,
    port     = TestConfig.port,
    user     = TestConfig.user,
    password = Some(TestConfig.password),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  test("client-side string escaping must stay safe under NO_BACKSLASH_ESCAPES sql_mode") {
    // ldbc escapes the quote as `\'`. Under NO_BACKSLASH_ESCAPES the backslash is literal, so the
    // quote still closes the literal and the rest becomes live SQL:
    //   WHERE t.name = 'zzz\' OR 1=1 -- '  =>  (name = 'zzz\') OR 1=1  (rest commented)  => always true.
    // The payload deliberately avoids inner quotes so the injected SQL stays syntactically valid.
    val payload = "zzz' OR 1=1 -- "

    val program = connection.use { conn =>
      for
        setStmt <- conn.createStatement()
        _       <- setStmt.executeUpdate("SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES'")
        ps      <- conn.clientPreparedStatement(
                "SELECT cnt FROM (SELECT 1 AS cnt, 'admin' AS name) t WHERE t.name = ?"
              )
        rs   <- ps.setString(1, payload) *> ps.executeQuery()
        rows <- rs.whileM[List, Int](rs.getInt(1))
      yield rows
    }

    // Secure expectation: the payload is a plain string that matches no row.
    assertFx(program, List.empty[Int])
  }

  /**
   * Regression guard for the finding that the OK packet read the 8-byte form of a length-encoded integer as 4 bytes.
   * An AUTO_INCREMENT value of 2^24 or more is sent in that form, so the status flags were read from the wrong bytes
   * and the tracked `NO_BACKSLASH_ESCAPES` state was switched off, letting the next string parameter break out.
   */
  test("an AUTO_INCREMENT value of 2^24 or more does not switch off the NO_BACKSLASH_ESCAPES tracking") {
    val payload = "zzz' OR 1=1 -- "

    val program = connection.use { conn =>
      for
        statement <- conn.createStatement()
        _         <-
          statement.executeUpdate(
            "CREATE TEMPORARY TABLE `nbe_large_id` (`id` BIGINT AUTO_INCREMENT PRIMARY KEY, `v` INT) AUTO_INCREMENT = 16777216"
          )
        _  <- statement.executeUpdate("SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES'")
        _  <- statement.executeUpdate("INSERT INTO `nbe_large_id` (`v`) VALUES (1)")
        ps <- conn.clientPreparedStatement(
                "SELECT cnt FROM (SELECT 1 AS cnt, 'admin' AS name) t WHERE t.name = ?"
              )
        rs   <- ps.setString(1, payload) *> ps.executeQuery()
        rows <- rs.whileM[List, Int](rs.getInt(1))
      yield rows
    }

    assertFx(program, List.empty[Int])
  }

  /**
   * Regression guard for the finding that `enquoteLiteral` and `enquoteNCharLiteral` read the sql_mode captured when
   * the connection was made, while prepared statements follow the live session. After `SET sql_mode` the literal was
   * escaped for the old mode, and the payload closed it. Selecting the literal must return the payload unchanged.
   */
  test("enquoteLiteral follows a NO_BACKSLASH_ESCAPES set after connecting") {
    val payload = "\\' OR 1=1 -- "

    val program = connection.use { conn =>
      for
        statement <- conn.createStatement()
        _         <- statement.executeUpdate("SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES'")
        literal   <- statement.enquoteLiteral(payload)
        rs        <- statement.executeQuery(s"SELECT $literal")
        rows      <- rs.whileM[List, String](rs.getString(1))
      yield rows
    }

    assertFx(program, List(payload))
  }

  test("enquoteNCharLiteral follows a NO_BACKSLASH_ESCAPES set after connecting") {
    val payload = "\\' OR 1=1 -- "

    val program = connection.use { conn =>
      for
        statement <- conn.createStatement()
        _         <- statement.executeUpdate("SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES'")
        literal   <- statement.enquoteNCharLiteral(payload)
        rs        <- statement.executeQuery(s"SELECT $literal")
        rows      <- rs.whileM[List, String](rs.getString(1))
      yield rows
    }

    assertFx(program, List(payload))
  }

  /**
   * Regression guard for the finding that the reply to COM_CHANGE_USER was read past the `NO_BACKSLASH_ESCAPES`
   * tracking. CHANGE_USER resets the session sql_mode, so the state set before it stayed on, string parameters were
   * escaped by doubling quotes while the server again read backslashes as escapes, and the payload broke out.
   */
  test("CHANGE_USER resets the NO_BACKSLASH_ESCAPES tracking along with the session") {
    val payload = "\\' OR 1=1 -- "

    val program = connection.use { conn =>
      for
        statement <- conn.createStatement()
        _         <- statement.executeUpdate("SET SESSION sql_mode = 'NO_BACKSLASH_ESCAPES'")
        _         <- conn.changeUser(TestConfig.user, TestConfig.password)
        ps        <- conn.clientPreparedStatement(
                "SELECT cnt FROM (SELECT 1 AS cnt, 'admin' AS name) t WHERE t.name = ?"
              )
        rs   <- ps.setString(1, payload) *> ps.executeQuery()
        rows <- rs.whileM[List, Int](rs.getInt(1))
      yield rows
    }

    assertFx(program, List.empty[Int])
  }
