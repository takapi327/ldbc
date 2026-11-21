/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import cats.MonadThrow

import munit.*

import ldbc.sql.DataSource

import ldbc.Connector

/**
 * Effect-agnostic connection fixture: a suite-level `Connector[F]` acquired in `beforeAll` and released
 * in `afterAll`, with optional `F[Unit]` hooks around each phase. Works for any `F` with a
 * `cats.MonadThrow` (IO / Fx / Future) — the connection is wrapped via the generic
 * `ldbc.tests.TestConnector.fromConnection`, and munit awaits the `F[Unit]` lifecycle hooks through the
 * suite's per-effect value transform.
 */
trait ConnectionFixture[F[_]]:
  def name: String

  def withBeforeAll(f: Connector[F] => F[Unit]): ConnectionFixture[F]

  def withAfterAll(f: Connector[F] => F[Unit]): ConnectionFixture[F]

  def withBeforeEach(f: Connector[F] => F[Unit]): ConnectionFixture[F]

  def withAfterEach(f: Connector[F] => F[Unit]): ConnectionFixture[F]

  def fixture: AnyFixture[Connector[F]]

object ConnectionFixture:

  private case class Impl[F[_]](
    name:              String,
    datasource:        DataSource[F],
    connectBeforeAll:  Connector[F] => F[Unit],
    connectAfterAll:   Connector[F] => F[Unit],
    connectBeforeEach: Connector[F] => F[Unit],
    connectAfterEach:  Connector[F] => F[Unit]
  )(using F: MonadThrow[F])
    extends ConnectionFixture[F]:
    override def withBeforeAll(f: Connector[F] => F[Unit]): ConnectionFixture[F] =
      copy(connectBeforeAll = f)

    override def withAfterAll(f: Connector[F] => F[Unit]): ConnectionFixture[F] =
      copy(connectAfterAll = f)

    override def withBeforeEach(f: Connector[F] => F[Unit]): ConnectionFixture[F] =
      copy(connectBeforeEach = f)

    override def withAfterEach(f: Connector[F] => F[Unit]): ConnectionFixture[F] =
      copy(connectAfterEach = f)

    override val fixture: AnyFixture[Connector[F]] =
      new AnyFixture[Connector[F]](name):
        @volatile private var value: Option[(Connector[F], F[Unit])] = None

        override def apply(): Connector[F] = value match
          case Some(v) => v._1
          case None    => throw new IllegalStateException(s"fixture '$name' was not initialised")

        override def beforeAll(): F[Unit] =
          F.flatMap(datasource.getConnection) { (rawConnection, close) =>
            val connector = TestConnector.fromConnection(rawConnection)
            F.map(connectBeforeAll(connector))(_ => this.value = Some((connector, close)))
          }

        override def afterAll(): F[Unit] =
          value.fold(F.unit) {
            case (conn, close) => F.flatMap(connectAfterAll(conn))(_ => close)
          }

        override def beforeEach(context: BeforeEach): F[Unit] =
          value.fold(F.unit) {
            case (conn, _) => connectBeforeEach(conn)
          }

        override def afterEach(context: AfterEach): F[Unit] =
          value.fold(F.unit) {
            case (conn, _) => connectAfterEach(conn)
          }

  def apply[F[_]](name: String, datasource: DataSource[F])(using F: MonadThrow[F]): ConnectionFixture[F] =
    Impl(name, datasource, _ => F.unit, _ => F.unit, _ => F.unit, _ => F.unit)
