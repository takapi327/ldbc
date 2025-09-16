/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.pooling.ldbc

import java.time.*
import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized
import scala.concurrent.duration.*

import org.openjdk.jmh.annotations.*

import cats.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.ResultSet

import ldbc.connector.*
import ldbc.connector.syntax.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = Array("-Xms4G", "-Xmx4G", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200"))
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Threads(1)
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
  var datasource: (DataSource[IO], IO[Unit]) = uninitialized

  @volatile
  var close: IO[Unit] = uninitialized

  @Setup(Level.Trial)
  def setupDataSource(): Unit =
    // スレッド数は1,2,4,8,16で検証されるため、最大16スレッドに対応
    val poolConfig = MySQLConfig.default
      .setHost("127.0.0.1")
      .setPort(13306)
      .setUser("ldbc")
      .setPassword("password")
      .setDatabase("benchmark")
      .setMaxConnections(24)
      .setMinConnections(16)
      .setConnectionTimeout(60.seconds)
      .setReadTimeout(60.seconds)
      .setValidationTimeout(10.seconds)
      .setIdleTimeout(30.minutes)
      .setMaxLifetime(60.minutes)
      .setConnectionTestQuery("SELECT 1")
      .setMaintenanceInterval(30.seconds)
      .setAliveBypassWindow(10.seconds)
      .setAdaptiveSizing(true)

    // プール作成前に少し待機して、初期化の安定性を向上
    Thread.sleep(1000)

    datasource = MySQLDataSource.pooling[IO](poolConfig).allocated.unsafeRunSync()

    // プール初期化後も少し待機して、全ての接続が確立されるのを待つ
    Thread.sleep(2000)

  @TearDown(Level.Trial)
  def tearDown(): Unit =
    datasource._2.unsafeRunSync()

  @Param(Array("500", "1000", "1500", "2000"))
  var len: Int = uninitialized

  @Benchmark
  def statement: List[BenchmarkType] =
    datasource._1.getConnection
      .use { conn =>
        for
          statement <- conn.createStatement()
          resultSet <- statement.executeQuery(s"SELECT * FROM jdbc_prepare_statement_test LIMIT $len")
          decoded   <- consume(resultSet)
          _         <- statement.close()
        yield decoded
      }
      .unsafeRunSync()

  @Benchmark
  def prepareStatement: List[BenchmarkType] =
    datasource._1.getConnection
      .use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM jdbc_prepare_statement_test LIMIT ?")
          _         <- statement.setInt(1, len)
          resultSet <- statement.executeQuery()
          decoded   <- consume(resultSet)
          _         <- statement.close()
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
