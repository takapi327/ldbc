/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*

import munit.*

import ldbc.dsl.*

import ldbc.connector.*

import ldbc.Connector

class LdbcStreamQueryTest extends StreamQueryTest:

  private val datasource = MySQLDataSource
    .build[IO](host, port, user)
    .setPassword(password)
    .setDatabase(database)
    .setSSL(SSL.None)
    .setUseCursorFetch(true)
    .setAllowPublicKeyRetrieval(true)

  override def connector: Connector[IO] = Connector.fromDataSource(datasource)

trait StreamQueryTest extends CatsEffectSuite:

  protected val host:     String = "127.0.0.1"
  protected val port:     Int    = 13306
  protected val user:     String = "ldbc"
  protected val password: String = "password"
  protected val database: String = "world"

  def connector: Connector[IO]

  test("Stream support test") {
    assertIO(
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
    assertIO(
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
    interceptIO[IllegalArgumentException] {
      sql"SELECT Name FROM `city`".query[String].stream(-1).take(1).compile.toList.readOnly(connector)
    }
  }

  test("Stream with zero fetchSize should fail") {
    interceptIO[IllegalArgumentException] {
      sql"SELECT Name FROM `city`".query[String].stream(0).take(1).compile.toList.readOnly(connector)
    }
  }
