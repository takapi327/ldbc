/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Regression tests for review finding R2-3: like cats-effect and ZIO, a `bracket` release must
 * never run concurrently with the `use` code it protects. `cancel()` may not fire finalizers while
 * a sync region or a blocking thunk is still executing (a concurrent release could, for example,
 * free native TLS memory that an in-flight `s2n_recv` is still touching); release runs only once
 * the in-flight step has finished, and is still guaranteed to run.
 */
class FxCancelSemanticsTest extends munit.FunSuite:

  /**
   * Suspends until `gate` opens, then resumes on a helper thread. The gate is opened by the test
   * only after `unsafeRun` has returned, guaranteeing the continuation runs off the test thread.
   */
  private def hop(gate: CountDownLatch): Fx[Unit] = Fx.async { cb =>
    val t = new Thread(
      () =>
        gate.await(5, TimeUnit.SECONDS)
        cb(Right(()))
      ,
      "fx-hop"
    )
    t.setDaemon(true)
    t.start()
    Fx.Canceler.noop
  }

  test("cancel during a sync use region defers release until the region completes") {
    val runReturned    = new CountDownLatch(1)
    val useStarted     = new CountDownLatch(1)
    val cancelReturned = new CountDownLatch(1)
    val released       = new AtomicBoolean(false)
    val releasedLatch  = new CountDownLatch(1)
    val overlapped     = new AtomicBoolean(false)

    val program = Fx.bracket(Fx.pure("resource"))(_ =>
      hop(runReturned).flatMap(_ =>
        Fx.delay {
          useStarted.countDown()
          cancelReturned.await(5, TimeUnit.SECONDS)
          if released.get() then overlapped.set(true)
        }.flatMap(_ => Fx.async[Unit](_ => Fx.Canceler.noop))
      )
    )(_ => Fx.delay { released.set(true); releasedLatch.countDown(); () })

    val canceler = program.unsafeRun(_ => ())
    runReturned.countDown()
    assert(useStarted.await(5, TimeUnit.SECONDS), "use did not start")
    canceler.cancel()
    assert(!released.get(), "release must not run while the sync use region is still executing")
    cancelReturned.countDown()
    assert(releasedLatch.await(5, TimeUnit.SECONDS), "release must still run after the region completes")
    assert(!overlapped.get(), "release ran concurrently with the use region")
  }

  test("cancel during a blocking use defers release until the thunk finishes") {
    val runReturned     = new CountDownLatch(1)
    val blockingStarted = new CountDownLatch(1)
    val finishBlocking  = new CountDownLatch(1)
    val released        = new AtomicBoolean(false)
    val releasedLatch   = new CountDownLatch(1)
    val overlapped      = new AtomicBoolean(false)

    val program = Fx.bracket(Fx.pure("resource"))(_ =>
      hop(runReturned).flatMap(_ =>
        Fx.blocking {
          blockingStarted.countDown()
          finishBlocking.await(5, TimeUnit.SECONDS)
          if released.get() then overlapped.set(true)
        }
      )
    )(_ => Fx.delay { released.set(true); releasedLatch.countDown(); () })

    val canceler = program.unsafeRun(_ => ())
    runReturned.countDown()
    assert(blockingStarted.await(5, TimeUnit.SECONDS), "blocking thunk did not start")
    canceler.cancel()
    assert(!released.get(), "release must not run while the blocking thunk is still executing")
    finishBlocking.countDown()
    assert(releasedLatch.await(5, TimeUnit.SECONDS), "release must still run once the thunk finishes")
    assert(!overlapped.get(), "release ran concurrently with the blocking thunk")
  }
