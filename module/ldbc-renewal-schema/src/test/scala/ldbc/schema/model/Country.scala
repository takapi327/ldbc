/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.model

import ldbc.dsl.codec.*
import ldbc.schema.*

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

  enum Continent(val value: String):
    case Asia          extends Continent("Asia")
    case Europe        extends Continent("Europe")
    case North_America extends Continent("North America")
    case Africa        extends Continent("Africa")
    case Oceania       extends Continent("Oceania")
    case Antarctica    extends Continent("Antarctica")
    case South_America extends Continent("South America")

  given Encoder[Continent] with
    override def encode(continent: Continent): String = continent.value

  given Decoder.Elem[Continent] =
    Decoder.Elem.mapping[String, Continent](str => Continent.valueOf(str.replace(" ", "_")))

class CountryTable extends Table[Country]("country"):

  def code:           Column[String]             = column[String]("Code")
  def name:           Column[String]             = column[String]("Name")
  def continent:      Column[Country.Continent]  = column[Country.Continent]("Continent")
  def region:         Column[String]             = column[String]("Region")
  def surfaceArea:    Column[BigDecimal]         = column[BigDecimal]("SurfaceArea")
  def indepYear:      Column[Option[Short]]      = column[Option[Short]]("IndepYear")
  def population:     Column[Int]                = column[Int]("Population")
  def lifeExpectancy: Column[Option[BigDecimal]] = column[Option[BigDecimal]]("LifeExpectancy")
  def gnp:            Column[Option[BigDecimal]] = column[Option[BigDecimal]]("GNP")
  def gnpOld:         Column[Option[BigDecimal]] = column[Option[BigDecimal]]("GNPOld")
  def localName:      Column[String]             = column[String]("LocalName")
  def governmentForm: Column[String]             = column[String]("GovernmentForm")
  def headOfState:    Column[Option[String]]     = column[Option[String]]("HeadOfState")
  def capital:        Column[Option[Int]]        = column[Option[Int]]("Capital")
  def code2:          Column[String]             = column[String]("Code2")

  override def * : Column[Country] =
    (code *: name *: continent *: region *: surfaceArea *: indepYear *: population *: lifeExpectancy *: gnp *: gnpOld *: localName *: governmentForm *: headOfState *: capital *: code2)
      .to[Country]