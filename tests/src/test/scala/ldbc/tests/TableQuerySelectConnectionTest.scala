/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.core.*
import ldbc.sql.*
import ldbc.query.builder.TableQuery
import ldbc.connector.SSL
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

import ldbc.tests.model.*

class LdbcTableQuerySelectConnectionTest extends TableQuerySelectConnectionTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: Resource[IO, Connection[IO]] =
    ldbc.connector.Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("world"),
      ssl      = SSL.Trusted
    )

class JdbcTableQuerySelectConnectionTest extends TableQuerySelectConnectionTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

trait TableQuerySelectConnectionTest extends CatsEffectSuite:

  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]

  def prefix:     "jdbc" | "ldbc"
  def connection: Resource[IO, Connection[IO]]

  private final val country          = TableQuery[IO, Country](Country.table)
  private final val city             = TableQuery[IO, City](City.table)
  private final val countryLanguage  = TableQuery[IO, CountryLanguage](CountryLanguage.table)
  private final val governmentOffice = TableQuery[IO, GovernmentOffice](GovernmentOffice.table)

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        country.selectAll.toList[Country].readOnly(conn).map(_.length)
      },
      239
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        city.selectAll.toList[City].readOnly(conn).map(_.length)
      },
      4079
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        countryLanguage.selectAll.toList[CountryLanguage].readOnly(conn).map(_.length)
      },
      984
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        countryLanguage.selectAll.toList[CountryLanguage].readOnly(conn).map(_.length)
      },
      984
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        governmentOffice.selectAll.toList[GovernmentOffice].readOnly(conn).map(_.length)
      },
      2
    )
  }

  test("The number of cases retrieved using the subquery matches the specified value.") {
    assertIO(
      connection.use { conn =>
        city
          .select(v => (v.name, v.countryCode))
          .where(_.countryCode _equals country.select(_.code).where(_.code _equals "JPN"))
          .toList
          .readOnly(conn)
          .map(_.length)
      },
      248
    )
  }

  test("The acquired data matches the specified model.") {
    assertIO(
      connection.use { conn =>
        country.selectAll
          .where(_.code _equals "JPN")
          .headOption[Country]
          .readOnly(conn)
      },
      Some(
        Country(
          "JPN",
          "Japan",
          Country.Continent.Asia,
          "Eastern Asia",
          BigDecimal.decimal(377829.00),
          Some(-660),
          126714000,
          Some(BigDecimal.decimal(80.7)),
          Some(BigDecimal.decimal(3787042.00)),
          Some(BigDecimal.decimal(4192638.00)),
          "Nihon/Nippon",
          "Constitutional Monarchy",
          Some("Akihito"),
          Some(1532),
          "JP"
        )
      )
    )
  }

  test("The acquired data matches the specified model.") {
    assertIO(
      connection.use { conn =>
        city.selectAll
          .where(_.id _equals 1532)
          .headOption[City]
          .readOnly(conn)
      },
      Some(City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230))
    )
  }

  test("The acquired data matches the specified model.") {
    assertIO(
      connection.use { conn =>
        countryLanguage.selectAll
          .where(_.countryCode _equals "JPN")
          .and(_.language _equals "Japanese")
          .headOption[CountryLanguage]
          .readOnly(conn)
      },
      Some(CountryLanguage("JPN", "Japanese", CountryLanguage.IsOfficial.T, BigDecimal.decimal(99.1)))
    )
  }

  test("The data retrieved by Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city join country)((city, country) => city.countryCode _equals country.code)
          .select((city, country) => (city.name, country.name))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .headOption
          .readOnly(conn)
      },
      Some(("Tokyo", "Japan"))
    )
  }

  test("The data retrieved by Join matches the specified model.") {
    case class CountryCity(cityName: String, countryName: String)
    assertIO(
      connection.use { conn =>
        (city join country)((city, country) => city.countryCode _equals country.code)
          .select((city, country) => (city.name, country.name))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .headOption[CountryCity]
          .readOnly(conn)
      },
      Some(CountryCity("Tokyo", "Japan"))
    )
  }

  test("The data retrieved by Left Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city leftJoin country)((city, country) => city.countryCode _equals country.code)
          .select((city, country) => (city.name, country.name))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .headOption
          .readOnly(conn)
      },
      Some(("Tokyo", Some("Japan")))
    )
  }

  test("The data retrieved by Left Join matches the specified model.") {
    case class CountryCity(cityName: String, countryName: Option[String])
    assertIO(
      connection.use { conn =>
        (city leftJoin country)((city, country) => city.countryCode _equals country.code)
          .select((city, country) => (city.name, country.name))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .headOption[CountryCity]
          .readOnly(conn)
      },
      Some(CountryCity("Tokyo", Some("Japan")))
    )
  }

  test("The data retrieved by Right Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city rightJoin country)((city, country) => city.countryCode _equals country.code)
          .select((city, country) => (city.name, country.name))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .headOption
          .readOnly(conn)
      },
      Some((Some("Tokyo"), "Japan"))
    )
  }

  test("The data retrieved by Right Join matches the specified model.") {
    case class CountryCity(cityName: Option[String], countryName: String)
    assertIO(
      connection.use { conn =>
        (city rightJoin country)((city, country) => city.countryCode _equals country.code)
          .select((city, country) => (city.name, country.name))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .headOption[CountryCity]
          .readOnly(conn)
      },
      Some(CountryCity(Some("Tokyo"), "Japan"))
    )
  }

  test("The retrieved data matches the specified value.") {
    assertIO(
      connection.use { conn =>
        city
          .select(v => (v.countryCode, v.id.count))
          .where(_.countryCode _equals "JPN")
          .headOption
          .readOnly(conn)
      },
      Some(("JPN", 248))
    )
  }

  test("The retrieved data matches the specified value.") {
    case class CountryCodeGroup(countryCode: String, length: Int)
    assertIO(
      connection.use { conn =>
        city
          .select(v => (v.countryCode, v.id.count))
          .groupBy(_._1)
          .toList[CountryCodeGroup]
          .readOnly(conn)
          .map(_.length)
      },
      232
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          codeOpt <- country.select(_.code).where(_.code _equals "JPN").headOption
          cities <- codeOpt match
                      case None => Kleisli.pure[IO, Connection[IO], List[City]](List.empty[City])
                      case Some(code *: EmptyTuple) =>
                        city
                          .select(v => (v.name, v.countryCode))
                          .where(_.countryCode _equals code)
                          .toList
        yield cities.length).readOnly(conn)
      },
      248
    )
  }
