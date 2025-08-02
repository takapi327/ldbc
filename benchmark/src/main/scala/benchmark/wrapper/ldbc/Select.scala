/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.wrapper.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.query.builder.*

import jdbc.connector.*

import benchmark.City

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var connector: Connector[IO] = uninitialized

  @volatile
  var query: TableQuery[City] = uninitialized

  @Setup
  def setup(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("world")
    ds.setUser("ldbc")
    ds.setPassword("password")

    connector = Connector.fromDataSource(MySQLDataSource.fromDataSource(ds, ExecutionContexts.synchronous))

    query = TableQuery[City]

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def querySelectN: List[(Int, String, String)] =
    query
      .select(city => city.id *: city.name *: city.countryCode)
      .limit(len)
      .query
      .to[List]
      .readOnly(connector)
      .unsafeRunSync()

  @Benchmark
  def dslSelectN: List[(Int, String, String)] =
    sql"SELECT ID, Name, CountryCode FROM city LIMIT $len"
      .query[(Int, String, String)]
      .to[List]
      .readOnly(connector)
      .unsafeRunSync()
