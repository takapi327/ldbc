/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.slick.jdbc

import slick.jdbc.MySQLProfile as SlickMySQLProfile

import ldbc.core.DataTypes
import ldbc.core.syntax.DataTypeConversion

import ldbc.slick.Alias
import ldbc.slick.syntax.TableSyntax
import ldbc.slick.relational.RelationalTableComponent

trait MySQLProfile extends SlickMySQLProfile, RelationalTableComponent:
  self: RelationalTableComponent =>

  trait LdbcAPI extends JdbcAPI, Alias, TableSyntax, DataTypes:
    val Table = self.SlickTable

    val SlickTableQuery = ldbc.slick.lifted.TableQuery

  override val api: LdbcAPI = new LdbcAPI {}

object MySQLProfile extends MySQLProfile
