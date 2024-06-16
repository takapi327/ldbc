/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import cats.effect.IO

import ldbc.sql.PreparedStatement
import ldbc.dsl.*
import ldbc.query.builder.Table

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
) derives Table

object Country:

  enum Continent(val value: String):
    case Asia          extends Continent("Asia")
    case Europe        extends Continent("Europe")
    case North_America extends Continent("North America")
    case Africa        extends Continent("Africa")
    case Oceania       extends Continent("Oceania")
    case Antarctica    extends Continent("Antarctica")
    case South_America extends Continent("South America")

    override def toString: String = value

  given Parameter[Continent] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: Continent): F[Unit] =
      statement.setString(index, value.toString)

  given ResultSetReader[IO, Continent] =
    ResultSetReader.mapping[IO, String, Continent](str => Continent.valueOf(str.replace(" ", "_")))
