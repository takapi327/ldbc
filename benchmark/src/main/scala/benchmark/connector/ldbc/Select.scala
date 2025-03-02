/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.ldbc

import java.time.*
import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import cats.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.ResultSet

import ldbc.connector.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

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

  given Tracer[IO] = Tracer.noop[IO]

  @volatile
  var connection: Resource[IO, LdbcConnection[IO]] = uninitialized

  @Setup
  def setupDataSource(): Unit =
    connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("benchmark"),
      ssl      = SSL.Trusted
    )

  @Param(Array("100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def statement: List[BenchmarkType] =
    connection
      .use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery(s"SELECT * FROM jdbc_statement_test LIMIT $len")
        yield consume(resultSet)
      }
      .unsafeRunSync()

  @Benchmark
  def prepareStatement: List[BenchmarkType] =
    connection
      .use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM jdbc_prepare_statement_test LIMIT ?")
          _         <- statement.setInt(1, len)
          resultSet <- statement.executeQuery()
        yield consume(resultSet)
      }
      .unsafeRunSync()

  private def consume(resultSet: ResultSet): List[BenchmarkType] =
    val builder = List.newBuilder[BenchmarkType]
    while resultSet.next() do
      val c1  = resultSet.getLong(1)
      val c2  = resultSet.getShort(2)
      val c3  = resultSet.getInt(3)
      val c4  = resultSet.getInt(4)
      val c5  = resultSet.getInt(5)
      val c6  = resultSet.getLong(6)
      val c7  = resultSet.getFloat(7)
      val c8  = resultSet.getDouble(8)
      val c9  = resultSet.getBigDecimal(9)
      val c10 = resultSet.getString(10)
      val c11 = resultSet.getString(11)
      val c12 = resultSet.getBoolean(12)
      val c13 = resultSet.getDate(13)
      val c14 = resultSet.getTime(14)
      val c15 = resultSet.getTimestamp(15)
      val c16 = resultSet.getTimestamp(16)

      builder += ((c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16))

    builder.result()
