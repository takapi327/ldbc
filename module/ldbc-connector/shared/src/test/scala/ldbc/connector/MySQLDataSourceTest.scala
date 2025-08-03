/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.effect.std.UUIDGen

import fs2.hashing.Hashing
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

class MySQLDataSourceTest extends FTestPlatform:

  test("MySQLDataSource should have correct default values when created with minimal parameters") {
    val dataSource = MySQLDataSource[IO, Unit](
      host = "localhost",
      port = 3306,
      user = "root"
    )

    assertEquals(dataSource.host, "localhost")
    assertEquals(dataSource.port, 3306)
    assertEquals(dataSource.user, "root")
    assertEquals(dataSource.logHandler, None)
    assertEquals(dataSource.password, None)
    assertEquals(dataSource.database, None)
    assertEquals(dataSource.debug, false)
    assertEquals(dataSource.ssl, SSL.None)
    assertEquals(dataSource.socketOptions, MySQLConfig.defaultSocketOptions)
    assertEquals(dataSource.readTimeout, Duration.Inf)
    assertEquals(dataSource.allowPublicKeyRetrieval, false)
    assertEquals(dataSource.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.CATALOG))
    assertEquals(dataSource.useCursorFetch, false)
    assertEquals(dataSource.useServerPrepStmts, false)
    assertEquals(dataSource.before, None)
    assertEquals(dataSource.after, None)
  }

  test("setHost should update host value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setHost("127.0.0.1")

    assertEquals(updated.host, "127.0.0.1")
    assertEquals(updated.port, dataSource.port)
    assertEquals(updated.user, dataSource.user)
  }

  test("setPort should update port value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setPort(3307)

    assertEquals(updated.port, 3307)
    assertEquals(updated.host, dataSource.host)
  }

  test("setUser should update user value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setUser("testuser")

    assertEquals(updated.user, "testuser")
    assertEquals(updated.host, dataSource.host)
  }

  test("setPassword should update password to Some value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setPassword("secret")

    assertEquals(updated.password, Some("secret"))
  }

  test("setDatabase should update database to Some value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setDatabase("testdb")

    assertEquals(updated.database, Some("testdb"))
  }

  test("setDebug should update debug value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setDebug(true)

    assertEquals(updated.debug, true)
  }

  test("setSSL should update SSL configuration") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setSSL(SSL.Trusted)

    assertEquals(updated.ssl, SSL.Trusted)
  }

  test("setSocketOptions should update socket options") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val newOptions = List(SocketOption.noDelay(false), SocketOption.keepAlive(true))
    val updated    = dataSource.setSocketOptions(newOptions)

    assertEquals(updated.socketOptions, newOptions)
  }

  test("setReadTimeout should update read timeout") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val timeout    = Duration("30s")
    val updated    = dataSource.setReadTimeout(timeout)

    assertEquals(updated.readTimeout, timeout)
  }

  test("setAllowPublicKeyRetrieval should update allowPublicKeyRetrieval value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setAllowPublicKeyRetrieval(true)

    assertEquals(updated.allowPublicKeyRetrieval, true)
  }

  test("setDatabaseTerm should update databaseTerm to Some value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setDatabaseTerm(DatabaseMetaData.DatabaseTerm.SCHEMA)

    assertEquals(updated.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.SCHEMA))
  }

  test("setTracer should update tracer to Some value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val tracer     = Tracer.noop[IO]
    val updated    = dataSource.setTracer(tracer)

    assertEquals(updated.tracer, Some(tracer))
  }

  test("setUseCursorFetch should update useCursorFetch value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setUseCursorFetch(true)

    assertEquals(updated.useCursorFetch, true)
  }

  test("setUseServerPrepStmts should update useServerPrepStmts value") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val updated    = dataSource.setUseServerPrepStmts(true)

    assertEquals(updated.useServerPrepStmts, true)
  }

  test("withBefore should add a before hook and change type parameter") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val beforeHook: Connection[IO] => IO[String] = _ => IO.pure("before result")
    val updated = dataSource.withBefore(beforeHook)

    assert(updated.before.isDefined)
    // Type parameter changes from Unit to String
    val _: MySQLDataSource[IO, String] = updated
  }

  test("withAfter should add an after hook") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val afterHook: (Unit, Connection[IO]) => IO[Unit] = (_, _) => IO.unit
    val updated = dataSource.withAfter(afterHook)

    assert(updated.after.isDefined)
  }

  test("withBeforeAfter should add both before and after hooks") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val beforeHook: Connection[IO] => IO[String]         = _ => IO.pure("before result")
    val afterHook:  (String, Connection[IO]) => IO[Unit] = (_, _) => IO.unit
    val updated = dataSource.withBeforeAfter(beforeHook, afterHook)

    assert(updated.before.isDefined)
    assert(updated.after.isDefined)
    // Type parameter changes from Unit to String
    val _: MySQLDataSource[IO, String] = updated
  }

  test("chaining multiple setter methods should work correctly") {
    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
      .setHost("127.0.0.1")
      .setPort(3307)
      .setUser("testuser")
      .setPassword("testpass")
      .setDatabase("testdb")
      .setDebug(true)
      .setSSL(SSL.Trusted)
      .setAllowPublicKeyRetrieval(true)
      .setUseCursorFetch(true)
      .setUseServerPrepStmts(true)

    assertEquals(dataSource.host, "127.0.0.1")
    assertEquals(dataSource.port, 3307)
    assertEquals(dataSource.user, "testuser")
    assertEquals(dataSource.password, Some("testpass"))
    assertEquals(dataSource.database, Some("testdb"))
    assertEquals(dataSource.debug, true)
    assertEquals(dataSource.ssl, SSL.Trusted)
    assertEquals(dataSource.allowPublicKeyRetrieval, true)
    assertEquals(dataSource.useCursorFetch, true)
    assertEquals(dataSource.useServerPrepStmts, true)
  }

  test("MySQLDataSource.fromConfig should create DataSource from MySQLConfig") {
    val config = MySQLConfig.default
      .setHost("confighost")
      .setPort(3308)
      .setUser("configuser")
      .setPassword("configpass")
      .setDatabase("configdb")
      .setDebug(true)

    val dataSource = MySQLDataSource.fromConfig[IO](config)

    assertEquals(dataSource.host, "confighost")
    assertEquals(dataSource.port, 3308)
    assertEquals(dataSource.user, "configuser")
    assertEquals(dataSource.password, Some("configpass"))
    assertEquals(dataSource.database, Some("configdb"))
    assertEquals(dataSource.debug, true)
  }

  test("MySQLDataSource.default should create DataSource with default config") {
    val dataSource = MySQLDataSource.default[IO]

    assertEquals(dataSource.host, "127.0.0.1")
    assertEquals(dataSource.port, 3306)
    assertEquals(dataSource.user, "root")
    assertEquals(dataSource.password, None)
    assertEquals(dataSource.database, None)
    assertEquals(dataSource.debug, false)
  }

  test("MySQLDataSource.build should create DataSource with specified parameters") {
    val dataSource = MySQLDataSource.build[IO](
      host = "buildhost",
      port = 3309,
      user = "builduser"
    )

    assertEquals(dataSource.host, "buildhost")
    assertEquals(dataSource.port, 3309)
    assertEquals(dataSource.user, "builduser")
    // Other values should be default
    assertEquals(dataSource.password, None)
    assertEquals(dataSource.database, None)
    assertEquals(dataSource.debug, false)
  }

  test("MySQLDataSource should be immutable - original should not change") {
    val original     = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val originalHost = original.host
    val originalPort = original.port

    // Make multiple changes
    val updated = original
      .setHost("newhost")
      .setPort(3308)
      .setUser("newuser")
      .setPassword("newpass")

    // Original should remain unchanged
    assertEquals(original.host, originalHost)
    assertEquals(original.port, originalPort)
    assertEquals(original.user, "root")
    assertEquals(original.password, None)

    // Updated should have new values
    assertEquals(updated.host, "newhost")
    assertEquals(updated.port, 3308)
    assertEquals(updated.user, "newuser")
    assertEquals(updated.password, Some("newpass"))
  }

  test("MySQLDataSource with custom socket options") {
    val customOptions = List(
      SocketOption.noDelay(false),
      SocketOption.keepAlive(true)
    )

    val dataSource = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
      .setSocketOptions(customOptions)

    assertEquals(dataSource.socketOptions.length, 3)
    assertEquals(dataSource.socketOptions, customOptions)
  }

  test("MySQLDataSource with different SSL configurations") {
    val dataSourceNone    = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
    val dataSourceTrusted = MySQLDataSource[IO, Unit]("localhost", 3306, "root").setSSL(SSL.Trusted)
    val dataSourceSystem  = MySQLDataSource[IO, Unit]("localhost", 3306, "root").setSSL(SSL.System)

    assertEquals(dataSourceNone.ssl, SSL.None)
    assertEquals(dataSourceTrusted.ssl, SSL.Trusted)
    assertEquals(dataSourceSystem.ssl, SSL.System)
  }

  test("MySQLDataSource with different DatabaseTerm values") {
    val dataSourceCatalog = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
      .setDatabaseTerm(DatabaseMetaData.DatabaseTerm.CATALOG)
    val dataSourceSchema = MySQLDataSource[IO, Unit]("localhost", 3306, "root")
      .setDatabaseTerm(DatabaseMetaData.DatabaseTerm.SCHEMA)

    assertEquals(dataSourceCatalog.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.CATALOG))
    assertEquals(dataSourceSchema.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.SCHEMA))
  }
