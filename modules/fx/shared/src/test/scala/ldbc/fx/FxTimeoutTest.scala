/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.duration.*

import ldbc.fx.syntax.*

/**
 * Cross-platform (JVM / JS / Native) tests for [[Fx.timeout]]: a fast effect returns its value and
 * the timer loses; a slow effect fails with the supplied error once the deadline fires (driven by the
 * platform scheduler — `setTimeout` on JS); and the loser of the race is cancelled rather than left
 * running (observed via `onCancel`). Written as `Fx`-valued tests via [[FxSuite]] so they run on the
 * single-threaded JS event loop too.
 */
class FxTimeoutTest extends FxSuite:

  test("a fast effect completes with its value before the deadline fires") {
    assertFx(Fx.timeout(Fx.sleep(50.millis) >> Fx.pure(42), 2.seconds)(new RuntimeException("late")), 42)
  }

  test("a slow effect fails with the supplied timeout error once the deadline fires") {
    interceptFx[RuntimeException](
      Fx.timeout(Fx.sleep(2.seconds) >> Fx.pure(1), 50.millis)(new RuntimeException("timed-out"))
    )
      .map(e => assertEquals(e.getMessage, "timed-out"))
  }

  test("the losing effect is cancelled when the deadline wins") {
    assertFx(
      Ref.of(false).flatMap { cancelled =>
        val slow = Fx.sleep(2.seconds).onCancel(cancelled.set(true))
        Fx.timeout(slow, 50.millis)(new RuntimeException("timed-out"))
          .handleErrorWith(_ => Fx.unit)
          .flatMap(_ => cancelled.get)
      },
      true
    )
  }
