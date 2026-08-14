/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

/**
 * Cross-platform (JVM / JS / Native) contract tests for [[Deferred]] — a write-once, read-many
 * async cell. The suspend/resume cases exercise the `Async` suspend + completion-callback resume
 * path (the one affected by runtime injection): a getter suspends via a fiber and is woken by
 * `complete`. Written as `Fx`-valued tests via [[FxSuite]] so they run on JS too.
 */
class DeferredTest extends FxSuite:

  test("get on an already-completed deferred returns the value immediately") {
    assertFx(
      for
        d <- Deferred[Int]
        _ <- d.complete(42)
        v <- d.get
      yield v,
      42
    )
  }

  test("tryGet is None before completion and Some after") {
    assertFx(
      for
        d      <- Deferred[Int]
        before <- d.tryGet
        _      <- d.complete(7)
        after  <- d.tryGet
      yield (before, after),
      (None, Some(7))
    )
  }

  test("complete is write-once: first returns true, later returns false, value is the first") {
    assertFx(
      for
        d  <- Deferred[Int]
        f1 <- d.complete(1)
        f2 <- d.complete(2)
        v  <- d.get
      yield (f1, f2, v),
      (true, false, 1)
    )
  }

  test("a suspended getter is woken by a later complete") {
    assertFx(
      for
        d   <- Deferred[Int]
        fib <- Fiber.start(d.get)
        _   <- d.complete(99)
        r   <- fib.joinWithNever
      yield r,
      99
    )
  }

  test("cancelling a getter removes its waiter; a later complete still succeeds and is observable") {
    assertFx(
      for
        d  <- Deferred[Int]
        f  <- Fiber.start(d.get) // registers a waiter, then suspends
        _  <- f.cancel           // must remove the waiter callback
        ok <- d.complete(7)      // still completes (first completion wins)
        v  <- d.get              // a fresh getter observes the value
      yield (ok, v),
      (true, 7)
    )
  }

  test("all pending getters are woken by a single complete") {
    assertFx(
      for
        d  <- Deferred[Int]
        f1 <- Fiber.start(d.get)
        f2 <- Fiber.start(d.get)
        f3 <- Fiber.start(d.get)
        _  <- d.complete(5)
        r1 <- f1.joinWithNever
        r2 <- f2.joinWithNever
        r3 <- f3.joinWithNever
      yield (r1, r2, r3),
      (5, 5, 5)
    )
  }
