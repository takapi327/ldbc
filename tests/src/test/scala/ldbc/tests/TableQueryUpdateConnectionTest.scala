/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.duration.DurationInt

import com.mysql.cj.jdbc.MysqlDataSource

import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.core.*
import ldbc.sql.*
import ldbc.sql.logging.LogHandler
import ldbc.query.builder.TableQuery
import ldbc.connector.SSL
import ldbc.dsl.io.*

import ldbc.tests.model.*

class LdbcTableQueryUpdateConnectionTest extends TableQueryUpdateConnectionTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  override def connection: Resource[IO, Connection[IO]] =
    ldbc.connector.Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("world2"),
      ssl      = SSL.Trusted
    )

class JdbcTableQueryUpdateConnectionTest extends TableQueryUpdateConnectionTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world2")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def prefix: "jdbc" | "ldbc" = "jdbc"

  override def connection: Resource[IO, Connection[IO]] =
    Resource.make(jdbc.connector.MysqlDataSource[IO](ds).getConnection)(_.close())

trait TableQueryUpdateConnectionTest extends CatsEffectSuite:

  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]

  def prefix:     "jdbc" | "ldbc"
  def connection: Resource[IO, Connection[IO]]

  private final val country         = TableQuery[IO, Country](Country.table)
  private final val city            = TableQuery[IO, City](City.table)
  private final val countryLanguage = TableQuery[IO, CountryLanguage](CountryLanguage.table)

  test(
    "New data can be registered with the value of Tuple."
  ) {
    assertIO(
      connection.use { conn =>
        country
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
          .rollback(conn)
      },
      1
    )
  }

  test(
    "New data can be registered with the value of Tuple."
  ) {
    assertIO(
      connection.use { conn =>
        country
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
          .rollback(conn)
      },
      2
    )
  }

  test(
    "New data can be registered from the model."
  ) {
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
    assertIO(
      connection.use { conn =>
        (country += newCountry).update
          .rollback(conn)
      },
      1
    )
  }

  test(
    "New data can be registered from the model."
  ) {
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
    assertIO(
      connection.use { conn =>
        (country ++= List(newCountry1, newCountry2)).update
          .autoCommit(conn)
      },
      2
    )
  }

  test(
    "Only specified items can be added to the data."
  ) {
    assertIO(
      connection.use { conn =>
        city
          .insertInto(v => (v.name, v.countryCode, v.district, v.population))
          .values(("Test", "T1", "T", 1))
          .update
          .rollback(conn)
      },
      1
    )
  }

  test(
    "Multiple additions of data can be made only for specified items."
  ) {
    assertIO(
      connection.use { conn =>
        city
          .insertInto(v => (v.name, v.countryCode, v.district, v.population))
          .values(List(("Test2", "T2", "T", 1), ("Test3", "T3", "T3", 2)))
          .update
          .rollback(conn)
      },
      2
    )
  }

  test(
    "A stand-alone update succeeds."
  ) {
    assertIO(
      connection.use { conn =>
        city
          .update("district", "Tokyo-test")
          .where(_.name _equals "Tokyo")
          .update
          .autoCommit(conn)
      },
      1
    )
  }

  test(
    "A stand-alone update from the model will be successful."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          cityOpt <- city.selectAll.where(_.countryCode _equals "JPN").and(_.name _equals "Tokyo").headOption[City]
          result <- cityOpt match
                      case None => Kleisli.pure[IO, Connection[IO], Int](0)
                      case Some(cityModel) =>
                        city
                          .update(cityModel.copy(district = "Tokyo-to"))
                          .where(v => (v.countryCode _equals "JPN") and (v.name _equals "Tokyo"))
                          .update
        yield result)
          .transaction(conn)
      },
      1
    )
  }

  test(
    "Multiple columns are successfully updated."
  ) {
    assertIO(
      connection.use { conn =>
        city
          .update("name", "Yokohama")
          .set("countryCode", "JPN")
          .set("district", "Kanagawa")
          .set("population", 2)
          .where(_.name _equals "Jokohama [Yokohama]")
          .update
          .autoCommit(conn)
      },
      1
    )
  }

  test(
    "The values of columns that do not satisfy the condition are not updated."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          _ <- city
                 .update("name", "update Odawara")
                 .set("district", "not update Kanagawa", false)
                 .where(_.id _equals 1637)
                 .update
          updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1637).unsafe
        yield updated)
          .transaction(conn)
      },
      ("update Odawara", "Kanagawa")
    )
  }

  // test(
  //  "If the primary key is duplicated, the data is updated."
  // ) {
  //  assertIO(
  //    connection.use { conn =>
  //      (for
  //        _       <- city.insertOrUpdates(List(City(1638, "update Kofu", "JPN", "Yamanashi", 199753))).update
  //        updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1638).unsafe
  //      yield updated)
  //        .transaction(conn)
  //    },
  //    ("update Kofu", "Yamanashi")
  //  )
  // }

  test(
    "If there are duplicate primary keys, only the specified columns are updated."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          _ <- (city += City(1639, "update Kushiro", "JPN", "not update Hokkaido", 197608))
                 .onDuplicateKeyUpdate(_.name)
                 .update
          updated <- city.select(v => (v.name, v.district)).where(_.id _equals 1639).unsafe
        yield updated)
          .transaction(conn)
      },
      ("update Kushiro", "Hokkaido")
    )
  }

  test(
    "Data is added if the primary key is not duplicated."
  ) {
    assertIOBoolean(
      connection.use { conn =>
        (for
          length <- city.select(_.id.count).unsafe.map(_._1 + 1)
          empty  <- city.selectAll.where(_.id _equals length).headOption
          _      <- city.insertOrUpdate((length, "Nishinomiya", "JPN", "Hyogo", 0)).update
          data   <- city.selectAll.where(_.id _equals length).headOption
        yield empty.isEmpty & data.nonEmpty)
          .transaction(conn)
      }
    )
  }

  test(
    "The value of AutoIncrement obtained during insert matches the specified value."
  ) {
    assertIOBoolean(
      IO.sleep(5.seconds) >> connection.use { conn =>
        (for
          result <- city
                      .insertInto(v => (v.name, v.countryCode, v.district, v.population))
                      .values(("Test4", "T4", "T", 1))
                      .returning("id")
          length <- city.select(_.id.count).unsafe.map(_._1)
        yield result === length)
          .transaction(conn)
      }
    )
  }

  test(
    "The update succeeds in the combined processing of multiple queries."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          codeOpt <- country
                       .select(_.code)
                       .where(_.name _equals "Test1")
                       .and(_.continent _equals Country.Continent.Asia)
                       .headOption
          result <- codeOpt match
                      case None => Kleisli.pure[IO, Connection[IO], Int](0)
                      case Some(code *: EmptyTuple) =>
                        city
                          .update("name", "Test1")
                          .set("countryCode", code)
                          .set("district", "TT")
                          .set("population", 2)
                          .where(_.name _equals "Test2")
                          .update
        yield result)
          .transaction(conn)
      },
      1
    )
  }

  test(
    "Bulk renewal succeeds."
  ) {
    assertIO(
      connection.use { conn =>
        countryLanguage
          .update("isOfficial", CountryLanguage.IsOfficial.T)
          .where(_.countryCode _equals "JPN")
          .update
          .autoCommit(conn)
      },
      6
    )
  }

  test(
    "Successful batch update with specified number."
  ) {
    assertIO(
      connection.use { conn =>
        countryLanguage
          .update("isOfficial", CountryLanguage.IsOfficial.T)
          .where(_.countryCode _equals "JPN")
          .limit(3)
          .update
          .autoCommit(conn)
      },
      3
    )
  }

  test(
    "Deletion by itself is successful."
  ) {
    assertIO(
      connection.use { conn =>
        country.delete
          .where(v => (v.code _equals "T5") or (v.code _equals "T6"))
          .update
          .autoCommit(conn)
      },
      2
    )
  }

  test(
    "The number of deletions in multiple cases matches the number specified."
  ) {
    assertIO(
      connection.use { conn =>
        countryLanguage.delete
          .where(_.countryCode _equals "AFG")
          .update
          .rollback(conn)
      },
      5
    )
  }
