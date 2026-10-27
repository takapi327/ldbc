/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.otel4s

import scala.concurrent.duration.FiniteDuration

import cats.syntax.all.*

import cats.effect.kernel.{ MonadCancelThrow, Resource as CatsResource }

import org.typelevel.otel4s.metrics.{ BucketBoundaries, Counter, Histogram, ObservableMeasurement }
import org.typelevel.otel4s.metrics as otelmetrics
import org.typelevel.otel4s.trace as oteltrace
import org.typelevel.otel4s.Attribute as OtelAttribute

import ldbc.sql.Attribute

import ldbc.effect.{ Concurrent, Resource }
import ldbc.telemetry.*

/**
 * otel4s-backed implementation of the DB-agnostic `ldbc.telemetry` SPI.
 *
 * Because the tracing SPI mirrors otel4s's own `Tracer` / `Span` / `SpanOps` shape, each operation delegates
 * directly to the corresponding otel4s effect — spans run natively on the caller's `F` (`IO`) with no
 * intermediate effect bridge. The only translation is `ldbc.sql.Attribute` to
 * `org.typelevel.otel4s.Attribute`.
 *
 * The metrics side implements [[ldbc.telemetry.Meter]] on top of otel4s instruments, so the driver's
 * [[ldbc.telemetry.DatabaseMetrics]] calls become real OpenTelemetry measurements. Instrument names, units,
 * descriptions and bucket boundaries are read from [[ldbc.telemetry.DbMetric]] rather than being
 * spelled out here, so this backend and `ldbc-zio-telemetry` cannot drift apart.
 */
object Otel4sTelemetry:

  private def toOtelAttribute(attribute: Attribute[?]): OtelAttribute[?] =
    (attribute.value: Any) match
      case value: String  => OtelAttribute(attribute.key, value)
      case value: Long    => OtelAttribute(attribute.key, value)
      case value: Int     => OtelAttribute(attribute.key, value.toLong)
      case value: Boolean => OtelAttribute(attribute.key, value)
      case value: Double  => OtelAttribute(attribute.key, value)
      case value          => OtelAttribute(attribute.key, value.toString)

  private def toOtelStatus(status: StatusCode): oteltrace.StatusCode = status match
    case StatusCode.Unset => oteltrace.StatusCode.Unset
    case StatusCode.Ok    => oteltrace.StatusCode.Ok
    case StatusCode.Error => oteltrace.StatusCode.Error

  /**
   * Adapts a `cats.effect.Resource` (the type otel4s returns for a registered batch callback) into the
   * effect-agnostic [[ldbc.effect.Resource]] the SPI is written against. Both sides run on the same `F`, so
   * this is a structural adaptation only.
   */
  private def fromCatsResource[F[_], A](
    resource: CatsResource[F, A]
  )(using MonadCancelThrow[F], Concurrent[F]): Resource[F, A] =
    Resource.make(resource.allocated)((pair: (A, F[Unit])) => pair._2).map(_._1)

  private def wrapSpan[F[_]](span: oteltrace.Span[F]): Span[F] = new Span[F]:
    override def addAttribute(attribute: Attribute[?]): F[Unit] =
      span.addAttribute(toOtelAttribute(attribute))
    override def addAttributes(attributes: Attribute[?]*): F[Unit] =
      span.addAttributes(attributes.map(toOtelAttribute)*)
    override def recordException(exception: Throwable, attributes: Attribute[?]*): F[Unit] =
      span.recordException(exception, attributes.map(toOtelAttribute)*)
    override def setStatus(status: StatusCode, description: String): F[Unit] =
      span.setStatus(toOtelStatus(status), description)

  private def wrapSpanOps[F[_]](spanOps: oteltrace.SpanOps[F]): SpanOps[F] = new SpanOps[F]:
    override def use[A](f: Span[F] => F[A]): F[A] = spanOps.use(span => f(wrapSpan(span)))

  private def wrapTracer[F[_]](tracer: oteltrace.Tracer[F]): Tracer[F] = new Tracer[F]:
    override def span(name: String, attributes: Attribute[?]*): SpanOps[F] =
      wrapSpanOps(tracer.span(name, attributes.map(toOtelAttribute)*))

  private def wrapTracerBuilder[F[_]: cats.Functor](builder: oteltrace.TracerBuilder[F]): TracerBuilder[F] =
    new TracerBuilder[F]:
      override def withVersion(version: String): TracerBuilder[F] =
        wrapTracerBuilder(builder.withVersion(version))
      override def withSchemaUrl(schemaUrl: String): TracerBuilder[F] =
        wrapTracerBuilder(builder.withSchemaUrl(schemaUrl))
      override def get: F[Tracer[F]] = builder.get.map(wrapTracer)

  /**
   * [[ldbc.telemetry.DatabaseMetrics]] backed by otel4s instruments.
   *
   * @param operationDuration    `db.client.operation.duration`
   * @param returnedRows         `db.client.response.returned_rows`
   * @param connectionCreateTime `db.client.connection.create_time`
   * @param connectionWaitTime   `db.client.connection.wait_time`
   * @param connectionUseTime    `db.client.connection.use_time`
   * @param connectionTimeouts   `db.client.connection.timeouts`
   * @param meter                the meter the observable pool gauges are registered on
   */
  private class Otel4sDatabaseMetrics[F[_]](
    operationDuration:    Histogram[F, Double],
    returnedRows:         Histogram[F, Double],
    connectionCreateTime: Histogram[F, Double],
    connectionWaitTime:   Histogram[F, Double],
    connectionUseTime:    Histogram[F, Double],
    connectionTimeouts:   Counter[F, Long],
    meter:                otelmetrics.Meter[F]
  )(using MonadCancelThrow[F], Concurrent[F])
    extends DatabaseMetrics[F]:

    private def toSeconds(duration: FiniteDuration): Double = duration.toNanos.toDouble / 1e9

    private def poolNameAttribute(poolName: String): OtelAttribute[String] =
      OtelAttribute(DbAttributes.DbClientConnectionPoolName.name, poolName)

    override def recordOperationDuration(duration: FiniteDuration, attributes: Attribute[?]*): F[Unit] =
      operationDuration.record(toSeconds(duration), attributes.map(toOtelAttribute)*)

    override def recordReturnedRows(rows: Long, attributes: Attribute[?]*): F[Unit] =
      returnedRows.record(rows.toDouble, attributes.map(toOtelAttribute)*)

    override def recordConnectionCreateTime(duration: FiniteDuration, poolName: String): F[Unit] =
      connectionCreateTime.record(toSeconds(duration), poolNameAttribute(poolName))

    override def recordConnectionWaitTime(duration: FiniteDuration, poolName: String): F[Unit] =
      connectionWaitTime.record(toSeconds(duration), poolNameAttribute(poolName))

    override def recordConnectionUseTime(duration: FiniteDuration, poolName: String): F[Unit] =
      connectionUseTime.record(toSeconds(duration), poolNameAttribute(poolName))

    override def recordConnectionTimeout(poolName: String): F[Unit] =
      connectionTimeouts.inc(poolNameAttribute(poolName))

    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  F[PoolMetricsState]
    ): Resource[F, Unit] =
      val poolNameAttr = poolNameAttribute(poolName)
      val stateIdle    = OtelAttribute(
        DbAttributes.DbClientConnectionState.name,
        DbAttributes.DbClientConnectionStateValue.Idle
      )
      val stateUsed = OtelAttribute(
        DbAttributes.DbClientConnectionState.name,
        DbAttributes.DbClientConnectionStateValue.Used
      )

      fromCatsResource(
        meter.batchCallback.of(
          observerOf(meter, DbMetric.ClientConnectionCount),
          observerOf(meter, DbMetric.ClientConnectionIdleMax),
          observerOf(meter, DbMetric.ClientConnectionIdleMin),
          observerOf(meter, DbMetric.ClientConnectionMax),
          observerOf(meter, DbMetric.ClientConnectionPendingRequests)
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
      )

  /** Builds the histogram described by `spec` on `meter`. */
  private def histogramOf[F[_]](meter: otelmetrics.Meter[F], spec: DbMetric): F[Histogram[F, Double]] =
    meter
      .histogram[Double](spec.name)
      .withUnit(spec.unit)
      .withDescription(spec.description)
      .withExplicitBucketBoundaries(BucketBoundaries(spec.boundaries*))
      .create

  /** Builds the counter described by `spec` on `meter`. */
  private def counterOf[F[_]](meter: otelmetrics.Meter[F], spec: DbMetric): F[Counter[F, Long]] =
    meter.counter[Long](spec.name).withUnit(spec.unit).withDescription(spec.description).create

  /** Builds the observable up-down counter described by `spec` on `meter`. */
  private def observerOf[F[_]](meter: otelmetrics.Meter[F], spec: DbMetric): F[ObservableMeasurement[F, Long]] =
    meter
      .observableUpDownCounter[Long](spec.name)
      .withUnit(spec.unit)
      .withDescription(spec.description)
      .createObserver

  private def wrapMeter[F[_]](
    meter: otelmetrics.Meter[F]
  )(using MonadCancelThrow[F], Concurrent[F]): Meter[F] = new Meter[F]:
    override def databaseMetrics: F[DatabaseMetrics[F]] =
      for
        operationDuration    <- histogramOf(meter, DbMetric.ClientOperationDuration)
        returnedRows         <- histogramOf(meter, DbMetric.ClientResponseReturnedRows)
        connectionCreateTime <- histogramOf(meter, DbMetric.ClientConnectionCreateTime)
        connectionWaitTime   <- histogramOf(meter, DbMetric.ClientConnectionWaitTime)
        connectionUseTime    <- histogramOf(meter, DbMetric.ClientConnectionUseTime)
        connectionTimeouts   <- counterOf(meter, DbMetric.ClientConnectionTimeouts)
      yield new Otel4sDatabaseMetrics[F](
        operationDuration,
        returnedRows,
        connectionCreateTime,
        connectionWaitTime,
        connectionUseTime,
        connectionTimeouts,
        meter
      )

  private def wrapMeterBuilder[F[_]](
    builder: otelmetrics.MeterBuilder[F]
  )(using MonadCancelThrow[F], Concurrent[F]): MeterBuilder[F] =
    new MeterBuilder[F]:
      override def withVersion(version: String): MeterBuilder[F] =
        wrapMeterBuilder(builder.withVersion(version))
      override def withSchemaUrl(schemaUrl: String): MeterBuilder[F] =
        wrapMeterBuilder(builder.withSchemaUrl(schemaUrl))
      override def get: F[Meter[F]] = builder.get.map(wrapMeter[F])

  /** Wraps an otel4s `TracerProvider` as the `ldbc.telemetry.TracerProvider` the driver consumes. */
  def tracerProvider[F[_]: cats.Functor](provider: oteltrace.TracerProvider[F]): TracerProvider[F] =
    new TracerProvider[F]:
      override def tracer(name: String): TracerBuilder[F] = wrapTracerBuilder(provider.tracer(name))

  /**
   * Wraps an otel4s `MeterProvider` as the `ldbc.telemetry.MeterProvider` the driver consumes.
   *
   * `MonadCancelThrow` is required because otel4s hands back the registered batch callback as a
   * `cats.effect.Resource`, and `ldbc.effect.Concurrent` because the SPI's own resource type is built on it.
   */
  def meterProvider[F[_]](
    provider: otelmetrics.MeterProvider[F]
  )(using MonadCancelThrow[F], Concurrent[F]): MeterProvider[F] =
    new MeterProvider[F]:
      override def meter(name: String): MeterBuilder[F] = wrapMeterBuilder(provider.meter(name))

  given derivedTracerProvider[F[_]: cats.Functor](using provider: oteltrace.TracerProvider[F]): TracerProvider[F] =
    tracerProvider(provider)

  given derivedMeterProvider[F[_]](using
    MonadCancelThrow[F],
    Concurrent[F],
    otelmetrics.MeterProvider[F]
  ): MeterProvider[F] =
    meterProvider(summon[otelmetrics.MeterProvider[F]])
