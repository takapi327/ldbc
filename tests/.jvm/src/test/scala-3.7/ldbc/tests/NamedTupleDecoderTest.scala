/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import munit.CatsEffectSuite

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.connector.{ ConnectionProvider as LdbcProvider, * }

import jdbc.connector.{ ConnectionProvider as JdbcProvider, * }

class LdbcNamedTupleDecoderTest extends NamedTupleDecoderTest:
  override def connection: Provider[IO] =
    LdbcProvider
      .default[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user, MySQLTestConfig.password, "world")
      .setSSL(SSL.Trusted)

class JdbcNamedTupleDecoderTest extends NamedTupleDecoderTest:

  val ds = new MysqlDataSource()
  ds.setServerName(MySQLTestConfig.host)
  ds.setPortNumber(MySQLTestConfig.port)
  ds.setDatabaseName("world")
  ds.setUser(MySQLTestConfig.user)
  ds.setPassword(MySQLTestConfig.password)

  override def connection: Provider[IO] =
    JdbcProvider.fromDataSource(ds, ExecutionContexts.synchronous)

trait NamedTupleDecoderTest extends CatsEffectSuite:

  def connection: Provider[IO]

  test("The results obtained by JOIN can be mapped to the class NamedTuple.") {
    case class City(id: Int, name: String) derives Decoder
    case class Country(code: String, name: String) derives Decoder

    assertIO(
      connection.use { conn =>
        sql"SELECT city.Id, city.Name, country.Code, country.Name FROM city JOIN country ON city.CountryCode = country.Code LIMIT 1"
          .query[(city: City, country: Country)]
          .to[Option]
          .readOnly(conn)
      },
      Some((City(1, "Kabul"), Country("AFG", "Afghanistan")))
    )
  }
