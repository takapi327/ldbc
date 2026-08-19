/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.net.SSL
import ldbc.mysql.telemetry.*
import munit.AnyFixture

import ldbc.fx.Resource




class SavepointTest extends FTestPlatform:

  given Tracer = Tracer.noop

  private val connection = Connection(
    host     = TestConfig.host,
    port     = TestConfig.port,
    user     = TestConfig.user,
    password = Some(TestConfig.password),
    database = Some("connector_test"),
    ssl      = SSL.Trusted
  )

  override def munitFixtures: Seq[AnyFixture[Unit]] = Seq(
    resourceFixture(
      "setup",
      Resource.make(
        connection.use { conn =>
          for
            s1 <- conn.clientPreparedStatement("CREATE TABLE IF NOT EXISTS `savepoint_test` (`c1` BIGINT)")
            s2 <- conn.clientPreparedStatement("TRUNCATE TABLE `savepoint_test`")
            _  <- s1.executeUpdate()
            _  <- s2.executeUpdate()
          yield ()
        }
      )(_ =>
        connection.use { conn =>
          for
            s1 <- conn.clientPreparedStatement("DROP TABLE IF EXISTS `savepoint_test`")
            _  <- s1.executeUpdate()
          yield ()
        }
      )
    )
  )

  test("A Savepoint with the specified name can be set.") {
    assertFx(
      connection.use { conn =>
        for savepoint <- conn.setSavepoint("test_savepoint")
        yield savepoint.getSavepointName()
      },
      "test_savepoint"
    )
  }

  test("A Savepoint can be set and rolled back.") {
    assertFx(
      connection.use { conn =>
        for
          _          <- conn.setAutoCommit(false)
          statement1 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
          _          <- statement1.setLong(1, 1L)
          _          <- statement1.executeUpdate()
          savepoint  <- conn.setSavepoint("test_savepoint")
          statement2 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
          _          <- statement2.setLong(1, 2L)
          _          <- statement2.executeUpdate()
          _          <- conn.rollback(savepoint)
          _          <- conn.commit()
          statement3 <- conn.clientPreparedStatement("SELECT count(*) FROM `savepoint_test` WHERE `c1` IN (?, ?)")
          _          <- statement3.setLong(1, 1L)
          _          <- statement3.setLong(2, 2L)
          resultSet  <- statement3.executeQuery()
          _          <- conn.setAutoCommit(true)
          value      <- resultSet.getInt(1)
        yield value
      },
      1
    )
  }

  test("A Savepoint can be set and released.") {
    assertFx(
      connection.use { conn =>
        for
          _          <- conn.setAutoCommit(false)
          statement1 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
          _          <- statement1.setLong(1, 1L)
          _          <- statement1.executeUpdate()
          savepoint  <- conn.setSavepoint("test_savepoint")
          statement2 <- conn.clientPreparedStatement("INSERT INTO `savepoint_test` VALUES (?)")
          _          <- statement2.setLong(1, 2L)
          _          <- statement2.executeUpdate()
          _          <- conn.releaseSavepoint(savepoint)
          _          <- conn.commit()
          statement3 <- conn.clientPreparedStatement("SELECT count(*) FROM `savepoint_test` WHERE `c1` IN (?, ?)")
          _          <- statement3.setLong(1, 1L)
          _          <- statement3.setLong(2, 2L)
          resultSet  <- statement3.executeQuery()
          _          <- conn.setAutoCommit(true)
          value      <- resultSet.getInt(1)
        yield value
      },
      2
    )
  }
