/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.jdbc

import java.util.concurrent.TimeUnit

import scala.util.Using

import com.mysql.cj.jdbc.MysqlDataSource

import org.openjdk.jmh.annotations.*

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Insert:

  @volatile
  var dataSource: MysqlDataSource = _

  @volatile
  var values: String = _

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

    values = (1 to len).map(_ => "(?, ?)").mkString(",")

    records = (1 to len).map(num => (num, s"record$num")).toList

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def insertN: Unit =
    Using
      .Manager { use =>
        val connection = use(dataSource.getConnection)
        connection.setAutoCommit(false)
        val statement = use(connection.prepareStatement(s"INSERT INTO test (c1, c2) VALUES $values"))
        records.zipWithIndex.foreach {
          case ((id, value), index) =>
            statement.setInt(index * 2 + 1, id)
            statement.setString(index * 2 + 2, value)
        }
        statement.executeUpdate()
        connection.rollback()
      }
      .getOrElse(throw new RuntimeException("Error during database operation"))

  @Benchmark
  def batchN: Unit =
    Using
      .Manager { use =>
        val connection = use(dataSource.getConnection)
        connection.setAutoCommit(false)
        val statement = use(connection.prepareStatement("INSERT INTO test (c1, c2) VALUES (?, ?)"))
        records.foreach {
          case (id, value) =>
            statement.setInt(1, id)
            statement.setString(2, value)
            statement.addBatch()
        }
        statement.executeBatch()
        connection.rollback()
      }
      .getOrElse(throw new RuntimeException("Error during database operation"))
