/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.effect.IO

import fs2.io.net.SocketOption

import org.typelevel.otel4s.trace.Tracer

import ldbc.sql.DatabaseMetaData

class ConnectionProviderTest extends FTestPlatform:

  /**
   * Default connection provider for testing
   */
  def defaultProvider: ConnectionProvider[IO, Unit] =
    ConnectionProvider
      .default[IO]("127.0.0.1", 13306, "ldbc")

  test("ConnectionProvider#setHost") {
    val provider = defaultProvider.setHost("localhost")
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setPort") {
    val provider = defaultProvider.setPort(3306)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setUser") {
    val provider = defaultProvider.setUser("root")
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setPassword") {
    val provider = defaultProvider.setPassword("newpassword")
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setDatabase") {
    val provider = defaultProvider.setDatabase("testdb")
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setDebug") {
    val provider = defaultProvider.setDebug(true)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setSSL") {
    val provider = defaultProvider.setSSL(SSL.Trusted)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#addSocketOption") {
    val option   = SocketOption.noDelay(false)
    val provider = defaultProvider.addSocketOption(option)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setSocketOptions") {
    val options  = List(SocketOption.noDelay(false))
    val provider = defaultProvider.setSocketOptions(options)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setReadTimeout") {
    val provider = defaultProvider.setReadTimeout(Duration(5000, "ms"))
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setAllowPublicKeyRetrieval") {
    val provider = defaultProvider.setAllowPublicKeyRetrieval(true)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setDatabaseTerm") {
    val provider = defaultProvider.setDatabaseTerm(DatabaseMetaData.DatabaseTerm.SCHEMA)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#setTracer") {
    val tracer   = Tracer.noop[IO]
    val provider = defaultProvider.setTracer(tracer)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#withBefore") {
    val before   = (conn: Connection[IO]) => IO.unit
    val provider = defaultProvider.withBefore(before)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#withAfter") {
    val after    = (unit: Unit, conn: Connection[IO]) => IO.unit
    val provider = defaultProvider.withAfter(after)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }

  test("ConnectionProvider#withBeforeAfter") {
    val before   = (conn: Connection[IO]) => IO.unit
    val after    = (unit: Unit, conn: Connection[IO]) => IO.unit
    val provider = defaultProvider.withBeforeAfter(before, after)
    assertNotEquals(
      provider,
      defaultProvider
    )
  }
