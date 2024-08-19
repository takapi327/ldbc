/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.wrapper.doobie

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.data.NonEmptyList

import cats.effect.*
import cats.effect.unsafe.implicits.global

import doobie.*
import doobie.implicits.*
import doobie.util.fragments.values

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var transactor: Resource[IO, Transactor[IO]] = uninitialized

  @volatile
  var records: NonEmptyList[(Int, String)] = uninitialized

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("benchmark")
    ds.setUser("ldbc")
    ds.setPassword("password")

    transactor = ExecutionContexts.fixedThreadPool[IO](1).map(ce => Transactor.fromDataSource[IO](ds, ce))

    records = NonEmptyList.fromListUnsafe((1 to len).map(num => (num, s"record$num")).toList)

  @Param(Array("10", "100", "1000", "2000"))
  var len: Int = uninitialized

  @Benchmark
  def insertN: Unit =
    transactor
      .use { xa =>
        (sql"INSERT INTO `doobie_wrapper_test` (c1, c2)" ++ values(records)).update.run
          .transact(xa)
      }
      .unsafeRunSync()

  // @Benchmark
  // def batchN: Unit =
  //  transactor
  //    .use { xa =>
  //      val sql = "INSERT INTO test (c1, c2) VALUES (?, ?)"
  //      Update[(Int, String)](sql)
  //        .updateMany(records)
  //        .transact(xa)
  //    }
  //    .unsafeRunSync()
