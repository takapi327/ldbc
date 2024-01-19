/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.dsl

import com.mysql.cj.jdbc.MysqlDataSource

import org.specs2.mutable.Specification
import org.specs2.specification.core.Fragments
import org.specs2.specification.BeforeAfterEach

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler
import ldbc.query.builder.TableQuery

object DDLTest extends Specification, BeforeAfterEach:

  private val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  private val dataSource = DataSource[IO](ds)

  given LogHandler[IO] = LogHandler.consoleLogger

  private val tableQuery = TableQuery[IO, User](table)

  override def before: Fragments =
    step((tableQuery.createTable.update.autoCommit(dataSource) >> IO.println("Complete create table")).unsafeRunSync())

  override def after: Fragments =
    step((tableQuery.dropTable.update.autoCommit(dataSource) >> IO.println("Complete drop table")).unsafeRunSync())

  "DDL Test" should {
    "When the table is created, the number of records is 0." in {
      val result *: EmptyTuple = tableQuery.select(_.id.count).unsafe.readOnly(dataSource).unsafeRunSync()
      result === 0
    }

    "Records can be inserted into tables created before the test begins." in {
      val result = tableQuery
        .insertInto(user => (user.name, user.age))
        .values(
          List(
            ("Alice", Some(20)),
            ("Bob", Some(21)),
            ("Carol", None)
          )
        )
        .update
        .autoCommit(dataSource)
        .unsafeRunSync()

      result === 3
    }
  }

case class User(
  id:   Long,
  name: String,
  age:  Option[Int]
)

val table = Table[User]("user")(
  column("id", BIGINT, AUTO_INCREMENT, PRIMARY_KEY),
  column("name", VARCHAR(255)),
  column("age", INT)
)
