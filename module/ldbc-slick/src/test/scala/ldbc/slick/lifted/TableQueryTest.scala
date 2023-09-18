/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.slick.lifted

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.mysql.cj.jdbc.MysqlDataSource

import org.scalatest.flatspec.AnyFlatSpec

import ldbc.core.model.*
import ldbc.slick.jdbc.MySQLProfile.api.*

case class Country(
                    code:           String,
                    name:           String,
                    continent:      Country.Continent,
                    region:         String,
                    surfaceArea:    BigDecimal,
                    indepYear:      Option[Short],
                    population:     Int,
                    lifeExpectancy: Option[BigDecimal],
                    gnp:            Option[BigDecimal],
                    gnpOld:         Option[BigDecimal],
                    localName:      String,
                    governmentForm: String,
                    headOfState:    Option[String],
                    capital:        Option[Int],
                    code2:          String
                  )

object Country:

  enum Continent(val value: String) extends Enum:
    case Asia          extends Continent("Asia")
    case Europe        extends Continent("Europe")
    case North_America extends Continent("North America")
    case Africa        extends Continent("Africa")
    case Oceania       extends Continent("Oceania")
    case Antarctica    extends Continent("Antarctica")
    case South_America extends Continent("South America")

    override def toString: String = value
  object Continent extends EnumDataType[Continent]

  given BaseColumnType[Continent] = MappedColumnType.base[Continent, String](
    { continent => continent.value },
    { str => Continent.valueOf(str.replace(" ", "_")) }
  )

  val table = Table[Country]("country")(
    column("Code", CHAR(3).DEFAULT(""), PRIMARY_KEY),
    column("Name", CHAR(52).DEFAULT("")),
    column("Continent", ENUM(using Continent).DEFAULT(Continent.Asia)),
    column("Region", CHAR(26).DEFAULT("")),
    column("SurfaceArea", DECIMAL(10, 2).DEFAULT(0.00)),
    column("IndepYear", SMALLINT.DEFAULT(None)),
    column("Population", INT.DEFAULT(0)),
    column("LifeExpectancy", DECIMAL(3, 1).DEFAULT(None)),
    column("GNP", DECIMAL(10, 2).DEFAULT(None)),
    column("GNPOld", DECIMAL(10, 2).DEFAULT(None)),
    column("LocalName", CHAR(45).DEFAULT("")),
    column("GovernmentForm", CHAR(45).DEFAULT("")),
    column("HeadOfState", CHAR(60).DEFAULT(None)),
    column("Capital", INT.DEFAULT(None)),
    column("Code2", CHAR(2).DEFAULT(""))
  )

class TableQueryTest extends AnyFlatSpec:

  private val dataSource = new MysqlDataSource()
  dataSource.setServerName("127.0.0.1")
  dataSource.setPortNumber(13306)
  dataSource.setDatabaseName("world")
  dataSource.setUser("ldbc")
  dataSource.setPassword("password")

  private val table = SlickTableQuery[Country](Country.table)
  private val db = Database.forDataSource(dataSource, None)

  it should "TableQuery Test" in {
    val result = Await.result(db.run(table.filter(_.population > 126713999).filter(_.population < 126714001).result.headOption), Duration.Inf)
    db.close
    assert(result === Some(
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
    ))
  }
