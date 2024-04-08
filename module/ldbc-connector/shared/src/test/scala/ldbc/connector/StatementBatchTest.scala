/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import cats.effect.*

import munit.CatsEffectSuite

class StatementBatchTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host                    = "127.0.0.1",
    port                    = 13306,
    user                    = "ldbc",
    password                = Some("password"),
    database                = Some("connector_test"),
    allowPublicKeyRetrieval = true
  )

  test("It will be the same as the list of updates specified as a result of executing the batch process.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _         <- statement.addBatch("CREATE TABLE `batch_test` (`c1` INT)")
          _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (1)")
          _         <- statement.addBatch("DROP TABLE `batch_test`")
          result    <- statement.executeBatch()
        yield result
      },
      List(0, 1, 0)
    )
  }

  test("If a batch query is cleared, only queries added after clearing will be executed.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _         <- statement.addBatch("CREATE TABLE `batch_test` (`c1` INT)")
          _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (1)")
          _         <- statement.clearBatch()
          _         <- statement.addBatch("CREATE TABLE `batch_test` (`c1` INT)")
          _         <- statement.addBatch("DROP TABLE `batch_test`")
          result    <- statement.executeBatch()
        yield result
      },
      List(0, 0)
    )
  }

  test("When you run an empty batch, you receive an empty result.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          result    <- statement.executeBatch()
        yield result
      },
      List.empty
    )
  }

  test(
    "If the batch execution of Client PreparedStatement is successful, the result will be a -2 array of the number of queries that were batch executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          statement         <- conn.createStatement()
          _                 <- statement.executeUpdate("CREATE TABLE `batch_test2` (`c1` INT, `c2` INT)")
          preparedStatement <- conn.clientPreparedStatement("INSERT INTO `batch_test2` VALUES (?, ?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
          _      <- statement.executeUpdate("DROP TABLE `batch_test2`")
        yield result
      },
      List(-2, -2, -2)
    )
  }

  test("Placeholder(?) and the number of parameters do not match, an IllegalArgumentException is raised.") {
    interceptMessageIO[IllegalArgumentException](
      "requirement failed: The number of parameters does not match the number of placeholders"
    )(
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("INSERT INTO `batch_test2` VALUES (?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result
      }
    )
  }

  test(
    "If the batch execution of Server PreparedStatement is successful, the result will be a -2 array of the number of queries that were batch executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          statement         <- conn.createStatement()
          _                 <- statement.executeUpdate("CREATE TABLE `batch_test3` (`c1` INT, `c2` INT)")
          preparedStatement <- conn.serverPreparedStatement("INSERT INTO `batch_test3` VALUES (?, ?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
          _      <- statement.executeUpdate("DROP TABLE `batch_test3`")
        yield result
      },
      List(-2, -2, -2)
    )
  }

  test("Placeholder(?) and the number of parameters do not match, an IllegalArgumentException is raised.") {
    interceptMessageIO[IllegalArgumentException](
      "requirement failed: The number of parameters does not match the number of placeholders"
    )(
      connection.use { conn =>
        for
          statement         <- conn.createStatement()
          _                 <- statement.executeUpdate("CREATE TABLE `batch_test4` (`c1` INT)")
          preparedStatement <- conn.serverPreparedStatement("INSERT INTO `batch_test4` VALUES (?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
          _      <- statement.executeUpdate("DROP TABLE `batch_test4`")
        yield result
      }
    )
  }
