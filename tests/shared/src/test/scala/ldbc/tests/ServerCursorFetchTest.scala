/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.duration.*

import cats.effect.*

import munit.*

import ldbc.sql.*

import ldbc.connector.*
import ldbc.connector.syntax.*

import ldbc.DataSource

class LdbcServerCursorFetchTest extends ServerCursorFetchTest:

  // In case of Scala.js, timeout occurs when FetchSize: 1, so it is necessary to extend the time.
  override def munitIOTimeout: Duration = 80.seconds

  override def datasource: DataSource[IO] = MySQLDataSource
    .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
    .setPassword(MySQLTestConfig.password)
    .setDatabase("world")
    .setSSL(SSL.Trusted)
    .setUseCursorFetch(true)

trait ServerCursorFetchTest extends CatsEffectSuite:

  protected val host:     String = MySQLTestConfig.host
  protected val port:     Int    = MySQLTestConfig.port
  protected val user:     String = MySQLTestConfig.user
  protected val password: String = MySQLTestConfig.password
  protected val database: String = "world"

  def datasource: DataSource[IO]

  test("Statement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      datasource.getConnection.use { conn =>
        for
          statement <- conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
          _         <- statement.setFetchSize(1)
          resultSet <- statement.executeQuery("SELECT * FROM `city`")
          result    <- resultSet.whileM[List, String](
                      resultSet.getString("Name")
                    )
        yield result.length
      },
      4079
    )
  }

  test("Statement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      datasource.getConnection.use { conn =>
        for
          statement <- conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
          _         <- statement.setFetchSize(5)
          resultSet <- statement.executeQuery("SELECT * FROM `city`")
          result    <- resultSet.whileM[List, String](
                      resultSet.getString("Name")
                    )
        yield result.length
      },
      4079
    )
  }

  test("PreparedStatement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM `city`")
          _         <- statement.setFetchSize(1)
          resultSet <- statement.executeQuery()
          result    <- resultSet.whileM[List, String](
                      resultSet.getString("Name")
                    )
        yield result.length
      },
      4079
    )
  }

  test("PreparedStatement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      datasource.getConnection.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM `city`")
          _         <- statement.setFetchSize(5)
          resultSet <- statement.executeQuery()
          result    <- resultSet.whileM[List, String](
                      resultSet.getString("Name")
                    )
        yield result.length
      },
      4079
    )
  }
