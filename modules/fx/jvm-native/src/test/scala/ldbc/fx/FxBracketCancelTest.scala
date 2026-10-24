/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

/**
 * Regression test for review finding B3: a resource whose async `acquire` completes right as the
 * run is cancelled must still be released. Without an uncancelable acquire, the acquire's completion
 * is dropped on cancel and `bracket` never registers/runs `release`, leaking the resource.
 */
class FxBracketCancelTest extends munit.FunSuite:

  test("bracket releases a resource acquired concurrently with cancellation") {
    val released      = new AtomicBoolean(false)
    val releasedLatch = new CountDownLatch(1)
    val fireAcquire   = new CountDownLatch(1)
    val runReturned   = new CountDownLatch(1)
    val outer         = new AtomicReference[Fx.Canceler](Fx.Canceler.noop)

    /** acquire: suspends, then produces the resource only once signalled. */
    val acquire: Fx[String] = Fx.async { cb =>
      val b = new Thread(
        () =>
          fireAcquire.await(5, TimeUnit.SECONDS)
          cb(Right("resource"))
        ,
        "fx-acquire-completer"
      )
      b.setDaemon(true)
      b.start()
      Fx.Canceler.noop
    }

    val program: Fx[Unit] =
      Fx.bracket(acquire)(_ => Fx.async[Unit](_ => Fx.Canceler.noop))(_ =>
        Fx.delay { released.set(true); releasedLatch.countDown(); () }
      )

    val runner = new Thread(
      () =>
        val c = program.unsafeRun(_ => ())
        outer.set(c)
        runReturned.countDown()
      ,
      "fx-runner"
    )
    runner.setDaemon(true)
    runner.start()

    assert(runReturned.await(5, TimeUnit.SECONDS), "run did not suspend/return")
    outer.get().cancel()
    fireAcquire.countDown()

    assert(releasedLatch.await(5, TimeUnit.SECONDS), "release must run for a resource acquired during cancel")
    assert(released.get())
  }
