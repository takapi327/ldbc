/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.zio

import ldbc.sql.{ Connection, DataSource }

import ldbc.effect.Async
import ldbc.free.KleisliInterpreter
import ldbc.logging.{ LogEvent, LogHandler }
import ldbc.DBIO

import zio.{ Scope, Task, ZIO }

/**
 * Package-private capability: a `Task` connector that can hand a scoped [[ldbc.sql.Connection]] to
 * `query.stream(connector, …)` for a `ZStream`'s lifetime.
 *
 *   - a `fromDataSource` connector acquires a **fresh** connection (released when the scope closes),
 *   - a `fromConnection` connector yields the **already-held** connection (its lifetime is the caller's).
 *
 * This is **not** public — the factories return a plain `ldbc.Connector[Task]`, and `.stream` recovers this
 * capability internally via a type test. Callers never see a ZIO-specific connector type.
 */
private[zio] trait StreamableConnector:
  private[zio] def scopedConnection: ZIO[Scope, Throwable, Connection[Task]]

/**
 * ZIO factories for `ldbc.Connector[Task]` — the same set `ldbc-mysql` offers minus `fromConfig`
 * ([[fromConnection]] / [[fromDataSource]]).
 *
 * Each returns the ordinary base [[ldbc.Connector]] (no ZIO-specific connector type is exposed) and privately
 * mixes in [[StreamableConnector]] so read-only streaming can hold a connection for a `ZStream`'s lifetime.
 */
object Connector:

  private val noopLogHandler: LogHandler[Task] = (_: LogEvent) => ZIO.unit

  /**
   * A connector that runs every `DBIO` against the supplied connection. Closing the connection is the
   * caller's responsibility. Streaming from it reuses that same connection.
   */
  def fromConnection(
    connection: Connection[Task],
    logHandler: LogHandler[Task] = noopLogHandler
  ): ldbc.Connector[Task] =
    new ldbc.Connector[Task] with StreamableConnector:
      private val interpreter = new KleisliInterpreter[Task](logHandler)

      override def run[A](dbio: DBIO[A]): Task[A] =
        dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

      override private[zio] def scopedConnection: ZIO[Scope, Throwable, Connection[Task]] =
        ZIO.succeed(connection)

  /**
   * A connector that acquires a connection from `source` for each `DBIO` run (and releases it after).
   * Streaming from it acquires a fresh connection held for the stream's lifetime.
   */
  def fromDataSource(
    source:     DataSource[Task],
    logHandler: LogHandler[Task] = noopLogHandler
  ): ldbc.Connector[Task] =
    new ldbc.Connector[Task] with StreamableConnector:
      private val interpreter = new KleisliInterpreter[Task](logHandler)

      override def run[A](dbio: DBIO[A]): Task[A] =
        summon[Async[Task]].bracket(source.getConnection)(pair =>
          dbio.foldMap(interpreter.ConnectionInterpreter).run(pair._1)
        )(pair => pair._2)

      override private[zio] def scopedConnection: ZIO[Scope, Throwable, Connection[Task]] =
        ZIO.acquireRelease(source.getConnection)((pair: (Connection[Task], Task[Unit])) => pair._2.orDie).map(_._1)
