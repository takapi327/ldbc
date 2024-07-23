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

import cats.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import ldbc.sql.Connection

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
class Select:

  @volatile
  var connection: Resource[IO, Connection[IO]] = uninitialized

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

  @Param(Array("10", "100", "1000", "2000", "4000"))
  var len: Int = uninitialized

  @Benchmark
  def selectN: List[(Long, String)] =
    connection
      .use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM insert_test LIMIT ?")
          _         <- statement.setInt(1, len)
          resultSet <- statement.executeQuery()
          records <- Monad[IO].whileM[List, (Long, String)](resultSet.next()) {
                       for
                         c1 <- resultSet.getLong(1)
                         c2 <- resultSet.getString(2)
                       yield (c1, c2)
                     }
        yield records
      }
      .unsafeRunSync()
