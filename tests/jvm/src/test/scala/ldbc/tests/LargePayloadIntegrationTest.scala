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
 * Checks what a real server does with messages too large for one packet.
 *
 * A packet header carries its length in three bytes, so anything past 16,777,215 bytes travels as
 * several packets that the receiver puts back together. These tests exercise that against a live
 * server in both directions, along with the limit that bounds how large a message may get.
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

  test("a value the server has to split across packets is reassembled") {
    val size    = 20971520
    val program = for
      _    <- sql"DROP TABLE IF EXISTS split_payload".update.commit(connector)
      _    <- sql"CREATE TABLE split_payload (id INT PRIMARY KEY, body LONGTEXT)".update.commit(connector)
      _    <- sql"INSERT INTO split_payload (id, body) VALUES (1, REPEAT('a', $size))".update.commit(connector)
      read <- sql"SELECT body FROM split_payload WHERE id = 1".query[String].to[Option].readOnly(connector)
      _    <- sql"DROP TABLE IF EXISTS split_payload".update.commit(connector)
    yield read
    program.map { read =>
      val body = read.getOrElse(fail("a 20MB value must come back as a row, not as an empty result"))
      assertEquals(body.length, size, "the continuation packets were not all folded into the row")
      assert(body.forall(_ == 'a'), "the reassembled value does not match what the server generated")
    }
  }

  test("a value too large for one packet survives a round trip in both directions") {
    val size    = 20 * 1024 * 1024
    val body    = "z" * size
    val program = for
      _       <- sql"DROP TABLE IF EXISTS round_trip_payload".update.commit(connector)
      _       <- sql"CREATE TABLE round_trip_payload (id INT PRIMARY KEY, body LONGTEXT)".update.commit(connector)
      _       <- sql"INSERT INTO round_trip_payload (id, body) VALUES (1, $body)".update.commit(connector)
      fetched <- sql"SELECT body FROM round_trip_payload WHERE id = 1".query[String].to[Option].readOnly(connector)
      _       <- sql"DROP TABLE IF EXISTS round_trip_payload".update.commit(connector)
    yield fetched
    program.map { fetched =>
      val read = fetched.getOrElse(fail("a 20MB statement must be accepted and read back"))
      assertEquals(read.length, size, "splitting the statement or reassembling the reply lost bytes")
      assert(read.forall(_ == 'z'), "the value came back altered")
    }
  }

  test("a value too large for one packet survives a round trip on the binary protocol") {
    val prepared =
      Connector.fromDataSource(
        MySQLDataSource.fromConfig[IO](
          MySQLConfig.default
            .setHost("127.0.0.1")
            .setPort(13306)
            .setUser("ldbc")
            .setPassword("password")
            .setDatabase("connector_test")
            .setSSL(SSL.Trusted)
            .setUseServerPrepStmts(true)
        )
      )
    val size    = 20 * 1024 * 1024
    val body    = "b" * size
    val program = for
      _      <- sql"DROP TABLE IF EXISTS binary_payload".update.commit(prepared)
      _      <- sql"CREATE TABLE binary_payload (id INT PRIMARY KEY, body LONGTEXT)".update.commit(prepared)
      _      <- sql"INSERT INTO binary_payload (id, body) VALUES (1, $body)".update.commit(prepared)
      stored <-
        sql"SELECT CHAR_LENGTH(body) FROM binary_payload WHERE id = 1".query[Int].to[Option].readOnly(prepared)
      fetched <- sql"SELECT body FROM binary_payload WHERE id = 1".query[String].to[Option].readOnly(prepared)
      _       <- sql"DROP TABLE IF EXISTS binary_payload".update.commit(prepared)
    yield (stored, fetched)
    program.map { (stored, fetched) =>
      assertEquals(stored, Some(size), "the 20MB parameter did not reach the server intact")
      val read = fetched.getOrElse(fail("a 20MB value must round-trip through a server-side prepared statement"))
      assertEquals(read.length, size, "the binary protocol row decoder lost bytes reassembling the value")
      assert(read.forall(_ == 'b'), "the value came back altered")
    }
  }

  test("a statement past the configured limit is refused rather than sent in pieces") {
    val narrowed =
      Connector.fromDataSource(
        MySQLDataSource.fromConfig[IO](
          MySQLConfig.default
            .setHost("127.0.0.1")
            .setPort(13306)
            .setUser("ldbc")
            .setPassword("password")
            .setDatabase("connector_test")
            .setSSL(SSL.Trusted)
            .setMaxAllowedPacket(4 * 1024 * 1024)
        )
      )
    val body    = "w" * (8 * 1024 * 1024)
    val program = for
      _      <- sql"DROP TABLE IF EXISTS narrow_payload".update.commit(narrowed)
      _      <- sql"CREATE TABLE narrow_payload (id INT PRIMARY KEY, body LONGTEXT)".update.commit(narrowed)
      result <- sql"INSERT INTO narrow_payload (id, body) VALUES (1, $body)".update.commit(narrowed).attempt
      _      <- sql"DROP TABLE IF EXISTS narrow_payload".update.commit(narrowed)
    yield result
    program.map { result =>
      val error = result.left.getOrElse(fail(s"8MB past a 4MB limit must be refused, but got $result"))
      assert(
        error.isInstanceOf[ldbc.mysql.exception.PacketTooBigException],
        s"the size limit must name itself, got ${ error.getClass.getName }: ${ error.getMessage }"
      )
    }
  }
