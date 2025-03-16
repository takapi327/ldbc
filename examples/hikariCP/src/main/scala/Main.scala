/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import com.zaxxer.hikari.HikariDataSource

import cats.effect.*

import ldbc.dsl.io.*

import jdbc.connector.*

case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)

object Main extends ResourceApp.Simple:

  private val ds = new HikariDataSource()
  ds.setJdbcUrl("jdbc:mysql://127.0.0.1:13306/world")
  ds.setUsername("ldbc")
  ds.setPassword("password")

  override def run: Resource[IO, Unit] =
    (for
      hikari     <- Resource.fromAutoCloseable(IO(ds))
      execution <- ExecutionContexts.fixedThreadPool[IO](hikari.getMaximumPoolSize)
       connection <- MySQLProvider.fromDataSource[IO](hikari, execution).createConnection()
    yield connection).evalMap { conn =>
      sql"SELECT * FROM `city` WHERE ID = ${ 1 }"
        .query[City]
        .to[Option]
        .readOnly(conn)
        .map(_.foreach(println))
    }
