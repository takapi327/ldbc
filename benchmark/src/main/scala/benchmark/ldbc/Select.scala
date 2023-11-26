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

import benchmark.City

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var dataSource: MysqlDataSource = _

  @volatile
  var noLog: LogHandler[IO] = _

  @volatile
  var query: TableQuery[IO, City] = _

  @Setup
  def setup(): Unit =
    dataSource = new MysqlDataSource()
    dataSource.setServerName("127.0.0.1")
    dataSource.setPortNumber(13306)
    dataSource.setDatabaseName("world")
    dataSource.setUser("ldbc")
    dataSource.setPassword("password")

    noLog = _ => IO.unit

    query = TableQuery[IO, City](City.table)

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def selectN: List[(Int, String, String)] =
    query
      .select(city => (city.id, city.name, city.countryCode))
      .limit(len)
      .query
      .toList(using noLog)
      .readOnly
      .run(dataSource)
      .unsafeRunSync()

object City:

  val table = Table[City]("city")(
    column("ID", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("Name", CHAR(35).DEFAULT("")),
    column("CountryCode", CHAR(3).DEFAULT("")),
    column("District", CHAR(20).DEFAULT("")),
    column("Population", INT.DEFAULT(0))
  )
