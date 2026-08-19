/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.authenticator

import ldbc.sql.SQLInvalidAuthorizationSpecException

import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.mysql.*
import ldbc.mysql.authenticator.MysqlClearPasswordPlugin
import ldbc.mysql.telemetry.*
import ldbc.net.SSL

class MysqlNativePasswordTest extends FTestPlatform:

  given Tracer = Tracer.noop

  test("A user using mysql_native_password can establish a connection with the MySQL server.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password")
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test(
    "Connections to MySQL servers using users with mysql_native_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection(
      host                    = "127.0.0.1",
      port                    = 13307,
      user                    = "ldbc_mysql_native_user",
      password                = Some("ldbc_mysql_native_password"),
      allowPublicKeyRetrieval = true
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test("Connections to MySQL servers using users with mysql_native_password will succeed for SSL connections.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test("Users using mysql_native_password can establish a connection with the MySQL server by specifying database.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test")
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with mysql_native_password will succeed."
  ) {
    val connection = Connection(
      host                    = "127.0.0.1",
      port                    = 13307,
      user                    = "ldbc_mysql_native_user",
      password                = Some("ldbc_mysql_native_password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with mysql_native_password will succeed with an SSL connection."
  ) {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test("You can connect to the database by specifying the default authentication plugin.") {
    val connection = Connection(
      host                        = "127.0.0.1",
      port                        = 13307,
      user                        = "ldbc_mysql_native_user",
      password                    = Some("ldbc_mysql_native_password"),
      defaultAuthenticationPlugin = Some(MysqlClearPasswordPlugin()),
      ssl                         = SSL.Trusted
    )
    assertFxBoolean(connection.use(_ => Fx.delay(true)))
  }

  test(
    "Using the MySQL Clear Password Plugin when SSL is not enabled causes an SQLInvalidAuthorizationSpecException to occur."
  ) {
    val connection = Connection(
      host                        = "127.0.0.1",
      port                        = 13307,
      user                        = "ldbc_mysql_native_user",
      password                    = Some("ldbc_mysql_native_password"),
      defaultAuthenticationPlugin = Some(MysqlClearPasswordPlugin())
    )
    interceptFx[SQLInvalidAuthorizationSpecException](connection.use(_ => Fx.delay(true)))
  }

  test("Can change from mysql_native_password user to caching_sha2_password user.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertFxBoolean(
      connection.use(_.changeUser("ldbc", "password")) *> Fx.pure(true),
      true
    )
  }

  test("Can change from mysql_native_password user to sha256_password user.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertFxBoolean(
      connection.use(_.changeUser("ldbc_sha256_user", "ldbc_sha256_password")) *> Fx.pure(true),
      true
    )
  }

  test("Can change from sha256_password user to mysql_native_password user.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertFxBoolean(
      connection.use(_.changeUser("ldbc_mysql_native_user", "ldbc_mysql_native_password")) *> Fx.pure(true),
      true
    )
  }

  test("Can change from caching_sha2_password user to mysql_native_password user.") {
    val connection = Connection(
      host     = "127.0.0.1",
      port     = 13307,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertFxBoolean(
      connection.use(_.changeUser("ldbc_mysql_native_user", "ldbc_mysql_native_password")) *> Fx.pure(true),
      true
    )
  }
