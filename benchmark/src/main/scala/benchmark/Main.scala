/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark

import java.time.*

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import cats.effect.*

import ldbc.sql.ResultSet
import ldbc.connector.*

object Main extends IOApp.Simple:

  type BenchmarkType = (
    Long,
      Short,
      Int,
      Int,
      Int,
      Long,
      Float,
      Double,
      BigDecimal,
      String,
      String,
      Boolean,
      LocalDate,
      LocalTime,
      LocalDateTime,
      LocalDateTime
    )
  
  val provider = ConnectionProvider
    .default[IO]("127.0.0.1", 13306, "ldbc", "password", "benchmark")
    .setSSL(SSL.Trusted)

  override def run: IO[Unit] =
    provider
      .use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery(s"SELECT * FROM jdbc_prepare_statement_test LIMIT ${ 1000 }")
          _ <- consume(resultSet)
        yield ()
      }

  private def consume(resultSet: ResultSet[IO]): IO[List[BenchmarkType]] = {
    val builder = mutable.ListBuffer.newBuilder[BenchmarkType]

    def loop(acc: mutable.Builder[BenchmarkType, ListBuffer[BenchmarkType]]): IO[List[BenchmarkType]] =
      resultSet.next().flatMap { hasNext =>
        if (hasNext) {
          for
            c1  <- resultSet.getLong(1)
            c2  <- resultSet.getShort(2)
            c3  <- resultSet.getInt(3)
            c4  <- resultSet.getInt(4)
            c5  <- resultSet.getInt(5)
            c6  <- resultSet.getLong(6)
            c7  <- resultSet.getFloat(7)
            c8  <- resultSet.getDouble(8)
            c9  <- resultSet.getBigDecimal(9)
            c10 <- resultSet.getString(10)
            c11 <- resultSet.getString(11)
            c12 <- resultSet.getBoolean(12)
            c13 <- resultSet.getDate(13)
            c14 <- resultSet.getTime(14)
            c15 <- resultSet.getTimestamp(15)
            c16 <- resultSet.getTimestamp(16)
            result <- loop(acc += ((c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16)))
          yield result
        } else {
          IO.pure(acc.result().toList)
        }
      }

    loop(builder)
  }
