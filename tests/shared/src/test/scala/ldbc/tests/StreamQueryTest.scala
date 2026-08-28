/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.Future

import cats.effect.*

import zio.Task

import munit.*

import ldbc.dsl.*

import ldbc.connector.*

import ldbc.catseffect.*
import ldbc.fx.Fx
import ldbc.Connector

class LdbcStreamQueryTest extends StreamQueryTest[IO] with IODatabaseSuite:

  private val datasource = MySQLDataSource
    .build[IO](host, port, user)
    .setPassword(password)
    .setDatabase(database)
    .setSSL(SSL.None)
    .setUseCursorFetch(true)
    .setAllowPublicKeyRetrieval(true)

  override def connector: Connector[IO] = ldbc.connector.Connector.fromDataSource(datasource)

class MysqlStreamQueryTest extends StreamQueryTest[IO] with IODatabaseSuite:
  import ldbc.catseffect.concurrentIO
  import ldbc.catseffect.Connector as MysqlConnector
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  private val datasource = MySQLDataSource
    .build[IO](host, port, user)
    .setPassword(password)
    .setDatabase(database)
    .setSSL(MysqlSSL.None)
    .setUseCursorFetch(true)
    .setAllowPublicKeyRetrieval(true)

  override def connector: Connector[IO] = MysqlConnector.fromDataSource(datasource)

class MysqlFxStreamQueryTest extends StreamQueryTest[Fx] with FxDatabaseSuite:
  import ldbc.fx.concurrentFx
  import ldbc.mysql.MySQLDataSource, ldbc.tests.TestConnector as MysqlConnector
  import ldbc.net.SSL as MysqlSSL

  override def connector: Connector[Fx] =
    MysqlConnector.fromDataSource(
      MySQLDataSource
        .build[Fx](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("world")
        .setSSL(MysqlSSL.None)
        .setUseCursorFetch(true)
        .setAllowPublicKeyRetrieval(true)
    )

class MysqlFutureStreamQueryTest extends StreamQueryTest[Future] with FutureDatabaseSuite:
  import ldbc.fx.concurrentFx
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  override def connector: Connector[Future] =
    ldbc.future.Connector.fromDataSource(
      MySQLDataSource
        .build[Fx](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("world")
        .setSSL(MysqlSSL.None)
        .setUseCursorFetch(true)
        .setAllowPublicKeyRetrieval(true)
    )

class MysqlZioStreamQueryTest extends StreamQueryTest[Task] with ZioDatabaseSuite:
  import ldbc.zio.concurrentTask
  import ldbc.mysql.MySQLDataSource
  import ldbc.net.SSL as MysqlSSL

  override def connector: Connector[Task] =
    ldbc.zio.Connector.fromDataSource(
      MySQLDataSource
        .build[Task](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
        .setPassword(MySQLTestConfig.password)
        .setDatabase("world")
        .setSSL(MysqlSSL.None)
        .setUseCursorFetch(true)
        .setAllowPublicKeyRetrieval(true)
    )

trait StreamQueryTest[F[_]] extends DatabaseSuite[F]:

  protected val host:     String = MySQLTestConfig.host
  protected val port:     Int    = MySQLTestConfig.port
  protected val user:     String = MySQLTestConfig.user
  protected val password: String = MySQLTestConfig.password
  protected val database: String = "world"

  def connector: Connector[F]

  test("Stream support test") {
    assertF(
      sql"SELECT Name FROM `city`".query[String].stream.take(5).compile.toList.readOnly(connector),
      List(
        "Kabul",
        "Qandahar",
        "Herat",
        "Mazar-e-Sharif",
        "Amsterdam"
      )
    )
  }

  test("Stream support test with fetchSize") {
    assertF(
      sql"SELECT Name FROM `city`".query[String].stream(2).take(5).compile.toList.readOnly(connector),
      List(
        "Kabul",
        "Qandahar",
        "Herat",
        "Mazar-e-Sharif",
        "Amsterdam"
      )
    )
  }

  test("Stream with negative fetchSize should fail") {
    interceptF[IllegalArgumentException] {
      sql"SELECT Name FROM `city`".query[String].stream(-1).take(1).compile.toList.readOnly(connector)
    }
  }

  test("Stream with zero fetchSize should fail") {
    interceptF[IllegalArgumentException] {
      sql"SELECT Name FROM `city`".query[String].stream(0).take(1).compile.toList.readOnly(connector)
    }
  }
