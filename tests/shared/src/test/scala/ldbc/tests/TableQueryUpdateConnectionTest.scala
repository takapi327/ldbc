/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.data.NonEmptyList
import cats.syntax.all.*

import cats.effect.*

import munit.*

import ldbc.dsl.*

import ldbc.query.builder.TableQuery

import ldbc.connector.*

import ldbc.tests.model.*
import ldbc.Connector

class LdbcTableQueryUpdateConnectionTest extends TableQueryUpdateConnectionTest:

  override def prefix: "jdbc" | "ldbc" = "ldbc"

  private val datasource = MySQLDataSource
    .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
    .setPassword(MySQLTestConfig.password)
    .setDatabase("world2")
    .setSSL(SSL.Trusted)

  override def connector: Connector[IO] = Connector.fromDataSource(datasource)

trait TableQueryUpdateConnectionTest extends CatsEffectSuite:

  def prefix:    "jdbc" | "ldbc"
  def connector: Connector[IO]

  private final val country         = TableQuery[Country]
  private final val city            = TableQuery[City]
  private final val countryLanguage = TableQuery[CountryLanguage]

  private def code(index: Int): String = prefix match
    case "jdbc" => s"J$index"
    case "ldbc" => s"L$index"

  private def cleanup: IO[Unit] =
    (for
      _ <- sql"DELETE FROM city WHERE CountryCode IN (${ code(1) }, ${ code(2) }, ${ code(3) }, ${ code(4) })".update
      _ <- sql"DELETE FROM city WHERE Name = 'Nishinomiya' AND CountryCode = 'JPN'".update
      _ <- sql"DELETE FROM city WHERE Name = 'Test4' AND CountryCode = ${ code(4) }".update
      _ <- sql"DELETE FROM city WHERE Name = 'Japan' AND CountryCode = 'JPN' AND District = 'Kanto'".update
      _ <- sql"DELETE FROM country WHERE Code IN (${ code(1) }, ${ code(2) }, ${ code(3) }, ${ code(4) }, ${ code(5) }, ${ code(6) })".update
    yield ()).commit(connector)

  override def munitFixtures = List(
    ResourceSuiteLocalFixture(
      "cleanup",
      Resource.make(cleanup)(_ => cleanup)
    )
  )

  test(
    "New data can be registered with the value of Tuple."
  ) {
    assertIO(
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
        .commit(connector),
      1
    )
  }

  test(
    "New data can be registered with the value of Tuple."
  ) {
    assertIO(
      country
        .insert(
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
        )
        .update
        .commit(connector),
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
      (country += newCountry).update.commit(connector),
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
      (country ++= NonEmptyList.of(newCountry1, newCountry2)).update.commit(connector),
      2
    )
  }

  test(
    "Only specified items can be added to the data."
  ) {
    assertIO(
      city
        .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
        .values(("Test", code(1), "T", 1))
        .update
        .commit(connector),
      1
    )
  }

  test(
    "Multiple additions of data can be made only for specified items."
  ) {
    assertIO(
      city
        .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
        .values(
          ("Test2", code(2), "T", 1),
          ("Test3", code(3), "T3", 2)
        )
        .update
        .commit(connector),
      2
    )
  }

  test(
    "A stand-alone update succeeds."
  ) {
    assertIO(
      city
        .update(_.district)("Tokyo-test")
        .where(_.name _equals "Tokyo")
        .update
        .commit(connector),
      1
    )
  }

  test(
    "A stand-alone update from the model will be successful."
  ) {
    assertIO(
      (for
        cityOpt <-
          city.selectAll.where(_.countryCode _equals "JPN").and(_.name _equals "Tokyo").query.to[Option]
        result <- cityOpt match
                    case None            => DBIO.pure(0)
                    case Some(cityModel) =>
                      city
                        .update(cityModel.copy(district = "Tokyo-to"))
                        .where(v => (v.countryCode _equals "JPN") and (v.name _equals "Tokyo"))
                        .update
      yield result)
        .transaction(connector),
      1
    )
  }

  test(
    "Multiple columns are successfully updated."
  ) {
    assertIO(
      city
        .update(c => c.name *: c.countryCode *: c.district *: c.population)(
          ("Jokohama [Yokohama]", "JPN", "Kanagawa", 2)
        )
        .where(_.name _equals "Jokohama [Yokohama]")
        .update
        .rollback(connector),
      1
    )
  }

  test(
    "The values of columns that do not satisfy the condition are not updated."
  ) {
    assertIO(
      (for
        _ <- city
               .update(_.name)("update Odawara")
               .set(_.district, "not update Kanagawa", false)
               .where(_.id _equals 1637)
               .update
        updated <- city.select(v => v.name *: v.district).where(_.id _equals 1637).query.unsafe
      yield updated)
        .transaction(connector),
      ("update Odawara", "Kanagawa")
    )
  }

  test(
    "If the primary key is duplicated, the data is updated."
  ) {
    assertIO(
      (for
        _ <-
          city
            .insert((1638, "update Kofu", "JPN", "Yamanashi", 199753))
            .onDuplicateKeyUpdate(_.name)
            .update
        updated <- city.select(v => v.name *: v.district).where(_.id _equals 1638).query.unsafe
      yield updated)
        .transaction(connector),
      ("update Kofu", "Yamanashi")
    )
  }

  test(
    "If there are duplicate primary keys, only the specified columns are updated."
  ) {
    assertIO(
      (for
        _ <- (city += City(1639, "update Kushiro", "JPN", "not update Hokkaido", 197608))
               .onDuplicateKeyUpdate(_.name)
               .update
        updated <- city.select(v => v.name *: v.district).where(_.id _equals 1639).query.unsafe
      yield updated)
        .transaction(connector),
      ("update Kushiro", "Hokkaido")
    )
  }

  test(
    "Data is added if the primary key is not duplicated."
  ) {
    assertIOBoolean(
      (for
        length <- city.select(_.id.count).query.unsafe.map(_ + 1)
        empty  <- city.selectAll.where(_.id _equals length).query.to[Option]
        _      <- city.insert((length, "Nishinomiya", "JPN", "Hyogo", 0)).onDuplicateKeyUpdate(_.name).update
        data   <- city.selectAll.where(_.id _equals length).query.to[Option]
      yield empty.isEmpty & data.nonEmpty)
        .transaction(connector)
    )
  }

  test(
    "The value of AutoIncrement obtained during insert matches the specified value."
  ) {
    assertIOBoolean(
      (for
        length <- city.select(_.id.count).query.unsafe.map(_ + 1)
        result <-
          city
            .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
            .values(("Test4", code(4), "T", 1))
            .returning[Int]
      yield result === length)
        .commit(connector)
    )
  }

  test("The value of AutoIncrement obtained during insert matches the specified value") {
    assertIO(
      city
        .insertInto(v => v.name *: v.countryCode *: v.district *: v.population)
        .select(
          country
            .select(c => c.name *: c.code *: c.region *: c.population)
            .where(_.code _equals "JPN")
        )
        .update
        .commit(connector),
      1
    )
  }

  test(
    "The update succeeds in the combined processing of multiple queries."
  ) {
    assertIO(
      (for
        codeOpt <- country
                     .select(_.code)
                     .where(_.name _equals "United States")
                     .and(_.continent _equals Country.Continent.North_America)
                     .query
                     .to[Option]
        result <- codeOpt match
                    case None       => DBIO.pure(0)
                    case Some(code) =>
                      city
                        .update(c => c.name *: c.district *: c.population)(("update New York", "TT", 2))
                        .where(v => v.name _equals "New York" and (v.countryCode _equals code))
                        .update
      yield result)
        .rollback(connector),
      1
    )
  }

  test(
    "Bulk renewal succeeds."
  ) {
    assertIO(
      countryLanguage
        .update(_.isOfficial)(CountryLanguage.IsOfficial.T)
        .where(_.countryCode _equals "JPN")
        .update
        .commit(connector),
      6
    )
  }

  test(
    "Successful batch update with specified number."
  ) {
    assertIO(
      countryLanguage
        .update(_.isOfficial)(CountryLanguage.IsOfficial.T)
        .where(_.countryCode _equals "JPN")
        .limit(3)
        .update
        .commit(connector),
      3
    )
  }

  test(
    "Deletion by itself is successful."
  ) {
    assertIO(
      country.delete
        .where(v => v.code _equals code(5) or (v.code _equals code(6)))
        .update
        .commit(connector),
      2
    )
  }

  test(
    "The number of deletions in multiple cases matches the number specified."
  ) {
    assertIO(
      countryLanguage.delete
        .where(_.countryCode _equals "AFG")
        .update
        .rollback(connector),
      5
    )
  }
