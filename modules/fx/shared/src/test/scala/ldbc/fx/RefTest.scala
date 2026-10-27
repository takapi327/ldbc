/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import ldbc.fx.syntax.*

/**
 * Cross-platform (JVM / JS / Native) contract tests for [[Ref]]. Written as `Fx`-valued tests via
 * [[FxSuite]] so they run on the single-threaded JS event loop as well.
 */
class RefTest extends FxSuite:

  test("of then get returns the initial value") {
    assertFx(Ref.of(1).flatMap(_.get), 1)
  }

  test("set replaces the value") {
    assertFx(Ref.of(1).flatMap(r => r.set(2) >> r.get), 2)
  }

  test("update applies the function") {
    assertFx(Ref.of(10).flatMap(r => r.update(_ + 5) >> r.get), 15)
  }

  test("modify returns the derived result and stores the new value") {
    assertFx(
      for
        r <- Ref.of(10)
        b <- r.modify(a => (a + 1, a * 2))
        v <- r.get
      yield (b, v),
      (20, 11)
    )
  }

  test("getAndUpdate returns the previous value") {
    assertFx(
      for
        r   <- Ref.of(5)
        old <- r.getAndUpdate(_ + 1)
        v   <- r.get
      yield (old, v),
      (5, 6)
    )
  }

  test("updateAndGet returns the updated value") {
    assertFx(Ref.of(5).flatMap(_.updateAndGet(_ + 1)), 6)
  }

  test("getAndSet returns the previous value and stores the new one") {
    assertFx(
      for
        r   <- Ref.of(5)
        old <- r.getAndSet(9)
        v   <- r.get
      yield (old, v),
      (5, 9)
    )
  }

  test("unsafe creates a ref usable as a plain value") {
    val r = Ref.unsafe(0)
    assertFx(r.update(_ + 3) >> r.get, 3)
  }

  test("many sequential updates accumulate") {
    // Sequential only: `Fiber.start` runs a synchronous body to completion inline, so fibers here
    // would not actually interleave. Real atomicity-under-contention is covered by the real-thread
    // `RefContentionTest` (jvm-native), which JS cannot express.
    assertFx(
      for
        r <- Ref.of(0)
        _ <- (1 to 100).toList.foldLeft(Fx.unit)((acc, _) => acc >> r.update(_ + 1))
        v <- r.get
      yield v,
      100
    )
  }
