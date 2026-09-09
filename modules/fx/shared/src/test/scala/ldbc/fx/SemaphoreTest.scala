/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.duration.*

import ldbc.fx.syntax.*

/**
 * Cross-platform (JVM / JS / Native) contract tests for [[Semaphore]] — a counting semaphore with
 * FIFO fairness. The suspend cases observe a `before`/`after` flag so they actually discriminate a
 * broken (non-blocking) semaphore: a waiter must NOT have acquired before the matching release. The
 * FIFO case uses two waiters and checks the serving order. Written as `Fx`-valued tests via
 * [[FxSuite]] so they run on the single-threaded JS event loop too.
 */
class SemaphoreTest extends FxSuite:

  private val boom = new RuntimeException("boom")

  test("acquire succeeds immediately when a permit is available") {
    assertFx(Semaphore(1).flatMap(s => s.acquire.map(_ => "ok")), "ok")
  }

  test("withPermit runs the effect and yields its result") {
    assertFx(Semaphore(1).flatMap(_.withPermit(Fx.pure(42))), 42)
  }

  test("withPermit returns the permit so a later acquire succeeds") {
    assertFx(
      for
        s <- Semaphore(1)
        a <- s.withPermit(Fx.pure(1))
        b <- s.withPermit(Fx.pure(2))
      yield (a, b),
      (1, 2)
    )
  }

  test("acquire with no permits does NOT proceed until a release (waiter observed still blocked)") {
    assertFx(
      for
        s        <- Semaphore(1)
        _        <- s.acquire    // take the only permit
        acquired <- Ref.of(false)
        fib      <- Fiber.start(s.acquire >> acquired.set(true))
        before   <- acquired.get // the waiter must still be blocked → false
        _        <- s.release    // now the waiter may proceed
        _        <- fib.joinWithNever
        after    <- acquired.get // now true
      yield (before, after),
      (false, true)
    )
  }

  test("permits are counted: an acquirer beyond the count stays blocked until a release") {
    assertFx(
      for
        s        <- Semaphore(2)
        _        <- s.acquire
        _        <- s.acquire    // permits now 0
        acquired <- Ref.of(false)
        fib      <- Fiber.start(s.acquire >> acquired.set(true))
        before   <- acquired.get // false: no permit available yet
        _        <- s.release
        _        <- fib.joinWithNever
        after    <- acquired.get
      yield (before, after),
      (false, true)
    )
  }

  test("two waiters are served in FIFO order") {
    assertFx(
      for
        s     <- Semaphore(1)
        _     <- s.acquire // hold the permit so both fibers must queue
        order <- Ref.of(List.empty[Int])
        f1    <- Fiber.start(s.acquire >> order.update(1 :: _) >> s.release)
        f2    <- Fiber.start(s.acquire >> order.update(2 :: _) >> s.release)
        // waiters are queued [f1, f2]; releasing hands the permit down the queue in order
        _   <- s.release
        _   <- f1.joinWithNever
        _   <- f2.joinWithNever
        ord <- order.get
      yield ord.reverse,
      List(1, 2)
    )
  }

  test("cancelling a queued acquirer does not leak the permit") {
    // A waiter cancelled while queued must be removed from the queue; otherwise `release` hands the
    // permit to the dead waiter and the next acquirer deadlocks. `timeout` turns a leak into a
    // failure instead of a hang.
    assertFx(
      for
        s <- Semaphore(1)
        _ <- s.acquire              // permit taken (permits = 0)
        f <- Fiber.start(s.acquire) // queues as a waiter, suspends
        _ <- f.cancel               // cancel the queued acquirer → it must deregister
        _ <- s.release              // makes a permit available again
        r <- Fx.timeout(s.acquire.map(_ => "ok"), 3.seconds)(new RuntimeException("permit leaked: acquire deadlocked"))
      yield r,
      "ok"
    )
  }

  test("withPermit releases the permit when the body is cancelled") {
    assertFx(
      for
        s <- Semaphore(1)
        f <- Fiber.start(s.withPermit(Fx.never[Unit])) // acquires, then blocks inside the body
        _ <- f.cancel                                  // bracket must release the permit on cancel
        r <- Fx.timeout(s.withPermit(Fx.pure("ok")), 3.seconds)(new RuntimeException("permit not released on cancel"))
      yield r,
      "ok"
    )
  }

  test("withPermit releases the permit even when the effect fails") {
    assertFx(
      for
        s <- Semaphore(1)
        _ <- s.withPermit(Fx.raiseError[Unit](boom)).handleErrorWith(_ => Fx.unit)
        r <- s.withPermit(Fx.pure(7)) // succeeds only if the permit was released on error
      yield r,
      7
    )
  }
