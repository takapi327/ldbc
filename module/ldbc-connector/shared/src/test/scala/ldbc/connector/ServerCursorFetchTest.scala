/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import ldbc.sql.ResultSet
import ldbc.connector.*
import ldbc.connector.syntax.*

class ServerCursorFetchTest extends FTestPlatform:
  
  private val provider = ConnectionProvider.default[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = "password",
    database = "world",
  )
    .setUseCursorFetch(true)
    .setSSL(SSL.None)
    .setAllowPublicKeyRetrieval(true)
  
  test("Statement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      provider.use { conn =>
        for
          statement <- conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
          _         <- statement.setFetchSize(1)
          resultSet <- statement.executeQuery("SELECT * FROM `city`")
          result <- resultSet.whileM[List, String](
            resultSet.getString("Name")
          )
        yield result.length
      },
      4079
    )
  }

  test("Statement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      provider.use { conn =>
        for
          statement <- conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
          _ <- statement.setFetchSize(5)
          resultSet <- statement.executeQuery("SELECT * FROM `city`")
          result <- resultSet.whileM[List, String](
            resultSet.getString("Name")
          )
        yield result.length
      },
      4079
    )
  }

  test("PreparedStatement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      provider.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM `city`")
          _ <- statement.setFetchSize(1)
          resultSet <- statement.executeQuery()
          result <- resultSet.whileM[List, String](
            resultSet.getString("Name")
          )
        yield result.length
      },
      4079
    )
  }

  test("PreparedStatement: Query result retrieval using server cursor matches the specified number of results.") {
    assertIO(
      provider.use { conn =>
        for
          statement <- conn.prepareStatement("SELECT * FROM `city`")
          _ <- statement.setFetchSize(5)
          resultSet <- statement.executeQuery()
          result <- resultSet.whileM[List, String](
            resultSet.getString("Name")
          )
        yield result.length
      },
      4079
    )
  }
