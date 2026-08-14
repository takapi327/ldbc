/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

/**
 * Deterministic regression test for the `currentCanceler` overwrite race (review finding B1): when a
 * fast async completes on another thread and the resumed continuation registers a NEW canceler, the
 * original registering thread must not overwrite it with its now-stale canceler — otherwise a later
 * `cancel()` fails to reach the in-flight operation.
 *
 * The race window (between publishing the suspension and recording the canceler) is opened
 * deterministically via the `private[fx]` `Fx.suspendHook` seam, gated to this test's runner thread.
 */
class FxCancelRaceTest extends munit.FunSuite:

  test("cancel reaches the in-flight async, not a stale one") {
    val c1Cancelled = new AtomicBoolean(false)
    val c2Cancelled = new AtomicBoolean(false)
    val canceler1   = new Fx.Canceler { override def cancel(): Unit = c1Cancelled.set(true) }
    val canceler2   = new Fx.Canceler { override def cancel(): Unit = c2Cancelled.set(true) }

    val fireCb1     = new CountDownLatch(1)
    val bResumed    = new CountDownLatch(1)
    val runReturned = new CountDownLatch(1)
    val outer       = new AtomicReference[Fx.Canceler](Fx.Canceler.noop)
    val runnerRef   = new AtomicReference[Thread](null)
    val firstHook   = new AtomicBoolean(false)

    /** async #1: completes from thread B only once the race window is open. */
    val async1: Fx[Unit] = Fx.async { cb1 =>
      val b = new Thread(
        () =>
          fireCb1.await(5, TimeUnit.SECONDS)
          cb1(Right(()))
          bResumed.countDown()
        ,
        "fx-b1-firer"
      )
      b.setDaemon(true)
      b.start()
      canceler1
    }

    /** async #2: never completes; canceler2 is the in-flight operation cancel() must reach. */
    val async2: Fx[Unit] = Fx.async(_ => canceler2)

    val program = async1.flatMap(_ => async2)

    Fx.suspendHook = () =>
      if (Thread.currentThread eq runnerRef.get()) && firstHook.compareAndSet(false, true) then
        fireCb1.countDown()
        bResumed.await(5, TimeUnit.SECONDS)
        ()

    try
      val runner = new Thread(
        () =>
          val c = program.unsafeRun(_ => ())
          outer.set(c)
          runReturned.countDown()
        ,
        "fx-runner"
      )
      runnerRef.set(runner)
      runner.setDaemon(true)
      runner.start()

      assert(runReturned.await(5, TimeUnit.SECONDS), "run did not return in time")
      outer.get().cancel()

      assert(c2Cancelled.get(), "cancel() must reach the in-flight async (canceler2)")
      assert(!c1Cancelled.get(), "the already-resumed async's canceler must not be cancelled")
    finally Fx.suspendHook = () => ()
  }
