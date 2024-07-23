/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import cats.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

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

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def selectN: List[(Long, String)] =
    connection
      .use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM insert_test LIMIT ?")
          _         <- statement.setInt(1, len)
          resultSet <- statement.executeQuery()
          records <- Monad[IO].whileM[List, (Long, String)](resultSet.next()) {
                       for
                         c1 <- resultSet.getLong(1)
                         c2 <- resultSet.getString(2)
                       yield (c1, c2)
                     }
        yield records
      }
      .unsafeRunSync()
