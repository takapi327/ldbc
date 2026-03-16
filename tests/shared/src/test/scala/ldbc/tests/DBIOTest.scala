/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.syntax.all.*

import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.dsl.*

import ldbc.connector.*
import ldbc.connector.exception.*

import ldbc.Connector

class DBIOTest extends CatsEffectSuite:

  private val datasource = MySQLDataSource
    .build[IO](MySQLTestConfig.host, MySQLTestConfig.port, MySQLTestConfig.user)
    .setPassword(MySQLTestConfig.password)
    .setSSL(SSL.Trusted)

  def connector: Connector[IO] = Connector.fromDataSource(datasource)

  test("DBIO#pure") {
    val program = DBIO.pure(1)
    assertIO(
      program.readOnly(connector),
      1
    )
  }

  test("DBIO#ap") {
    val program1 = DBIO.pure(1)
    val program2 = DBIO.pure[Int => Int](_ + 1)
    val program3 = program2.ap(program1)
    assertIO(
      program3.readOnly(connector),
      2
    )
  }

  test("DBIO#map") {
    val program1 = DBIO.pure(1)
    val program2 = program1.map(_ + 1)
    assertIO(
      program2.readOnly(connector),
      2
    )
  }

  test("DBIO#flatMap") {
    val program1 = DBIO.pure(1)
    val program2 = program1.flatMap(n => DBIO.pure(n + 1))
    assertIO(
      program2.readOnly(connector),
      2
    )
  }

  test("DBIO#tailRecM") {
    val program1 = DBIO.pure(1)
    val program2 = program1.tailRecM[DBIO, String](_.map(n => Right(n.toString)))
    assertIO(
      program2.readOnly(connector),
      "1"
    )
  }

  test("DBIO#raiseError") {
    val program = DBIO.raiseError[Int](new Exception("error"))
    interceptMessageIO[Exception]("error")(
      program.readOnly(connector)
    )
  }

  test("DBIO#handleErrorWith") {
    val program1 = DBIO.raiseError[Int](new Exception("error"))
    val program2 = program1.handleErrorWith(_ => DBIO.pure(0))
    assertIO(
      program2.readOnly(connector),
      0
    )
  }

  test("DBIO#attempt#Right") {
    val program = DBIO.pure(1)
    assertIO(
      program.attempt.readOnly(connector),
      Right(1)
    )
  }

  test("DBIO#attempt#Left") {
    val program = DBIO.raiseError[Int](new Exception("error"))
    assertIOBoolean(
      program.attempt.readOnly(connector).map(_.isLeft)
    )
  }

  test("DBIO#updateRaw") {
    assertIO(
      (for
        r1 <- DBIO.updateRaw("CREATE DATABASE `dbio`;").commit(connector)
        r2 <- DBIO.updateRaw("DROP DATABASE `dbio`;").commit(connector)
      yield List(r1, r2)),
      List(1, 0)
    )
  }

  test("DBIO#updateRaw#Exception") {
    interceptIO[SQLSyntaxErrorException](
      DBIO.updateRaw("CREATE `dbio`;").commit(connector)
    )
  }

  test("DBIO#updateRaws") {
    val sql = """
      |CREATE DATABASE `dbio`;
      |DROP DATABASE `dbio`;
      |""".stripMargin
    assertIO(
      DBIO.updateRaws(sql).commit(connector).map(_.toList),
      List(1, 0)
    )
  }

  test("DBIO#updateRaws#Exception") {
    val sql = """
                |CREATE DATABASE `dbio`
                |DROP DATABASE `dbio`
                |""".stripMargin
    interceptIO[BatchUpdateException](
      DBIO.updateRaws(sql).commit(connector).map(_.toList)
    )
  }
