/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.Monad

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.AnyFixture

import ldbc.connector.exception.*

class StatementBatchTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host     = TestConfig.host,
    port     = TestConfig.port,
    user     = TestConfig.user,
    password = Some(TestConfig.password),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  override def munitFixtures: Seq[AnyFixture[Unit]] = Seq(
    ResourceTestLocalFixture(
      "setup",
      Resource.make(
        connection
          .use { conn =>
            for
              statement <- conn.createStatement()
              _         <- statement.executeUpdate("CREATE TABLE IF NOT EXISTS `batch_test` (`c1` BIGINT, `c2` INT)")
              _         <- statement.executeUpdate("TRUNCATE TABLE `batch_test`")
              _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (1, 1)")
              _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (2, 2)")
              _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (3, 3)")
              _         <- statement.executeBatch()
            yield ()
          }
      )(_ =>
        connection
          .use { conn =>
            for
              statement <- conn.createStatement()
              _         <- statement.executeUpdate("DROP TABLE IF EXISTS `batch_test`")
            yield ()
          }
      )
    )
  )

  test("It will be the same as the list of updates specified as a result of executing the batch process.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _         <- statement.addBatch("CREATE TABLE `batch_test_2` (`c1` INT)")
          _         <- statement.addBatch("INSERT INTO `batch_test_2` VALUES (1)")
          _         <- statement.addBatch("DROP TABLE `batch_test_2`")
          result    <- statement.executeBatch()
        yield result.toList
      },
      List(0, 1, 0)
    )
  }

  test("If a batch query is cleared, only queries added after clearing will be executed.") {
    assertIO(
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _         <- statement.addBatch("CREATE TABLE `batch_test_3` (`c1` INT)")
          _         <- statement.addBatch("INSERT INTO `batch_test_3` VALUES (1)")
          _         <- statement.clearBatch()
          _         <- statement.addBatch("CREATE TABLE `batch_test_3` (`c1` INT)")
          _         <- statement.addBatch("DROP TABLE `batch_test_3`")
          result    <- statement.executeBatch()
        yield result.toList
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
        yield result.toList
      },
      List.empty
    )
  }

  test("Insert BatchUpdateException is raised if the batch command fails.") {
    interceptIO[BatchUpdateException](
      connection.use { conn =>
        for
          statement <- conn.createStatement()
          _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (1, 1)")
          _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (2, 2)")
          _         <- statement.addBatch("INSERT INTO `batch_test` VALUES (3, 'failed')")
          result    <- statement.executeBatch()
        yield result.toList
      }
    )
  }

  test(
    "If the batch execution of Client PreparedStatement is successful, the result will be a -2 array of the number of queries that were batch executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("INSERT INTO `batch_test` VALUES (?, ?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      },
      List(-2, -2, -2)
    )
  }

  test("Placeholder(?) and the number of parameters do not match, an IllegalArgumentException is raised.") {
    interceptIO[IllegalArgumentException](
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("INSERT INTO `batch_test2` VALUES (?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      }
    )
  }

  test(
    "If the batch execution of Server PreparedStatement is successful, the result will be a -2 array of the number of queries that were batch executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          preparedStatement <- conn.serverPreparedStatement("INSERT INTO `batch_test` VALUES (?, ?)")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      },
      List(-2, -2, -2)
    )
  }

  test(
    "If the batch execution of Server PreparedStatement is successful, the result will be a -2 array of the number of queries that were batch executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("INSERT INTO `batch_test` VALUES (?, ?), (?, ?)")
          _ <- preparedStatement.setInt(1, 4) *> preparedStatement.setInt(2, 4) *> preparedStatement.setInt(
                 3,
                 5
               ) *> preparedStatement.setInt(4, 5) *> preparedStatement.addBatch()
          _ <- preparedStatement.setInt(1, 6) *> preparedStatement.setInt(2, 6) *> preparedStatement.setInt(
                 3,
                 7
               ) *> preparedStatement.setInt(4, 7) *> preparedStatement.addBatch()
          _ <- preparedStatement.setInt(1, 8) *> preparedStatement.setInt(2, 8) *> preparedStatement.setInt(
                 3,
                 9
               ) *> preparedStatement.setInt(4, 9) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      },
      List(-2, -2, -2)
    )
  }

  test(
    "If the batch execution of Server PreparedStatement is successful, the result will be a -2 array of the number of queries that were batch executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("INSERT INTO `batch_test` VALUES (?, ?)")
          _      <- preparedStatement.setInt(1, 4) *> preparedStatement.setInt(2, 4) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 5) *> preparedStatement.setInt(2, 6) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 6) *> preparedStatement.setInt(2, 7) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      },
      List(-2, -2, -2)
    )
  }

  test(
    "If the Update batch command is successful, it returns an array of the number of records affected for each query executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("UPDATE `batch_test` SET `c2` = ? WHERE `c1` = ?")
          _      <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _      <- preparedStatement.setInt(1, 3) *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      },
      List(1, 1, 1)
    )
  }

  test(
    "Update BatchUpdateException is raised if the batch command fails."
  ) {
    interceptIO[BatchUpdateException](
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("UPDATE `batch_test` SET `c2` = ? WHERE `c1` = ?")
          _ <- preparedStatement.setInt(1, 1) *> preparedStatement.setInt(2, 1) *> preparedStatement.addBatch()
          _ <- preparedStatement.setInt(1, 2) *> preparedStatement.setInt(2, 2) *> preparedStatement.addBatch()
          _ <-
            preparedStatement.setString(1, "failed") *> preparedStatement.setInt(2, 3) *> preparedStatement.addBatch()
          result <- preparedStatement.executeBatch()
        yield result.toList
      }
    )
  }

  test(
    "If the Delete batch command is successful, it returns an array of the number of records affected for each query executed."
  ) {
    assertIO(
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("DELETE from `batch_test` WHERE `c1` = ?")
          _                 <- preparedStatement.setInt(1, 1) *> preparedStatement.addBatch()
          _                 <- preparedStatement.setInt(1, 2) *> preparedStatement.addBatch()
          _                 <- preparedStatement.setInt(1, 3) *> preparedStatement.addBatch()
          result            <- preparedStatement.executeBatch()
        yield result.toList
      },
      List(1, 1, 1)
    )
  }

  test(
    "Delete BatchUpdateException is raised if the batch command fails."
  ) {
    interceptIO[BatchUpdateException](
      connection.use { conn =>
        for
          preparedStatement <- conn.clientPreparedStatement("DELETE from `batch_test` WHERE `c1` = ?")
          _                 <- preparedStatement.setInt(1, 1) *> preparedStatement.addBatch()
          _                 <- preparedStatement.setInt(1, 2) *> preparedStatement.addBatch()
          _                 <- preparedStatement.setString(1, "failed") *> preparedStatement.addBatch()
          result            <- preparedStatement.executeBatch()
        yield result
      }
    )
  }

  /**
   * Inserts `payload` through a batch of the prepared statement made by `prepare`, and returns the rows
   * of the table.
   */
  private def insertBatch(
    table:   String,
    payload: String,
    prepare: (LdbcConnection[IO], String) => IO[ldbc.sql.PreparedStatement[IO]]
  ): IO[List[(Long, String)]] =
    connection.use { conn =>
      for
        statement <- conn.createStatement()
        _         <- statement.executeUpdate(s"CREATE TABLE IF NOT EXISTS `$table` (`c1` BIGINT, `c2` VARCHAR(255))")
        _         <- statement.executeUpdate(s"TRUNCATE TABLE `$table`")
        prepared  <- prepare(conn, s"INSERT INTO `$table` (`c1`, `c2`) VALUES (?, ?)")
        _         <- prepared.setLong(1, 1L) *> prepared.setString(2, payload) *> prepared.addBatch()
        _         <- prepared.executeBatch()
        resultSet <- statement.executeQuery(s"SELECT `c1`, `c2` FROM `$table` ORDER BY `c1`")
        rows      <- Monad[IO].whileM[List, (Long, String)](resultSet.next()) {
                  for
                    c1 <- resultSet.getLong(1)
                    c2 <- resultSet.getString(2)
                  yield (c1, c2)
                }
        _ <- statement.executeUpdate(s"DROP TABLE IF EXISTS `$table`")
      yield rows
    }

  /**
   * Regression guard for the batch INSERT SQL-injection finding. The payload carries no quote, so
   * escaping alone never stopped it: the old implementation split the bound statement on `VALUES`
   * and sent `... VALUES (99, 0x696E6A6563746564)#')`, inserting a row the caller never bound.
   */
  private val injectionPayload = "VALUES (99, 0x696E6A6563746564)#"

  test("A bound value containing VALUES is inserted as data by a client batch INSERT.") {
    assertIO(
      insertBatch("client_batch_injection_test", injectionPayload, _.clientPreparedStatement(_)),
      List((1L, injectionPayload))
    )
  }

  test("A bound value containing VALUES is inserted as data by a server batch INSERT.") {
    assertIO(
      insertBatch("server_batch_injection_test", injectionPayload, _.serverPreparedStatement(_)),
      List((1L, injectionPayload))
    )
  }
