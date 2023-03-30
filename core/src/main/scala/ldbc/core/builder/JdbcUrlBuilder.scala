/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.core.builder

import ldbc.core.Database

/**
 * Object for generating Jdbc URLs based on Database type.
 */
object JdbcUrlBuilder:

  def build(database: Database): String =
    database.databaseType match
      case Database.Type.MySQL    => buildForMySQL(database)
      case Database.Type.AWSMySQL => buildForAWSMySQL(database)

  private def buildForMySQL(database: Database): String =
    s"jdbc:mysql://${ database.host }:${ database.port }/${ database.name }"

  private def buildForAWSMySQL(database: Database): String =
    s"jdbc:mysql:aws://${database.host}:${database.port}/${database.name}"
