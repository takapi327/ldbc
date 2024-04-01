/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import org.typelevel.otel4s.trace.Tracer

import com.comcast.ip4s.UnknownHostException

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.connector.exception.MySQLException

class ConnectionTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  test("Passing an empty string to host causes MySQLException") {
    val connection = Connection[IO](
      host = "",
      port = 13306,
      user = "root"
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }

  test("UnknownHostException occurs when invalid host is passed") {
    val connection = Connection[IO](
      host = "host",
      port = 13306,
      user = "root"
    )
    interceptIO[UnknownHostException] {
      connection.use(_ => IO.unit)
    }
  }

  test("Passing a negative value to Port causes MySQLException") {
    val connection = Connection[IO](
      host = "127.0.0.1",
      port = -1,
      user = "root"
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }

  test("MySQLException occurs when passing more than 65535 values to Port") {
    val connection = Connection[IO](
      host = "127.0.0.1",
      port = 65536,
      user = "root"
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }

  test("A user using mysql_native_password can establish a connection with the MySQL server.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password")
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "Connections to MySQL servers using users with mysql_native_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_mysql_native_user",
      password                = Some("ldbc_mysql_native_password"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with mysql_native_password will succeed for SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Users using mysql_native_password can establish a connection with the MySQL server by specifying database.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test")
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with mysql_native_password will succeed."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_mysql_native_user",
      password                = Some("ldbc_mysql_native_password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with mysql_native_password will succeed with an SSL connection."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_mysql_native_user",
      password = Some("ldbc_mysql_native_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with sha256_password will fail for non-SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password")
    )
    interceptIO[MySQLException] {
      connection.use(_ => IO.unit)
    }
  }

  test(
    "Connections to MySQL servers using users with sha256_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_sha256_user",
      password                = Some("ldbc_sha256_password"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with sha256_password will succeed for SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with sha256_password will succeed."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc_sha256_user",
      password                = Some("ldbc_sha256_password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with sha256_password will succeed with an SSL connection."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc_sha256_user",
      password = Some("ldbc_sha256_password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "Connections to MySQL servers using users with caching_sha2_password will succeed if allowPublicKeyRetrieval is enabled for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc",
      password                = Some("password"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Connections to MySQL servers using users with caching_sha2_password will succeed for SSL connections.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If the login information of a user using caching_sha2_password is cached, the connection to the MySQL server will succeed even for non-SSL connections."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password")
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "If allowPublicKeyRetrieval is enabled for non-SSL connections, a connection to a MySQL server specifying a database using a user with caching_sha2_password will succeed."
  ) {
    val connection = Connection[IO](
      host                    = "127.0.0.1",
      port                    = 13306,
      user                    = "ldbc",
      password                = Some("password"),
      database                = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test(
    "A connection to a MySQL server with a database specified using a user with caching_sha2_password will succeed with an SSL connection."
  ) {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )
    assertIOBoolean(connection.use(_ => IO(true)))
  }

  test("Schema change will change the currently connected Schema.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use { conn =>
        conn.setSchema("world") *> conn.getSchema
      },
      "world"
    )
  }

  test("Statistics of the MySQL server can be obtained.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      ssl      = SSL.Trusted
    )

    assertIO(
      connection.use(_.getStatistics.map(_.toString)),
      "COM_STATISTICS Response Packet"
    )
  }

  test("The connection is valid.") {
    val connection = Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("connector_test"),
      allowPublicKeyRetrieval = true
    )

    assertIOBoolean(connection.use(_.isValid))
  }
