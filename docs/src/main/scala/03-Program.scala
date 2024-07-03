import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.Executor
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

@main def program3(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: Executor[IO, (List[Int], Option[Int], Int)] =
    for
      result1 <- sql"SELECT 1".query[Int].to[List]
      result2 <- sql"SELECT 2".query[Int].to[Option]
      result3 <- sql"SELECT 3".query[Int].unsafe
    yield (result1, result2, result3)
  // #program

  // #connection
  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 3306,
    user     = "ldbc",
    password = Some("password")
  )
  // #connection

  // #run
  connection.use { conn =>
    program.readOnly(conn)
  }.unsafeRunSync()
  // (List(1), Some(2), 3)
  // #run
