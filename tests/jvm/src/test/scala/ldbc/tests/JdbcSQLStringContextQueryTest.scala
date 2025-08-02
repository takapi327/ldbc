/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import java.sql.{ SQLException, SQLSyntaxErrorException }

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import ldbc.Connector
import ldbc.dsl.*
import ldbc.dsl.codec.Codec

import jdbc.connector.*

class JdbcSQLStringContextQueryTest extends SQLStringContextQueryTest:

  val ds = new MysqlDataSource()
  ds.setServerName("127.0.0.1")
  ds.setPortNumber(13306)
  ds.setDatabaseName("world")
  ds.setUser("ldbc")
  ds.setPassword("password")

  override def connector: Connector[IO] = Connector.fromDataSource(MySQLDataSource.fromDataSource(ds, ExecutionContexts.synchronous))

  test(
    "Attempting to decode something that does not match the value of Enum raises a SQLException."
  ) {
    enum MyEnum:
      case A, B, C

    given Codec[MyEnum] = Codec.derivedEnum[MyEnum]

    interceptIO[SQLSyntaxErrorException](
      sql"SELECT D"
        .query[MyEnum]
        .to[Option]
        .readOnly(connector)
    )
  }

  test(
    "If the number of columns retrieved is different from the number of fields in the class to be mapped, an exception is raised."
  ) {
    case class City(id: Int, name: String, age: Int)

    interceptIO[SQLException](
      sql"SELECT Id, Name FROM city LIMIT 1"
        .query[City]
        .to[Option]
        .readOnly(connector)
    )
  }
