/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*

import io.circe.*
import io.circe.syntax.*

import ldbc.statement.formatter.Naming

import ldbc.schema.*

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

  private val cityTable = TableQuery[CityTable]

  private val dataSource = MySQLDataSource
    .build[IO]("127.0.0.1", 13306, "ldbc")
    .setPassword("password")
    .setDatabase("world")
    .setSSL(SSL.Trusted)
  private val connector = Connector.fromDataSource(dataSource)

  private def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "cities" =>
      for
        cities <- cityTable.selectAll.query.to[List].readOnly(connector)
        result <- Ok(cities.asJson)
      yield result
  }

  override def run(args: List[String]): Resource[IO, Unit] =
    for
      _    <- EmberServerBuilder
             .default[IO]
             .withHttpApp(routes.orNotFound)
             .build
    yield ()
