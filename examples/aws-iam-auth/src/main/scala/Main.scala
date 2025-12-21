/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import com.comcast.ip4s.*

import cats.effect.*
import cats.effect.std.{ Console, Env }

import io.circe.*
import io.circe.syntax.*

import ldbc.dsl.*

import ldbc.connector.*

import ldbc.amazon.plugin.AwsIamAuthenticationPlugin
import ldbc.logging.*

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

object Main extends ResourceApp.Forever:

  private val logHandler: LogHandler[IO] = {
    case LogEvent.Success(sql, args) =>
      Console[IO].println(
        s"""Successful Statement Execution:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      )
    case LogEvent.ProcessingFailure(sql, args, failure) =>
      Console[IO].errorln(
        s"""Failed ResultSet Processing:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      ) >> Console[IO].printStackTrace(failure)
    case LogEvent.ExecFailure(sql, args, failure) =>
      Console[IO].errorln(
        s"""Failed Statement Execution:
           |  $sql
           |
           | arguments = [${ args.mkString(",") }]
           |""".stripMargin
      ) >> Console[IO].printStackTrace(failure)
  }

  private def routes(connector: Connector[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "healthcheck" => Ok("Healthcheck")
    case GET -> Root / "cities"      =>
      for
        cities <- sql"SELECT * FROM city".query[City].to[List].readOnly(connector)
        result <- Ok(cities.asJson)
      yield result
  }

  override def run(args: List[String]): Resource[IO, Unit] =
    for
      hostname <- Resource.eval(Env[IO].get("AURORA_HOST").flatMap {
                    case Some(v) => IO.pure(v)
                    case None    => IO.raiseError(new RuntimeException("AURORA_HOST is not set"))
                  })
      username <- Resource.eval(Env[IO].get("AURORA_USER").flatMap {
                    case Some(v) => IO.pure(v)
                    case None    => IO.raiseError(new RuntimeException("AURORA_USER is not set"))
                  })
      config = MySQLConfig.default.setHost(hostname).setUser(username).setDatabase("world").setSSL(SSL.Trusted)
      plugin = AwsIamAuthenticationPlugin.default[IO]("ap-northeast-1", hostname, username)
      datasource <- MySQLDataSource.pooling[IO](config, plugins = List(plugin))
      connector = Connector.fromDataSource(datasource, Some(logHandler))
      _ <- EmberServerBuilder
             .default[IO]
             .withHost(host"0.0.0.0")
             .withPort(port"9000")
             .withHttpApp(routes(connector).orNotFound)
             .build
    yield ()
