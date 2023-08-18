/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import com.mysql.cj.jdbc.MysqlDataSource

import org.specs2.mutable.Specification

import cats.data.Kleisli
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.core.*
import ldbc.core.model.*
import ldbc.sql.{ ResultSetReader, Connection }
import ldbc.dsl.io.{ *, given }
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery

case class Country(
  code:           String,
  name:           String,
  continent:      Country.Continent,
  region:         String,
  surfaceArea:    BigDecimal,
  indepYear:      Option[Short],
  population:     Int,
  lifeExpectancy: Option[BigDecimal],
  gnp:            Option[BigDecimal],
  gnpOld:         Option[BigDecimal],
  localName:      String,
  governmentForm: String,
  headOfState:    Option[String],
  capital:        Option[Int],
  code2:          String
)

object Country:

  enum Continent extends Enum:
    case Asia, Europe, North_America, Africa, Oceania, Antarctica, South_America
  object Continent extends EnumDataType[Continent]

  given ResultSetReader[IO, Continent] =
    ResultSetReader.mapping[IO, String, Continent](str => Continent.valueOf(str.replace(" ", "_")))

  val table: Table[Country] = Table[Country]("country")(
    column("Code", CHAR(3).DEFAULT(""), PRIMARY_KEY),
    column("Name", CHAR(52).DEFAULT("")),
    column("Continent", ENUM(using Continent).DEFAULT(Continent.Asia)),
    column("Region", CHAR(26).DEFAULT("")),
    column("SurfaceArea", DECIMAL(10, 2).DEFAULT(0.00)),
    column("IndepYear", SMALLINT.DEFAULT(None)),
    column("Population", INT.DEFAULT(0)),
    column("LifeExpectancy", DECIMAL(3, 1).DEFAULT(None)),
    column("GNP", DECIMAL(10, 2).DEFAULT(None)),
    column("GNPOld", DECIMAL(10, 2).DEFAULT(None)),
    column("LocalName", CHAR(45).DEFAULT("")),
    column("GovernmentForm", CHAR(45).DEFAULT("")),
    column("HeadOfState", CHAR(60).DEFAULT(None)),
    column("Capital", INT.DEFAULT(None)),
    column("Code2", CHAR(2).DEFAULT(""))
  )

case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)

object City:

  val table: Table[City] = Table[City]("city")(
    column("ID", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("Name", CHAR(35).DEFAULT("")),
    column("CountryCode", CHAR(3).DEFAULT("")),
    column("District", CHAR(20).DEFAULT("")),
    column("Population", INT.DEFAULT(0))
  )
    .keySet(v => INDEX_KEY(v.countryCode))
    .keySet(v => CONSTRAINT("city_ibfk_1", FOREIGN_KEY(v.countryCode, REFERENCE(Country.table, Country.table.code))))

case class CountryLanguage(
  countryCode: String,
  language:    String,
  isOfficial:  CountryLanguage.IsOfficial,
  percentage:  BigDecimal
)

object CountryLanguage:

  enum IsOfficial extends Enum:
    case T, F
  object IsOfficial extends EnumDataType[IsOfficial]

  given ResultSetReader[IO, IsOfficial] =
    ResultSetReader.mapping[IO, String, IsOfficial](str => IsOfficial.valueOf(str))

  val table: Table[CountryLanguage] = Table[CountryLanguage]("countrylanguage")(
    column("CountryCode", CHAR(3).DEFAULT("")),
    column("Language", CHAR(30).DEFAULT("")),
    column("IsOfficial", ENUM(using IsOfficial).DEFAULT(IsOfficial.F)),
    column("Percentage", DECIMAL(4, 1).DEFAULT(0.0))
  )
    .keySet(v => PRIMARY_KEY(v.countryCode, v.language))
    .keySet(v => INDEX_KEY(v.countryCode))
    .keySet(v =>
      CONSTRAINT("countryLanguage_ibfk_1", FOREIGN_KEY(v.countryCode, REFERENCE(Country.table, Country.table.code)))
    )

object DatabaseConnectionTest extends Specification:

  private val dataSource = new MysqlDataSource()
  dataSource.setServerName("127.0.0.1")
  dataSource.setPortNumber(13306)
  dataSource.setDatabaseName("world")
  dataSource.setUser("ldbc")
  dataSource.setPassword("password")

  given LogHandler[IO] = LogHandler.consoleLogger

  private val country         = TableQuery[IO, Country](Country.table)
  private val city            = TableQuery[IO, City](City.table)
  private val countryLanguage = TableQuery[IO, CountryLanguage](CountryLanguage.table)

  "Database Connection Test" should {
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = country.selectAll.toList[Country].readOnly.run(dataSource).unsafeRunSync()
      result.length === 239
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = city.selectAll.toList[City].readOnly.run(dataSource).unsafeRunSync()
      result.length === 4079
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = countryLanguage.selectAll.toList[CountryLanguage].readOnly.run(dataSource).unsafeRunSync()
      result.length === 984
    }

    "The number of cases retrieved using the subquery matches the specified value." in {
      val result = city
        .select[(String, String)](v => (v.name, v.countryCode))
        .where(_.countryCode _equals country.select[String](_.code).where(_.code _equals "JPN"))
        .toList
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result.length === 248
    }

    "The acquired data matches the specified model." in {
      val result = country.selectAll
        .where(_.code _equals "JPN")
        .headOption[Country]
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result === Some(
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
    }

    "The acquired data matches the specified model." in {
      val result = city.selectAll
        .where(_.id _equals 1532)
        .headOption[City]
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result === Some(City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230))
    }

    "The acquired data matches the specified model." in {
      val result = countryLanguage.selectAll
        .where(_.countryCode _equals "JPN")
        .and(_.language _equals "Japanese")
        .headOption[CountryLanguage]
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result === Some(CountryLanguage("JPN", "Japanese", CountryLanguage.IsOfficial.T, BigDecimal.decimal(99.1)))
    }

    "The data retrieved by Join matches the specified model." in {
      val result = (city join country)
        .on((city, country) => city.countryCode _equals country.code)
        .select[(String, String)]((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result === Some(("Tokyo", "Japan"))
    }

    "The data retrieved by Join matches the specified model." in {
      case class CountryCity(cityName: String, countryName: String)

      val result = (city join country)
        .on((city, country) => city.countryCode _equals country.code)
        .select[(String, String)]((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption[CountryCity]
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result === Some(CountryCity("Tokyo", "Japan"))
    }

    "The retrieved data matches the specified value." in {
      val result = city.select[(String, Int)](v => (v.countryCode, v.id.count))
        .where(_.countryCode _equals "JPN")
        .headOption
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result === Some(("JPN", 248))
    }

    "The acquired data matches the specified model." in {
      case class CountryCodeGroup(countryCode: String, length: Int)

      val result = city.select[(String, Int)](v => (v.countryCode, v.id.count))
        .groupBy(_._1)
        .toList[CountryCodeGroup]
        .readOnly
        .run(dataSource)
        .unsafeRunSync()
      result.length === 232
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      (for
        codeOpt <- country.select[String](_.code).where(_.code _equals "JPN").headOption
        cities <- codeOpt match
          case None => Kleisli.pure[IO, Connection[IO], List[(String, String)]](List.empty[(String, String)])
          case Some(code *: EmptyTuple) =>
            city.select[(String, String)](v => (v.name, v.countryCode))
              .where(_.countryCode _equals code)
              .toList
      yield cities.length === 248).readOnly
        .run(dataSource)
        .unsafeRunSync()
    }
  }
