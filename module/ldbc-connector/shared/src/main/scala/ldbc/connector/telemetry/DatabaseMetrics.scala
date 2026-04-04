/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import scala.concurrent.duration.FiniteDuration

import cats.syntax.all.*
import cats.Applicative
import cats.Monad

import cats.effect.Resource

import org.typelevel.otel4s.metrics.*
import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.semconv.experimental.attributes.DbExperimentalAttributes
import org.typelevel.otel4s.semconv.metrics.DbMetrics
import org.typelevel.otel4s.semconv.experimental.metrics.DbExperimentalMetrics

/**
 * OpenTelemetry metrics for database operations.
 *
 * Implements metrics according to OpenTelemetry database semantic conventions:
 * - db.client.operation.duration (Stable)
 * - db.client.response.returned_rows (Development)
 * - Connection pool metrics (Development)
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/db/database-metrics/]]
 */
trait DatabaseMetrics[F[_]]:

  /**
   * Records the duration of a database operation.
   *
   * @param duration Operation duration
   * @param attributes Additional attributes
   */
  def recordOperationDuration(
    duration:   FiniteDuration,
    attributes: Attribute[?]*
  ): F[Unit]

  /**
   * Records the number of rows returned by an operation.
   *
   * @param rows Number of rows
   * @param attributes Additional attributes
   */
  def recordReturnedRows(
    rows:       Long,
    attributes: Attribute[?]*
  ): F[Unit]

  /**
   * Records connection creation time.
   *
   * @param duration Time to create a connection
   * @param poolName Pool name
   */
  def recordConnectionCreateTime(
    duration: FiniteDuration,
    poolName: String
  ): F[Unit]

  /**
   * Records connection wait time (time to acquire from pool).
   *
   * @param duration Wait time
   * @param poolName Pool name
   */
  def recordConnectionWaitTime(
    duration: FiniteDuration,
    poolName: String
  ): F[Unit]

  /**
   * Records connection use time (time between borrow and return).
   *
   * @param duration Use time
   * @param poolName Pool name
   */
  def recordConnectionUseTime(
    duration: FiniteDuration,
    poolName: String
  ): F[Unit]

  /**
   * Increments the connection timeout counter.
   *
   * @param poolName Pool name
   */
  def recordConnectionTimeout(poolName: String): F[Unit]

  /**
   * Registers an observable callback for pool gauge metrics.
   *
   * Uses OTel BatchCallback to report pool state as absolute values
   * at export time. Metrics reported:
   * - db.client.connection.count (idle/used)
   * - db.client.connection.idle.max
   * - db.client.connection.idle.min
   * - db.client.connection.max
   * - db.client.connection.pending_requests
   *
   * @param poolName Pool name
   * @param minConnections Minimum number of idle connections maintained
   * @param maxConnections Maximum number of connections allowed
   * @param stateProvider Effect that provides the current pool state snapshot
   * @return Resource that manages the callback lifecycle
   */
  def registerPoolStateCallback(
    poolName:       String,
    minConnections: Int,
    maxConnections: Int,
    stateProvider:  F[PoolMetricsState]
  ): Resource[F, Unit]

object DatabaseMetrics:

  /**
   * Bucket boundaries for operation duration histogram.
   * Per OpenTelemetry spec: [0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1, 5, 10]
   */
  val operationDurationBuckets: BucketBoundaries = BucketBoundaries(
    0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0
  )

  /**
   * Bucket boundaries for returned rows histogram.
   * Per OpenTelemetry spec: [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000]
   */
  val returnedRowsBuckets: BucketBoundaries = BucketBoundaries(
    1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000
  )

  /**
   * Creates a no-op metrics instance.
   */
  def noop[F[_]: Applicative]: DatabaseMetrics[F] = new DatabaseMetrics[F]:
    override def recordOperationDuration(duration: FiniteDuration, attributes: Attribute[?]*): F[Unit] =
      Applicative[F].unit
    override def recordReturnedRows(rows: Long, attributes: Attribute[?]*): F[Unit] =
      Applicative[F].unit
    override def recordConnectionCreateTime(duration: FiniteDuration, poolName: String): F[Unit] =
      Applicative[F].unit
    override def recordConnectionWaitTime(duration: FiniteDuration, poolName: String): F[Unit] =
      Applicative[F].unit
    override def recordConnectionUseTime(duration: FiniteDuration, poolName: String): F[Unit] =
      Applicative[F].unit
    override def recordConnectionTimeout(poolName: String): F[Unit] =
      Applicative[F].unit
    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  F[PoolMetricsState]
    ): Resource[F, Unit] = Resource.unit[F]

  /**
   * Implementation of DatabaseMetrics using otel4s instruments.
   */
  private class Impl[F[_]: Monad](
    operationDuration:    Histogram[F, Double],
    returnedRows:         Histogram[F, Double],
    connectionCreateTime: Histogram[F, Double],
    connectionWaitTime:   Histogram[F, Double],
    connectionUseTime:    Histogram[F, Double],
    connectionTimeouts:   Counter[F, Long],
    meter:                Meter[F]
  ) extends DatabaseMetrics[F]:

    private def durationToSeconds(d: FiniteDuration): Double =
      d.toNanos.toDouble / 1e9

    override def recordOperationDuration(
      duration:   FiniteDuration,
      attributes: Attribute[?]*
    ): F[Unit] =
      operationDuration.record(durationToSeconds(duration), attributes*)

    override def recordReturnedRows(
      rows:       Long,
      attributes: Attribute[?]*
    ): F[Unit] =
      returnedRows.record(rows.toDouble, attributes*)

    override def recordConnectionCreateTime(
      duration: FiniteDuration,
      poolName: String
    ): F[Unit] =
      connectionCreateTime.record(
        durationToSeconds(duration),
        DbExperimentalAttributes.DbClientConnectionPoolName(poolName)
      )

    override def recordConnectionWaitTime(
      duration: FiniteDuration,
      poolName: String
    ): F[Unit] =
      connectionWaitTime.record(
        durationToSeconds(duration),
        DbExperimentalAttributes.DbClientConnectionPoolName(poolName)
      )

    override def recordConnectionUseTime(
      duration: FiniteDuration,
      poolName: String
    ): F[Unit] =
      connectionUseTime.record(
        durationToSeconds(duration),
        DbExperimentalAttributes.DbClientConnectionPoolName(poolName)
      )

    override def recordConnectionTimeout(poolName: String): F[Unit] =
      connectionTimeouts.inc(
        DbExperimentalAttributes.DbClientConnectionPoolName(poolName)
      )

    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  F[PoolMetricsState]
    ): Resource[F, Unit] =
      val poolNameAttr = DbExperimentalAttributes.DbClientConnectionPoolName(poolName)
      val stateIdle    = DbExperimentalAttributes.DbClientConnectionState(
                           DbExperimentalAttributes.DbClientConnectionStateValue.Idle.value
                         )
      val stateUsed    = DbExperimentalAttributes.DbClientConnectionState(
                           DbExperimentalAttributes.DbClientConnectionStateValue.Used.value
                         )

      meter.batchCallback.of(
        meter
          .observableUpDownCounter[Long](DbExperimentalMetrics.ClientConnectionCount.name)
          .withUnit(DbExperimentalMetrics.ClientConnectionCount.unit)
          .withDescription(DbExperimentalMetrics.ClientConnectionCount.description)
          .createObserver,
        meter
          .observableUpDownCounter[Long](DbExperimentalMetrics.ClientConnectionIdleMax.name)
          .withUnit(DbExperimentalMetrics.ClientConnectionIdleMax.unit)
          .withDescription(DbExperimentalMetrics.ClientConnectionIdleMax.description)
          .createObserver,
        meter
          .observableUpDownCounter[Long](DbExperimentalMetrics.ClientConnectionIdleMin.name)
          .withUnit(DbExperimentalMetrics.ClientConnectionIdleMin.unit)
          .withDescription(DbExperimentalMetrics.ClientConnectionIdleMin.description)
          .createObserver,
        meter
          .observableUpDownCounter[Long](DbExperimentalMetrics.ClientConnectionMax.name)
          .withUnit(DbExperimentalMetrics.ClientConnectionMax.unit)
          .withDescription(DbExperimentalMetrics.ClientConnectionMax.description)
          .createObserver,
        meter
          .observableUpDownCounter[Long](DbExperimentalMetrics.ClientConnectionPendingRequests.name)
          .withUnit(DbExperimentalMetrics.ClientConnectionPendingRequests.unit)
          .withDescription(DbExperimentalMetrics.ClientConnectionPendingRequests.description)
          .createObserver
      ) { (connCount, idleMax, idleMin, connMax, pendingReqs) =>
        stateProvider.flatMap { state =>
          connCount.record(state.idleCount, poolNameAttr, stateIdle) *>
            connCount.record(state.usedCount, poolNameAttr, stateUsed) *>
            idleMax.record(maxConnections.toLong, poolNameAttr) *>
            idleMin.record(minConnections.toLong, poolNameAttr) *>
            connMax.record(maxConnections.toLong, poolNameAttr) *>
            pendingReqs.record(state.pendingRequestCount, poolNameAttr)
        }
      }

  /**
   * Creates a DatabaseMetrics instance from an otel4s Meter.
   *
   * @param meter The otel4s Meter instance
   * @return Resource containing the metrics instance
   */
  def fromMeter[F[_]: Monad](meter: Meter[F]): Resource[F, DatabaseMetrics[F]] =
    given Meter[F] = meter
    for
      // db.client.operation.duration (Histogram, Required, Stable)
      operationDuration <- Resource.eval(
                             DbMetrics.ClientOperationDuration.create[F, Double](operationDurationBuckets)
                           )

      // db.client.response.returned_rows (Histogram, Recommended, Development)
      returnedRows <- Resource.eval(
                        DbExperimentalMetrics.ClientResponseReturnedRows.create[F, Double](returnedRowsBuckets)
                      )

      // db.client.connection.create_time (Histogram, Recommended, Development)
      connectionCreateTime <- Resource.eval(
                                DbExperimentalMetrics.ClientConnectionCreateTime.create[F, Double](
                                  operationDurationBuckets
                                )
                              )

      // db.client.connection.wait_time (Histogram, Recommended, Development)
      connectionWaitTime <- Resource.eval(
                              DbExperimentalMetrics.ClientConnectionWaitTime.create[F, Double](
                                operationDurationBuckets
                              )
                            )

      // db.client.connection.use_time (Histogram, Recommended, Development)
      connectionUseTime <- Resource.eval(
                             DbExperimentalMetrics.ClientConnectionUseTime.create[F, Double](
                               operationDurationBuckets
                             )
                           )

      // db.client.connection.timeouts (Counter, Recommended, Development)
      connectionTimeouts <- Resource.eval(
                              DbExperimentalMetrics.ClientConnectionTimeouts.create[F, Long]
                            )
    yield new Impl(
      operationDuration,
      returnedRows,
      connectionCreateTime,
      connectionWaitTime,
      connectionUseTime,
      connectionTimeouts,
      meter
    )
