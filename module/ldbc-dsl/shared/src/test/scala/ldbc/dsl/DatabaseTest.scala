/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import org.specs2.mutable.Specification

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.sql.*
import ldbc.sql.logging.LogHandler
import ldbc.dsl.io.*
import ldbc.dsl.logging.ConsoleLogHandler
import ldbc.query.builder.TableQuery
import ldbc.dsl.schema.*

object DatabaseTest extends Specification:

  given LogHandler[IO] = ConsoleLogHandler[IO]

  private val db = Database.fromMySQLDriver[IO]("world2", "127.0.0.1", 13306, "ldbc", "password")

  private val country         = TableQuery[IO, Country](Country.table)
  private val city            = TableQuery[IO, City](City.table)
  private val countryLanguage = TableQuery[IO, CountryLanguage](CountryLanguage.table)

  "Database Connection Test" should {
    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = db.readOnly(country.selectAll.toList[Country]).unsafeRunSync()
      result.length === 239
    }

    "A method that takes a Database model as an argument is successfully processed." in {
      val result = country.selectAll.toList[Country].readOnly(db).unsafeRunSync()
      result.length === 239
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = db.readOnly(city.selectAll.toList[City]).unsafeRunSync()
      result.length === 4079
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      val result = db.readOnly(countryLanguage.selectAll.toList[CountryLanguage]).unsafeRunSync()
      result.length === 984
    }

    "The number of cases retrieved using the subquery matches the specified value." in {
      val result = db
        .readOnly(
          city
            .select(v => (v.name, v.countryCode))
            .where(_.countryCode _equals country.select(_.code).where(_.code _equals "JPN"))
            .toList
        )
        .unsafeRunSync()
      result.length === 248
    }

    "The acquired data matches the specified model." in {
      val result = db
        .readOnly(
          country.selectAll
            .where(_.code _equals "JPN")
            .headOption[Country]
        )
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
      val result = db
        .readOnly(
          city.selectAll
            .where(_.id _equals 1532)
            .headOption[City]
        )
        .unsafeRunSync()
      result === Some(City(1532, "Tokyo", "JPN", "Tokyo-to", 7980230))
    }

    "The acquired data matches the specified model." in {
      val result = db
        .readOnly(
          countryLanguage.selectAll
            .where(_.countryCode _equals "JPN")
            .and(_.language _equals "Japanese")
            .headOption[CountryLanguage]
        )
        .unsafeRunSync()
      result === Some(CountryLanguage("JPN", "Japanese", CountryLanguage.IsOfficial.T, BigDecimal.decimal(99.1)))
    }

    "The data retrieved by Join matches the specified model." in {
      val result = db
        .readOnly(
          (city join country)((city, country) => city.countryCode _equals country.code)
            .select((city, country) => (city.name, country.name))
            .where((_, country) => country.code _equals "JPN")
            .and((city, _) => city.name _equals "Tokyo")
            .headOption
        )
        .unsafeRunSync()
      result === Some(("Tokyo", "Japan"))
    }

    "The data retrieved by Join matches the specified model." in {
      case class CountryCity(cityName: String, countryName: String)

      val result = db
        .readOnly(
          (city join country)((city, country) => city.countryCode _equals country.code)
            .select((city, country) => (city.name, country.name))
            .where((_, country) => country.code _equals "JPN")
            .and((city, _) => city.name _equals "Tokyo")
            .headOption[CountryCity]
        )
        .unsafeRunSync()
      result === Some(CountryCity("Tokyo", "Japan"))
    }

    "The data retrieved by Left Join matches the specified model." in {
      val result = db
        .readOnly(
          (city leftJoin country)((city, country) => city.countryCode _equals country.code)
            .select((city, country) => (city.name, country.name))
            .where((_, country) => country.code _equals "JPN")
            .and((city, _) => city.name _equals "Tokyo")
            .headOption
        )
        .unsafeRunSync()
      result === Some(("Tokyo", Some("Japan")))
    }

    "The data retrieved by Left Join matches the specified model." in {
      case class CountryCity(cityName: String, countryName: Option[String])

      val result = db
        .readOnly(
          (city leftJoin country)((city, country) => city.countryCode _equals country.code)
            .select((city, country) => (city.name, country.name))
            .where((_, country) => country.code _equals "JPN")
            .and((city, _) => city.name _equals "Tokyo")
            .headOption[CountryCity]
        )
        .unsafeRunSync()
      result === Some(CountryCity("Tokyo", Some("Japan")))
    }

    "The data retrieved by Right Join matches the specified model." in {
      val result = db
        .readOnly(
          (city rightJoin country)((city, country) => city.countryCode _equals country.code)
            .select((city, country) => (city.name, country.name))
            .where((_, country) => country.code _equals "JPN")
            .and((city, _) => city.name _equals "Tokyo")
            .headOption
        )
        .unsafeRunSync()
      result === Some((Some("Tokyo"), "Japan"))
    }

    "The data retrieved by Right Join matches the specified model." in {
      case class CountryCity(cityName: Option[String], countryName: String)

      val result = db
        .readOnly(
          (city rightJoin country)((city, country) => city.countryCode _equals country.code)
            .select((city, country) => (city.name, country.name))
            .where((_, country) => country.code _equals "JPN")
            .and((city, _) => city.name _equals "Tokyo")
            .headOption[CountryCity]
        )
        .unsafeRunSync()
      result === Some(CountryCity(Some("Tokyo"), "Japan"))
    }

    "The retrieved data matches the specified value." in {
      val result = db
        .readOnly(
          city
            .select(v => (v.countryCode, v.id.count))
            .where(_.countryCode _equals "JPN")
            .headOption
        )
        .unsafeRunSync()
      result === Some(("JPN", 248))
    }

    "The acquired data matches the specified model." in {
      case class CountryCodeGroup(countryCode: String, length: Int)

      val result = db
        .readOnly(
          city
            .select(v => (v.countryCode, v.id.count))
            .groupBy(_._1)
            .toList[CountryCodeGroup]
        )
        .unsafeRunSync()
      result.length === 232
    }

    "The results of all cases retrieved are transformed into a model, and the number of cases matches the specified value." in {
      db.readOnly(for
        codeOpt <- country.select(_.code).where(_.code _equals "JPN").headOption
        cities <- codeOpt match
                    case None => Connection.pure[IO, List[(String, String)]](List.empty[(String, String)])
                    case Some(code *: EmptyTuple) =>
                      city
                        .select(v => (v.name, v.countryCode))
                        .where(_.countryCode _equals code)
                        .toList
      yield cities.length === 248)
        .unsafeRunSync()
    }

    "New data can be registered with the value of Tuple." in {
      val result = db
        .autoCommit(
          country
            .insert(
              (
                "D1",
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
                "D1"
              )
            )
            .update
        )
        .unsafeRunSync()

      result === 1
    }

    "New data can be registered from multiple tuple values." in {
      val result = db
        .autoCommit(
          country
            .insert(
              (
                "D2",
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
                "D2"
              ),
              (
                "D3",
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
                "D3"
              )
            )
            .update
        )
        .unsafeRunSync()

      result === 2
    }

    "New data can be registered from the model." in {
      val newCountry = Country(
        "D4",
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
        "D4"
      )
      val result = db
        .autoCommit((country += newCountry).update)
        .unsafeRunSync()

      result === 1
    }

    "New data can be registered from multiple models." in {
      val newCountry1 = Country(
        "D5",
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
        "D5"
      )
      val newCountry2 = Country(
        "D6",
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
        "D6"
      )
      val result = db
        .autoCommit(
          (country ++= List(newCountry1, newCountry2)).update
        )
        .unsafeRunSync()

      result === 2
    }

    "Only specified items can be added to the data." in {
      val result = db
        .autoCommit(
          city
            .insertInto(v => (v.name, v.countryCode, v.district, v.population))
            .values(("Test", "D1", "T", 1))
            .update
        )
        .unsafeRunSync()

      result === 1
    }

    "Multiple additions of data can be made only for specified items." in {
      val result = db
        .autoCommit(
          city
            .insertInto(v => (v.name, v.countryCode, v.district, v.population))
            .values(
              List(("Test2", "D2", "T", 1), ("Test3", "D3", "D3", 2))
            )
            .update
        )
        .unsafeRunSync()

      result === 2
    }

    "A stand-alone update succeeds." in {
      val result = db
        .autoCommit(
          city
            .update("district", "Tokyo-test")
            .where(_.name _equals "Tokyo")
            .update
        )
        .unsafeRunSync()

      result === 1
    }

    "A stand-alone update from the model will be successful." in {
      db.transaction(for
        cityOpt <- city.selectAll.where(_.countryCode _equals "JPN").and(_.name _equals "Tokyo").headOption[City]
        result <- cityOpt match
                    case None => Connection.pure[IO, Int](0)
                    case Some(cityModel) =>
                      city
                        .update(cityModel.copy(district = "Tokyo-to"))
                        .where(v => (v.countryCode _equals "JPN") and (v.name _equals "Tokyo"))
                        .update
      yield result === 1)
        .unsafeRunSync()
    }

    "Multiple columns are successfully updated." in {
      val result = db
        .autoCommit(
          city
            .update("name", "Yokohama")
            .set("countryCode", "JPN")
            .set("district", "Kanagawa")
            .set("population", 2)
            .where(_.name _equals "Jokohama [Yokohama]")
            .update
        )
        .unsafeRunSync()

      result === 1
    }

    "The values of columns that do not satisfy the condition are not updated." in {
      val result = db
        .transaction(for
          _ <- city
                 .update("name", "update Odawara")
                 .set("district", "not update Kanagawa", false)
                 .where(_.id _equals 1637)
                 .update
          updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1637).unsafe
        yield updated)
        .unsafeRunSync()
      (result._1 === "update Odawara") and (result._2 !== Some("not update Kanagawa"))
    }

    "The update succeeds in the combined processing of multiple queries." in {
      db.transaction(for
        codeOpt <- country
                     .select(_.code)
                     .where(_.name _equals "Test1")
                     .and(_.continent _equals Country.Continent.Asia)
                     .headOption
        result <- codeOpt match
                    case None => Connection.pure[IO, Int](0)
                    case Some(code *: EmptyTuple) =>
                      city
                        .update("name", "Test1")
                        .set("countryCode", code)
                        .set("district", "TT")
                        .set("population", 2)
                        .where(_.name _equals "Test2")
                        .update
      yield result === 1)
        .unsafeRunSync()
    }

    "Bulk renewal succeeds." in {
      val result = db
        .autoCommit(
          countryLanguage
            .update("isOfficial", CountryLanguage.IsOfficial.T)
            .where(_.countryCode _equals "JPN")
            .update
        )
        .unsafeRunSync()

      result === 6
    }

    "Successful batch update with specified number." in {
      val result = db
        .autoCommit(
          countryLanguage
            .update("isOfficial", CountryLanguage.IsOfficial.T)
            .where(_.countryCode _equals "JPN")
            .limit(3)
            .update
        )
        .unsafeRunSync()

      result === 3
    }

    "Deletion by itself is successful." in {
      val result = db
        .autoCommit(
          country.delete.where(_.code _equals "D5").update
        )
        .unsafeRunSync()
      result === 1
    }

    "The number of deletions in multiple cases matches the number specified." in {
      val result =
        db.autoCommit(countryLanguage.delete.where(_.countryCode _equals "AFG").update).unsafeRunSync()
      result === 5
    }
  }
