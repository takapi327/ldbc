/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.connector.codec.all.*

class SavepointTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  private val connection = Connection[IO](
    host = "127.0.0.1",
    port = 13306,
    user = "ldbc",
    password = Some("password"),
    database = Some("connector_test"),
    ssl = SSL.Trusted
  )

  override def beforeEach(context: BeforeEach): Unit =
    connection.use { conn =>
      for
        s1 <- conn.clientPreparedStatement("CREATE TABLE IF NOT EXISTS `savepoint_test` (`c1` BIGINT)")
        s2 <- conn.clientPreparedStatement("TRUNCATE TABLE `savepoint_test`")
        _ <- s1.executeUpdate()
        _ <- s2.executeUpdate()
      yield ()
    }.unsafeRunSync()

  override def afterEach(context: AfterEach): Unit =
    connection.use { conn =>
      for
        s1 <- conn.clientPreparedStatement("DROP TABLE IF EXISTS `savepoint_test`")
        _ <- s1.executeUpdate()
      yield ()
    }.unsafeRunSync()

  test("A Savepoint with the specified name can be set.") {
    assertIO(connection.use { conn =>
      for
        savepoint <- conn.setSavepoint("test_savepoint")
      yield savepoint.getSavepointName
    }, "test_savepoint")
  }

  test("A Savepoint can be set and rolled back.") {
    assertIO(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(false)
        statement1 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
        _ <- statement1.setLong(1, 1L)
        _         <- statement1.executeUpdate()
        savepoint <- conn.setSavepoint("test_savepoint")
        statement2 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
        _ <- statement2.setLong(1, 2L)
        _         <- statement2.executeUpdate()
        _ <- conn.rollback(savepoint)
        _ <- conn.commit()
        statement3 <- conn.clientPreparedStatement("SELECT * FROM `savepoint_test` WHERE `c1` IN (?, ?)")
        _ <- statement3.setLong(1, 1L)
        _ <- statement3.setLong(2, 2L)
        resultSet <- statement3.executeQuery()
      yield resultSet.decode(bigint).length
    }, 1)
  }

  test("A Savepoint can be set and released.") {
    assertIO(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(false)
        statement1 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
        _ <- statement1.setLong(1, 1L)
        _         <- statement1.executeUpdate()
        savepoint <- conn.setSavepoint("test_savepoint")
        statement2 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
        _ <- statement2.setLong(1, 2L)
        _         <- statement2.executeUpdate()
        _ <- conn.releaseSavepoint(savepoint)
        _ <- conn.commit()
        statement3 <- conn.clientPreparedStatement("SELECT * FROM `savepoint_test` WHERE `c1` IN (?, ?)")
        _ <- statement3.setLong(1, 1L)
        _ <- statement3.setLong(2, 2L)
        resultSet <- statement3.executeQuery()
      yield resultSet.decode(bigint).length
    }, 2)
  }
