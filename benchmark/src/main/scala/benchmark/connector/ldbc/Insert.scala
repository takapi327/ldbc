/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  given Tracer[IO] = Tracer.noop[IO]

  @volatile
  var connection: Resource[IO, LdbcConnection[IO]] = uninitialized

  @volatile
  var values: String = uninitialized

  @volatile
  var records: List[(Int, String)] = List.empty

  @Setup
  def setup(): Unit =
    connection = Connection[IO](
      host = "127.0.0.1",
      port = 13306,
      user = "ldbc",
      password = Some("password"),
      database = Some("benchmark"),
      ssl = SSL.Trusted
    )

    values = (1 to len).map(_ => "(?, ?)").mkString(",")

    records = (1 to len).map(num => (num, s"record$num")).toList

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def insertN(): Unit =
    connection.use { conn =>
      for
        statement <- conn.prepareStatement(s"INSERT INTO insert_test (c1, c2) VALUES $values")
        _ <- records.zipWithIndex.foldLeft(IO.unit) {
          case (acc, ((id, value), index)) =>
            acc *>
              statement.setInt(index * 2 + 1, id) *>
              statement.setString(index * 2 + 2, value)
        }
        _ <- statement.executeUpdate()
      yield ()
    }.unsafeRunSync()
