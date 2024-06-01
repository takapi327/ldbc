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
import ldbc.query.builder.TableQuery
import ldbc.sql.DataSource
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

import benchmark.City

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var dataSource: DataSource[IO] = uninitialized

  @volatile
  var noLog: LogHandler[IO] = uninitialized

  @volatile
  var query: TableQuery[IO, City] = uninitialized

  @Setup
  def setup(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("world")
    ds.setUser("ldbc")
    ds.setPassword("password")
    dataSource = jdbc.connector.MysqlDataSource[IO](ds)

    noLog = _ => IO.unit

    query = TableQuery[IO, City](City.table)

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def selectN: List[(Int, String, String)] =
    given LogHandler[IO] = noLog
    (for
      connection <- dataSource.getConnection
      result <- query
                  .select(city => (city.id, city.name, city.countryCode))
                  .limit(len)
                  .toList
                  .readOnly(connection)
    yield result)
      .unsafeRunSync()

object City:

  val table = Table[City]("city")(
    column("ID", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("Name", CHAR(35).DEFAULT("")),
    column("CountryCode", CHAR(3).DEFAULT("")),
    column("District", CHAR(20).DEFAULT("")),
    column("Population", INT.DEFAULT(0))
  )
