/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.telemetry

/**
 * The instrument kind a [[MetricSpec]] is recorded on. Backends map these onto whatever their metrics
 * library calls the same concept.
 */
enum InstrumentKind:
  /** A distribution of values with explicit bucket boundaries. */
  case Histogram

  /** A monotonically increasing count. */
  case Counter

  /** A non-monotonic value read from a callback at export time. */
  case ObservableUpDownCounter

/**
 * The description of one metric instrument: everything a backend needs to create it.
 *
 * @param name        the instrument name, e.g. `db.client.operation.duration`
 * @param unit        the UCUM unit or the curly-brace annotation, e.g. `s` or `{connection}`
 * @param description the human-readable description exported alongside the metric
 * @param kind        the instrument kind
 * @param boundaries  explicit bucket boundaries, for [[InstrumentKind.Histogram]] only
 */
final case class MetricSpec(
  name:        String,
  unit:        String,
  description: String,
  kind:        InstrumentKind,
  boundaries:  List[Double] = Nil
)

/**
 * The metric instruments the driver and the pool emit, inlined from the OpenTelemetry database metrics
 * semantic conventions (v1.39.0).
 *
 * They live here, in the CE-free core, so that every backend (`ldbc-otel4s`, `ldbc-zio-telemetry`, ...)
 * creates the *same* instruments from a single definition. Backends differ in how they build an
 * instrument, not in what it is called, what unit it carries, or how its buckets are laid out — which is
 * what would silently drift if each spelled the descriptors out itself. This mirrors how
 * [[ldbc.telemetry.DbAttributes]] inlines the attribute keys rather than depending on a semconv artifact.
 *
 * @see [[https://opentelemetry.io/docs/specs/semconv/database/database-metrics/]]
 */
object DbMetricSpecs:

  /**
   * Bucket boundaries for the duration histograms, in seconds. Fixed by the semantic conventions:
   * `[0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1, 5, 10]`.
   */
  val durationBoundaries: List[Double] = List(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0)

  /**
   * Bucket boundaries for the returned-rows histogram. Fixed by the semantic conventions:
   * `[1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000]`.
   */
  val returnedRowsBoundaries: List[Double] =
    List(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0)

  /** `db.client.operation.duration` — how long a database client operation took. */
  val clientOperationDuration: MetricSpec = MetricSpec(
    name        = "db.client.operation.duration",
    unit        = "s",
    description = "Duration of database client operations.",
    kind        = InstrumentKind.Histogram,
    boundaries  = durationBoundaries
  )

  /** `db.client.response.returned_rows` — how many rows an operation returned. */
  val clientResponseReturnedRows: MetricSpec = MetricSpec(
    name        = "db.client.response.returned_rows",
    unit        = "{row}",
    description = "The actual number of records returned by the database operation.",
    kind        = InstrumentKind.Histogram,
    boundaries  = returnedRowsBoundaries
  )

  /** `db.client.connection.create_time` — how long establishing a new physical connection took. */
  val clientConnectionCreateTime: MetricSpec = MetricSpec(
    name        = "db.client.connection.create_time",
    unit        = "s",
    description = "The time it took to create a new connection.",
    kind        = InstrumentKind.Histogram,
    boundaries  = durationBoundaries
  )

  /** `db.client.connection.wait_time` — how long acquiring a connection from the pool took. */
  val clientConnectionWaitTime: MetricSpec = MetricSpec(
    name        = "db.client.connection.wait_time",
    unit        = "s",
    description = "The time it took to obtain an open connection from the pool.",
    kind        = InstrumentKind.Histogram,
    boundaries  = durationBoundaries
  )

  /** `db.client.connection.use_time` — how long a borrowed connection was held. */
  val clientConnectionUseTime: MetricSpec = MetricSpec(
    name        = "db.client.connection.use_time",
    unit        = "s",
    description = "The time between borrowing a connection and returning it to the pool.",
    kind        = InstrumentKind.Histogram,
    boundaries  = durationBoundaries
  )

  /** `db.client.connection.timeouts` — how many acquisitions timed out. */
  val clientConnectionTimeouts: MetricSpec = MetricSpec(
    name        = "db.client.connection.timeouts",
    unit        = "{timeout}",
    description = "The number of connection timeouts that have occurred trying to obtain a connection from the pool.",
    kind        = InstrumentKind.Counter
  )

  /** `db.client.connection.count` — connections per [[DbAttributes.DbClientConnectionState]]. */
  val clientConnectionCount: MetricSpec = MetricSpec(
    name        = "db.client.connection.count",
    unit        = "{connection}",
    description = "The number of connections that are currently in state described by the `state` attribute.",
    kind        = InstrumentKind.ObservableUpDownCounter
  )

  /** `db.client.connection.idle.max` — the configured upper bound on idle connections. */
  val clientConnectionIdleMax: MetricSpec = MetricSpec(
    name        = "db.client.connection.idle.max",
    unit        = "{connection}",
    description = "The maximum number of idle open connections allowed.",
    kind        = InstrumentKind.ObservableUpDownCounter
  )

  /** `db.client.connection.idle.min` — the configured lower bound on idle connections. */
  val clientConnectionIdleMin: MetricSpec = MetricSpec(
    name        = "db.client.connection.idle.min",
    unit        = "{connection}",
    description = "The minimum number of idle open connections allowed.",
    kind        = InstrumentKind.ObservableUpDownCounter
  )

  /** `db.client.connection.max` — the configured maximum pool size. */
  val clientConnectionMax: MetricSpec = MetricSpec(
    name        = "db.client.connection.max",
    unit        = "{connection}",
    description = "The maximum number of open connections allowed.",
    kind        = InstrumentKind.ObservableUpDownCounter
  )

  /** `db.client.connection.pending_requests` — how many callers are waiting for a connection. */
  val clientConnectionPendingRequests: MetricSpec = MetricSpec(
    name        = "db.client.connection.pending_requests",
    unit        = "{request}",
    description = "The number of current pending requests for an open connection.",
    kind        = InstrumentKind.ObservableUpDownCounter
  )

  /** Every spec, in a stable order. Useful for cross-backend conformance tests. */
  val all: List[MetricSpec] = List(
    clientOperationDuration,
    clientResponseReturnedRows,
    clientConnectionCreateTime,
    clientConnectionWaitTime,
    clientConnectionUseTime,
    clientConnectionTimeouts,
    clientConnectionCount,
    clientConnectionIdleMax,
    clientConnectionIdleMin,
    clientConnectionMax,
    clientConnectionPendingRequests
  )

  /** The specs registered through [[DatabaseMetrics.registerPoolStateCallback]], in callback order. */
  val poolStateGauges: List[MetricSpec] = List(
    clientConnectionCount,
    clientConnectionIdleMax,
    clientConnectionIdleMin,
    clientConnectionMax,
    clientConnectionPendingRequests
  )
