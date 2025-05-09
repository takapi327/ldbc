/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*
import com.mysql.cj.jdbc.MysqlDataSource
import jdbc.connector.{ConnectionProvider as JdbcProvider, *}
import ldbc.connector.{ConnectionProvider as LdbcProvider, *}
import ldbc.dsl.*
import ldbc.dsl.codec.*
import munit.CatsEffectSuite

class LdbcSQLStringContextQueryTest extends SQLStringContextQueryTest:
  override def connection: Provider[IO] =
    LdbcProvider
      .default[IO]("127.0.0.1", 13306, "ldbc", "password", "world")
      .setSSL(SSL.Trusted)

class JdbcSQLStringContextQueryTest extends SQLStringContextQueryTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def connection: Provider[IO] =
    JdbcProvider.fromDataSource(ds, ExecutionContexts.synchronous)

trait SQLStringContextQueryTest extends CatsEffectSuite:

  def connection: Provider[IO]

  test("The results obtained by JOIN can be mapped to the class NamedTuple.") {
    case class City(id: Int, name: String)
    case class Country(code: String, name: String)

    given Decoder[City] = Decoder.derived[City]
    given Decoder[Country] = Decoder.derived[Country]

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
