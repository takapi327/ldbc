/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import com.mysql.cj.jdbc.MysqlDataSource

import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.sql.*
import ldbc.connector.SSL
import ldbc.schema.TableQuery
import ldbc.schema.syntax.io.*

import ldbc.tests.model.*

class LdbcTableSchemaSelectConnectionTest extends TableSchemaSelectConnectionTest:

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

class JdbcTableSchemaSelectConnectionTest extends TableSchemaSelectConnectionTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

trait TableSchemaSelectConnectionTest extends CatsEffectSuite:

  given Tracer[IO] = Tracer.noop[IO]

  def prefix: "jdbc" | "ldbc"

  def connection: Resource[IO, Connection[IO]]

  private final val country:          TableQuery[CountryTable]          = TableQuery[CountryTable]
  private final val city:             TableQuery[CityTable]             = TableQuery[CityTable]
  private final val countryLanguage:  TableQuery[CountryLanguageTable]  = TableQuery[CountryLanguageTable]
  private final val governmentOffice: TableQuery[GovernmentOfficeTable] = TableQuery[GovernmentOfficeTable]

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        country.selectAll.query.to[List].readOnly(conn).map(_.length)
      },
      239
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        city.selectAll.query.to[List].readOnly(conn).map(_.length)
      },
      4079
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        countryLanguage.selectAll.query.to[List].readOnly(conn).map(_.length)
      },
      984
    )
  }

  test(
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value."
  ) {
    assertIO(
      connection.use { conn =>
        governmentOffice.selectAll.query.to[List].readOnly(conn).map(_.length)
      },
      3
    )
  }

  test("The number of cases retrieved using the subquery matches the specified value.") {
    assertIO(
      connection.use { conn =>
        city
          .select(v => v.name *: v.countryCode)
          .where(_.countryCode _equals country.select(_.code).where(_.code _equals "JPN"))
          .query
          .to[List]
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
          .query
          .to[Option]
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
          .query
          .to[Option]
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
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(CountryLanguage("JPN", "Japanese", CountryLanguage.IsOfficial.T, BigDecimal.decimal(99.1)))
    )
  }

  test("The data retrieved by Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city join country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: country.name)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(("Tokyo", "Japan"))
    )
  }

  test("The data retrieved by Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city join country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: country.name)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(("Tokyo", "Japan"))
    )
  }

  test("The data retrieved by Left Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city leftJoin country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: country.name)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(("Tokyo", Some("Japan")))
    )
  }

  test("The data retrieved by Left Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city leftJoin country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: country.name)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(("Tokyo", Some("Japan")))
    )
  }

  test("The data retrieved by Right Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city rightJoin country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: country.name)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some((Some("Tokyo"), "Japan"))
    )
  }

  test("The data retrieved by Right Join matches the specified model.") {
    assertIO(
      connection.use { conn =>
        (city rightJoin country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: country.name)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some((Some("Tokyo"), "Japan"))
    )
  }

  test("The retrieved data matches the specified value.") {
    assertIO(
      connection.use { conn =>
        city
          .select(v => v.countryCode *: v.id.count)
          .where(_.countryCode _equals "JPN")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(("JPN", 248))
    )
  }

  test("The retrieved data matches the specified value.") {
    assertIO(
      connection.use { conn =>
        city
          .select(v => v.countryCode *: v.id.count)
          .groupBy(_.countryCode)
          .query
          .to[List]
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
          codeOpt <- country.select(_.code).where(_.code _equals "JPN").query.to[Option]
          cities <- codeOpt match
                      case None => DBIO.pure[IO, List[(String, String)]](List.empty)
                      case Some(code) =>
                        city
                          .select(v => v.name *: v.countryCode)
                          .where(_.countryCode _equals code)
                          .query
                          .to[List]
        yield cities.length).readOnly(conn)
      },
      248
    )
  }

  test(
    "If a record is retrieved from a Table model with sellectAll, it is converted to that model and the record is retrieved."
  ) {
    assertIO(
      connection.use { conn =>
        city.selectAll.query.to[Option].readOnly(conn)
      },
      Some(City(4079, "Rafah", "PSE", "Rafah", 92020))
    )
  }

  test(
    "When a record is retrieved with sellectAll after performing a join, it is converted to the respective model and the record can be retrieved."
  ) {
    assertIO(
      connection.use { conn =>
        (city join country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.* *: country.*)
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(
        (
          City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230),
          Country(
            "JPN",
            "Japan",
            Country.Continent.Asia,
            "Eastern Asia",
            BigDecimal(377829.00),
            Some(-660),
            126714000,
            Some(80.7),
            Some(BigDecimal(3787042.00)),
            Some(BigDecimal(4192638.00)),
            "Nihon/Nippon",
            "Constitutional Monarchy",
            Some("Akihito"),
            Some(1532),
            "JP"
          )
        )
      )
    )
  }

  test("Even if a column join is performed at join time, the retrieved data will match the specified values.") {
    assertIO(
      connection.use { conn =>
        (city join country)
          .on((city, country) => city.countryCode _equals country.code)
          .select((city, country) => city.name *: (city.population ++ country.population))
          .where((_, country) => country.code _equals "JPN")
          .and((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(("Tokyo", 134694230))
    )
  }

  test("If selectAll is performed with Left Join, the model with no value will be None.") {
    assertIO(
      connection.use { conn =>
        (city leftJoin governmentOffice)
          .on((city, governmentOffice) => city.id _equals governmentOffice.cityId)
          .select((city, governmentOffice) => city.* *: governmentOffice.*)
          .where((city, _) => city.name _equals "Osaka")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(
        (
          City(1534, "Osaka", "JPN", "Osaka", 2595674),
          None
        )
      )
    )
  }

  test("If you do selectAll with Left Join, the model with the value is wrapped in Some.") {
    assertIO(
      connection.use { conn =>
        (city leftJoin governmentOffice)
          .on((city, governmentOffice) => city.id _equals governmentOffice.cityId)
          .select((city, governmentOffice) => city.* *: governmentOffice.*)
          .where((city, _) => city.name _equals "Tokyo")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(
        (
          City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230),
          Some(GovernmentOffice(3, 1532, "Tokyo Metropolitan Government", Some(java.time.LocalDate.of(2023, 12, 13))))
        )
      )
    )
  }

  test("If selectAll is performed with Right Join, the model with no value will be None.") {
    assertIO(
      connection.use { conn =>
        (governmentOffice rightJoin city)
          .on((governmentOffice, city) => governmentOffice.cityId _equals city.id)
          .select((governmentOffice, city) => governmentOffice.* *: city.*)
          .where((_, city) => city.id _equals 1534)
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(
        (
          None,
          City(1534, "Osaka", "JPN", "Osaka", 2595674)
        )
      )
    )
  }

  test("If you do selectAll with Right Join, the model with the value is wrapped in Some.") {
    assertIO(
      connection.use { conn =>
        (governmentOffice rightJoin city)
          .on((governmentOffice, city) => governmentOffice.cityId _equals city.id)
          .select((governmentOffice, city) => governmentOffice.* *: city.*)
          .where((_, city) => city.id _equals 1532)
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(
        (
          Some(GovernmentOffice(3, 1532, "Tokyo Metropolitan Government", Some(java.time.LocalDate.of(2023, 12, 13)))),
          City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230)
        )
      )
    )
  }

  test(
    "If you use selectAll to retrieve records in a query with multiple Right Join joins, some with values will be set to Some and none without values will be set to None."
  ) {
    assertIO(
      connection.use { conn =>
        (governmentOffice rightJoin city)
          .on((governmentOffice, city) => governmentOffice.cityId _equals city.id)
          .rightJoin(country)
          .on((_, city, country) => country.code _equals city.countryCode)
          .select((governmentOffice, city, country) => governmentOffice.* *: city.* *: country.*)
          .where((_, _, country) => country.code _equals "JPN")
          .query
          .to[Option]
          .readOnly(conn)
      },
      Some(
        (
          None,
          Some(City(1779, "Tsuyama", "JPN", "Okayama", 91170)),
          Country(
            "JPN",
            "Japan",
            Country.Continent.Asia,
            "Eastern Asia",
            BigDecimal(377829.00),
            Some(-660),
            126714000,
            Some(80.7),
            Some(BigDecimal(3787042.00)),
            Some(BigDecimal(4192638.00)),
            "Nihon/Nippon",
            "Constitutional Monarchy",
            Some("Akihito"),
            Some(1532),
            "JP"
          )
        )
      )
    )
  }

  test(
    "When a record is retrieved with selectAll in a query using multiple Right Join joins, if there are only records in the base table, all other values will be None."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          _ <- (country += Country(
                 "XXX",
                 "XXX",
                 Country.Continent.Asia,
                 "XXX",
                 BigDecimal(0),
                 None,
                 0,
                 None,
                 None,
                 None,
                 "XXX",
                 "XXX",
                 None,
                 None,
                 "XX"
               )).update
          result <-
            (governmentOffice rightJoin city)
              .on((governmentOffice, city) => governmentOffice.cityId _equals city.id)
              .rightJoin(country)
              .on((_, city, country) => country.code _equals city.countryCode)
              .select((governmentOffice, city, country) => governmentOffice.* *: city.* *: country.*)
              .where((_, _, country) => country.code _equals "XXX")
              .query
              .to[Option]
          _ <- country.delete.where(_.code _equals "XXX").update
        yield result).transaction(conn)
      },
      Some(
        (
          None,
          None,
          Country(
            "XXX",
            "XXX",
            Country.Continent.Asia,
            "XXX",
            BigDecimal(0),
            None,
            0,
            None,
            None,
            None,
            "XXX",
            "XXX",
            None,
            None,
            "XX"
          )
        )
      )
    )
  }
