/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import java.time.*

import com.mysql.cj.jdbc.MysqlDataSource

import cats.syntax.all.*

import cats.effect.*

import munit.*

import ldbc.sql.*

import ldbc.dsl.io.*

import ldbc.connector.{ MySQLProvider as LdbcProvider, * }

import jdbc.connector.{ MySQLProvider as JdbcProvider, * }

class LdbcCodecTest extends CodecTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: Provider[IO] =
    LdbcProvider
      .default[IO]("127.0.0.1", 13306, "ldbc", "password", "world")
      .setSSL(SSL.Trusted)

class JdbcCodecTest extends CodecTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: Provider[IO] =
    JdbcProvider.fromDataSource(ds, ExecutionContexts.synchronous)

trait CodecTest extends CatsEffectSuite:

  def prefix:     "jdbc" | "ldbc"
  def connection: Provider[IO]

  override def beforeAll(): Unit =
    connection
      .use { conn =>
        sql"CREATE DATABASE IF NOT EXISTS codec_test".update.commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  override def afterAll(): Unit =
    connection
      .use { conn =>
        sql"DROP DATABASE IF EXISTS codec_test".update.commit(conn) *> IO.unit
      }
      .unsafeRunSync()

  test("Encoder and Decoder work properly for data of type Boolean.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE boolean_test (flag BOOLEAN)".update
            _      <- sql"INSERT INTO boolean_test (flag) VALUES (${ true })".update
            result <- sql"SELECT * FROM boolean_test WHERE flag = ${ true }".query[Boolean].to[Option]
          yield result).transaction(conn)
      },
      Some(true)
    )
  }

  test("Encoder and Decoder work properly for data of type Byte.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE byte_test (id TINYINT)".update
            _      <- sql"INSERT INTO byte_test (id) VALUES (${ 1.toByte })".update
            result <- sql"SELECT * FROM byte_test WHERE id = ${ 1.toByte }".query[Byte].to[Option]
          yield result).transaction(conn)
      },
      Some(1.toByte)
    )
  }

  test("Encoder and Decoder work properly for data of type Short.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE short_test (id SMALLINT)".update
            _      <- sql"INSERT INTO short_test (id) VALUES (${ 1.toShort })".update
            result <- sql"SELECT * FROM short_test WHERE id = ${ 1.toShort }".query[Short].to[Option]
          yield result).transaction(conn)
      },
      Some(1.toShort)
    )
  }

  test("Encoder and Decoder work properly for data of type Int.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE int_test (id INT)".update
            _      <- sql"INSERT INTO int_test (id) VALUES (1)".update
            result <- sql"SELECT * FROM int_test WHERE id = ${ 1 }".query[Int].to[Option]
          yield result).transaction(conn)
      },
      Some(1)
    )
  }

  test("Encoder and Decoder work properly for data of type Long.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE long_test (id BIGINT)".update
            _      <- sql"INSERT INTO long_test (id) VALUES (1)".update
            result <- sql"SELECT * FROM long_test WHERE id = ${ 1 }".query[Long].to[Option]
          yield result).transaction(conn)
      },
      Some(1L)
    )
  }

  test("Encoder and Decoder work properly for data of type Float.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE float_test (id FLOAT)".update
            _      <- sql"INSERT INTO float_test (id) VALUES (1.0)".update
            result <- sql"SELECT * FROM float_test WHERE id = ${ 1.0f }".query[Float].to[Option]
          yield result).transaction(conn)
      },
      Some(1.0f)
    )
  }

  test("Encoder and Decoder work properly for data of type Double.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE double_test (id DOUBLE)".update
            _      <- sql"INSERT INTO double_test (id) VALUES (1.0)".update
            result <- sql"SELECT * FROM double_test WHERE id = ${ 1.0 }".query[Double].to[Option]
          yield result).transaction(conn)
      },
      Some(1.0)
    )
  }

  test("Encoder and Decoder work properly for data of type BigDecimal.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE bigdecimal_test (id DECIMAL(10, 2))".update
            _      <- sql"INSERT INTO bigdecimal_test (id) VALUES (1.0)".update
            result <- sql"SELECT * FROM bigdecimal_test WHERE id = ${ BigDecimal(1.0) }".query[BigDecimal].to[Option]
          yield result).transaction(conn)
      },
      Some(BigDecimal(1.0))
    )
  }

  test("Encoder and Decoder work properly for data of type String.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE string_test (name VARCHAR(255))".update
            _      <- sql"INSERT INTO string_test (name) VALUES ('Takahiko')".update
            result <- sql"SELECT * FROM string_test WHERE name = ${ "Takahiko" }".query[String].to[Option]
          yield result).transaction(conn)
      },
      Some("Takahiko")
    )
  }

  test("Encoder and Decoder work properly for data of type Array[Byte].") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _ <- sql"CREATE TABLE array_byte_test (data BLOB)".update
            _ <- sql"INSERT INTO array_byte_test (data) VALUES (${ Array[Byte](1, 2, 3) })".update
            result <-
              sql"SELECT * FROM array_byte_test WHERE data = ${ Array[Byte](1, 2, 3) }".query[Array[Byte]].to[Option]
          yield result.map(_.mkString(","))).transaction(conn)
      },
      Some(Array[Byte](1, 2, 3).mkString(","))
    )
  }

  test("Encoder and Decoder work properly for data of type LocalTime.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _ <- sql"CREATE TABLE local_time_test (time TIME)".update
            _ <- sql"INSERT INTO local_time_test (time) VALUES (${ LocalTime.of(12, 34, 56) })".update
            result <-
              sql"SELECT * FROM local_time_test WHERE time = ${ LocalTime.of(12, 34, 56) }".query[LocalTime].to[Option]
          yield result).transaction(conn)
      },
      Some(LocalTime.of(12, 34, 56))
    )
  }

  test("Encoder and Decoder work properly for data of type LocalDate.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _ <- sql"CREATE TABLE local_date_test (date DATE)".update
            _ <- sql"INSERT INTO local_date_test (date) VALUES (${ LocalDate.of(2023, 4, 5) })".update
            result <-
              sql"SELECT * FROM local_date_test WHERE date = ${ LocalDate.of(2023, 4, 5) }".query[LocalDate].to[Option]
          yield result).transaction(conn)
      },
      Some(LocalDate.of(2023, 4, 5))
    )
  }

  test("Encoder and Decoder work properly for data of type LocalDateTime.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _ <- sql"CREATE TABLE local_date_time_test (date_time DATETIME)".update
            _ <-
              sql"INSERT INTO local_date_time_test (date_time) VALUES (${ LocalDateTime.of(2023, 4, 5, 12, 34, 56) })".update
            result <-
              sql"SELECT * FROM local_date_time_test WHERE date_time = ${ LocalDateTime.of(2023, 4, 5, 12, 34, 56) }"
                .query[LocalDateTime]
                .to[Option]
          yield result).transaction(conn)
      },
      Some(LocalDateTime.of(2023, 4, 5, 12, 34, 56))
    )
  }

  test("Encoder and Decoder work properly for data of type Year.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE year_test (year YEAR)".update
            _      <- sql"INSERT INTO year_test (year) VALUES (${ Year.of(2023) })".update
            result <- sql"SELECT * FROM year_test WHERE year = ${ Year.of(2023) }".query[Year].to[Option]
          yield result).transaction(conn)
      },
      Some(Year.of(2023))
    )
  }

  test("Encoder and Decoder work properly for data of type YearMonth.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _ <- sql"CREATE TABLE year_month_test (ymonth VARCHAR(7))".update
            _ <- sql"INSERT INTO year_month_test (ymonth) VALUES (${ YearMonth.of(2023, 4) })".update
            result <-
              sql"SELECT * FROM year_month_test WHERE ymonth = ${ YearMonth.of(2023, 4) }".query[YearMonth].to[Option]
          yield result).transaction(conn)
      },
      Some(YearMonth.of(2023, 4))
    )
  }

  test("Encoder and Decoder work properly for data of type None.type.") {
    assertIO(
      connection.use { conn =>
        conn.setCatalog("codec_test") *>
          (for
            _      <- sql"CREATE TABLE none_test (none INT)".update
            _      <- sql"INSERT INTO none_test (none) VALUES (NULL)".update
            result <- sql"SELECT * FROM none_test WHERE none IS NULL".query[Option[String]].to[Option]
          yield result).transaction(conn)
      },
      Some(None)
    )
  }
