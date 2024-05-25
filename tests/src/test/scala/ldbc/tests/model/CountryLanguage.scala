/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import cats.effect.IO

import ldbc.core.*
import ldbc.core.model.*
import ldbc.sql.*

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

  given Parameter[IO, IsOfficial] with
    override def bind(statement: PreparedStatement[IO], index: Int, value: IsOfficial): IO[Unit] =
      statement.setString(index, value.toString)

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
