/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package benchmark.ldbc

import java.util.concurrent.TimeUnit

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.dsl.io.*
import ldbc.dsl.logging.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class PlainSelect:

  @volatile
  var dataSource: MysqlDataSource = _

  @volatile
  var noLog: LogHandler[IO] = _

  @Setup
  def setup(): Unit =
    dataSource = new MysqlDataSource()
    dataSource.setServerName("127.0.0.1")
    dataSource.setPortNumber(13306)
    dataSource.setDatabaseName("world")
    dataSource.setUser("ldbc")
    dataSource.setPassword("password")

    noLog = _ => IO.unit

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def selectN: List[(Int, String, String)] =
    sql"SELECT ID, Name, CountryCode FROM city LIMIT $len"
      .query[(Int, String, String)]
      .toList(using noLog)
      .readOnly
      .run(dataSource)
      .unsafeRunSync()
