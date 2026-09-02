/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }

/**
 * Test for the opt-in [[Fx.interruptible]] combinator (review finding M3): unlike `blocking`,
 * cancelling an `interruptible` effect interrupts the executing thread, so a thunk that honours
 * interruption aborts early instead of running to completion.
 */
class FxInterruptibleTest extends munit.FunSuite:

  test("cancelling an interruptible effect interrupts its blocking thunk") {
    val started     = new CountDownLatch(1)
    val interrupted = new CountDownLatch(1)

    val fx = Fx.interruptible {
      started.countDown()
      try Thread.sleep(10000)
      catch { case _: InterruptedException => interrupted.countDown() }
    }

    val canceler = fx.unsafeRun(_ => ())
    assert(started.await(5, TimeUnit.SECONDS), "the thunk did not start")
    canceler.cancel()
    assert(interrupted.await(5, TimeUnit.SECONDS), "cancel must interrupt the blocking thunk")
  }
