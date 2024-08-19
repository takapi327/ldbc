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

import cats.data.NonEmptyList

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.Connection
import ldbc.dsl.SQL
import ldbc.query.builder.Table
import ldbc.query.builder.syntax.io.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var connection: Resource[IO, Connection[IO]] = uninitialized

  @volatile
  var query: Table[Test] = uninitialized

  @volatile
  var queryRecords: List[(Int, String)] = List.empty

  @volatile
  var dslRecords: SQL = uninitialized

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("benchmark")
    ds.setUser("ldbc")
    ds.setPassword("password")

    val datasource = jdbc.connector.MysqlDataSource[IO](ds)

    connection = Resource.make(datasource.getConnection)(_.close())

    queryRecords = (1 to len).map(num => (num, s"record$num")).toList
    dslRecords = comma(NonEmptyList.fromListUnsafe((1 to len).map(num => parentheses(p"$num, ${ "record" + num }")).toList))

    query = Table[Test]("ldbc_wrapper_query_test")

  @Param(Array("10", "100", "1000", "2000"))
  var len: Int = uninitialized

  @Benchmark
  def queryInsertN: Unit =
    connection
      .use { conn =>
        query
          .insertInto(test => (test.c1, test.c2))
          .values(queryRecords)
          .update
          .commit(conn)
      }
      .unsafeRunSync()

  @Benchmark
  def dslInsertN: Unit =
    connection
      .use { conn =>
        (sql"INSERT INTO `ldbc_wrapper_dsl_test` (`c1`, `c2`) VALUES" ++ dslRecords).update
          .commit(conn)
      }
      .unsafeRunSync()

case class Test(c0: Option[Int], c1: Int, c2: String) derives Table
