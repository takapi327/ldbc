/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import cats.MonadError

import ldbc.sql.{ Connection, DataSource }

import ldbc.effect.{ Async, Concurrent, Resource as EffResource }
import ldbc.free.KleisliInterpreter
import ldbc.logging.{ LogEvent, LogHandler }
import ldbc.net.effect.{ IoEngine, TlsUpgrade }
import ldbc.DBIO

/**
 * MySQL factories for [[ldbc.Connector]].
 *
 * Because the `DBIO` interpreter is tagless (`KleisliInterpreter[F]` folds a program into any effect with
 * a `cats.MonadError`), these factories are generic over the effect `F`: one definition serves every
 * effect that runs the driver natively (`IO` / `Task` / `Fx`). The driver, pool and interpretation all run
 * directly on `F` — no cross-effect bridge.
 *
 * Effects that satisfy `ldbc.effect.Concurrent` (`IO` / `Task` / `Fx`) use these directly. `Future` cannot
 * satisfy `Concurrent`, so `Future` users compose the `Fx`-backed bridge instead:
 * `ldbc.future.Connector.fromDataSource(MySQLDataSource.fromConfig[ldbc.fx.Fx](config))`.
 *
 * Lifecycle hooks are expressed as effect-agnostic `DBIO` programs: `before` runs when a connection is put
 * into use and its result is threaded to `after`, which runs when the connection is released.
 *
 * Connection pooling is supplied externally: build a `ldbc.pool.PooledDataSource` (itself a `DataSource[F]`)
 * and hand it to [[fromDataSource]], so the pool lifecycle is owned by the caller's `Resource` rather than
 * by the connector.
 */
object Connector:

  private def noopLogger[F[_]](using F: MonadError[F, Throwable]): LogHandler[F] = (_: LogEvent) => F.pure(())

  private def runHook[F[_], A](dbio: DBIO[A], connection: Connection[F])(using
    MonadError[F, Throwable]
  ): F[A] =
    dbio.foldMap(new KleisliInterpreter[F](noopLogger).ConnectionInterpreter).run(connection)

  /**
   * A [[ldbc.Connector]] over `F` that runs every `DBIO` against the supplied connection. Closing the
   * connection is the caller's responsibility.
   *
   * @param connection the connection to run against
   * @param logHandler an optional log handler (a no-op handler is used if absent)
   */
  def fromConnection[F[_]](
    connection: Connection[F],
    logHandler: Option[LogHandler[F]] = None
  )(using F: MonadError[F, Throwable]): ldbc.Connector[F] =
    new ldbc.Connector[F]:
      private val interpreter = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): F[A] =
        dbio.foldMap(interpreter.ConnectionInterpreter).run(connection)

  /**
   * A [[ldbc.Connector]] over `F` that acquires a connection from the given data source for each run,
   * interprets the `DBIO` in `F`, and releases the connection afterwards.
   *
   * @param dataSource the connection source
   * @param logHandler an optional log handler (a no-op handler is used if absent)
   */
  def fromDataSource[F[_]](
    dataSource: DataSource[F],
    logHandler: Option[LogHandler[F]] = None
  )(using MonadError[F, Throwable], Async[F]): ldbc.Connector[F] =
    new ldbc.Connector[F]:
      private val interpreter = new KleisliInterpreter[F](logHandler.getOrElse(noopLogger))
      override def run[A](dbio: DBIO[A]): F[A] =
        summon[Async[F]].bracket(dataSource.getConnection)((pair: (Connection[F], F[Unit])) =>
          dbio.foldMap(interpreter.ConnectionInterpreter).run(pair._1)
        )((pair: (Connection[F], F[Unit])) => pair._2)

  /**
   * A [[ldbc.Connector]] over `F` that opens a fresh MySQL connection per run from the given config.
   *
   * @param config the MySQL connection configuration
   */
  def fromConfig[F[_]](
    config: MySQLConfig
  )(using Concurrent[F], IoEngine[F], TlsUpgrade[F], MonadError[F, Throwable]): ldbc.Connector[F] =
    fromDataSource(MySQLDataSource.fromConfig[F](config))

  /**
   * Like [[fromConfig]], but runs `before`/`after` `DBIO` hooks around each connection's use.
   *
   * @param config the MySQL connection configuration
   * @param before run once the connection is acquired; its result is threaded to `after`
   * @param after  run on release, receiving `before`'s result
   * @tparam A the value carried from `before` to `after`
   */
  def fromConfig[F[_], A](
    config: MySQLConfig,
    before: DBIO[A],
    after:  A => DBIO[Unit]
  )(using Concurrent[F], IoEngine[F], TlsUpgrade[F], MonadError[F, Throwable]): ldbc.Connector[F] =
    fromDataSource(hooked(MySQLDataSource.fromConfig[F](config), before, after))

  /**
   * Wraps a data source so each acquired connection runs `before` on acquire and `after` on release, with
   * `after` running before the underlying connection is released.
   */
  private def hooked[F[_], A](base: DataSource[F], before: DBIO[A], after: A => DBIO[Unit])(using
    MonadError[F, Throwable],
    Async[F]
  ): DataSource[F] =
    new DataSource[F]:
      override def getConnection: F[(Connection[F], F[Unit])] =
        EffResource
          .make(base.getConnection)((pair: (Connection[F], F[Unit])) => pair._2)
          .flatMap { (pair: (Connection[F], F[Unit])) =>
            val connection = pair._1
            EffResource
              .make(runHook(before, connection))(a => runHook(after(a), connection))
              .map(_ => connection)
          }
          .allocatedCase
