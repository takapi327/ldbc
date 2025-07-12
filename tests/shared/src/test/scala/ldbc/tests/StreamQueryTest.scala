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

class LdbcStreamQueryTest extends StreamQueryTest:

  override def provider: Provider[IO] =
    ConnectionProvider
      .default[IO](host, port, user, password, database)
      .setSSL(SSL.None)
      .setUseCursorFetch(true)
      .setAllowPublicKeyRetrieval(true)

trait StreamQueryTest extends CatsEffectSuite:

  protected val host:     String = "127.0.0.1"
  protected val port:     Int    = 13306
  protected val user:     String = "ldbc"
  protected val password: String = "password"
  protected val database: String = "world"

  def provider: Provider[IO]

  test("Stream support test") {
    assertIO(
      provider.use { conn =>
        sql"SELECT Name FROM `city`".query[String].stream.take(5).compile.toList.readOnly(conn)
      },
      List(
        "Kabul",
        "Qandahar",
        "Herat",
        "Mazar-e-Sharif",
        "Amsterdam"
      )
    )
  }

  test("Stream support test") {
    assertIO(
      provider.use { conn =>
        sql"SELECT Name FROM `city`".query[String].stream(2).take(5).compile.toList.readOnly(conn)
      },
      List(
        "Kabul",
        "Qandahar",
        "Herat",
        "Mazar-e-Sharif",
        "Amsterdam"
      )
    )
  }
