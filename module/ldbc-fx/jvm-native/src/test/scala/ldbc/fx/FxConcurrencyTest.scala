/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger, AtomicReference }

/**
 * Tests for behaviours that require real threads and blocking waits: cancel/complete races, the
 * blocking pool's thread identity, and cancellation-time resource release. Shared by the JVM and
 * Scala Native platforms (both provide real multithreading and `java.util.concurrent`); JS is
 * excluded as it is single-threaded. Platform-agnostic semantics are covered by `FxTest`.
 */
class FxConcurrencyTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 5000): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  test("cancel vs complete race: no double completion, no crash") {
    var maxCompletions = 0
    var crashed        = false
    var i              = 0
    while i < 5000 do
      val completed = new AtomicInteger(0)
      val fx = Fx.async[Int] { cb =>
        val t = new Thread(() => cb(Right(1)))
        t.start()
        Fx.Canceler.noop
      }
      try
        val canc = fx.unsafeRun(_ => completed.incrementAndGet())
        canc.cancel()
      catch { case _: Throwable => crashed = true }
      val c = completed.get()
      if c > maxCompletions then maxCompletions = c
      i += 1
    assert(!crashed)
    assert(maxCompletions <= 1, s"maxCompletions=$maxCompletions")
  }

  test("blocking runs on the blocking pool") {
    val threadName = new AtomicReference[String]("")
    val fx         = Fx.blocking { threadName.set(Thread.currentThread.getName); 7 }
    assertEquals(runSync(fx), Right(7))
    assert(threadName.get().startsWith("fx-blocking"), threadName.get())
  }

  test("bracket releases on cancel") {
    val released = new AtomicBoolean(false)
    val fx =
      Fx.bracket(Fx.pure("r"))(_ => Fx.async[Int](_ => Fx.Canceler.noop))(_ => Fx.delay { released.set(true); () })
    val canc = fx.unsafeRun(_ => ())
    Thread.sleep(30)
    canc.cancel()
    Thread.sleep(30)
    assert(released.get())
  }
