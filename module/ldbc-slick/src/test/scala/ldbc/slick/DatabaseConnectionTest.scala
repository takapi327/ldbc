/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.slick

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.mysql.cj.jdbc.MysqlDataSource

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.slick.jdbc.MySQLProfile.api.*

import model.Country

class DatabaseConnectionTest extends AnyFlatSpec:

  private val dataSource = new MysqlDataSource()
  dataSource.setServerName("127.0.0.1")
  dataSource.setPortNumber(13306)
  dataSource.setDatabaseName("world")
  dataSource.setUser("ldbc")
  dataSource.setPassword("password")

  private val table = SlickTableQuery[Country](Country.table)
  private val db    = Database.forDataSource(dataSource, None)

  it should "TableQuery Test" in {
    val result = Await.result(
      db.run(table.filter(_.population > 126713999).filter(_.population < 126714001).result.headOption),
      Duration.Inf
    )
    db.close
    assert(
      result === Some(
        Country(
          "JPN",
          "Japan",
          Country.Continent.Asia,
          "Eastern Asia",
          BigDecimal.decimal(377829.00),
          Some(-660),
          126714000,
          Some(BigDecimal.decimal(80.7)),
          Some(BigDecimal.decimal(3787042.00)),
          Some(BigDecimal.decimal(4192638.00)),
          "Nihon/Nippon",
          "Constitutional Monarchy",
          Some("Akihito"),
          Some(1532),
          "JP"
        )
      )
    )
  }
