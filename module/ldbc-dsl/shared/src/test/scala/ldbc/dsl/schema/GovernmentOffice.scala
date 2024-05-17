/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.schema

import java.time.LocalDate

import ldbc.dsl.io.*

case class GovernmentOffice(
  id:                Int,
  cityId:            Int,
  name:              String,
  establishmentDate: Option[LocalDate]
)

object GovernmentOffice:

  val table: Table[GovernmentOffice] = Table[GovernmentOffice]("government_office")(
    column("ID", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("CityID", INT),
    column("Name", CHAR(35).DEFAULT("")),
    column("EstablishmentDate", DATE)
  )
    .keySet(v => CONSTRAINT("government_office_ibfk_1", FOREIGN_KEY(v.cityId, REFERENCE(City.table, City.table.id))))
