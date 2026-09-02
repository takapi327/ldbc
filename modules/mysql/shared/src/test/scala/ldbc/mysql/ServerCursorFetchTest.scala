/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import scala.concurrent.duration.*

import ldbc.sql.ResultSet

import ldbc.fx.concurrentFx
import ldbc.fx.Fx
import ldbc.mysql.*
import ldbc.mysql.syntax.*
import ldbc.net.SSL

class ServerCursorFetchTest extends FTestPlatform:

  // In case of Scala.js, timeout occurs when FetchSize: 1, so it is necessary to extend the time.
  override def munitTimeout: Duration = 60.seconds

  private val datasource = MySQLDataSource
    .build(
      host = TestConfig.host,
      port = TestConfig.port,
      user = TestConfig.user
    )
    .setPassword(TestConfig.password)
    .setDatabase("world")
    .setUseCursorFetch(true)
    .setSSL(SSL.None)
    .setAllowPublicKeyRetrieval(true)

  test("Statement: Query result retrieval using server cursor matches the specified number of results.") {
    assertFx(
      datasource.use { conn =>
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
    assertFx(
      datasource.use { conn =>
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
    assertFx(
      datasource.use { conn =>
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
    assertFx(
      datasource.use { conn =>
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
