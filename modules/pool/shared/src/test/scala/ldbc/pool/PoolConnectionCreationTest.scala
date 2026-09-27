/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.sql.{ Connection, SQLTransientConnectionException }

import ldbc.effect.{ Ref as EffectRef, Resource }
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * Covers how the pool establishes physical connections: that callers never wait on one, that no more
 * are started than the pool has room for, that a creation which outlives its budget gives its capacity
 * back, and that a failure reaches the caller as the cause of its timeout.
 */
class PoolConnectionCreationTest extends FxSuite:

  private def config(min: Int, max: Int, connectionTimeout: FiniteDuration): ConnectionPoolConfig =
    ConnectionPoolConfig(
      minConnections    = min,
      maxConnections    = max,
      connectionTimeout = connectionTimeout,
      adaptiveSizing    = false
    )

  /** A `create` that counts how many times it is run and how many connections it has closed. */
  private final class CountingCreate(
    val started:  EffectRef[Fx, Int],
    val released: EffectRef[Fx, Int],
    delay:        FiniteDuration,
    failUntil:    Int,
    failWith:     () => Throwable
  ):
    val resource: Resource[Fx, Connection[Fx]] =
      Resource.make(
        started.updateAndGet(_ + 1).flatMap { attempt =>
          (if delay > Duration.Zero then Fx.sleep(delay) else Fx.unit) *>
            (if attempt <= failUntil then Fx.raiseError[Connection[Fx]](failWith())
             else MockConnection().map(c => (c: Connection[Fx])))
        }
      )(connection => released.update(_ + 1) *> connection.close())

  private def countingCreate(
    delay:     FiniteDuration = Duration.Zero,
    failUntil: Int = 0,
    failWith:  () => Throwable = () => new RuntimeException("cannot connect")
  ): Fx[CountingCreate] =
    for
      started  <- EffectRef.of[Fx, Int](0)
      released <- EffectRef.of[Fx, Int](0)
    yield new CountingCreate(started, released, delay, failUntil, failWith)

  private def withPool[A](create: CountingCreate, poolConfig: ConnectionPoolConfig)(
    body: PooledDataSource[Fx] => Fx[A]
  ): Fx[A] =
    PooledDataSource.fromConfig(poolConfig, create.resource).use(body)

  test("a caller is not made to wait for a slow connection to be established") {
    countingCreate(delay = 3.seconds).flatMap { create =>
      withPool(create, config(0, 2, 300.millis)) { datasource =>
        for
          start   <- Fx.monotonic
          result  <- datasource.getConnection.attempt
          elapsed <- Fx.monotonic.map(_ - start)
        yield
          val error = result.left.getOrElse(fail(s"establishing the connection far outlasts the timeout: $result"))
          assert(
            error.isInstanceOf[SQLTransientConnectionException],
            s"an acquisition timeout is retryable and must say so by its type, but got $error"
          )
          assert(
            elapsed < 900.millis,
            s"the caller waited for the connection to be established rather than for its own timeout ($elapsed)"
          )
      }
    }
  }

  test("a burst of callers starts no more connections than the pool has room for") {
    countingCreate(delay = 100.millis).flatMap { create =>
      withPool(create, config(0, 4, 5.seconds)) { datasource =>
        for
          fibers  <- (1 to 32).toList.traverse(_ => datasource.use(_ => Fx.sleep(50.millis)).attempt.start)
          _       <- fibers.traverse_(_.joinWithNever)
          started <- create.started.get
          status  <- datasource.status
        yield
          assertEquals(started, 4, "one connection was started per unit of capacity, and no more")
          assert(status.total <= 4, s"the pool never exceeds its maximum size (${ status.total })")
      }
    }
  }

  test("a creation that outlives its budget gives its capacity back") {
    countingCreate(delay = 2.seconds).flatMap { create =>
      withPool(create, config(0, 1, 250.millis)) { datasource =>
        for
          first     <- datasource.getConnection.attempt
          _         <- Fx.sleep(350.millis)
          abandoned <- datasource.abandonedCreations
          state     <- datasource.poolState.get
          second    <- datasource.getConnection.attempt
          started   <- create.started.get
        yield
          assert(first.isLeft, "the first caller times out while the connection is still being established")
          assertEquals(abandoned, 1L, "the overrunning creation released its reservation")
          assertEquals(state.creating, 0, "no capacity is held by the abandoned attempt")
          assert(second.isLeft, "the second caller also times out, but for want of a connection, not capacity")
          assertEquals(started, 2, "the freed capacity let a second creation start")
      }
    }
  }

  test("an abandoned creation is still either adopted or closed when it finishes") {
    countingCreate(delay = 400.millis).flatMap { create =>
      withPool(create, config(0, 1, 250.millis)) { datasource =>
        for
          _         <- datasource.getConnection.attempt
          _         <- Fx.sleep(800.millis)
          abandoned <- datasource.abandonedCreations
          status    <- datasource.status
          released  <- create.released.get
        yield
          assertEquals(abandoned, 1L)
          assert(
            status.total == 1 || released == 1,
            s"the connection was neither taken into the pool nor closed (total: ${ status.total })"
          )
      }
    }
  }

  test("a failed creation is retried within the caller's window") {
    countingCreate(failUntil = 2).flatMap { create =>
      withPool(create, config(0, 2, 3.seconds)) { datasource =>
        for
          start   <- Fx.monotonic
          result  <- datasource.use(_ => Fx.unit).attempt
          elapsed <- Fx.monotonic.map(_ - start)
          started <- create.started.get
        yield
          assert(result.isRight, s"the third attempt succeeds well inside the window, but got $result")
          assertEquals(started, 3, "the first two failures were retried")
          assert(elapsed < 2.seconds, s"the caller did not have to wait out its timeout ($elapsed)")
      }
    }
  }

  test("an acquisition timeout carries the reason the pool could not grow") {
    val boom = new RuntimeException("no route to host")
    countingCreate(failUntil = Int.MaxValue, failWith = () => boom).flatMap { create =>
      withPool(create, config(0, 1, 300.millis)) { datasource =>
        datasource.getConnection.attempt.map { result =>
          val error = result.left.getOrElse(fail("the pool cannot create a connection, so acquisition must fail"))
          assert(
            error.isInstanceOf[SQLTransientConnectionException],
            s"expected a transient connection exception, got $error"
          )
          assertEquals(
            Option(error.getCause),
            Some(boom: Throwable),
            "the creation failure is the reason the caller waited for nothing"
          )
        }
      }
    }
  }

  test("a creation failure stops being reported once a connection succeeds") {
    countingCreate(failUntil = 1).flatMap { create =>
      withPool(create, config(0, 1, 500.millis)) { datasource =>
        for
          _        <- datasource.use(_ => Fx.unit)
          acquired <- datasource.getConnection
          result   <- datasource.getConnection.attempt
          _        <- acquired._2
        yield
          val error = result.left.getOrElse(fail("the pool is full, so the second acquisition must time out"))
          assertEquals(
            Option(error.getCause),
            None,
            "the earlier failure was superseded by a success and must not be blamed for a full pool"
          )
      }
    }
  }

  test("the pool reports how many connections it is establishing") {
    countingCreate(delay = 500.millis).flatMap { create =>
      withPool(create, config(0, 2, 3.seconds)) { datasource =>
        for
          caller     <- datasource.use(_ => Fx.unit).attempt.start
          _          <- Fx.sleep(150.millis)
          inProgress <- datasource.status
          _          <- caller.joinWithNever
          settled    <- datasource.status
        yield
          assertEquals(inProgress.creating, 1)
          assertEquals(inProgress.committed, 1, "capacity in flight counts towards what the pool has committed to")
          assertEquals(settled.creating, 0)
          assertEquals(settled.total, 1)
      }
    }
  }

  test("a pool whose connections have all died still answers within the acquisition budget") {
    val budget     = 500.millis
    val perCheck   = 400.millis
    val poolSize   = 6
    val poolConfig = ConnectionPoolConfig(
      minConnections    = poolSize,
      maxConnections    = poolSize,
      connectionTimeout = budget,
      validationTimeout = perCheck,
      aliveBypassWindow = Duration.Zero,
      adaptiveSizing    = false
    )
    val unresponsive = Resource.make(
      MockConnection(validationDelay = 1.minute).map(c => (c: Connection[Fx]))
    )(_ => Fx.unit)

    PooledDataSource.fromConfig(poolConfig, unresponsive).use { datasource =>
      for
        start   <- Fx.monotonic
        result  <- datasource.getConnection.attempt
        elapsed <- Fx.monotonic.map(_ - start)
      yield
        assert(result.isLeft, s"no connection can pass validation, so acquisition must fail, but got $result")
        assert(
          elapsed < budget * 3,
          s"the caller waited $elapsed for a $budget budget; each discarded connection is costing a full " +
            s"validation outside the budget instead of inside it"
        )
    }
  }

  test("the pool hands its acquisition budget to the data source as an upper bound") {
    final class RecordingSource(val budget: Option[FiniteDuration]) extends ldbc.sql.DataSource[Fx]:
      override def getConnection: Fx[(Connection[Fx], Fx[Unit])] =
        Fx.raiseError(new RuntimeException("not used"))
      override def withConnectTimeout(timeout: FiniteDuration): ldbc.sql.DataSource[Fx] =
        new RecordingSource(Some(timeout))

    val source = new RecordingSource(None)
    val handed = source.withConnectTimeout(config(0, 1, 250.millis).connectionTimeout)
    assertEquals(
      handed.asInstanceOf[RecordingSource].budget,
      Some(250.millis),
      "the pool passes its own acquisition timeout down as the budget"
    )
    Fx.unit
  }

  test("a shorter connect timeout survives pooling and is used for every retry") {
    final class Unreachable(connectTimeout: FiniteDuration, attempts: EffectRef[Fx, Int])
      extends ldbc.sql.DataSource[Fx]:
      override def getConnection: Fx[(Connection[Fx], Fx[Unit])] =
        attempts.update(_ + 1) *>
          Fx.sleep(connectTimeout) *>
          Fx.raiseError(new RuntimeException(s"connect timed out after $connectTimeout"))
      override def withConnectTimeout(timeout: FiniteDuration): ldbc.sql.DataSource[Fx] =
        new Unreachable(connectTimeout.min(timeout), attempts)

    EffectRef.of[Fx, Int](0).flatMap { attempts =>
      PooledDataSource
        .fromDataSource(config(0, 1, 2.seconds), new Unreachable(300.millis, attempts))
        .use { datasource =>
          datasource.getConnection.attempt *> attempts.get.map { tried =>
            assert(
              tried >= 3,
              s"a 300ms connect timeout inside a 2s budget leaves room for several attempts, but only $tried ran; " +
                "the pool stretched the timeout instead of capping it"
            )
          }
        }
    }
  }

  test("startup fails when the initial connections cannot be established") {
    countingCreate(failUntil = Int.MaxValue).flatMap { create =>
      PooledDataSource
        .fromConfig(config(1, 2, 500.millis), create.resource)
        .use(_ => Fx.unit)
        .attempt
        .map { result =>
          assert(result.isLeft, s"a pool that cannot reach the database must not start quietly, but got $result")
        }
    }
  }
