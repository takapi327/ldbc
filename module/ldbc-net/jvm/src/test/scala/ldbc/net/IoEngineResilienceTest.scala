/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.net

import java.nio.channels.Selector
import java.util.concurrent.{ CountDownLatch, TimeUnit }

/**
 * Regression test for review finding B2: an exception thrown while the single selector thread
 * processes a queued task (or a key) must not kill the thread — otherwise every connection driven by
 * that engine hangs forever. The test enqueues a throwing task, then a signalling task, and asserts
 * the second still runs (i.e. the loop survived the first).
 */
class IoEngineResilienceTest extends munit.FunSuite:

  test("selector thread survives an exception thrown by a queued task"):
    val engine = new SelectorEngine(Selector.open())
    val thread = new Thread(() => engine.loop(), "test-io-engine")
    thread.setDaemon(true)
    thread.start()

    engine.enqueue(() => throw new RuntimeException("boom"))

    val survived = new CountDownLatch(1)
    engine.enqueue(() => survived.countDown())

    assert(survived.await(3, TimeUnit.SECONDS), "selector thread died on a task exception")
