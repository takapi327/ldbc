/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

object Main extends IOApp.Simple:

  given Tracer[IO] = Tracer.noop[IO]

  private def connection: Resource[IO, Connection[IO]] =
    Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user = "ldbc",
      password = Some("password"),
      ssl = SSL.Trusted,
    )

  def run: IO[Unit] =
    connection.use { conn =>
      for
        str <- sql"SELECT 'Hello, World!'".query[String].unsafe.readOnly(conn)
      yield println(str)
    }
