/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.connector.jdbc

import java.util.concurrent.TimeUnit

import scala.compiletime.uninitialized

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.Connection

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Batch:

  @volatile
  var connection: Resource[IO, Connection[IO]] = uninitialized

  @volatile
  var values: String = uninitialized

  @volatile
  var records: List[(Int, String)] = List.empty

  @Setup
  def setupDataSource(): Unit =
    val ds = new MysqlDataSource()
    ds.setServerName("127.0.0.1")
    ds.setPortNumber(13306)
    ds.setDatabaseName("benchmark")
    ds.setUser("ldbc")
    ds.setPassword("password")
    ds.setRewriteBatchedStatements(true)

    val datasource = jdbc.connector.MysqlDataSource[IO](ds)

    connection = Resource.make(datasource.getConnection)(_.close())

    values = (1 to len).map(_ => "(?, ?)").mkString(",")

    records = (1 to len).map(num => (num, s"record$num")).toList

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def statement(): Unit =
    connection
      .use { conn =>
        for
          statement <- conn.createStatement()
          _ <- records.foldLeft(IO.unit) {
                 case (acc, (id, value)) =>
                   acc *>
                     statement.addBatch(s"INSERT INTO jdbc_statement_test (c1, c2) VALUES ${ records
                         .map { case (v1, v2) => s"($v1, '$v2')" }
                         .mkString(", ") }")
               }
          _ <- statement.executeBatch()
        yield ()
      }
      .unsafeRunSync()

  @Benchmark
  def prepareStatement(): Unit =
    connection
      .use { conn =>
        for
          statement <- conn.prepareStatement(s"INSERT INTO jdbc_prepare_statement_test (c1, c2) VALUES (?, ?)")
          _ <- records.foldLeft(IO.unit) {
                 case (acc, (id, value)) =>
                   acc *>
                     statement.setInt(1, id) *>
                     statement.setString(2, value) *>
                     statement.addBatch()
               }
          _ <- statement.executeBatch()
        yield ()
      }
      .unsafeRunSync()