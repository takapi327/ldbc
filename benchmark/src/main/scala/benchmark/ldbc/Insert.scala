/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.core.*
import ldbc.sql.DataSource
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery
import ldbc.query.builder.syntax.io.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var dataSource: DataSource[IO] = uninitialized

  @volatile
  var noLog: LogHandler[IO] = uninitialized

  @volatile
  var query: TableQuery[Test] = uninitialized

  @volatile
  var records: List[(Int, String)] = List.empty

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("world")
    ds.setUser("ldbc")
    ds.setPassword("password")
    dataSource = jdbc.connector.MysqlDataSource[IO](ds)

    records = (1 to len).map(num => (num, s"record$num")).toList

    noLog = _ => IO.unit

    query = TableQuery[Test](Test.table)

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def insertN: Unit =
    given LogHandler[IO] = noLog
    (for
      connection <- dataSource.getConnection
      result <- query
                  .insertInto(test => (test.c1, test.c2))
                  .values(records)
                  .update
                  .rollback(connection)
    yield result).unsafeRunSync()

case class Test(id: Option[Int], c1: Int, c2: String)
object Test:

  val table = Table[Test]("test")(
    column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("c1", INT),
    column("c2", VARCHAR(255))
  )
