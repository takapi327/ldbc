/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*

import io.circe.*
import io.circe.syntax.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.dsl.logging.LogHandler

import ldbc.statement.formatter.Naming

import ldbc.schema.*
import ldbc.schema.syntax.io.*

import ldbc.connector.*

import org.http4s.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder

case class City(
  id:          Int,
  name:        String,
  countryCode: String,
  district:    String,
  population:  Int
)

object City:

  given Encoder[City] = Encoder.derived[City]

class CityTable extends Table[City]("city"):

  given Naming = Naming.PASCAL

  def id:          Column[Int]    = int("ID").unsigned.autoIncrement.primaryKey
  def name:        Column[String] = char(35)
  def countryCode: Column[String] = char(3).unique
  def district:    Column[String] = char(20)
  def population:  Column[Int]    = int()

  override def * : Column[City] = (id *: name *: countryCode *: district *: population).to[City]

object Main extends ResourceApp.Forever:

  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.console[IO]

  private val cityTable = TableQuery[CityTable]

  private def connection: Resource[IO, Connection[IO]] =
    Connection[IO](
      host     = "127.0.0.1",
      port     = 13306,
      user     = "ldbc",
      password = Some("password"),
      database = Some("world"),
      ssl      = SSL.Trusted
    )

  private def routes(conn: Connection[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "cities" =>
      for
        cities <- cityTable.selectAll.query.to[List].readOnly(conn)
        result <- Ok(cities.asJson)
      yield result
  }

  override def run(args: List[String]): Resource[IO, Unit] =
    for
      conn <- connection
      _ <- EmberServerBuilder
             .default[IO]
             .withHttpApp(routes(conn).orNotFound)
             .build
    yield ()
