/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.jdbc

import java.time.*
import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import com.mysql.cj.jdbc.MysqlDataSource

import cats.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.*

import ldbc.connector.syntax.*

import ldbc.DataSource
import jdbc.connector.*

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

  @volatile
  var datasource: DataSource[IO] = uninitialized

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("benchmark")
    ds.setUser("ldbc")
    ds.setPassword("password")
    ds.setUseSSL(true)

    datasource = MySQLDataSource.fromDataSource[IO](ds, ExecutionContexts.synchronous)

  @Param(Array("100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def statement: List[BenchmarkType] =
    datasource
      .createConnection()
      .use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery(s"SELECT * FROM jdbc_statement_test LIMIT $len")
          decoded   <- consume(resultSet)
        yield decoded
      }
      .unsafeRunSync()

  @Benchmark
  def prepareStatement: List[BenchmarkType] =
    datasource
      .createConnection()
      .use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM jdbc_prepare_statement_test LIMIT ?")
          _         <- statement.setInt(1, len)
          resultSet <- statement.executeQuery()
          decoded   <- consume(resultSet)
        yield decoded
      }
      .unsafeRunSync()

  private def consume(resultSet: ResultSet[IO]): IO[List[BenchmarkType]] =
    resultSet.whileM[List, BenchmarkType] {
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
      yield (c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16)
    }
