/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.wrapper.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.Connection
import ldbc.query.builder.Table
import ldbc.query.builder.syntax.io.*

import benchmark.City

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var connection: Resource[IO, Connection[IO]] = uninitialized

  @volatile
  var query: Table[City] = uninitialized

  @Setup
  def setup(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("world")
    ds.setUser("ldbc")
    ds.setPassword("password")

    val datasource = jdbc.connector.MysqlDataSource[IO](ds)

    connection = Resource.make(datasource.getConnection)(_.close())

    query = Table[City]("city")

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def querySelectN: List[(Int, String, String)] =
    connection.use { conn =>
      query
        .select(city => (city.id, city.name, city.countryCode))
        .limit(len)
        .query
        .to[List]
        .readOnly(conn)
    }.unsafeRunSync()

  @Benchmark
  def dslSelectN: List[(Int, String, String)] =
    connection.use { conn =>
      sql"SELECT ID, Name, CountryCode FROM city LIMIT $len"
        .query[(Int, String, String)]
        .to[List]
        .readOnly(conn)
    }.unsafeRunSync()
