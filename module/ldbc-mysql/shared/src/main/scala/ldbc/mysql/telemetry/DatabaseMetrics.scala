/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.telemetry

import scala.concurrent.duration.FiniteDuration

import ldbc.sql.Attribute

import ldbc.effect.{ Concurrent, Resource }

/**
 * The metrics SPI for database operations, following the OpenTelemetry database semantic conventions
 * (operation duration, returned rows, connection pool timings). This is the CE-free trait; the core
 * ships only the no-op implementation ([[DatabaseMetrics.noop]]). A real OpenTelemetry-backed
 * implementation is provided later by the observability bridge, not by the core.
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/database-metrics/]]
 */
trait DatabaseMetrics[F[_]]:

  /**
   * Records the duration of a database operation.
   *
   * @param duration   the operation duration
   * @param attributes additional attributes
   */
  def recordOperationDuration(duration: FiniteDuration, attributes: Attribute[?]*): F[Unit]

  /**
   * Records the number of rows returned by an operation.
   *
   * @param rows       the number of rows
   * @param attributes additional attributes
   */
  def recordReturnedRows(rows: Long, attributes: Attribute[?]*): F[Unit]

  /**
   * Records connection creation time.
   *
   * @param duration the time taken to create a connection
   * @param poolName the pool name
   */
  def recordConnectionCreateTime(duration: FiniteDuration, poolName: String): F[Unit]

  /**
   * Records connection wait time (time to acquire from the pool).
   *
   * @param duration the wait time
   * @param poolName the pool name
   */
  def recordConnectionWaitTime(duration: FiniteDuration, poolName: String): F[Unit]

  /**
   * Records connection use time (time between borrow and return).
   *
   * @param duration the use time
   * @param poolName the pool name
   */
  def recordConnectionUseTime(duration: FiniteDuration, poolName: String): F[Unit]

  /**
   * Increments the connection-timeout counter.
   *
   * @param poolName the pool name
   */
  def recordConnectionTimeout(poolName: String): F[Unit]

  /**
   * Registers an observable callback for pool gauge metrics.
   *
   * @param poolName       the pool name
   * @param minConnections the minimum number of idle connections maintained
   * @param maxConnections the maximum number of connections allowed
   * @param stateProvider  an effect providing the current pool state snapshot
   * @return a resource managing the callback lifecycle
   */
  def registerPoolStateCallback(
    poolName:       String,
    minConnections: Int,
    maxConnections: Int,
    stateProvider:  F[PoolMetricsState]
  ): Resource[F, Unit]

object DatabaseMetrics:

  /** A metrics instance that records nothing. */
  def noop[F[_]](using F: Concurrent[F]): DatabaseMetrics[F] = new DatabaseMetrics[F]:
    override def recordOperationDuration(duration:    FiniteDuration, attributes: Attribute[?]*): F[Unit] = F.unit
    override def recordReturnedRows(rows:             Long, attributes:           Attribute[?]*): F[Unit] = F.unit
    override def recordConnectionCreateTime(duration: FiniteDuration, poolName:   String):        F[Unit] = F.unit
    override def recordConnectionWaitTime(duration:   FiniteDuration, poolName:   String):        F[Unit] = F.unit
    override def recordConnectionUseTime(duration:    FiniteDuration, poolName:   String):        F[Unit] = F.unit
    override def recordConnectionTimeout(poolName:    String):                                    F[Unit] = F.unit
    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  F[PoolMetricsState]
    ): Resource[F, Unit] = Resource.pure(())

  /**
   * Builds a [[DatabaseMetrics[F]]] from a [[Meter]]. In the CE-free core this always yields the no-op
   * implementation, regardless of the meter — real instrument wiring is the observability bridge's job.
   *
   * @param meter the meter (ignored by the core)
   * @return a resource containing the no-op metrics instance
   */
  def fromMeter[F[_]](@annotation.unused meter: Meter)(using F: Concurrent[F]): Resource[F, DatabaseMetrics[F]] =
    Resource.pure(noop)
