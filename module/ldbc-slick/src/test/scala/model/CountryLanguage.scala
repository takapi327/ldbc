/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package model

import ldbc.core.model.*
import ldbc.slick.jdbc.MySQLProfile.api.*

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

  given BaseColumnType[IsOfficial] = MappedColumnType.base[IsOfficial, String](
    { _.toString },
    { str => IsOfficial.valueOf(str) }
  )

  val table = Table[CountryLanguage]("countrylanguage")(
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
