/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.slick

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.compiletime.uninitialized

import slick.jdbc.MySQLProfile.api.*

import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var db: Database = uninitialized

  @volatile
  var query: TableQuery[TestTable] = uninitialized

  @volatile
  var records: List[Test] = List.empty

  @Setup
  def setupDataSource(): Unit =
    db = Database.forURL(
      url      = "jdbc:mysql://127.0.0.1:13306/world",
      user     = "ldbc",
      password = "password",
      driver   = "com.mysql.cj.jdbc.Driver"
    )

    query = TableQuery[TestTable]

    records = (1 to len).map(num => Test(None, num, s"record$num")).toList

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def insertN: Unit =
    Await.result(
      db.run(query ++= records),
      Duration.Inf
    )

  // @Benchmark
  // def batchN: Unit =
  //  Await.result(
  //    db.run(query ++= records),
  //    Duration.Inf
  //  )

case class Test(id: Option[Int], c1: Int, c2: String)
class TestTable(tag: Tag) extends Table[Test](tag, "test"):
  def id = column[Int]("id", O.AutoInc, O.PrimaryKey)
  def c1 = column[Int]("c1")
  def c2 = column[String]("c2")

  def * = (id, c1, c2).mapTo[Test]
