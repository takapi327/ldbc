/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.Connection as SQLConnection

import ldbc.connector.exception.*

class TransactionTest extends FTestPlatform:

  given Tracer[IO] = Tracer.noop[IO]

  test("Transactions initiated in a session are read-only.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _        <- conn.setReadOnly(true)
        readOnly <- conn.isReadOnly
      yield readOnly
    })
  }

  test("Transactions initiated in a session are write-only.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _        <- conn.setReadOnly(false)
        readOnly <- conn.isReadOnly
      yield !readOnly
    })
  }

  test("Transactions initiated in a session are in auto commit mode.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _          <- conn.setAutoCommit(true)
        autoCommit <- conn.getAutoCommit()
      yield autoCommit
    })
  }

  test("Transactions initiated in a session do not enter autocommit mode.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _          <- conn.setAutoCommit(false)
        autoCommit <- conn.getAutoCommit()
      yield !autoCommit
    })
  }

  test("If a transaction initiated in a session is not in autocommit mode, it can be committed manually.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(false)
        _ <- conn.commit()
      yield true
    })
  }

  test(
    "If a transaction initiated in a session is in autocommit mode, a manual commit will result in a SQLNonTransientException."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    interceptIO[SQLNonTransientException](connection.use { conn =>
      for
        _ <- conn.setAutoCommit(true)
        _ <- conn.commit()
      yield true
    })
  }

  test("If a transaction initiated in a session is not in autocommit mode, it can be rollback manually.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(false)
        _ <- conn.rollback()
      yield true
    })
  }

  test(
    "If a transaction initiated in a session is in autocommit mode, a manual rollback will result in a SQLNonTransientException."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    interceptIO[SQLNonTransientException](connection.use { conn =>
      for
        _ <- conn.setAutoCommit(true)
        _ <- conn.rollback()
      yield true
    })
  }

  test("Transaction isolation level becomes READ_UNCOMMITTED.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_READ_UNCOMMITTED)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_READ_UNCOMMITTED
    })
  }

  test("Transaction isolation level becomes READ_COMMITTED.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_READ_COMMITTED)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_READ_COMMITTED
    })
  }

  test("Transaction isolation level becomes SERIALIZABLE.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_SERIALIZABLE)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_SERIALIZABLE
    })
  }

  test("Transaction isolation level becomes REPEATABLE_READ.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _     <- conn.setTransactionIsolation(SQLConnection.TRANSACTION_REPEATABLE_READ)
        level <- conn.getTransactionIsolation()
      yield level == SQLConnection.TRANSACTION_REPEATABLE_READ
    })
  }

  test("The update process is reflected by the commit.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _         <- conn.setReadOnly(false)
        _         <- conn.setAutoCommit(false)
        statement <- conn.clientPreparedStatement("INSERT INTO `transaction_test` VALUES (1)")
        _         <- statement.executeUpdate()
        _         <- conn.commit()
        query     <- conn.clientPreparedStatement("SELECT * FROM `transaction_test` WHERE `c1` = ?")
        _         <- query.setLong(1, 1L)
        resultSet <- query.executeQuery()
      yield
        val builder = List.newBuilder[Long]
        while resultSet.next() do builder += resultSet.getLong(1)
        builder.result().contains(1L)
    })
  }

  test("The update process is not reflected by the rollback.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use { conn =>
      for
        _         <- conn.setReadOnly(false)
        _         <- conn.setAutoCommit(false)
        statement <- conn.clientPreparedStatement("INSERT INTO `transaction_test` VALUES (2)")
        _         <- statement.executeUpdate()
        _         <- conn.rollback()
        query     <- conn.clientPreparedStatement("SELECT * FROM `transaction_test` WHERE `c1` = ?")
        _         <- query.setLong(1, 2L)
        resultSet <- query.executeQuery()
      yield
        val builder = List.newBuilder[Long]
        while resultSet.next() do builder += resultSet.getLong(1)
        builder.result().isEmpty
    })
  }
