/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.duration.DurationInt

import com.mysql.cj.jdbc.MysqlDataSource

import cats.syntax.all.*

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import munit.*

import ldbc.sql.*
import ldbc.connector.SSL
import ldbc.query.builder.*
import ldbc.query.builder.syntax.io.*
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

  given Tracer[IO] = Tracer.noop[IO]

  def prefix:     "jdbc" | "ldbc"
  def connection: Resource[IO, Connection[IO]]

  private final val country         = TableQuery[Country]("country")
  private final val city            = TableQuery[City]("city")
  private final val countryLanguage = TableQuery[CountryLanguage]("countrylanguage")

  private def code(index: Int): String = prefix match
    case "jdbc" => s"J$index"
    case "ldbc" => s"L$index"

  test(
    "New data can be registered with the value of Tuple."
  ) {
    assertIO(
      connection.use { conn =>
        country
          .insert(
            (
              code(1),
              s"${ prefix }_Test1",
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
              code(1)
            )
          )
          .update
          .commit(conn)
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
            List(
              (
                code(2),
                s"${ prefix }_Test2",
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
                code(2)
              ),
              (
                code(3),
                s"${ prefix }_Test3",
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
                code(3)
              )
            )*
          )
          .update
          .commit(conn)
      },
      2
    )
  }

  test(
    "New data can be registered from the model."
  ) {
    val newCountry = Country(
      code(4),
      s"${ prefix }_Test4",
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
      code(4)
    )
    assertIO(
      connection.use { conn =>
        (country += newCountry).update
          .commit(conn)
      },
      1
    )
  }

  test(
    "New data can be registered from the model."
  ) {
    val newCountry1 = Country(
      code(5),
      s"${ prefix }_Test5",
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
      code(5)
    )
    val newCountry2 = Country(
      code(6),
      s"${ prefix }_Test6",
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
      code(6)
    )
    assertIO(
      connection.use { conn =>
        (country ++= List(newCountry1, newCountry2)).update
          .commit(conn)
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
          .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
          .values(("Test", code(1), "T", 1))
          .update
          .commit(conn)
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
          .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
          .values(
            ("Test2", code(2), "T", 1),
            ("Test3", code(3), "T3", 2)
          )
          .update
          .commit(conn)
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
          .update(_.district)("Tokyo-test")
          .where(_.name _equals "Tokyo")
          .update
          .commit(conn)
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
          cityOpt <-
            city.selectAll.where(_.countryCode _equals "JPN").and(_.name _equals "Tokyo").query.to[Option]
          result <- cityOpt match
                      case None => Executor.pure[IO, Int](0)
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
          .update(c => c.name *: c.countryCode *: c.district *: c.population)(
            ("Jokohama [Yokohama]", "JPN", "Kanagawa", 2)
          )
          .where(_.name _equals "Jokohama [Yokohama]")
          .update
          .rollback(conn)
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
                 .update(_.name)("update Odawara")
                 .set(_.district, "not update Kanagawa", false)
                 .where(_.id _equals 1637)
                 .update
          updated <- city.select(v => v.name *: v.district).where(_.id _equals 1637).query.unsafe
        yield updated)
          .transaction(conn)
      },
      ("update Odawara", "Kanagawa")
    )
  }

  test(
    "If the primary key is duplicated, the data is updated."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          _ <-
            city
              .insert((1638, "update Kofu", "JPN", "Yamanashi", 199753))
              .onDuplicateKeyUpdate(_.name)
              .update
          updated <- city.select(v => v.name *: v.district).where(_.id _equals 1638).query.unsafe
        yield updated)
          .transaction(conn)
      },
      ("update Kofu", "Yamanashi")
    )
  }

  test(
    "If there are duplicate primary keys, only the specified columns are updated."
  ) {
    assertIO(
      connection.use { conn =>
        (for
          _ <- (city += City(1639, "update Kushiro", "JPN", "not update Hokkaido", 197608))
                 .onDuplicateKeyUpdate(_.name)
                 .update
          updated <- city.select(v => v.name *: v.district).where(_.id _equals 1639).query.unsafe
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
          length <- city.select(_.id.count).query.unsafe.map(_ + 1)
          empty  <- city.selectAll.where(_.id _equals length).query.to[Option]
          _      <- city.insert((length, "Nishinomiya", "JPN", "Hyogo", 0)).onDuplicateKeyUpdate(_.name).update
          data   <- city.selectAll.where(_.id _equals length).query.to[Option]
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
          length <- city.select(_.id.count).query.unsafe.map(_ + 1)
          result <-
            city
              .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
              .values(("Test4", code(4), "T", 1))
              .returning[Int]
        yield result === length)
          .commit(conn)
      }
    )
  }
  
  test("") {
    assertIO(
      connection.use { conn =>
        city
          .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
          .select(
            country
              .select(c => c.name *: c.code *: c.region *: c.population)
              .where(_.code _equals "JPN")
          )
          .update
          .commit(conn)
      },
      1
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
                       .where(_.name _equals "United States")
                       .and(_.continent _equals Country.Continent.North_America)
                       .query
                       .to[Option]
          result <- codeOpt match
                      case None => Executor.pure[IO, Int](0)
                      case Some(code) =>
                        city
                          .update(c => c.name *: c.district *: c.population)(("update New York", "TT", 2))
                          .where(v => v.name _equals "New York" and (v.countryCode _equals code))
                          .update
        yield result)
          .rollback(conn)
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
          .update(_.isOfficial)(CountryLanguage.IsOfficial.T)
          .where(_.countryCode _equals "JPN")
          .update
          .commit(conn)
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
          .update(_.isOfficial)(CountryLanguage.IsOfficial.T)
          .where(_.countryCode _equals "JPN")
          .limit(3)
          .update
          .commit(conn)
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
          .where(v => v.code _equals code(5) or (v.code _equals code(6)))
          .update
          .commit(conn)
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
