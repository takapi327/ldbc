/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl

import com.mysql.cj.jdbc.MysqlDataSource

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import org.specs2.specification.BeforeAfterSpec

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.sql.*
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery

object DDLTest extends Specification, BeforeAfterSpec:

  private val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  private val dataSource = DataSourceIO[IO](ds)

  given LogHandler[IO] = LogHandler.consoleLogger

  private val tableQuery = TableQuery[IO, User](table)

  override def beforeSpec: Fragments =
    step((tableQuery.createTable.update.autoCommit(dataSource) >> IO.println("Complete create table")).unsafeRunSync())

  override def afterSpec: Fragments =
    step((tableQuery.dropTable.update.autoCommit(dataSource) >> IO.println("Complete drop table")).unsafeRunSync())

  "DDL Test" should {
    "When the table is created, the number of records is 0." in {
      val result *: EmptyTuple = tableQuery.select(_.id.count).unsafe.readOnly(dataSource).unsafeRunSync()
      result === 0
    }

    "Records can be inserted into tables created before the test begins." in {
      val result = tableQuery
        .insertInto(user => (user.name, user.age))
        .values(List(
          ("Alice", Some(20)),
          ("Bob", Some(21)),
          ("Carol", None)
        ))
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 3
    }
  }

case class User(
  id: Long,
  name: String,
  age: Option[Int]
)

val table = Table[User]("user")(
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("age", INT)
)
