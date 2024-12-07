/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.model

import ldbc.dsl.codec.*
import ldbc.schema.*

case class CountryLanguage(
  countryCode: String,
  language:    String,
  isOfficial:  CountryLanguage.IsOfficial,
  percentage:  BigDecimal
)

object CountryLanguage:

  enum IsOfficial:
    case T, F

  object IsOfficial

  given Encoder[IsOfficial] with
    override def encode(isOfficial: IsOfficial): String = isOfficial.toString

  given Decoder.Elem[IsOfficial] =
    Decoder.Elem.mapping[String, IsOfficial](str => IsOfficial.valueOf(str))

class CountryLanguageTable extends Table[CountryLanguage]("countrylanguage"):

  def countryCode: Column[String]                     = column[String]("CountryCode")
  def language:    Column[String]                     = column[String]("Language")
  def isOfficial:  Column[CountryLanguage.IsOfficial] = column[CountryLanguage.IsOfficial]("IsOfficial")
  def percentage:  Column[BigDecimal]                 = column[BigDecimal]("Percentage")

  override def * : Column[CountryLanguage] = (countryCode *: language *: isOfficial *: percentage).to[CountryLanguage]
