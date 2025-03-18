/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.wrapper.ldbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*

import com.mysql.cj.jdbc.MysqlDataSource

import cats.data.NonEmptyList

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.*

import ldbc.dsl.SQL

import ldbc.query.builder.syntax.*
import ldbc.query.builder.Table

import jdbc.connector.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var provider: Provider[IO] = uninitialized

  @volatile
  var query: TableQuery[Test] = uninitialized

  @volatile
  var records: NonEmptyList[(Int, String)] = uninitialized

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

    provider = MySQLProvider.fromDataSource(ds, ExecutionContexts.synchronous)

    records = NonEmptyList.fromListUnsafe((1 to len).map(num => (num, s"record$num")).toList)

    query = TableQuery[Test]

  @Param(Array("10"))
  var len: Int = uninitialized

  @Benchmark
  def queryInsertN: Unit =
    provider
      .use { conn =>
        query
          .insertInto(test => test.c1 *: test.c2)
          .values(records)
          .update
          .commit(conn)
      }
      .unsafeRunSync()

  @Benchmark
  def dslInsertN: Unit =
    provider
      .use { conn =>
        (sql"INSERT INTO `ldbc_wrapper_dsl_test` (`c1`, `c2`) " ++ values(records)).update
          .commit(conn)
      }
      .unsafeRunSync()

case class Test(c0: Option[Int], c1: Int, c2: String)
object Test:
  given Table[Test] = Table.derived[Test]("ldbc_wrapper_query_test")
