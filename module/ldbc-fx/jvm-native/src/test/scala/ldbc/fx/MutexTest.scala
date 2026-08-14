/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

/**
 * Tests for [[Mutex]]: mutual exclusion under real-thread contention and FIFO-ish fairness. The
 * critical-section counter must never exceed 1 concurrent holder, and every contender must
 * eventually acquire (no lost wakeups / deadlock).
 */
class MutexTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 10000): A =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then throw new RuntimeException("timeout")
    ref.get().fold(throw _, identity)

  test("lock.surround gives mutual exclusion under concurrent contention") {
    val mutex   = runSync(Mutex.create)
    val inside  = new AtomicInteger(0)
    val maxSeen = new AtomicInteger(0)
    val errors  = new AtomicInteger(0)

    val threads = (1 to 16).map { _ =>
      new Thread(() =>
        var i = 0
        while i < 200 do
          try
            runSync(mutex.surround(Fx.delay {
              val n = inside.incrementAndGet()
              maxSeen.updateAndGet(m => if n > m then n else m)
              inside.decrementAndGet()
              ()
            }))
          catch { case _: Throwable => errors.incrementAndGet() }
          i += 1
      )
    }
    threads.foreach(_.start())
    threads.foreach(_.join())

    assertEquals(errors.get(), 0, "no surround should fail")
    assertEquals(maxSeen.get(), 1, s"observed ${maxSeen.get()} concurrent holders; mutex is not exclusive")
  }

  test("a second acquire waits until the first releases, then proceeds (no deadlock)") {
    val mutex     = runSync(Mutex.create)
    val order     = new AtomicReference[List[String]](Nil)
    val firstHeld = new CountDownLatch(1)
    val release   = new CountDownLatch(1)

    val holder = new Thread(() =>
      runSync(mutex.surround(Fx.delay {
        order.updateAndGet("first-in" :: _)
        firstHeld.countDown()
        release.await(5, TimeUnit.SECONDS)
        order.updateAndGet("first-out" :: _)
        ()
      }))
    )
    holder.setDaemon(true)
    holder.start()
    assert(firstHeld.await(5, TimeUnit.SECONDS), "first holder did not enter")

    val contender = new Thread(() =>
      runSync(mutex.surround(Fx.delay { order.updateAndGet("second-in" :: _); () }))
    )
    contender.setDaemon(true)
    contender.start()

    Thread.sleep(100)
    assert(!order.get().contains("second-in"), "second acquire must not enter while the first holds the lock")
    release.countDown()
    holder.join(5000)
    contender.join(5000)
    assertEquals(order.get().reverse, List("first-in", "first-out", "second-in"))
  }
