/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import zio.*
import zio.http.*
import zio.json.*
import zio.interop.catz.*

import ldbc.connector.*
import ldbc.zio.interop.*
import ldbc.dsl.*

object Main extends ZIOAppDefault:

  private val poolConfig = MySQLConfig.default
    .setHost("localhost")
    .setPort(13306)
    .setUser("ldbc")
    .setPassword("password")
    .setDatabase("world")
    .setSSL(SSL.Trusted)
    .setMinConnections(5)
    .setMaxConnections(10)

  private val connectorLayer: ZLayer[Any, Throwable, Connector[Task]] =
    ZLayer.scoped {
      MySQLDataSource.pooling[Task](poolConfig).map(ds => Connector.fromDataSource[Task](ds)).toScopedZIO
    }

  private val routes = Routes(
    Method.GET / Root -> handler(Response.text("Hello, World!")),
    Method.GET / Root / "countries" -> handler {
      for
        connector <- ZIO.service[Connector[Task]]
        countries <- sql"SELECT Name FROM country".query[String].to[List].readOnly(connector)
      yield Response.json(countries.toJson)
    }.catchAll { error =>
      handler(Response.json(Map("error" -> error.getMessage).toJson))
    },
  )

  override def run =
    Server.serve(routes)
      .provide(
        Server.default,
        connectorLayer
      )
