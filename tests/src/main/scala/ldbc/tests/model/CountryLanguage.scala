/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests.model

import ldbc.sql.PreparedStatement
import ldbc.dsl.*
import ldbc.dsl.codec.Decoder
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

  given Parameter[IsOfficial] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, value: IsOfficial): F[Unit] =
      statement.setString(index, value.toString)

  given Decoder.Elem[IsOfficial] =
    Decoder.Elem.mapping[String, IsOfficial](str => IsOfficial.valueOf(str))
