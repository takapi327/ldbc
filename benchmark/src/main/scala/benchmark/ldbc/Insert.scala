/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.core.*
import ldbc.query.builder.TableQuery
import ldbc.dsl.io.*
import ldbc.dsl.logging.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var dataSource: MysqlDataSource = _

  @volatile
  var noLog: LogHandler[IO] = _

  @volatile
  var query: TableQuery[IO, Test] = _

  @volatile
  var records: List[(Int, String)] = List.empty

  @Setup
  def setupDataSource(): Unit =
    dataSource = new MysqlDataSource()
    dataSource.setServerName("127.0.0.1")
    dataSource.setPortNumber(13306)
    dataSource.setDatabaseName("world")
    dataSource.setUser("ldbc")
    dataSource.setPassword("password")

    records = (1 to len).map(num => (num, s"record$num")).toList

    noLog = _ => IO.unit

    query = TableQuery[IO, Test](Test.table)

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def insertN: Unit =
    given LogHandler[IO] = noLog
    query
      .insertInto(test => (test.c1, test.c2))
      .values(records)
      .update
      .rollback(dataSource)
      .unsafeRunSync()

case class Test(id: Option[Int], c1: Int, c2: String)
object Test:

  val table = Table[Test]("test")(
    column("id", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("c1", INT),
    column("c2", VARCHAR(255))
  )
