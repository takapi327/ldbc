import cats.syntax.all.*

import cats.effect.*
import cats.effect.unsafe.implicits.global

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.dsl.Executor
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler

@main def setup(): Unit =

  // #given
  given Tracer[IO]     = Tracer.noop[IO]
  given LogHandler[IO] = LogHandler.noop[IO]
  // #given

  val createDatabase: Executor[IO, Int] =
    sql"CREATE DATABASE IF NOT EXISTS todo".update
  
  val createTable: Executor[IO, Int] =
    sql"""
         CREATE TABLE `task` (
           `id` INT NOT NULL AUTO_INCREMENT,
           `name` VARCHAR(255) NOT NULL,
           `done` BOOLEAN NOT NULL DEFAULT FALSE,
           PRIMARY KEY (`id`)
         )
       """.update

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
    (createDatabase *> conn.setSchema("todo") *> createTable).transaction(conn)
  }.unsafeRunSync()
  // #run
