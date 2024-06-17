/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.lepus

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.sql.DataSource
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.Table
import ldbc.query.builder.syntax.io.*

import benchmark.City

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var dataSource: DataSource[IO] = uninitialized

  @volatile
  var noLog: LogHandler[IO] = uninitialized

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

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def selectN: List[(Int, String, String)] =
    given LogHandler[IO] = noLog
    (for
      connection <- dataSource.getConnection
      result <- Table[City]
                  .select(city => (city.id, city.name, city.countryCode))
                  .limit(len)
                  .query
                  .to[List]
                  .readOnly(connection)
    yield result)
      .unsafeRunSync()
