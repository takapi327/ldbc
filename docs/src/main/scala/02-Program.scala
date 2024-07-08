import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

@main def program2(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: Executor[IO, Option[Int]] = sql"SELECT 2".query[Int].to[Option]
  // #program

  // #connection
  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 13306,
    user     = "ldbc",
    password = Some("password")
  )
  // #connection

  // #run
  connection
    .use { conn =>
      program.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
  // Some(2)
  // #run
