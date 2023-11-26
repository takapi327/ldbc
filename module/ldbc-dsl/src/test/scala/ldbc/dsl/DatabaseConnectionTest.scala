/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import com.mysql.cj.jdbc.MysqlDataSource

import org.specs2.mutable.Specification
import org.specs2.execute.Result

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.sql.*
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery
import ldbc.dsl.schema.*

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
      val result = country.selectAll.toList[Country].readOnly(dataSource).unsafeRunSync()
      result.length === 239
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = city.selectAll.toList[City].readOnly(dataSource).unsafeRunSync()
      result.length === 4079
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = countryLanguage.selectAll.toList[CountryLanguage].readOnly(dataSource).unsafeRunSync()
      result.length === 984
    }

    "The number of cases retrieved using the subquery matches the specified value." in {
      val result = city
        .select(v => (v.name, v.countryCode))
        .where(_.countryCode _equals country.select(_.code).where(_.code _equals "JPN"))
        .toList
        .readOnly(dataSource)
        .unsafeRunSync()
      result.length === 248
    }

    "The acquired data matches the specified model." in {
      val result = country.selectAll
        .where(_.code _equals "JPN")
        .headOption[Country]
        .readOnly(dataSource)
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
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230))
    }

    "The acquired data matches the specified model." in {
      val result = countryLanguage.selectAll
        .where(_.countryCode _equals "JPN")
        .and(_.language _equals "Japanese")
        .headOption[CountryLanguage]
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(CountryLanguage("JPN", "Japanese", CountryLanguage.IsOfficial.T, BigDecimal.decimal(99.1)))
    }

    "The data retrieved by Join matches the specified model." in {
      val result = (city join country)((city, country) => city.countryCode _equals country.code)
        .select((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(("Tokyo", "Japan"))
    }

    "The data retrieved by Join matches the specified model." in {
      case class CountryCity(cityName: String, countryName: String)

      val result = (city join country)((city, country) => city.countryCode _equals country.code)
        .select((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption[CountryCity]
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(CountryCity("Tokyo", "Japan"))
    }

    "The data retrieved by Left Join matches the specified model." in {
      val result = (city leftJoin country)((city, country) => city.countryCode _equals country.code)
        .select((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(("Tokyo", Some("Japan")))
    }

    "The data retrieved by Left Join matches the specified model." in {
      case class CountryCity(cityName: String, countryName: Option[String])

      val result = (city leftJoin country)((city, country) => city.countryCode _equals country.code)
        .select((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption[CountryCity]
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(CountryCity("Tokyo", Some("Japan")))
    }

    "The data retrieved by Right Join matches the specified model." in {
      val result = (city rightJoin country)((city, country) => city.countryCode _equals country.code)
        .select((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some((Some("Tokyo"), "Japan"))
    }

    "The data retrieved by Right Join matches the specified model." in {
      case class CountryCity(cityName: Option[String], countryName: String)

      val result = (city rightJoin country)((city, country) => city.countryCode _equals country.code)
        .select((city, country) => (city.name, country.name))
        .where((_, country) => country.code _equals "JPN")
        .and((city, _) => city.name _equals "Tokyo")
        .headOption[CountryCity]
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(CountryCity(Some("Tokyo"), "Japan"))
    }

    "The retrieved data matches the specified value." in {
      val result = city
        .select(v => (v.countryCode, v.id.count))
        .where(_.countryCode _equals "JPN")
        .headOption
        .readOnly(dataSource)
        .unsafeRunSync()
      result === Some(("JPN", 248))
    }

    "The acquired data matches the specified model." in {
      case class CountryCodeGroup(countryCode: String, length: Int)

      val result = city
        .select(v => (v.countryCode, v.id.count))
        .groupBy(_._1)
        .toList[CountryCodeGroup]
        .readOnly(dataSource)
        .unsafeRunSync()
      result.length === 232
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      (for
        codeOpt <- country.select(_.code).where(_.code _equals "JPN").headOption
        cities <- codeOpt match
                    case None => ConnectionIO.pure[IO, List[(String, String)]](List.empty[(String, String)])
                    case Some(code *: EmptyTuple) =>
                      city
                        .select(v => (v.name, v.countryCode))
                        .where(_.countryCode _equals code)
                        .toList
      yield cities.length === 248)
        .readOnly(dataSource)
        .unsafeRunSync()
    }

    "New data can be registered with the value of Tuple." in {
      val result = country
        .insert(
          (
            "T1",
            "Test1",
            Country.Continent.Asia,
            "Northeast",
            BigDecimal.decimal(390757.00),
            None,
            1,
            None,
            None,
            None,
            "Test",
            "Test",
            None,
            None,
            "T1"
          )
        )
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 1
    }

    "New data can be registered from multiple tuple values." in {
      val result = country
        .insert(
          (
            "T2",
            "Test2",
            Country.Continent.Asia,
            "Northeast",
            BigDecimal.decimal(390757.00),
            None,
            1,
            None,
            None,
            None,
            "Test",
            "Test",
            None,
            None,
            "T2"
          ),
          (
            "T3",
            "Test3",
            Country.Continent.Asia,
            "Northeast",
            BigDecimal.decimal(390757.00),
            None,
            1,
            None,
            None,
            None,
            "Test",
            "Test",
            None,
            None,
            "T3"
          )
        )
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 2
    }

    "New data can be registered from the model." in {
      val newCountry = Country(
        "T4",
        "Test4",
        Country.Continent.Asia,
        "Northeast",
        BigDecimal.decimal(390757.00),
        None,
        1,
        None,
        None,
        None,
        "Test",
        "Test",
        None,
        None,
        "T4"
      )
      val result = (country += newCountry).update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 1
    }

    "New data can be registered from multiple models." in {
      val newCountry1 = Country(
        "T5",
        "Test5",
        Country.Continent.Asia,
        "Northeast",
        BigDecimal.decimal(390757.00),
        None,
        1,
        None,
        None,
        None,
        "Test",
        "Test",
        None,
        None,
        "T5"
      )
      val newCountry2 = Country(
        "T6",
        "Test6",
        Country.Continent.North_America,
        "Northeast",
        BigDecimal.decimal(390757.00),
        None,
        1,
        None,
        None,
        None,
        "Test",
        "Test",
        None,
        None,
        "T6"
      )
      val result = (country ++= List(newCountry1, newCountry2)).update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 2
    }

    "Only specified items can be added to the data." in {
      val result = city
        .insertInto(v => (v.name, v.countryCode, v.district, v.population))
        .values(("Test", "T1", "T", 1))
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 1
    }

    "Multiple additions of data can be made only for specified items." in {
      val result = city
        .insertInto(v => (v.name, v.countryCode, v.district, v.population))
        .values(List(("Test2", "T2", "T", 1), ("Test3", "T3", "T3", 2)))
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 2
    }

    "A stand-alone update succeeds." in {
      val result = city
        .update("district", "Tokyo-test")
        .where(_.name _equals "Tokyo")
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 1
    }

    "A stand-alone update from the model will be successful." in {
      (for
        cityOpt <- city.selectAll.where(_.countryCode _equals "JPN").and(_.name _equals "Tokyo").headOption[City]
        result <- cityOpt match
                    case None => ConnectionIO.pure[IO, Int](0)
                    case Some(cityModel) =>
                      city
                        .update(cityModel.copy(district = "Tokyo-to"))
                        .where(v => (v.countryCode _equals "JPN") and (v.name _equals "Tokyo"))
                        .update
      yield result === 1)
        .transaction(dataSource)
        .unsafeRunSync()
    }

    "Multiple columns are successfully updated." in {
      val result = city
        .update("name", "Yokohama")
        .set("countryCode", "JPN")
        .set("district", "Kanagawa")
        .set("population", 2)
        .where(_.name _equals "Jokohama [Yokohama]")
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 1
    }

    "The values of columns that do not satisfy the condition are not updated." in {
      val result = (for
        _ <- city
               .update("name", "update Odawara")
               .set("district", "not update Kanagawa", false)
               .where(_.id _equals 1637)
               .update
        updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1637).unsafe
      yield updated)
        .transaction(dataSource)
        .unsafeRunSync()
      (result._1 === "update Odawara") and (result._2 !== Some("not update Kanagawa"))
    }

    "If the primary key is duplicated, the data is updated." in {
      val result = (for
        _       <- city.insertOrUpdates(List(City(1638, "update Kofu", "JPN", "Yamanashi", 199753))).update
        updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1638).unsafe
      yield updated)
        .transaction(dataSource)
        .unsafeRunSync()
      (result._1 === "update Kofu") and (result._2 !== Some("not update Yamanashi"))
    }

    "If there are duplicate primary keys, only the specified columns are updated." in {
      val result = (for
        _ <- (city += City(1639, "update Kushiro", "JPN", "not update Hokkaido", 197608))
               .onDuplicateKeyUpdate(_.name)
               .update
        updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1639).unsafe
      yield updated)
        .transaction(dataSource)
        .unsafeRunSync()
      (result._1 === "update Kushiro") and (result._2 !== Some("not update Hokkaido"))
    }

    "Data is added if the primary key is not duplicated." in {
      (for
        empty <- city.selectAll.where(_.id _equals 5000).headOption
        _     <- city.insertOrUpdate((5000, "Nishinomiya", "JPN", "Hyogo", 0)).update
        data  <- city.selectAll.where(_.id _equals 5000).headOption
      yield empty.isEmpty and data.nonEmpty)
        .transaction(dataSource)
        .unsafeRunSync()
    }

    "The update succeeds in the combined processing of multiple queries." in {
      (for
        codeOpt <- country
                     .select(_.code)
                     .where(_.name _equals "Test1")
                     .and(_.continent _equals Country.Continent.Asia)
                     .headOption
        result <- codeOpt match
                    case None => ConnectionIO.pure[IO, Int](0)
                    case Some(code *: EmptyTuple) =>
                      city
                        .update("name", "Test1")
                        .set("countryCode", code)
                        .set("district", "TT")
                        .set("population", 2)
                        .where(_.name _equals "Test2")
                        .update
      yield result === 1)
        .transaction(dataSource)
        .unsafeRunSync()
    }

    "Bulk renewal succeeds." in {
      val result = countryLanguage
        .update("isOfficial", CountryLanguage.IsOfficial.T)
        .where(_.countryCode _equals "JPN")
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 6
    }

    "Successful batch update with specified number." in {
      val result = countryLanguage
        .update("isOfficial", CountryLanguage.IsOfficial.T)
        .where(_.countryCode _equals "JPN")
        .limit(3)
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 3
    }

    "Deletion by itself is successful." in {
      val result = country.delete.where(_.code _equals "T5").update.autoCommit(dataSource).unsafeRunSync()
      result === 1
    }

    "The number of deletions in multiple cases matches the number specified." in {
      val result =
        countryLanguage.delete.where(_.countryCode _equals "AFG").update.autoCommit(dataSource).unsafeRunSync()
      result === 5
    }
  }
