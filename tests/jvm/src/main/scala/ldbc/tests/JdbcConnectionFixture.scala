/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.effect.*

import munit.*
import munit.catseffect.IOFixture
import munit.catseffect.ResourceFixture.FixtureNotInstantiatedException

import ldbc.sql.*

import jdbc.connector.ConnectionProvider

private case class JdbcConnectionFixture(
  name:              String,
  provider:          ConnectionProvider[IO],
  connectBeforeAll:  Connection[IO] => IO[Unit],
  connectAfterAll:   Connection[IO] => IO[Unit],
  connectBeforeEach: Connection[IO] => IO[Unit],
  connectAfterEach:  Connection[IO] => IO[Unit]
) extends ConnectionFixture:
  override def withBeforeAll(f: Connection[IO] => IO[Unit]): ConnectionFixture =
    copy(connectBeforeAll = f)

  override def withAfterAll(f: Connection[IO] => IO[Unit]): ConnectionFixture =
    copy(connectAfterAll = f)

  override def withBeforeEach(f: Connection[IO] => IO[Unit]): ConnectionFixture =
    copy(connectBeforeEach = f)

  override def withAfterEach(f: Connection[IO] => IO[Unit]): ConnectionFixture =
    copy(connectAfterEach = f)

  override val fixture: IOFixture[Connection[IO]] =
    new IOFixture[Connection[IO]](name):
      @volatile private var value: Option[(Connection[IO], IO[Unit])] = None

      override def apply(): Connection[IO] = value match
        case Some(v) => v._1
        case None    => throw new FixtureNotInstantiatedException(name)

      override def beforeAll(): IO[Unit] =
        provider.createConnection().allocated.flatMap {
          case (conn, close) =>
            connectBeforeAll(conn) *> IO(this.value = Some((conn, close)))
        }

      override def afterAll(): IO[Unit] =
        value.fold(IO.unit) {
          case (conn, close) =>
            connectAfterAll(conn) *> close
        }

      override def beforeEach(context: BeforeEach): IO[Unit] =
        value.fold(IO.unit) {
          case (conn, _) => connectBeforeEach(conn) *> IO.unit
        }

      override def afterEach(context: AfterEach): IO[Unit] =
        value.fold(IO.unit) {
          case (conn, _) => connectAfterEach(conn) *> IO.unit
        }

object JdbcConnectionFixture:
  def apply(name: String, provider: ConnectionProvider[IO]): ConnectionFixture =
    JdbcConnectionFixture(name, provider, _ => IO.unit, _ => IO.unit, _ => IO.unit, _ => IO.unit)
