/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests for the completion-aware cancel path added for the frontend bridges: [[Fx.unsafeRunCancelable]]
 * returns a [[Fx.CancelToken]] whose `cancel: Fx[Unit]` completes only after the run's cancel-path
 * finalizers have actually drained (課題A), and cancelling a masked `bracket` acquire must not
 * interrupt it (課題B / Fix B).
 */
class FxCancelTokenTest extends munit.FunSuite:

  private val timeout = 5

  test("CancelToken.cancel completes only after an async release has finished draining") {
    val useSuspended = new CountDownLatch(1)
    val releaseRan   = new AtomicBoolean(false)

    /** A release that only settles asynchronously on a helper thread after a short delay. */
    val asyncRelease: Fx[Unit] = Fx.async { cb =>
      val t = new Thread(
        () =>
          Thread.sleep(50)
          releaseRan.set(true)
          cb(Right(()))
        ,
        "fx-async-release"
      )
      t.setDaemon(true)
      t.start()
      Fx.Canceler.noop
    }

    val program = Fx.bracket(Fx.pure("resource"))(_ =>
      Fx.async[Unit] { _ =>
        useSuspended.countDown()
        Fx.Canceler.noop
      }
    )(_ => asyncRelease)

    val token = program.unsafeRunCancelable(_ => ())
    assert(useSuspended.await(timeout.toLong, TimeUnit.SECONDS), "use did not suspend")

    val cancelCompleted = new CountDownLatch(1)
    token.cancel.unsafeRun(_ => cancelCompleted.countDown())

    assert(cancelCompleted.await(timeout.toLong, TimeUnit.SECONDS), "token.cancel did not complete (hung)")
    assert(releaseRan.get(), "token.cancel completed before the async release finished (no backpressure)")
  }

  test("CancelToken.cancel after normal completion completes immediately (no hang)") {
    val finished = new CountDownLatch(1)
    val token    = Fx.pure(42).unsafeRunCancelable(_ => finished.countDown())
    assert(finished.await(timeout.toLong, TimeUnit.SECONDS), "program did not finish")

    val cancelCompleted = new CountDownLatch(1)
    token.cancel.unsafeRun(_ => cancelCompleted.countDown())
    assert(cancelCompleted.await(timeout.toLong, TimeUnit.SECONDS), "cancel after completion must not hang")
  }

  test("cancel does not interrupt a masked bracket acquire (Fix B)") {
    val acquireSuspended     = new CountDownLatch(1)
    val resumeAcquire        = new CountDownLatch(1)
    val acquireCancelerFired = new AtomicBoolean(false)

    val maskedAcquire: Fx[String] = Fx.async { cb =>
      val t = new Thread(
        () =>
          acquireSuspended.countDown()
          resumeAcquire.await(timeout.toLong, TimeUnit.SECONDS)
          cb(Right("resource"))
        ,
        "fx-masked-acquire"
      )
      t.setDaemon(true)
      t.start()
      new Fx.Canceler:
        override def cancel(): Unit = acquireCancelerFired.set(true)
    }

    val program = Fx.bracket(maskedAcquire)(_ => Fx.async[Unit](_ => Fx.Canceler.noop))(_ => Fx.unit)

    val token = program.unsafeRunCancelable(_ => ())
    assert(acquireSuspended.await(timeout.toLong, TimeUnit.SECONDS), "acquire did not suspend")

    token.cancel.unsafeRun(_ => ())
    Thread.sleep(100)
    assert(!acquireCancelerFired.get(), "masked acquire canceler must NOT fire on cancel (uncancelable contract)")

    resumeAcquire.countDown()
  }
