/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.telemetry

import scala.concurrent.duration.FiniteDuration

import cats.Applicative
import cats.Monad
import cats.syntax.all.*

import cats.effect.Resource

import org.typelevel.otel4s.metrics.*
import org.typelevel.otel4s.Attribute

import TelemetryAttribute.*

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
  val OperationDurationBuckets: BucketBoundaries = BucketBoundaries(
    0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0
  )

  /**
   * Bucket boundaries for returned rows histogram.
   * Per OpenTelemetry spec: [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000]
   */
  val ReturnedRowsBuckets: BucketBoundaries = BucketBoundaries(
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
   * Creates a DatabaseMetrics instance from an otel4s Meter.
   *
   * @param meter The otel4s Meter instance
   * @return Resource containing the metrics instance
   */
  def fromMeter[F[_]: Monad](meter: Meter[F]): Resource[F, DatabaseMetrics[F]] =
    for
      // db.client.operation.duration (Histogram, Required, Stable)
      operationDuration <- Resource.eval(
                             meter
                               .histogram[Double](METRIC_DB_CLIENT_OPERATION_DURATION)
                               .withUnit("s")
                               .withDescription("Duration of database client operations")
                               .withExplicitBucketBoundaries(OperationDurationBuckets)
                               .create
                           )

      // db.client.response.returned_rows (Histogram, Recommended, Development)
      returnedRows <- Resource.eval(
                        meter
                          .histogram[Double](METRIC_DB_CLIENT_RESPONSE_RETURNED_ROWS)
                          .withUnit("{row}")
                          .withDescription("The actual number of records returned by the database operation")
                          .withExplicitBucketBoundaries(ReturnedRowsBuckets)
                          .create
                      )

      // db.client.connection.create_time (Histogram, Recommended, Development)
      connectionCreateTime <- Resource.eval(
                                meter
                                  .histogram[Double](METRIC_DB_CLIENT_CONNECTION_CREATE_TIME)
                                  .withUnit("s")
                                  .withDescription("The time it took to create a new connection")
                                  .create
                              )

      // db.client.connection.wait_time (Histogram, Recommended, Development)
      connectionWaitTime <- Resource.eval(
                              meter
                                .histogram[Double](METRIC_DB_CLIENT_CONNECTION_WAIT_TIME)
                                .withUnit("s")
                                .withDescription("The time it took to obtain an open connection from the pool")
                                .create
                            )

      // db.client.connection.use_time (Histogram, Recommended, Development)
      connectionUseTime <- Resource.eval(
                             meter
                               .histogram[Double](METRIC_DB_CLIENT_CONNECTION_USE_TIME)
                               .withUnit("s")
                               .withDescription("The time between borrowing a connection and returning it to the pool")
                               .create
                           )

      // db.client.connection.timeouts (Counter, Recommended, Development)
      connectionTimeouts <-
        Resource.eval(
          meter
            .counter[Long](METRIC_DB_CLIENT_CONNECTION_TIMEOUTS)
            .withUnit("{timeout}")
            .withDescription(
              "The number of connection timeouts that have occurred trying to obtain a connection from the pool"
            )
            .create
        )
    yield new DatabaseMetricsImpl(
      operationDuration,
      returnedRows,
      connectionCreateTime,
      connectionWaitTime,
      connectionUseTime,
      connectionTimeouts,
      meter
    )

/**
 * Implementation of DatabaseMetrics using otel4s instruments.
 */
private class DatabaseMetricsImpl[F[_]: Monad](
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
      TelemetryAttribute.dbClientConnectionPoolName(poolName)
    )

  override def recordConnectionWaitTime(
    duration: FiniteDuration,
    poolName: String
  ): F[Unit] =
    connectionWaitTime.record(
      durationToSeconds(duration),
      TelemetryAttribute.dbClientConnectionPoolName(poolName)
    )

  override def recordConnectionUseTime(
    duration: FiniteDuration,
    poolName: String
  ): F[Unit] =
    connectionUseTime.record(
      durationToSeconds(duration),
      TelemetryAttribute.dbClientConnectionPoolName(poolName)
    )

  override def recordConnectionTimeout(poolName: String): F[Unit] =
    connectionTimeouts.inc(
      TelemetryAttribute.dbClientConnectionPoolName(poolName)
    )

  override def registerPoolStateCallback(
    poolName:       String,
    minConnections: Int,
    maxConnections: Int,
    stateProvider:  F[PoolMetricsState]
  ): Resource[F, Unit] =
    val poolNameAttr = TelemetryAttribute.dbClientConnectionPoolName(poolName)
    val stateIdle    = TelemetryAttribute.dbClientConnectionState(CONNECTION_STATE_IDLE)
    val stateUsed    = TelemetryAttribute.dbClientConnectionState(CONNECTION_STATE_USED)

    meter.batchCallback.of(
      meter
        .observableUpDownCounter[Long](METRIC_DB_CLIENT_CONNECTION_COUNT)
        .withUnit("{connection}")
        .withDescription("The number of connections that are currently in state described by the state attribute")
        .createObserver,
      meter
        .observableUpDownCounter[Long](METRIC_DB_CLIENT_CONNECTION_IDLE_MAX)
        .withUnit("{connection}")
        .withDescription("The maximum number of idle open connections allowed")
        .createObserver,
      meter
        .observableUpDownCounter[Long](METRIC_DB_CLIENT_CONNECTION_IDLE_MIN)
        .withUnit("{connection}")
        .withDescription("The minimum number of idle open connections allowed")
        .createObserver,
      meter
        .observableUpDownCounter[Long](METRIC_DB_CLIENT_CONNECTION_MAX)
        .withUnit("{connection}")
        .withDescription("The maximum number of open connections allowed")
        .createObserver,
      meter
        .observableUpDownCounter[Long](METRIC_DB_CLIENT_CONNECTION_PENDING_REQUESTS)
        .withUnit("{request}")
        .withDescription("The number of current pending requests for an open connection")
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
