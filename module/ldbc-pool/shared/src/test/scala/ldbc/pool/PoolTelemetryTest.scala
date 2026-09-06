/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

import ldbc.sql.{ Attribute, Connection, DataSource }

import ldbc.effect.{ Ref, Resource }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite
import ldbc.telemetry.{ DatabaseMetrics, Meter, PoolMetricsState }

/**
 * Verifies that the pool drives the [[ldbc.telemetry.DatabaseMetrics]] SPI — the half of the connector's
 * pool metrics that was missing after the port. A recording [[DatabaseMetrics]] stands in for a real
 * backend, so this asserts the pool's call sites and the observable-gauge lifecycle without depending on
 * otel4s; that the SPI in turn produces real OpenTelemetry instruments is covered by
 * `ldbc.otel4s.Otel4sTelemetryTest`.
 */
class PoolTelemetryTest extends FxSuite:

  /**
   * A [[DatabaseMetrics]] that appends the name of every call it receives, for assertions.
   *
   * @param failCreateTime when true, `recordConnectionCreateTime` records the call and then raises, so a
   *                       test can pin how the pool behaves if the metrics backend itself fails
   */
  private final class RecordingMetrics(failCreateTime: Boolean = false) extends DatabaseMetrics[Fx]:
    val calls: ConcurrentLinkedQueue[String] = new ConcurrentLinkedQueue[String]()

    /** The state provider handed to [[registerPoolStateCallback]], so a test can pull the live snapshot. */
    @volatile var stateProvider: Option[Fx[PoolMetricsState]] = None

    private def record(name: String): Fx[Unit] = Fx.delay { calls.add(name); () }

    def recorded: List[String] = calls.iterator().asScala.toList

    override def recordOperationDuration(duration: FiniteDuration, attributes: Attribute[?]*): Fx[Unit] =
      record("operationDuration")
    override def recordReturnedRows(rows: Long, attributes: Attribute[?]*): Fx[Unit] =
      record("returnedRows")
    override def recordConnectionCreateTime(duration: FiniteDuration, poolName: String): Fx[Unit] =
      record(s"createTime:$poolName") *>
        (if failCreateTime then Fx.raiseError(new RuntimeException("metrics backend is down")) else Fx.unit)
    override def recordConnectionWaitTime(duration: FiniteDuration, poolName: String): Fx[Unit] =
      record(s"waitTime:$poolName")
    override def recordConnectionUseTime(duration: FiniteDuration, poolName: String): Fx[Unit] =
      record(s"useTime:$poolName")
    override def recordConnectionTimeout(poolName: String): Fx[Unit] =
      record(s"timeout:$poolName")
    override def registerPoolStateCallback(
      poolName:       String,
      minConnections: Int,
      maxConnections: Int,
      stateProvider:  Fx[PoolMetricsState]
    ): Resource[Fx, Unit] =
      Resource.make(
        Fx.delay {
          this.stateProvider = Some(stateProvider)
          calls.add(s"register:$poolName:$minConnections:$maxConnections")
          ()
        }
      )(_ => Fx.delay { calls.add("unregister"); () })

  private def recordingMeter(metrics: RecordingMetrics): Meter[Fx] = new Meter[Fx]:
    override def databaseMetrics: Resource[Fx, DatabaseMetrics[Fx]] = Resource.pure(metrics)

  private def mockCreate: Resource[Fx, Connection[Fx]] =
    Resource.make(MockConnection().map(c => (c: Connection[Fx])))(c => c.close())

  /** A `create` that always fails, for the connection-creation failure path. */
  private def failingCreate: Resource[Fx, Connection[Fx]] =
    Resource.eval(Fx.raiseError[Connection[Fx]](new RuntimeException("cannot connect")))

  /** The allocated-form [[ldbc.sql.DataSource]] the `fromDataSource*` factories consume. */
  private def mockDataSource(create: Resource[Fx, Connection[Fx]] = mockCreate): DataSource[Fx] =
    new DataSource[Fx]:
      override def getConnection: Fx[(Connection[Fx], Fx[Unit])] = create.allocatedCase

  private def config(min: Int, max: Int, connectionTimeout: FiniteDuration = 5.seconds): ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections    = min,
      maxConnections    = max,
      connectionTimeout = connectionTimeout,
      adaptiveSizing    = false,
      poolName          = "telemetry-pool"
    )

  private def pool(
    config:  ConnectionPoolConfig,
    metrics: RecordingMetrics,
    create:  Resource[Fx, Connection[Fx]] = mockCreate
  ): Resource[Fx, PooledDataSource[Fx]] =
    PooledDataSource.fromConfig(config, create, meter = Some(recordingMeter(metrics)))

  test("creating the pool's minimum connections records a connection create time for each") {
    val metrics = new RecordingMetrics
    pool(config(3, 5), metrics).use(_ => Fx.unit).map { _ =>
      assertEquals(metrics.recorded.count(_ == "createTime:telemetry-pool"), 3)
    }
  }

  test("acquiring a connection records the wait time, and releasing it records the use time") {
    val metrics = new RecordingMetrics
    pool(config(1, 5), metrics).use { datasource =>
      datasource.use(_ => Fx.unit).map { _ =>
        assertEquals(metrics.recorded.count(_ == "waitTime:telemetry-pool"), 1)
        assertEquals(metrics.recorded.count(_ == "useTime:telemetry-pool"), 1)
      }
    }
  }

  test("growing the pool beyond its idle connections still records a wait time for the new connection") {
    val metrics = new RecordingMetrics
    pool(config(1, 3), metrics).use { datasource =>
      datasource.use(_ => datasource.use(_ => Fx.unit)).map { _ =>
        assertEquals(metrics.recorded.count(_ == "waitTime:telemetry-pool"), 2)
      }
    }
  }

  test("an acquisition timeout is recorded as a connection timeout") {
    val metrics = new RecordingMetrics
    pool(config(1, 1, connectionTimeout = 300.millis), metrics).use { datasource =>
      datasource
        .use(_ => datasource.use(_ => Fx.unit).attempt)
        .map { result =>
          assert(result.isLeft, s"the second acquisition times out, but got $result")
          assertEquals(metrics.recorded.count(_ == "timeout:telemetry-pool"), 1)
        }
    }
  }

  test("the pool state gauges are registered with the pool bounds and unregistered on shutdown") {
    val metrics = new RecordingMetrics
    pool(config(2, 7), metrics).use(_ => Fx.unit).map { _ =>
      val recorded = metrics.recorded
      assert(
        recorded.contains("register:telemetry-pool:2:7"),
        s"the gauges are registered with min/max, but got $recorded"
      )
      assert(recorded.contains("unregister"), s"the gauges are unregistered on release, but got $recorded")
      assert(
        recorded.indexOf("register:telemetry-pool:2:7") < recorded.indexOf("unregister"),
        "registration happens before unregistration"
      )
    }
  }

  test("the registered state provider reports the live idle / used / pending counts") {
    val metrics = new RecordingMetrics
    pool(config(2, 5), metrics).use { datasource =>
      datasource.use { _ =>
        metrics.stateProvider match
          case None           => Fx.delay(fail("the pool never registered a state provider"))
          case Some(provider) =>
            provider.map { state =>
              assertEquals(state.usedCount, 1L)
              assertEquals(state.idleCount, 1L)
              assertEquals(state.pendingRequestCount, 0L)
            }
      }
    }
  }

  test("a pool built without a meter behaves the same, and Meter.noop accepts every call site") {
    val withoutMeter = PooledDataSource.fromConfig(config(1, 2), mockCreate).use { datasource =>
      datasource.use(_ => Fx.unit) *> datasource.status.map(status => assertEquals(status.total, 1))
    }
    val withNoopMeter =
      PooledDataSource.fromConfig(config(1, 2), mockCreate, meter = Some(Meter.noop[Fx])).use { datasource =>
        datasource.use(_ => Fx.unit) *> datasource.status.map(status => assertEquals(status.total, 1))
      }
    withoutMeter *> withNoopMeter
  }

  test("a failing metrics backend records the create time once, not twice") {
    val metrics = new RecordingMetrics(failCreateTime = true)
    PooledDataSource
      .fromConfig(config(1, 2), mockCreate, meter = Some(recordingMeter(metrics)))
      .use(_ => Fx.unit)
      .attempt
      .map { _ =>
        assertEquals(
          metrics.recorded.count(_ == "createTime:telemetry-pool"),
          1,
          s"a failing recording must not be retried on the error path: ${ metrics.recorded }"
        )
      }
  }

  test("fromDataSource forwards the meter to the pool") {
    val metrics = new RecordingMetrics
    PooledDataSource
      .fromDataSource(config(1, 3), mockDataSource(), meter = Some(recordingMeter(metrics)))
      .use(datasource => datasource.use(_ => Fx.unit))
      .map { _ =>
        assertEquals(metrics.recorded.count(_ == "waitTime:telemetry-pool"), 1)
        assertEquals(metrics.recorded.count(_ == "useTime:telemetry-pool"), 1)
      }
  }

  test("fromConfigWithBeforeAfter forwards the meter to the pool and still runs its hooks") {
    val metrics = new RecordingMetrics
    Ref.of[Fx, Int](0).flatMap { hookCalls =>
      PooledDataSource
        .fromConfigWithBeforeAfter[Fx, Unit](
          config(1, 3),
          mockCreate,
          before = _ => hookCalls.update(_ + 1),
          after = (_, _) => hookCalls.update(_ + 1),
          meter = Some(recordingMeter(metrics))
        )
        .use(datasource => datasource.use(_ => Fx.unit))
        .flatMap(_ => hookCalls.get)
        .map { hooks =>
          assertEquals(hooks, 2, "the before and after hooks both ran")
          assertEquals(metrics.recorded.count(_ == "waitTime:telemetry-pool"), 1)
          assertEquals(metrics.recorded.count(_ == "useTime:telemetry-pool"), 1)
        }
    }
  }

  test("fromDataSourceWithBeforeAfter forwards the meter to the pool and still runs its hooks") {
    val metrics = new RecordingMetrics
    Ref.of[Fx, Int](0).flatMap { hookCalls =>
      PooledDataSource
        .fromDataSourceWithBeforeAfter[Fx, Unit](
          config(1, 3),
          mockDataSource(),
          before = _ => hookCalls.update(_ + 1),
          after = (_, _) => hookCalls.update(_ + 1),
          meter = Some(recordingMeter(metrics))
        )
        .use(datasource => datasource.use(_ => Fx.unit))
        .flatMap(_ => hookCalls.get)
        .map { hooks =>
          assertEquals(hooks, 2, "the before and after hooks both ran")
          assertEquals(metrics.recorded.count(_ == "waitTime:telemetry-pool"), 1)
          assertEquals(metrics.recorded.count(_ == "useTime:telemetry-pool"), 1)
        }
    }
  }

  test("a failed connection creation records the create time exactly once and propagates the error") {
    val metrics = new RecordingMetrics
    PooledDataSource
      .fromConfig(config(1, 2), failingCreate, meter = Some(recordingMeter(metrics)))
      .use(_ => Fx.unit)
      .attempt
      .map { result =>
        assert(result.isLeft, s"the pool fails to start when it cannot create a connection, but got $result")
        assertEquals(
          metrics.recorded.count(_ == "createTime:telemetry-pool"),
          1,
          s"recorded exactly once on the failure path: ${ metrics.recorded }"
        )
      }
  }

  test("a release that removes the connection still records the use time exactly once") {
    val metrics = new RecordingMetrics
    // aliveBypassWindow = 0 forces validation on release, and the connection reports itself invalid,
    // so the release takes the remove-from-pool branch rather than the return-to-pool one.
    val invalidating = config(0, 2).copy(aliveBypassWindow = Duration.Zero)
    val create       = Resource.make(MockConnection(isValidResult = false).map(c => (c: Connection[Fx])))(_.close())
    PooledDataSource
      .fromConfig(invalidating, create, meter = Some(recordingMeter(metrics)))
      .use(datasource => datasource.use(_ => Fx.unit) *> datasource.status)
      .map { status =>
        assertEquals(status.total, 0, "the invalid connection was removed from the pool on release")
        assertEquals(metrics.recorded.count(_ == "useTime:telemetry-pool"), 1)
      }
  }
