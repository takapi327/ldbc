/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.schema

import ldbc.sql.*

case class City(
                 id:          Int,
                 name:        String,
                 countryCode: String,
                 district:    String,
                 population:  Int
               )

object City:

  val table: Table[City] = Table[City]("city")(
    column("ID", INT, AUTO_INCREMENT, PRIMARY_KEY),
    column("Name", CHAR(35).DEFAULT("")),
    column("CountryCode", CHAR(3).DEFAULT("")),
    column("District", CHAR(20).DEFAULT("")),
    column("Population", INT.DEFAULT(0))
  )
    .keySet(v => INDEX_KEY(v.countryCode))
    .keySet(v => CONSTRAINT("city_ibfk_1", FOREIGN_KEY(v.countryCode, REFERENCE(Country.table, Country.table.code))))
