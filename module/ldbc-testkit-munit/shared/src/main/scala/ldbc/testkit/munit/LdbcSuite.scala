/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.testkit.munit

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.connector.{ Connector as LdbcConnector, MySQLDataSource }

import ldbc.testkit.RollbackHandler
import ldbc.Connector

/**
 * Base trait for ldbc integration tests using MUnit.
 *
 * Extend this trait and implement [[dataSource]] to obtain a database connection.
 * Use [[ephemeralTest]] for tests that should not persist their changes, and
 * [[persistentTest]] for tests that require actual commits (e.g. DDL).
 *
 * @example {{{
 * class UserRepositoryTest extends LdbcSuite:
 *
 *   override def dataSource: MySQLDataSource[IO, Unit] =
 *     MySQLDataSource
 *       .build[IO]("localhost", 3306, "root")
 *       .setPassword("password")
 *       .setDatabase("test_db")
 *       .setSSL(SSL.Trusted)
 *
 *   ephemeralTest("create and findById") { connector =>
 *     val repo = UserRepositoryImpl[IO](connector)
 *     for
 *       created <- repo.create(User(name = "Alice"))
 *       found   <- repo.findById(created.id)
 *     yield assertEquals(found, Some(created))
 *   }
 * }}}
 */
trait LdbcSuite extends CatsEffectSuite:

  /**
   * The [[MySQLDataSource]] used to obtain database connections for tests.
   * Implement this in your test class to configure the connection.
   */
  def dataSource: MySQLDataSource[IO, Unit]

  /**
   * Registers a test that runs inside a transaction which is automatically rolled back
   * on completion, regardless of success or failure.
   *
   * Any `commit()` calls inside the test body (including those made by Repository
   * implementations) are intercepted and converted to no-ops, so database state is
   * never persisted.
   *
   * Not suitable for tests that include DDL statements (CREATE TABLE, DROP TABLE, etc.)
   * because MySQL implicitly commits before executing DDL.
   *
   * @param name the test name
   * @param body the test body, receiving a [[Connector]] backed by a rolled-back transaction
   */
  def ephemeralTest(name: String)(body: Connector[IO] => IO[Unit]): Unit =
    test(name) {
      RollbackHandler.resource[IO](dataSource).use(body)
    }

  /**
   * Registers a test that runs with a real connection where commits are applied permanently.
   * Use this for DDL tests or when actual persistence is required.
   *
   * @param name the test name
   * @param body the test body, receiving a standard [[Connector]]
   */
  def persistentTest(name: String)(body: Connector[IO] => IO[Unit]): Unit =
    test(name) {
      dataSource.getConnection.use { connection =>
        body(LdbcConnector.fromConnection[IO](connection))
      }
    }

  /**
   * Asserts that the result of `fa` has exactly `expected` elements.
   *
   * @example {{{
   * assertCount(repo.findAll(), 3)
   * }}}
   */
  def assertCount[A](fa: IO[List[A]], expected: Int): IO[Unit] =
    assertIO(fa.map(_.size), expected)

  /**
   * Asserts that the result of `fa` is an empty list.
   *
   * @example {{{
   * assertEmpty(repo.findAll())
   * }}}
   */
  def assertEmpty[A](fa: IO[List[A]]): IO[Unit] =
    assertIO(fa.map(_.isEmpty), true)

  /**
   * Asserts that the result of `fa` contains the same elements as `expected`,
   * regardless of order.
   *
   * @example {{{
   * assertRowsUnordered(repo.findAll(), List(user1, user2, user3))
   * }}}
   */
  def assertRowsUnordered[A](fa: IO[List[A]], expected: List[A]): IO[Unit] =
    assertIO(fa.map(_.toSet), expected.toSet)
