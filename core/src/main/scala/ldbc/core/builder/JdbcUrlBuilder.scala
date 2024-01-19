/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
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
