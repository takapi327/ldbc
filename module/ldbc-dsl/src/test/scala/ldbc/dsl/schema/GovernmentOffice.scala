/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.schema

import java.time.LocalDate

import ldbc.sql.*

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
