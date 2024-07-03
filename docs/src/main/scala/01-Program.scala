import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.Executor
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

@main def program1(): Unit =
  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: Executor[IO, Int] = Executor.pure[IO, Int](1)
  // #program

  // #connection
  def connection = Connection[IO](
    host     = "127.0.0.1",
    port     = 3306,
    user     = "ldbc",
    password = Some("password"),
    ssl      = SSL.None
  )
  // #connection

  // #run
  connection.use { conn =>
    program.readOnly(conn)
  }.unsafeRunSync()
  // 1
  // #run
