/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import ldbc.dsl.*
import ldbc.dsl.codec.{ Encoder, Decoder }
import ldbc.query.builder.Table

case class CountryLanguage(
  countryCode: String,
  language:    String,
  isOfficial:  CountryLanguage.IsOfficial,
  percentage:  BigDecimal
) derives Table

object CountryLanguage:

  enum IsOfficial:
    case T, F

  object IsOfficial

  given Encoder[IsOfficial] with
    override def encode(isOfficial: IsOfficial): String = isOfficial.toString

  given Decoder.Elem[IsOfficial] =
    Decoder.Elem.mapping[String, IsOfficial](str => IsOfficial.valueOf(str))
