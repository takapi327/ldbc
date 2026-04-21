/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package example

import cats.effect.kernel.Resource
import cats.effect.IO

import ldbc.dsl.*

import ldbc.connector.{ MySQLDataSource, SSL }

import ldbc.testkit.munit.*

class UserRepositoryTest extends LdbcSuite:

  override def dataSource: MySQLDataSource[IO, Unit] =
    MySQLDataSource
      .build[IO](
        sys.env.getOrElse("MYSQL_HOST", "127.0.0.1"),
        sys.env.getOrElse("MYSQL_PORT", "13306").toInt,
        "ldbc"
      )
      .setPassword("password")
      .setDatabase("connector_test")
      .setSSL(SSL.Trusted)

  private val tableFixture = ResourceSuiteLocalFixture(
    "users-table",
    Resource.make(
      sql"""CREATE TABLE users (
        id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name  VARCHAR(255) NOT NULL,
        email VARCHAR(255) NOT NULL UNIQUE
      )""".update.commit(connector).void
    )(_ => sql"DROP TABLE IF EXISTS users".update.commit(connector).void)
  )

  override def munitFixtures = List(tableFixture)

  // ephemeralTest: DB への変更はテスト終了後に自動ロールバックされる
  ephemeralTest("create で登録したユーザーを findById で取得できる") { connector =>
    val repo = UserRepositoryImpl(connector)
    for
      id   <- repo.create("Alice", "alice@example.com")
      user <- repo.findById(id)
    yield assertEquals(user.map(_.name), Some("Alice"))
  }

  ephemeralTest("存在しない ID は None を返す") { connector =>
    val repo = UserRepositoryImpl(connector)
    assertIO(repo.findById(99999L), None)
  }

  ephemeralTest("複数ユーザーを登録して findAll で全件取得できる") { connector =>
    val repo = UserRepositoryImpl(connector)
    assertCount(
      for
        _      <- repo.create("Alice", "alice@example.com")
        _      <- repo.create("Bob", "bob@example.com")
        _      <- repo.create("Carol", "carol@example.com")
        result <- repo.findAll()
      yield result,
      3
    )
  }

  ephemeralTest("delete でユーザーを削除できる") { connector =>
    val repo = UserRepositoryImpl(connector)
    assertEmpty(
      for
        id     <- repo.create("Alice", "alice@example.com")
        _      <- repo.delete(id)
        result <- repo.findAll()
      yield result
    )
  }

  ephemeralTest("ephemeralTest 終了後、他のテストにデータが残らない") { connector =>
    val repo = UserRepositoryImpl(connector)
    // このテストが先に実行されても後続テストに影響しない
    assertCount(
      for
        _      <- repo.create("Ghost", "ghost@example.com")
        result <- repo.findAll()
      yield result,
      1
    )
  }

  ephemeralTest("assertRowsUnordered で順序を無視して検証できる") { connector =>
    val repo = UserRepositoryImpl(connector)
    assertRowsUnordered(
      for
        _      <- repo.create("Alice", "alice@example.com")
        _      <- repo.create("Bob", "bob@example.com")
        result <- repo.findAll()
      yield result.map(_.name),
      List("Bob", "Alice")
    )
  }
