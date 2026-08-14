/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.duration.*

import ldbc.fx.syntax.*

/**
 * Cross-platform (JVM / JS / Native) tests for [[Fiber]]: `join` reports the outcome, `joinWithNever`
 * re-raises errors, and — the behaviour that distinguishes it from fire-and-forget — `cancel` settles
 * the fiber as [[Outcome.Canceled]] so a later `join` returns promptly instead of hanging. Written as
 * `Fx`-valued tests via [[FxSuite]] so they run on the single-threaded JS event loop too.
 */
class FiberTest extends FxSuite:

  private val boom = new RuntimeException("boom")

  test("join returns Succeeded with the value"):
    assertFx(Fiber.start(Fx.sleep(50.millis) >> Fx.pure(42)).flatMap(_.join), Outcome.Succeeded(42))

  test("join returns Errored for a failing fiber"):
    Fiber.start(Fx.sleep(50.millis) >> Fx.raiseError[Int](boom)).flatMap(_.join).map {
      case Outcome.Errored(e) => assertEquals(e.getMessage, "boom")
      case other              => fail(s"expected Errored, got $other")
    }

  test("joinWithNever re-raises the fiber's error"):
    interceptFx[RuntimeException](Fiber.start(Fx.raiseError[Int](boom)).flatMap(_.joinWithNever))
      .map(e => assertEquals(e.getMessage, "boom"))

  test("cancel settles the fiber as Canceled and join does not hang"):
    // The fiber body sleeps 10s; cancel must settle the outcome so `join` returns without waiting.
    assertFx(
      for
        fiber   <- Fiber.start(Fx.sleep(10.seconds) >> Fx.pure(1))
        _       <- Fx.sleep(50.millis)
        _       <- fiber.cancel
        outcome <- fiber.join
      yield outcome,
      Outcome.Canceled
    )

  test("cancel after completion keeps the successful outcome"):
    assertFx(
      for
        fiber   <- Fiber.start(Fx.pure(7))
        _       <- Fx.sleep(50.millis)
        _       <- fiber.cancel
        outcome <- fiber.join
      yield outcome,
      Outcome.Succeeded(7)
    )
