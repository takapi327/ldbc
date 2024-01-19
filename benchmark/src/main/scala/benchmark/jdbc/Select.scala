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
class Select:

  @volatile
  var dataSource: MysqlDataSource = _

  @Setup
  def setupDataSource(): Unit =
    dataSource = new MysqlDataSource()
    dataSource.setServerName("127.0.0.1")
    dataSource.setPortNumber(13306)
    dataSource.setDatabaseName("world")
    dataSource.setUser("ldbc")
    dataSource.setPassword("password")

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = _

  @Benchmark
  def selectN: List[(Int, String, String)] =
    Using
      .Manager { use =>
        val connection = use(dataSource.getConnection)
        val statement  = use(connection.prepareStatement("SELECT ID, Name, CountryCode FROM city LIMIT ?"))
        statement.setInt(1, len)
        val resultSet = use(statement.executeQuery())
        val records   = List.newBuilder[(Int, String, String)]
        while resultSet.next() do {
          val code   = resultSet.getInt(1)
          val name   = resultSet.getString(2)
          val region = resultSet.getString(3)
          records += ((code, name, region))
        }
        records.result()
      }
      .getOrElse(throw new RuntimeException("Error during database operation"))
