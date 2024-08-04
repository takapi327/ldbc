/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.ldbc

import java.util.concurrent.TimeUnit
import java.time.*

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.ResultSet
import ldbc.connector.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  type BenchmarkType = (Long, Short, Int, Int, Int, Long, Float, Double, BigDecimal, String, String, Boolean, LocalDate, LocalTime, LocalDateTime, LocalDateTime)

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

  @Param(Array("4000"))
  var len: Int = uninitialized

  @Benchmark
  def statement: List[BenchmarkType] =
    connection
      .use { conn =>
        for
          // 処理時間の計測を入れる
          statement <- conn.createStatement()
          //start <- IO(System.nanoTime())
          resultSet <- statement.executeQuery(s"SELECT * FROM jdbc_statement_test LIMIT $len")
          //end <- IO(System.nanoTime())
          //_ <- IO.println("============") *> IO(println(s"statement: ${end - start} nanos"))
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
  //        _ <- statement.setInt(1, len)
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
