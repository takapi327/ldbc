/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.AtomicReference

import ldbc.fx.syntax.*

/**
 * Real-thread contention test for [[Ref]] (JVM / Native only — JS is single-threaded and cannot
 * create genuine contention). Many threads hammer the same `Ref` with `update`; a lock-free CAS loop
 * must lose no updates, so the final count equals the total number of increments. A naive
 * read-then-write (non-atomic) update would drop updates under contention and fail this.
 */
class RefContentionTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 30000): A =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then throw new RuntimeException("timeout")
    ref.get().fold(throw _, identity)

  test("update loses no increments under real-thread contention") {
    val threadCount = 8
    val perThread   = 2000
    val ref         = runSync(Ref.of(0))
    val increments: Fx[Unit] =
      (1 to perThread).foldLeft(Fx.unit)((acc, _) => acc >> ref.update(_ + 1))

    val threads = (1 to threadCount).map(_ => new Thread(() => runSync(increments)))
    threads.foreach(_.start())
    threads.foreach(_.join())

    assertEquals(runSync(ref.get), threadCount * perThread)
  }
