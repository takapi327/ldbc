/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.builder

import ldbc.core.Database

/** Object for generating Jdbc URLs based on Database type.
  */
object JdbcUrlBuilder:

  def build(database: Database): String =
    database.port match
      case Some(port) => s"jdbc:${ database.databaseType.name }://${ database.host }:$port/${ database.name }"
      case None       => s"jdbc:${ database.databaseType.name }://${ database.host }/${ database.name }"
