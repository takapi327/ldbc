/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }

import cats.effect.*

import ldbc.Connector
import ldbc.dsl.*
import ldbc.dsl.codec.Codec

import jdbc.connector.*

case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)
object City:
  given Codec[City] = Codec.derived[City]

object Main extends ResourceApp.Simple:

  private val config = new HikariConfig()
  config.setJdbcUrl("jdbc:mysql://127.0.0.1:13306/world")
  config.setUsername("ldbc")
  config.setPassword("password")
  config.setMaximumPoolSize(10)
  config.setMinimumIdle(5)
  config.setIdleTimeout(300000) // 5分
  config.setMaxLifetime(600000) // 10分

  private val ds = new HikariDataSource(config)

  override def run: Resource[IO, Unit] =
    (for
      hikari     <- Resource.fromAutoCloseable(IO(ds))
      execution  <- ExecutionContexts.fixedThreadPool[IO](hikari.getMaximumPoolSize)
    yield Connector.fromDataSource(MySQLDataSource.fromDataSource[IO](hikari, execution))).evalMap { conn =>
      for
        city <- sql"SELECT * FROM `city` WHERE ID = ${ 1 }".query[City].to[Option].readOnly(conn)
        // トランザクションの例
        _ <- sql"UPDATE `city` SET population = ${ city.map(_.population + 1000) }".update.transaction(conn)
      yield ()
    }
