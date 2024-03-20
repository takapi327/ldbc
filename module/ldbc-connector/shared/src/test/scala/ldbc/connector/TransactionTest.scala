/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.CatsEffectSuite

import ldbc.connector.exception.MySQLException

class TransactionTest extends CatsEffectSuite:

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
        autoCommit <- conn.getAutoCommit
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
        autoCommit <- conn.getAutoCommit
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

  test("If a transaction initiated in a session is in autocommit mode, a manual commit will result in a MySQLException.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    interceptMessageIO[MySQLException]("message: Can't call commit when autocommit=true")(connection.use { conn =>
      for
        _ <- conn.setAutoCommit(true)
        _ <- conn.commit()
      yield true
    })
  }