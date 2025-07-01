/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*

import munit.*

import ldbc.sql.*
import ldbc.connector.*
import ldbc.dsl.*

class LdbcStreamQueryTest extends StreamQueryTest:

  override def provider: Provider[IO] =
    ConnectionProvider.default[IO](host, port, user, password, database)
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

  def stream(preparedStatement: ldbc.sql.PreparedStatement[IO]): fs2.Stream[IO, String] = fs2.Stream.bracket(preparedStatement.executeQuery())(_.close()).flatMap { resultSet =>
    fs2.Stream.unfoldEval(resultSet) { rs =>
      rs.next().flatMap {
        case true  => rs.getString("Name").map(name => Some((name, rs)))
        case false => IO.pure(None)
      }
    }
  }

  test("Stream support test") {
    assertIO(
      provider.use { conn =>
        sql"SELECT Name FROM `city`".query[String].stream(conn).take(5).compile.toList
      },
      List(
        "Kabul",
        "Qandahar",
        "Herat",
        "Mazar-e-Sharif",
        "Tirana"
      )
    )
  }

  //test("") {
  //  assertIO(
  //    provider.use { conn =>
  //      for
  //        preparedStatement <- conn.prepareStatement("SELECT Name FROM `city`")
  //        _ <- preparedStatement.setFetchSize(1)
  //        names             <- stream(preparedStatement).compile.toList
  //      yield names
  //    },
  //    List(
  //      "Kabul",
  //      "Qandahar",
  //      "Herat",
  //      "Mazar-e-Sharif",
  //      "Tirana"
  //    )
  //  )
  //}
