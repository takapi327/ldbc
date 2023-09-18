/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick.jdbc

import slick.jdbc.MySQLProfile as SlickMySQLProfile

import ldbc.core.{ DataTypes, Alias, Table as CoreTable }
import ldbc.slick.lifted.TableQueryBuilder

trait MySQLProfile extends SlickMySQLProfile:
  self =>

  trait LdbcAPI extends JdbcAPI, Alias, DataTypes:
    val Table: CoreTable.type = CoreTable

    val SlickTableQuery: TableQueryBuilder = TableQueryBuilder(self)

  override val api: LdbcAPI = new LdbcAPI {}

object MySQLProfile extends MySQLProfile
