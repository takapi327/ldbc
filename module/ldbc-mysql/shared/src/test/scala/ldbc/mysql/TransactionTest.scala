/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.sql.Connection as SQLConnection
import ldbc.sql.SQLNonTransientException

import ldbc.fx.concurrentFx
import ldbc.fx.Fx
import ldbc.mysql.syntax.*
import ldbc.telemetry.*
import ldbc.net.SSL

class TransactionTest extends FTestPlatform:

  given Tracer[Fx] = Tracer.noop[Fx]

  test("Transactions initiated in a session are read-only.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _        <- conn.setReadOnly(true)
        readOnly <- conn.isReadOnly
      yield readOnly
    })
  }

  test("Transactions initiated in a session are write-only.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _        <- conn.setReadOnly(false)
        readOnly <- conn.isReadOnly
      yield !readOnly
    })
  }

  test("Transactions initiated in a session are in auto commit mode.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _          <- conn.setAutoCommit(true)
        autoCommit <- conn.getAutoCommit()
      yield autoCommit
    })
  }

  test("Transactions initiated in a session do not enter autocommit mode.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _          <- conn.setAutoCommit(false)
        autoCommit <- conn.getAutoCommit()
      yield !autoCommit
    })
  }

  test("If a transaction initiated in a session is not in autocommit mode, it can be committed manually.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(false)
        _ <- conn.commit()
      yield true
    })
  }

  test(
    "If a transaction initiated in a session is in autocommit mode, a manual commit will result in a SQLNonTransientException."
  ) {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    interceptFx[SQLNonTransientException](connection.use { conn =>
      for
        _ <- conn.setAutoCommit(true)
        _ <- conn.commit()
      yield true
    })
  }

  test("If a transaction initiated in a session is not in autocommit mode, it can be rollback manually.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(false)
        _ <- conn.rollback()
      yield true
    })
  }

  test(
    "If a transaction initiated in a session is in autocommit mode, a manual rollback will result in a SQLNonTransientException."
  ) {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    interceptFx[SQLNonTransientException](connection.use { conn =>
      for
        _ <- conn.setAutoCommit(true)
        _ <- conn.rollback()
      yield true
    })
  }

  test("Transaction isolation level becomes READ_UNCOMMITTED.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_READ_UNCOMMITTED)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_READ_UNCOMMITTED
    })
  }

  test("Transaction isolation level becomes READ_COMMITTED.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_READ_COMMITTED)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_READ_COMMITTED
    })
  }

  test("Transaction isolation level becomes SERIALIZABLE.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_SERIALIZABLE)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_SERIALIZABLE
    })
  }

  test("Transaction isolation level becomes REPEATABLE_READ.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_REPEATABLE_READ)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_REPEATABLE_READ
    })
  }

  test("The update process is reflected by the commit.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _         <- conn.setReadOnly(false)
        _         <- conn.setAutoCommit(false)
        statement <- conn.clientPreparedStatement("INSERT INTO `transaction_test` VALUES (1)")
        _         <- statement.executeUpdate()
        _         <- conn.commit()
        query     <- conn.clientPreparedStatement("SELECT * FROM `transaction_test` WHERE `c1` = ?")
        _         <- query.setLong(1, 1L)
        resultSet <- query.executeQuery()
        result    <- resultSet.whileM[List, Long] {
                    resultSet.getLong(1)
                  }
      yield result.contains(1L)
    })
  }

  test("The update process is not reflected by the rollback.") {
    val connection = Connection[Fx](
      host     = TestConfig.host,
      port     = TestConfig.port,
      user     = TestConfig.user,
      password = Some(TestConfig.password),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use { conn =>
      for
        _         <- conn.setReadOnly(false)
        _         <- conn.setAutoCommit(false)
        statement <- conn.clientPreparedStatement("INSERT INTO `transaction_test` VALUES (2)")
        _         <- statement.executeUpdate()
        _         <- conn.rollback()
        query     <- conn.clientPreparedStatement("SELECT * FROM `transaction_test` WHERE `c1` = ?")
        _         <- query.setLong(1, 2L)
        resultSet <- query.executeQuery()
        result    <- resultSet.whileM[List, Long] {
                    resultSet.getLong(1)
                  }
      yield result.isEmpty
    })
  }
