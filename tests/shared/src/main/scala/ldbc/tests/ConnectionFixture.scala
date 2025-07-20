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

import ldbc.connector.ConnectionProvider

import ldbc.Connector

trait ConnectionFixture:
  def name: String

  def withBeforeAll(f: Connector[IO] => IO[Unit]): ConnectionFixture

  def withAfterAll(f: Connector[IO] => IO[Unit]): ConnectionFixture

  def withBeforeEach(f: Connector[IO] => IO[Unit]): ConnectionFixture

  def withAfterEach(f: Connector[IO] => IO[Unit]): ConnectionFixture

  def fixture: IOFixture[Connector[IO]]

object ConnectionFixture:

  private case class Impl(
    name:              String,
    provider:          ConnectionProvider[IO, Unit],
    connectBeforeAll:  Connector[IO] => IO[Unit],
    connectAfterAll:   Connector[IO] => IO[Unit],
    connectBeforeEach: Connector[IO] => IO[Unit],
    connectAfterEach:  Connector[IO] => IO[Unit]
  ) extends ConnectionFixture:
    override def withBeforeAll(f: Connector[IO] => IO[Unit]): ConnectionFixture =
      copy(connectBeforeAll = f)

    override def withAfterAll(f: Connector[IO] => IO[Unit]): ConnectionFixture =
      copy(connectAfterAll = f)

    override def withBeforeEach(f: Connector[IO] => IO[Unit]): ConnectionFixture =
      copy(connectBeforeEach = f)

    override def withAfterEach(f: Connector[IO] => IO[Unit]): ConnectionFixture =
      copy(connectAfterEach = f)

    override val fixture: IOFixture[Connector[IO]] =
      new IOFixture[Connector[IO]](name):
        @volatile private var value: Option[(Connector[IO], IO[Unit])] = None

        override def apply(): Connector[IO] = value match
          case Some(v) => v._1
          case None    => throw new FixtureNotInstantiatedException(name)

        override def beforeAll(): IO[Unit] =
          provider.createConnection().map(Connector.fromConnection).allocated.flatMap {
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

  def apply(name: String, provider: ConnectionProvider[IO, Unit]): ConnectionFixture =
    Impl(name, provider, _ => IO.unit, _ => IO.unit, _ => IO.unit, _ => IO.unit)
