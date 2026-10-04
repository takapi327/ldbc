/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.catseffect.concurrentIO
import ldbc.catseffect.Connector
import ldbc.mysql.{ MySQLConfig, MySQLDataSource }
import ldbc.net.SSL

/**
 * Checks that a row larger than the historical 64KB default round-trips without the caller having to
 * configure anything, which is what adopting the server's `max_allowed_packet` buys.
 *
 * Docker MySQL at 127.0.0.1:13306.
 */
class LargePayloadIntegrationTest extends CatsEffectSuite:

  override def munitIOTimeout: scala.concurrent.duration.Duration =
    scala.concurrent.duration.Duration(120, "s")

  private val connector =
    Connector.fromDataSource(
      MySQLDataSource.fromConfig[IO](
        MySQLConfig.default
          .setHost("127.0.0.1")
          .setPort(13306)
          .setUser("ldbc")
          .setPassword("password")
          .setDatabase("connector_test")
          .setSSL(SSL.Trusted)
      )
    )

  test("a row well past the old 64KB default round-trips on an unconfigured connection") {
    val body    = "x" * (512 * 1024)
    val program = for
      _       <- sql"DROP TABLE IF EXISTS large_payload".update.commit(connector)
      _       <- sql"CREATE TABLE large_payload (id INT PRIMARY KEY, body LONGTEXT)".update.commit(connector)
      _       <- sql"INSERT INTO large_payload (id, body) VALUES (1, $body)".update.commit(connector)
      fetched <- sql"SELECT body FROM large_payload WHERE id = 1".query[String].to[Option].readOnly(connector)
      _       <- sql"DROP TABLE IF EXISTS large_payload".update.commit(connector)
    yield fetched.map(_.length)
    assertIO(program, Some(512 * 1024))
  }

  test("the session adopts the server's max_allowed_packet") {
    val program = for
      reported <- sql"SELECT @@max_allowed_packet".query[Long].to[Option].readOnly(connector)
      body = "y" * (2 * 1024 * 1024)
      _       <- sql"DROP TABLE IF EXISTS large_payload_2".update.commit(connector)
      _       <- sql"CREATE TABLE large_payload_2 (id INT PRIMARY KEY, body LONGTEXT)".update.commit(connector)
      _       <- sql"INSERT INTO large_payload_2 (id, body) VALUES (1, $body)".update.commit(connector)
      fetched <-
        sql"SELECT CHAR_LENGTH(body) FROM large_payload_2 WHERE id = 1".query[Int].to[Option].readOnly(connector)
      _ <- sql"DROP TABLE IF EXISTS large_payload_2".update.commit(connector)
    yield (reported, fetched)
    program.map { (reported, fetched) =>
      assert(
        reported.exists(_ > 2 * 1024 * 1024),
        s"this test assumes the server allows more than 2MB, but it reports $reported"
      )
      assertEquals(fetched, Some(2 * 1024 * 1024), "a 2MB statement is accepted because the server allows it")
    }
  }

  test("a value the server has to split across packets is reported, not silently dropped") {
    val program = for
      _    <- sql"DROP TABLE IF EXISTS split_payload".update.commit(connector)
      _    <- sql"CREATE TABLE split_payload (id INT PRIMARY KEY, body LONGTEXT)".update.commit(connector)
      _    <- sql"INSERT INTO split_payload (id, body) VALUES (1, REPEAT('a', 20971520))".update.commit(connector)
      read <- sql"SELECT body FROM split_payload WHERE id = 1".query[String].to[Option].readOnly(connector).attempt
      _    <- sql"DROP TABLE IF EXISTS split_payload".update.commit(connector)
    yield read
    program.map { read =>
      val error = read.left.getOrElse(
        fail(s"a 20MB value cannot be delivered in one packet, so reading it must fail rather than return $read")
      )
      assert(
        error.isInstanceOf[ldbc.sql.SQLFeatureNotSupportedException],
        s"the reason must name the unsupported protocol feature, got ${ error.getClass.getName }: ${ error.getMessage }"
      )
    }
  }
