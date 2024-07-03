import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.Executor
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

@main def program4(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #program
  val program: Executor[IO, Int] =
    sql"INSERT INTO task (name, done) VALUES ('task 1', false)".update
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
    program.commit(conn)
  }.unsafeRunSync()
  // (List(1), Some(2), 3)
  // #run
