/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.doobie

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

import cats.data.NonEmptyList

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import doobie.*
import doobie.implicits.*
import doobie.util.fragments.values

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var xa: Transactor[IO] = _

  @volatile
  var records: NonEmptyList[(Int, String)] = _

  @Setup
  def setupDataSource(): Unit =
    xa = Transactor.after.set(
      Transactor.fromDriverManager[IO](
        "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://127.0.0.1:13306/world",
        "ldbc",
        "password",
        None
      ),
      HC.rollback
    )

    records = NonEmptyList.fromListUnsafe((1 to len).map(num => (num, s"record$num")).toList)

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def insertN: Unit =
    (sql"INSERT INTO test (c1, c2)" ++ values(records)).update.run
      .transact(xa)
      .unsafeRunSync()

  @Benchmark
  def batchN: Unit =
    val sql = "INSERT INTO test (c1, c2) VALUES (?, ?)"
    Update[(Int, String)](sql)
      .updateMany(records)
      .transact(xa)
      .unsafeRunSync()
