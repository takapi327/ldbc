/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick.lifted

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.slick.jdbc.MySQLProfile.api.*

import model.{ Country, City, CountryLanguage }

class TableQueryTest extends AnyFlatSpec:

  it should "The select query statement generated from Table is equal to the specified query statement." in {
    assert(
      TableQueryTest.countryQuery1 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country`"
    )
    assert(
      TableQueryTest.countryQuery2 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country`"
    )
    assert(
      TableQueryTest.countryQuery3 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` where `Code` = 'JPN'"
    )
    assert(
      TableQueryTest.countryQuery4 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` where (`Population` > 126713999) and (`Population` < 126714001)"
    )
    assert(
      TableQueryTest.countryQuery5 === "select `Code` from `country`"
    )
    assert(
      TableQueryTest.countryQuery6 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` limit 10,5"
    )
    assert(
      TableQueryTest.countryQuery7 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` order by isnull(`IndepYear`) desc,`IndepYear` desc"
    )
    assert(
      TableQueryTest.countryQuery8 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` where (`Code` = 'JPN') or (`Code` = 'JOR')"
    )
    assert(
      TableQueryTest.countryQuery9 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` where `Code` = 'JPN'"
    )
    assert(
      TableQueryTest.countryQuery10 === "select `Code`, `Name`, `Continent`, `Region`, `SurfaceArea`, `IndepYear`, `Population`, `LifeExpectancy`, `GNP`, `GNPOld`, `LocalName`, `GovernmentForm`, `HeadOfState`, `Capital`, `Code2` from `country` where `Code` = 'JPN'"
    )
    assert(TableQueryTest.cityQuery1 === "select min(`Population`) from `city`")
    assert(TableQueryTest.cityQuery2 === "select max(`Population`) from `city`")
    assert(TableQueryTest.cityQuery3 === "select sum(`Population`) from `city`")
    assert(TableQueryTest.cityQuery4 === "select avg(`Population`) from `city`")
    assert(TableQueryTest.cityQuery5 === "select count(1) from `city`")
    assert(
      TableQueryTest.cityQuery6 === "select exists(select `District`, `CountryCode`, `Name`, `ID`, `Population` from `city`)"
    )
    assert(
      TableQueryTest.cityQuery7 === "(select `Population` as x2 from `city`) union (select `Population` as x2 from `city`)"
    )

    // assert(
    //  TableQueryTest.cityQuery1 === "select x2.`Name`, x3.`Name` from `country` x2, `city` x3"
    // )
    // assert(
    //  TableQueryTest.cityQuery2 === "select x2.`Name`, x3.`Name` from `country` x2, `city` x3 where x2.`Code` = x3.`CountryCode`"
    // )
    // assert(
    //  TableQueryTest.cityQuery3 === "select x2.`Name`, x3.`Name` from `country` x2, `city` x3 where x2.`Code` = x3.`CountryCode`"
    // )
  }

  it should "The insert query statement generated from Table is equal to the specified query statement." in {
    assert(
      TableQueryTest.countryLanguageQuery1 === "insert into `countrylanguage` (`CountryCode`,`Language`,`IsOfficial`,`Percentage`)  values (?,?,?,?)"
    )
    assert(
      TableQueryTest.countryLanguageQuery2 === "insert into `countrylanguage` (`CountryCode`,`Language`,`IsOfficial`,`Percentage`)  values (?,?,?,?)"
    )
    assert(
      TableQueryTest.countryLanguageQuery3 === "insert into `countrylanguage` (`CountryCode`,`Language`)  values (?,?)"
    )
    assert(
      TableQueryTest.cityQuery8 === "insert into `city` (`ID`,`Name`,`CountryCode`,`District`,`Population`)  values (?,?,?,?,?)"
    )
  }

  it should "The update query statement generated from Table is equal to the specified query statement." in {
    assert(TableQueryTest.countryLanguageQuery4 === "update `countrylanguage` set `CountryCode` = ?, `Language` = ?")
    assert(
      TableQueryTest.countryLanguageQuery5 === "update `countrylanguage` set `CountryCode` = ?, `Language` = ? where `countrylanguage`.`CountryCode` = 'JPN'"
    )
    // assert(TableQueryTest.countryLanguageQuery6 === "")
  }

object TableQueryTest:

  private val countries        = SlickTableQuery[Country](Country.table)
  private val cities           = SlickTableQuery[City](City.table)
  private val countryLanguages = SlickTableQuery[CountryLanguage](CountryLanguage.table)

  val countryQuery1 = countries.result.headOption.statements.head
  val countryQuery2 = countries.result.statements.head
  val countryQuery3 = countries.filter(_.code === "JPN").result.headOption.statements.head
  val countryQuery4 =
    countries.filter(_.population > 126713999).filter(_.population < 126714001).result.headOption.statements.head
  val countryQuery5 = countries.map(_.code).result.headOption.statements.head
  val countryQuery6 = countries.drop(10).take(5).result.headOption.statements.head
  val countryQuery7 = countries.sortBy(_.indepYear.desc.nullsFirst).result.headOption.statements.head
  val countryQuery8 = countries
    .filter { country =>
      List(
        Some("JPN").map(country.code === _),
        Some("JOR").map(country.code === _),
        None
      )
        .collect {
          case Some(criteria) => criteria
        }
        .reduceLeftOption(_ || _)
        .getOrElse(true: Rep[Boolean])
    }
    .result
    .statements
    .head
  val none: Option[String] = None
  val countryQuery9 =
    countries.filterOpt(Some("JPN"))(_.code === _).filterOpt(none)(_.code === _).result.statements.head
  val countryQuery10 =
    countries.filterIf(true)(_.code === "JPN").filterIf(false)(_.code === "JOR").result.statements.head

  val cityQuery1 = cities.map(_.population).min.result.statements.head
  val cityQuery2 = cities.map(_.population).max.result.statements.head
  val cityQuery3 = cities.map(_.population).sum.result.statements.head
  val cityQuery4 = cities.map(_.population).avg.result.statements.head
  val cityQuery5 = cities.length.result.statements.head
  val cityQuery6 = cities.exists.result.statements.head
  val cityQuery7 = (cities.map(_.population) union cities.map(_.population)).result.statements.head
  val cityQuery8 = ((cities returning cities
    .map(_.id) into ((ciry, id) => ciry.copy(id = id))) += City(0, "", "", "", 0)).statements.head

  // val cityQuery1 = (countries join cities).map((country, city) => (country.name, city.name)).result.headOption.statements.head
  // val cityQuery2 = (countries join cities on (_.code === _.countryCode)).map((country, city) => (country.name, city.name)).result.headOption.statements.head
  // val cityQuery3 = (countries joinLeft cities on (_.code === _.countryCode)).map((country, city) => (country.name, city.map(_.name))).result.headOption.statements.head
  // val cityQuery4 = (countries joinRight cities on (_.code === _.countryCode)).map((country, city) => (country.map(_.name), city.name)).result.headOption.statements.head

  val countryLanguageQuery1 = (countryLanguages += CountryLanguage(
    "JPN",
    "",
    CountryLanguage.IsOfficial.T,
    BigDecimal.decimal(2.2)
  )).statements.head
  val countryLanguageQuery2 = (countryLanguages ++= Seq(
    CountryLanguage("JPN", "", CountryLanguage.IsOfficial.T, BigDecimal.decimal(2.2)),
    CountryLanguage("JPN", "", CountryLanguage.IsOfficial.T, BigDecimal.decimal(2.2))
  )).statements.head
  val countryLanguageQuery3 = (countryLanguages.map(c => (c.countryCode, c.language)) += ("JPN", "")).statements.head
  val countryLanguageQuery4 =
    countryLanguages.map(c => (c.countryCode, c.language)).update(("JPN", "Update")).statements.head
  val countryLanguageQuery5 = countryLanguages
    .filter(_.countryCode === "JPN")
    .map(c => (c.countryCode, c.language))
    .update(("JPN", "Update"))
    .statements
    .head
  // val countryLanguageQuery6 = countryLanguages.insertOrUpdate(CountryLanguage("JPN", "", CountryLanguage.IsOfficial.T, BigDecimal.decimal(2.2))).statements.head
