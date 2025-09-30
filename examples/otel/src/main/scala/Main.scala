/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*

import org.typelevel.otel4s.oteljava.OtelJava

import ldbc.dsl.*

import ldbc.connector.*

import io.opentelemetry.api.GlobalOpenTelemetry

object Main extends IOApp.Simple:

  private val serviceName = "ldbc-otel-example"

  private val dataSource = MySQLDataSource
    .build[IO]("127.0.0.1", 13307, "ldbc")
    .setPassword("password")
    .setDatabase("world")

  private def resource: Resource[IO, Connector[IO]] =
    for
      otel <- Resource
                .eval(IO.delay(GlobalOpenTelemetry.get))
                .evalMap(OtelJava.fromJOpenTelemetry[IO])
      tracer     <- Resource.eval(otel.tracerProvider.get(serviceName))
    yield Connector.fromDataSource(dataSource.setTracer(tracer))

  override def run: IO[Unit] =
    resource.use { conn =>
      sql"SELECT name FROM city".query[String].to[List].readOnly(conn).flatMap { cities =>
        IO.println(cities)
      }
    }
