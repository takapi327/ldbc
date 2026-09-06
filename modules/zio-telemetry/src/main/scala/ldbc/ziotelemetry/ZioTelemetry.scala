/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.ziotelemetry

import scala.concurrent.duration.FiniteDuration

import ldbc.sql.Attribute

import ldbc.effect.{ Concurrent, Resource }
import ldbc.telemetry.*

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode as JStatusCode
import zio.{ Chunk, Exit, Scope, Task, ZIO }
import zio.telemetry.opentelemetry.metrics.{ Meter as ZMeter, ObservableMeasurement }
import zio.telemetry.opentelemetry.tracing.Tracing

/**
 * zio-telemetry-backed implementation of the DB-agnostic `ldbc.telemetry` SPI, running natively on ZIO's
 * `Task` (no `zio-interop-cats`). Because zio-telemetry wraps the JVM-only OpenTelemetry Java SDK, this
 * adapter is JVM only.
 *
 * A span is opened with `Tracing.span`, whose default `StatusMapper` already marks the span `ERROR` when the
 * body fails. The SPI's imperative `Span` operations (`addAttribute` / `recordException` / `setStatus`) run
 * against the current span — `setAttribute` via zio-telemetry, and exception/status via the underlying
 * OpenTelemetry Java span obtained from `getCurrentSpanUnsafe`.
 *
 * The provided [[zio.telemetry.opentelemetry.tracing.Tracing]] already fixes the instrumentation name and
 * version, so [[ldbc.telemetry.TracerBuilder]]'s `withVersion` / `withSchemaUrl` and `tracer`'s name are
 * accepted but ignored. The same holds for [[zio.telemetry.opentelemetry.metrics.Meter]] and
 * [[ldbc.telemetry.MeterBuilder]].
 *
 * The metrics side builds its instruments from [[ldbc.telemetry.DbMetricSpecs]], the same definitions
 * `ldbc-otel4s` uses, so both backends export identical names, units, descriptions and bucket boundaries.
 * One behavioural difference remains: zio-telemetry has no batch-callback API, so the five pool state
 * gauges are registered as five independent observables. Each reads the pool state when it is exported,
 * whereas the otel4s backend reads it once per batch — so the five values may come from marginally
 * different snapshots.
 */
object ZioTelemetry:

  private def toJavaAttributes(attributes: Seq[Attribute[?]]): Attributes =
    val builder = Attributes.builder()
    attributes.foreach { attribute =>
      (attribute.value: Any) match
        case value: String  => builder.put(attribute.key, value)
        case value: Long    => builder.put(attribute.key, value)
        case value: Int     => builder.put(attribute.key, value.toLong)
        case value: Boolean => builder.put(attribute.key, value)
        case value: Double  => builder.put(attribute.key, value)
        case value          => builder.put(attribute.key, value.toString)
    }
    builder.build()

  private def toJavaStatus(status: StatusCode): JStatusCode = status match
    case StatusCode.Unset => JStatusCode.UNSET
    case StatusCode.Ok    => JStatusCode.OK
    case StatusCode.Error => JStatusCode.ERROR

  private def wrapSpan(tracing: Tracing): Span[Task] = new Span[Task]:
    override def addAttribute(attribute: Attribute[?]): Task[Unit] =
      (attribute.value: Any) match
        case value: String  => tracing.setAttribute(attribute.key, value)
        case value: Long    => tracing.setAttribute(attribute.key, value)
        case value: Int     => tracing.setAttribute(attribute.key, value.toLong)
        case value: Boolean => tracing.setAttribute(attribute.key, value)
        case value: Double  => tracing.setAttribute(attribute.key, value)
        case value          => tracing.setAttribute(attribute.key, value.toString)

    override def addAttributes(attributes: Attribute[?]*): Task[Unit] =
      ZIO.foreachDiscard(attributes)(addAttribute)

    override def recordException(exception: Throwable, attributes: Attribute[?]*): Task[Unit] =
      tracing.getCurrentSpanUnsafe.map(_.recordException(exception, toJavaAttributes(attributes))).unit

    override def setStatus(status: StatusCode, description: String): Task[Unit] =
      tracing.getCurrentSpanUnsafe.map(_.setStatus(toJavaStatus(status), description)).unit

  private def wrapTracer(tracing: Tracing): Tracer[Task] = new Tracer[Task]:
    override def span(name: String, attributes: Attribute[?]*): SpanOps[Task] = new SpanOps[Task]:
      override def use[A](f: Span[Task] => Task[A]): Task[A] =
        tracing.span(name, attributes = toJavaAttributes(attributes))(f(wrapSpan(tracing)))

  /** Wraps a zio-telemetry `Tracing` as the `ldbc.telemetry.TracerProvider` the driver consumes. */
  def tracerProvider(tracing: Tracing): TracerProvider[Task] = new TracerProvider[Task]:
    override def tracer(name: String): TracerBuilder[Task] = new TracerBuilder[Task]:
      override def withVersion(version:     String): TracerBuilder[Task] = this
      override def withSchemaUrl(schemaUrl: String): TracerBuilder[Task] = this
      override def get:                              Task[Tracer[Task]]  = ZIO.succeed(wrapTracer(tracing))

  /** Attaches the pool name every connection metric carries. */
  private def poolAttributes(poolName: String): Attributes =
    toJavaAttributes(Seq(DbAttributes.DbClientConnectionPoolName(poolName)))

  /**
   * Runs a scoped ZIO for the lifetime of an [[ldbc.effect.Resource]].
   *
   * The scope is acquired as its own resource *before* `scoped` runs, so that a failure partway through
   * `scoped` still closes it. Registering the pool gauges is a sequence of scoped registrations; folding
   * both steps into one `acquire` would mean a failure on the third registration left the first two
   * attached to a scope nobody could close, leaking their callbacks (and the pool they capture).
   */
  private def scopedToResource(scoped: ZIO[Scope, Throwable, Unit])(using Concurrent[Task]): Resource[Task, Unit] =
    Resource
      .make(Scope.make: Task[Scope.Closeable])(scope => scope.close(Exit.unit).unit)
      .flatMap(scope => Resource.eval(scope.extend[Any](scoped)))

  /**
   * [[ldbc.telemetry.DatabaseMetrics]] backed by zio-telemetry instruments.
   *
   * @param meter                the meter the observable pool gauges are registered on
   * @param operationDuration    `db.client.operation.duration`
   * @param returnedRows         `db.client.response.returned_rows`
   * @param connectionCreateTime `db.client.connection.create_time`
   * @param connectionWaitTime   `db.client.connection.wait_time`
   * @param connectionUseTime    `db.client.connection.use_time`
   * @param connectionTimeouts   `db.client.connection.timeouts`
   */
  private class ZioDatabaseMetrics(
    meter:                ZMeter,
    operationDuration:    zio.telemetry.opentelemetry.metrics.Histogram[Double],
    returnedRows:         zio.telemetry.opentelemetry.metrics.Histogram[Double],
    connectionCreateTime: zio.telemetry.opentelemetry.metrics.Histogram[Double],
    connectionWaitTime:   zio.telemetry.opentelemetry.metrics.Histogram[Double],
    connectionUseTime:    zio.telemetry.opentelemetry.metrics.Histogram[Double],
    connectionTimeouts:   zio.telemetry.opentelemetry.metrics.Counter[Long]
  )(using Concurrent[Task])
    extends DatabaseMetrics[Task]:

    private def toSeconds(duration: FiniteDuration): Double = duration.toNanos.toDouble / 1e9

    override def recordOperationDuration(duration: FiniteDuration, attributes: Attribute[?]*): Task[Unit] =
      operationDuration.record(toSeconds(duration), toJavaAttributes(attributes))

    override def recordReturnedRows(rows: Long, attributes: Attribute[?]*): Task[Unit] =
      returnedRows.record(rows.toDouble, toJavaAttributes(attributes))

    override def recordConnectionCreateTime(duration: FiniteDuration, poolName: String): Task[Unit] =
      connectionCreateTime.record(toSeconds(duration), poolAttributes(poolName))

    override def recordConnectionWaitTime(duration: FiniteDuration, poolName: String): Task[Unit] =
      connectionWaitTime.record(toSeconds(duration), poolAttributes(poolName))

    override def recordConnectionUseTime(duration: FiniteDuration, poolName: String): Task[Unit] =
      connectionUseTime.record(toSeconds(duration), poolAttributes(poolName))

    override def recordConnectionTimeout(poolName: String): Task[Unit] =
      connectionTimeouts.inc(poolAttributes(poolName))

    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  Task[PoolMetricsState]
    ): Resource[Task, Unit] =
      val poolAttrs = poolAttributes(poolName)
      def stateAttrs(state: String): Attributes =
        toJavaAttributes(
          Seq(DbAttributes.DbClientConnectionPoolName(poolName), DbAttributes.DbClientConnectionState(state))
        )

      def gauge(spec: MetricSpec)(callback: ObservableMeasurement[Long] => Task[Unit]): ZIO[Scope, Throwable, Unit] =
        meter.observableUpDownCounter(spec.name, Some(spec.unit), Some(spec.description))(callback)

      val scoped: ZIO[Scope, Throwable, Unit] =
        for
          _ <- gauge(DbMetricSpecs.clientConnectionCount) { observer =>
                 stateProvider.flatMap { state =>
                   observer.record(state.idleCount, stateAttrs(DbAttributes.DbClientConnectionStateValue.Idle)) *>
                     observer.record(state.usedCount, stateAttrs(DbAttributes.DbClientConnectionStateValue.Used))
                 }
               }
          _ <- gauge(DbMetricSpecs.clientConnectionIdleMax)(_.record(maxConnections.toLong, poolAttrs))
          _ <- gauge(DbMetricSpecs.clientConnectionIdleMin)(_.record(minConnections.toLong, poolAttrs))
          _ <- gauge(DbMetricSpecs.clientConnectionMax)(_.record(maxConnections.toLong, poolAttrs))
          _ <- gauge(DbMetricSpecs.clientConnectionPendingRequests) { observer =>
                 stateProvider.flatMap(state => observer.record(state.pendingRequestCount, poolAttrs))
               }
        yield ()

      scopedToResource(scoped)

  /**
   * Wraps a zio-telemetry `Meter` as the `ldbc.telemetry.MeterProvider` the driver consumes.
   *
   * The `ldbc.effect.Concurrent[Task]` instance is taken implicitly rather than depended on directly, so
   * this module stays independent of `ldbc-zio`; import `ldbc.zio.given` at the call site.
   */
  def meterProvider(zmeter: ZMeter)(using Concurrent[Task]): MeterProvider[Task] = new MeterProvider[Task]:
    override def meter(name: String): MeterBuilder[Task] = new MeterBuilder[Task]:
      override def withVersion(version:     String): MeterBuilder[Task] = this
      override def withSchemaUrl(schemaUrl: String): MeterBuilder[Task] = this
      override def get:                              Task[Meter[Task]]  = ZIO.succeed(wrapMeter(zmeter))

  private def wrapMeter(zmeter: ZMeter)(using Concurrent[Task]): Meter[Task] = new Meter[Task]:
    override def databaseMetrics: Resource[Task, DatabaseMetrics[Task]] =
      def histogram(spec: MetricSpec): Task[zio.telemetry.opentelemetry.metrics.Histogram[Double]] =
        zmeter.histogram(spec.name, Some(spec.unit), Some(spec.description), Some(Chunk.fromIterable(spec.boundaries)))
      def counter(spec: MetricSpec): Task[zio.telemetry.opentelemetry.metrics.Counter[Long]] =
        zmeter.counter(spec.name, Some(spec.unit), Some(spec.description))
      for
        operationDuration    <- Resource.eval(histogram(DbMetricSpecs.clientOperationDuration))
        returnedRows         <- Resource.eval(histogram(DbMetricSpecs.clientResponseReturnedRows))
        connectionCreateTime <- Resource.eval(histogram(DbMetricSpecs.clientConnectionCreateTime))
        connectionWaitTime   <- Resource.eval(histogram(DbMetricSpecs.clientConnectionWaitTime))
        connectionUseTime    <- Resource.eval(histogram(DbMetricSpecs.clientConnectionUseTime))
        connectionTimeouts   <- Resource.eval(counter(DbMetricSpecs.clientConnectionTimeouts))
      yield new ZioDatabaseMetrics(
        zmeter,
        operationDuration,
        returnedRows,
        connectionCreateTime,
        connectionWaitTime,
        connectionUseTime,
        connectionTimeouts
      )
