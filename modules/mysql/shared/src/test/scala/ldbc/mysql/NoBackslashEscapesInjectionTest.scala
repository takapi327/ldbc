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
