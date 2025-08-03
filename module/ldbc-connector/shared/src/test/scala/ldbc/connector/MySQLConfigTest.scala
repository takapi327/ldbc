/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import fs2.io.net.*

import ldbc.sql.DatabaseMetaData

class MySQLConfigTest extends FTestPlatform:

  test("MySQLConfig.default should have correct default values") {
    val config = MySQLConfig.default
    
    assertEquals(config.host, "127.0.0.1")
    assertEquals(config.port, 3306)
    assertEquals(config.user, "root")
    assertEquals(config.password, None)
    assertEquals(config.database, None)
    assertEquals(config.debug, false)
    assertEquals(config.ssl, SSL.None)
    assertEquals(config.socketOptions, MySQLConfig.defaultSocketOptions)
    assertEquals(config.readTimeout, Duration.Inf)
    assertEquals(config.allowPublicKeyRetrieval, false)
    assertEquals(config.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.CATALOG))
    assertEquals(config.useCursorFetch, false)
    assertEquals(config.useServerPrepStmts, false)
  }

  test("setHost should update host value") {
    val config = MySQLConfig.default
    val updated = config.setHost("localhost")
    
    assertEquals(updated.host, "localhost")
    // Ensure other values remain unchanged
    assertEquals(updated.port, config.port)
    assertEquals(updated.user, config.user)
  }

  test("setPort should update port value") {
    val config = MySQLConfig.default
    val updated = config.setPort(3307)
    
    assertEquals(updated.port, 3307)
    assertEquals(updated.host, config.host)
  }

  test("setUser should update user value") {
    val config = MySQLConfig.default
    val updated = config.setUser("testuser")
    
    assertEquals(updated.user, "testuser")
    assertEquals(updated.host, config.host)
  }

  test("setPassword should update password to Some value") {
    val config = MySQLConfig.default
    val updated = config.setPassword("secret")
    
    assertEquals(updated.password, Some("secret"))
  }

  test("setDatabase should update database to Some value") {
    val config = MySQLConfig.default
    val updated = config.setDatabase("testdb")
    
    assertEquals(updated.database, Some("testdb"))
  }

  test("setDebug should update debug value") {
    val config = MySQLConfig.default
    val updated = config.setDebug(true)
    
    assertEquals(updated.debug, true)
  }

  test("setSSL should update SSL configuration") {
    val config = MySQLConfig.default
    val updated = config.setSSL(SSL.Trusted)
    
    assertEquals(updated.ssl, SSL.Trusted)
  }

  test("setSocketOptions should update socket options") {
    val config = MySQLConfig.default
    val newOptions = List(SocketOption.noDelay(false), SocketOption.keepAlive(true))
    val updated = config.setSocketOptions(newOptions)
    
    assertEquals(updated.socketOptions, newOptions)
  }

  test("setReadTimeout should update read timeout") {
    val config = MySQLConfig.default
    val timeout = Duration("30s")
    val updated = config.setReadTimeout(timeout)
    
    assertEquals(updated.readTimeout, timeout)
  }

  test("setAllowPublicKeyRetrieval should update allowPublicKeyRetrieval value") {
    val config = MySQLConfig.default
    val updated = config.setAllowPublicKeyRetrieval(true)
    
    assertEquals(updated.allowPublicKeyRetrieval, true)
  }

  test("setDatabaseTerm should update databaseTerm to Some value") {
    val config = MySQLConfig.default
    val updated = config.setDatabaseTerm(DatabaseMetaData.DatabaseTerm.SCHEMA)
    
    assertEquals(updated.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.SCHEMA))
  }

  test("setUseCursorFetch should update useCursorFetch value") {
    val config = MySQLConfig.default
    val updated = config.setUseCursorFetch(true)
    
    assertEquals(updated.useCursorFetch, true)
  }

  test("setUseServerPrepStmts should update useServerPrepStmts value") {
    val config = MySQLConfig.default
    val updated = config.setUseServerPrepStmts(true)
    
    assertEquals(updated.useServerPrepStmts, true)
  }

  test("MySQLConfig should be immutable - original config should not change") {
    val original = MySQLConfig.default
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

  test("chaining multiple setter methods should work correctly") {
    val config = MySQLConfig.default
      .setHost("localhost")
      .setPort(3307)
      .setUser("testuser")
      .setPassword("testpass")
      .setDatabase("testdb")
      .setDebug(true)
      .setSSL(SSL.Trusted)
      .setAllowPublicKeyRetrieval(true)
      .setUseCursorFetch(true)
      .setUseServerPrepStmts(true)
    
    assertEquals(config.host, "localhost")
    assertEquals(config.port, 3307)
    assertEquals(config.user, "testuser")
    assertEquals(config.password, Some("testpass"))
    assertEquals(config.database, Some("testdb"))
    assertEquals(config.debug, true)
    assertEquals(config.ssl, SSL.Trusted)
    assertEquals(config.allowPublicKeyRetrieval, true)
    assertEquals(config.useCursorFetch, true)
    assertEquals(config.useServerPrepStmts, true)
  }

  test("MySQLConfig with custom socket options") {
    val customOptions = List(
      SocketOption.noDelay(false),
      SocketOption.keepAlive(true),
      SocketOption.receiveBufferSize(8192)
    )
    
    val config = MySQLConfig.default.setSocketOptions(customOptions)
    
    assertEquals(config.socketOptions.length, 3)
    assertEquals(config.socketOptions, customOptions)
  }

  test("MySQLConfig with different SSL configurations") {
    val configNone = MySQLConfig.default
    val configTrusted = MySQLConfig.default.setSSL(SSL.Trusted)
    val configSystem = MySQLConfig.default.setSSL(SSL.System)
    
    assertEquals(configNone.ssl, SSL.None)
    assertEquals(configTrusted.ssl, SSL.Trusted)
    assertEquals(configSystem.ssl, SSL.System)
  }

  test("MySQLConfig with different DatabaseTerm values") {
    val configCatalog = MySQLConfig.default.setDatabaseTerm(DatabaseMetaData.DatabaseTerm.CATALOG)
    val configSchema = MySQLConfig.default.setDatabaseTerm(DatabaseMetaData.DatabaseTerm.SCHEMA)
    
    assertEquals(configCatalog.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.CATALOG))
    assertEquals(configSchema.databaseTerm, Some(DatabaseMetaData.DatabaseTerm.SCHEMA))
  }