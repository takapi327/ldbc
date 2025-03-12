/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.jdbc

import java.time.*
import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.Connection

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var connection: Resource[IO, Connection[IO]] = uninitialized

  @volatile
  var values: String = uninitialized

  @volatile
  var records: List[
    (
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
  ] = List.empty

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("benchmark")
    ds.setUser("ldbc")
    ds.setPassword("password")
    ds.setUseSSL(true)

    val datasource = jdbc.connector.MysqlDataSource[IO](ds)

    connection = Resource.make(datasource.getConnection)(_.close())

    values = (1 to len).map(_ => "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").mkString(",")

    records = (1 to len)
      .map(num =>
        (
          1.toShort,
          1,
          1,
          1,
          1L,
          1.1f,
          1.1,
          BigDecimal.decimal(1.1),
          s"varchar $num",
          s"text $num",
          true,
          LocalDate.now(),
          LocalTime.now(),
          LocalDateTime.now(),
          LocalDateTime.now()
        )
      )
      .toList

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def statement(): Unit =
    connection
      .use { conn =>
        for
          statement <- conn.createStatement()
          _         <- conn.setAutoCommit(false)
          _ <-
            statement.executeUpdate(
              s"INSERT INTO jdbc_statement_test (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15) VALUES ${ records
                  .map {
                    case (v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15) =>
                      s"($v1, $v2, $v3, $v4, $v5, $v6, $v7, $v8, '$v9', '$v10', $v11, '$v12', '$v13', '${ v14.toString
                          .replace("T", " ") }', '${ v15.toString.replace("T", " ") }')"
                  }
                  .mkString(",") }"
            )
          _ <- conn.rollback()
        yield ()
      }
      .unsafeRunSync()

  @Benchmark
  def prepareStatement(): Unit =
    connection
      .use { conn =>
        for
          statement <-
            conn.prepareStatement(
              s"INSERT INTO jdbc_prepare_statement_test (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15) VALUES $values"
            )
          _ <- conn.setAutoCommit(false)
          _ <- records.zipWithIndex.foldLeft(IO.unit) {
                 case (acc, ((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15), index)) =>
                   acc *>
                     statement.setShort(index * 15 + 1, v1) *>
                     statement.setInt(index * 15 + 2, v2) *>
                     statement.setInt(index * 15 + 3, v3) *>
                     statement.setInt(index * 15 + 4, v4) *>
                     statement.setLong(index * 15 + 5, v5) *>
                     statement.setFloat(index * 15 + 6, v6) *>
                     statement.setDouble(index * 15 + 7, v7) *>
                     statement.setBigDecimal(index * 15 + 8, v8.bigDecimal) *>
                     statement.setString(index * 15 + 9, v9) *>
                     statement.setString(index * 15 + 10, v10) *>
                     statement.setBoolean(index * 15 + 11, v11) *>
                     statement.setDate(index * 15 + 12, v12) *>
                     statement.setTime(index * 15 + 13, v13) *>
                     statement.setTimestamp(index * 15 + 14, v14) *>
                     statement.setTimestamp(index * 15 + 15, v15)
               }
          _ <- statement.executeUpdate()
          _ <- conn.rollback()
        yield ()
      }
      .unsafeRunSync()
