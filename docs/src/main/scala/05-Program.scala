/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import scala.language.implicitConversions

import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.io.*

@main def program5(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  // #customType
  enum TaskStatus(val done: Boolean, val name: String):
    case Pending extends TaskStatus(false, "Pending")
    case Done    extends TaskStatus(true, "Done")
  // #customType

  // #customParameter
  given Parameter[TaskStatus] with
    override def bind[F[_]](statement: PreparedStatement[F], index: Int, status: TaskStatus): F[Unit] =
      statement.setBoolean(index, status.done)
  // #customParameter

  // #program1
  val program1: Executor[IO, Int] =
    sql"INSERT INTO task (name, done) VALUES (${ "task 1" }, ${ TaskStatus.Done })".update
  // #program1

  // #customReader
  given ResultSetReader[IO, TaskStatus] =
    ResultSetReader.mapping[IO, Boolean, TaskStatus] {
      case true  => TaskStatus.Done
      case false => TaskStatus.Pending
    }
  // #customReader

  // #program2
  val program2: Executor[IO, (String, TaskStatus)] =
    sql"SELECT name, done FROM task WHERE id = 1".query[(String, TaskStatus)].unsafe
  // #program2

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
      program1.commit(conn) *> program2.readOnly(conn).map(println(_))
    }
    .unsafeRunSync()
  // ("task 1", Done)
  // #run
