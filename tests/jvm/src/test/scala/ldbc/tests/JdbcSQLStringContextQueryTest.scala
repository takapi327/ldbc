/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import java.sql.SQLSyntaxErrorException

import com.mysql.cj.jdbc.MysqlDataSource

import cats.effect.*

import ldbc.sql.*
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

  override def connection: Provider[IO] =
    ConnectionProvider.fromDataSource(ds, ExecutionContexts.synchronous)

  test(
    "Attempting to decode something that does not match the value of Enum raises a SQLException."
  ) {
    enum MyEnum:
      case A, B, C

    given Codec[MyEnum] = Codec.derivedEnum[MyEnum]

    interceptIO[SQLSyntaxErrorException](
      connection.use { conn =>
        sql"SELECT D"
          .query[MyEnum]
          .to[Option]
          .readOnly(conn)
      }
    )
  }
