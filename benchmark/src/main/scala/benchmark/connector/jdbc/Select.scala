/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.jdbc

import java.util.concurrent.TimeUnit
import java.time.*

import scala.compiletime.uninitialized

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.{Connection, ResultSet}

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  type BenchmarkType = (Long, Short, Int, Int, Int, Long, Float, Double, BigDecimal, String, String, Boolean, LocalDate, LocalTime, LocalDateTime, LocalDateTime)

  @volatile
  var connection: Resource[IO, Connection[IO]] = uninitialized

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("benchmark")
    ds.setUser("ldbc")
    ds.setPassword("password")

    val datasource = jdbc.connector.MysqlDataSource[IO](ds)

    connection = Resource.make(datasource.getConnection)(_.close())

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def statement: List[BenchmarkType] =
    connection
      .use { conn =>
        for
          // 処理時間の計測を入れる
          statement <- conn.createStatement()
          start <- IO(System.nanoTime())
          resultSet <- statement.executeQuery(s"SELECT * FROM jdbc_statement_test LIMIT $len")
          end <- IO(System.nanoTime())
          _ <- IO.println("============") *> IO(println(s"statement: ${end - start} nanos"))
          records <- consume(resultSet)
        yield records
      }
      .unsafeRunSync()

  //@Benchmark
  //def prepareStatement: List[BenchmarkType] =
  //  connection
  //    .use { conn =>
  //      for
  //        statement <- conn.prepareStatement("SELECT * FROM jdbc_prepare_statement_test LIMIT ?")
  //        _         <- statement.setInt(1, len)
  //        resultSet <- statement.executeQuery()
  //        records <- consume(resultSet)
  //      yield records
  //    }
  //    .unsafeRunSync()

  private def consume(resultSet: ResultSet[IO]): IO[List[BenchmarkType]] =
    def loop(acc: Vector[BenchmarkType]): IO[Vector[BenchmarkType]] =
      resultSet.next().flatMap {
        case false => Monad[IO].pure(acc)
        case true =>
          for
            c1 <- resultSet.getLong(1)
            c2 <- resultSet.getShort(2)
            c3 <- resultSet.getInt(3)
            c4 <- resultSet.getInt(4)
            c5 <- resultSet.getInt(5)
            c6 <- resultSet.getLong(6)
            c7 <- resultSet.getFloat(7)
            c8 <- resultSet.getDouble(8)
            c9 <- resultSet.getBigDecimal(9)
            c10 <- resultSet.getString(10)
            c11 <- resultSet.getString(11)
            c12 <- resultSet.getBoolean(12)
            c13 <- resultSet.getDate(13)
            c14 <- resultSet.getTime(14)
            c15 <- resultSet.getTimestamp(15)
            c16 <- resultSet.getTimestamp(16)
          yield
            acc <+> Vector((c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16))
      }

    loop(Vector.empty).map(_.toList)
