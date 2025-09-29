/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import java.time.*

import cats.effect.*

import munit.*

import ldbc.dsl.*

import ldbc.connector.*

class LdbcCodecTest extends CodecTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: ConnectionFixture =
    ConnectionFixture(
      "connection",
      MySQLDataSource
        .build[IO]("127.0.0.1", 13306, "ldbc")
        .setPassword("password")
        .setDatabase("world")
        .setSSL(SSL.Trusted)
    )

trait CodecTest extends CatsEffectSuite:

  def prefix:     "jdbc" | "ldbc"
  def connection: ConnectionFixture

  private lazy val connectionFixture = connection
    .withBeforeAll(conn => sql"CREATE DATABASE IF NOT EXISTS codec_test".update.commit(conn) *> IO.unit)
    .withAfterAll(conn => sql"DROP DATABASE IF EXISTS codec_test".update.commit(conn) *> IO.unit)
    .fixture

  override def munitFixtures = List(connectionFixture)

  test("Encoder and Decoder work properly for data of type Boolean.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE boolean_test (flag BOOLEAN)".update
        _      <- sql"INSERT INTO boolean_test (flag) VALUES (${ true })".update
        result <- sql"SELECT * FROM boolean_test WHERE flag = ${ true }".query[Boolean].to[Option]
      yield result).transaction(connectionFixture()),
      Some(true)
    )
  }

  test("Encoder and Decoder work properly for data of type Byte.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE byte_test (id TINYINT)".update
        _      <- sql"INSERT INTO byte_test (id) VALUES (${ 1.toByte })".update
        result <- sql"SELECT * FROM byte_test WHERE id = ${ 1.toByte }".query[Byte].to[Option]
      yield result).transaction(connectionFixture()),
      Some(1.toByte)
    )
  }

  test("Encoder and Decoder work properly for data of type Short.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE short_test (id SMALLINT)".update
        _      <- sql"INSERT INTO short_test (id) VALUES (${ 1.toShort })".update
        result <- sql"SELECT * FROM short_test WHERE id = ${ 1.toShort }".query[Short].to[Option]
      yield result).transaction(connectionFixture()),
      Some(1.toShort)
    )
  }

  test("Encoder and Decoder work properly for data of type Int.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE int_test (id INT)".update
        _      <- sql"INSERT INTO int_test (id) VALUES (1)".update
        result <- sql"SELECT * FROM int_test WHERE id = ${ 1 }".query[Int].to[Option]
      yield result).transaction(connectionFixture()),
      Some(1)
    )
  }

  test("Encoder and Decoder work properly for data of type Long.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE long_test (id BIGINT)".update
        _      <- sql"INSERT INTO long_test (id) VALUES (1)".update
        result <- sql"SELECT * FROM long_test WHERE id = ${ 1 }".query[Long].to[Option]
      yield result).transaction(connectionFixture()),
      Some(1L)
    )
  }

  test("Encoder and Decoder work properly for data of type Float.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE float_test (id FLOAT)".update
        _      <- sql"INSERT INTO float_test (id) VALUES (1.0)".update
        result <- sql"SELECT * FROM float_test WHERE id = ${ 1.0f }".query[Float].to[Option]
      yield result).transaction(connectionFixture()),
      Some(1.0f)
    )
  }

  test("Encoder and Decoder work properly for data of type Double.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE double_test (id DOUBLE)".update
        _      <- sql"INSERT INTO double_test (id) VALUES (1.0)".update
        result <- sql"SELECT * FROM double_test WHERE id = ${ 1.0 }".query[Double].to[Option]
      yield result).transaction(connectionFixture()),
      Some(1.0)
    )
  }

  test("Encoder and Decoder work properly for data of type BigDecimal.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE bigdecimal_test (id DECIMAL(10, 2))".update
        _      <- sql"INSERT INTO bigdecimal_test (id) VALUES (1.0)".update
        result <- sql"SELECT * FROM bigdecimal_test WHERE id = ${ BigDecimal(1.0) }".query[BigDecimal].to[Option]
      yield result).transaction(connectionFixture()),
      Some(BigDecimal(1.0))
    )
  }

  test("Encoder and Decoder work properly for data of type String.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE string_test (name VARCHAR(255))".update
        _      <- sql"INSERT INTO string_test (name) VALUES ('Takahiko')".update
        result <- sql"SELECT * FROM string_test WHERE name = ${ "Takahiko" }".query[String].to[Option]
      yield result).transaction(connectionFixture()),
      Some("Takahiko")
    )
  }

  test("Encoder and Decoder work properly for data of type Array[Byte].") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE array_byte_test (data BLOB)".update
        _      <- sql"INSERT INTO array_byte_test (data) VALUES (${ Array[Byte](1, 2, 3) })".update
        result <-
          sql"SELECT * FROM array_byte_test WHERE data = ${ Array[Byte](1, 2, 3) }".query[Array[Byte]].to[Option]
      yield result.map(_.mkString(","))).transaction(connectionFixture()),
      Some(Array[Byte](1, 2, 3).mkString(","))
    )
  }

  test("Encoder and Decoder work properly for data of type LocalTime.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE local_time_test (time TIME)".update
        _      <- sql"INSERT INTO local_time_test (time) VALUES (${ LocalTime.of(12, 34, 56) })".update
        result <-
          sql"SELECT * FROM local_time_test WHERE time = ${ LocalTime.of(12, 34, 56) }".query[LocalTime].to[Option]
      yield result).transaction(connectionFixture()),
      Some(LocalTime.of(12, 34, 56))
    )
  }

  test("Encoder and Decoder work properly for data of type LocalDate.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE local_date_test (date DATE)".update
        _      <- sql"INSERT INTO local_date_test (date) VALUES (${ LocalDate.of(2023, 4, 5) })".update
        result <-
          sql"SELECT * FROM local_date_test WHERE date = ${ LocalDate.of(2023, 4, 5) }".query[LocalDate].to[Option]
      yield result).transaction(connectionFixture()),
      Some(LocalDate.of(2023, 4, 5))
    )
  }

  test("Encoder and Decoder work properly for data of type LocalDateTime.") {
    assertIO(
      (for
        _ <- sql"USE codec_test".update
        _ <- sql"CREATE TABLE local_date_time_test (date_time DATETIME)".update
        _ <-
          sql"INSERT INTO local_date_time_test (date_time) VALUES (${ LocalDateTime.of(2023, 4, 5, 12, 34, 56) })".update
        result <-
          sql"SELECT * FROM local_date_time_test WHERE date_time = ${ LocalDateTime.of(2023, 4, 5, 12, 34, 56) }"
            .query[LocalDateTime]
            .to[Option]
      yield result).transaction(connectionFixture()),
      Some(LocalDateTime.of(2023, 4, 5, 12, 34, 56))
    )
  }

  test("Encoder and Decoder work properly for data of type Year.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE year_test (year YEAR)".update
        _      <- sql"INSERT INTO year_test (year) VALUES (${ Year.of(2023) })".update
        result <- sql"SELECT * FROM year_test WHERE year = ${ Year.of(2023) }".query[Year].to[Option]
      yield result).transaction(connectionFixture()),
      Some(Year.of(2023))
    )
  }

  test("Encoder and Decoder work properly for data of type YearMonth.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE year_month_test (ymonth VARCHAR(7))".update
        _      <- sql"INSERT INTO year_month_test (ymonth) VALUES (${ YearMonth.of(2023, 4) })".update
        result <-
          sql"SELECT * FROM year_month_test WHERE ymonth = ${ YearMonth.of(2023, 4) }".query[YearMonth].to[Option]
      yield result).transaction(connectionFixture()),
      Some(YearMonth.of(2023, 4))
    )
  }

  test("Encoder and Decoder work properly for data of type None.type.") {
    assertIO(
      (for
        _      <- sql"USE codec_test".update
        _      <- sql"CREATE TABLE none_test (none INT)".update
        _      <- sql"INSERT INTO none_test (none) VALUES (NULL)".update
        result <- sql"SELECT * FROM none_test WHERE none IS NULL".query[Option[String]].to[Option]
      yield result).transaction(connectionFixture()),
      Some(None)
    )
  }
