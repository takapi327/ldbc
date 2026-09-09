/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, TimeUnit }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

import scala.concurrent.duration.*

import ldbc.fx.syntax.*

/**
 * Tests for the concurrent syntax ([[Semaphore]], `parTraverse`, `parTraverseN`, `parTupled`): results
 * are collected in order, `parTraverseN` and the semaphore genuinely bound concurrency, and a permit is
 * returned even when the guarded effect fails.
 */
class ParallelSyntaxTest extends munit.FunSuite:

  private def runSync[A](fx: Fx[A], timeoutMs: Long = 10000): Either[Throwable, A] =
    val latch = new CountDownLatch(1)
    val ref   = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => ref.set(r); latch.countDown() }
    if !latch.await(timeoutMs, TimeUnit.MILLISECONDS) then Left(new RuntimeException("timeout")) else ref.get()

  /**
   * An effect that tracks the peak number of simultaneous runs in `peak`. The decrement runs via
   * `guarantee`, so a failing `body` still records its exit and does not leak the live count.
   */
  private def tracked(current: AtomicInteger, peak: AtomicInteger)(body: Fx[Unit]): Fx[Unit] =
    Fx.delay {
      val now = current.incrementAndGet()
      peak.updateAndGet(p => math.max(p, now))
      ()
    } >> body.guarantee(Fx.delay { current.decrementAndGet(); () })

  test("parTraverse collects results in order") {
    val out = runSync((1 to 5).toList.parTraverse(i => Fx.sleep((20 * (6 - i)).millis) >> Fx.pure(i * i)))
    assertEquals(out, Right(List(1, 4, 9, 16, 25)))
  }

  test("parTraverse runs elements concurrently") {
    val current = new AtomicInteger(0)
    val peak    = new AtomicInteger(0)
    val out     = runSync((1 to 5).toList.parTraverse(_ => tracked(current, peak)(Fx.sleep(100.millis))))
    assertEquals(out.map(_.size), Right(5))
    assert(peak.get() >= 2, s"expected concurrent execution, peak=${ peak.get() }")
  }

  test("parTraverseN bounds concurrency to n") {
    val current = new AtomicInteger(0)
    val peak    = new AtomicInteger(0)
    val out     = runSync((1 to 8).toList.parTraverseN(2)(_ => tracked(current, peak)(Fx.sleep(80.millis))))
    assertEquals(out.map(_.size), Right(8))
    assert(peak.get() <= 2, s"concurrency should be capped at 2, but peaked at ${ peak.get() }")
  }

  test("parTraverseN still preserves result order") {
    val out = runSync((1 to 6).toList.parTraverseN(3)(i => Fx.pure(i * 10)))
    assertEquals(out, Right(List(10, 20, 30, 40, 50, 60)))
  }

  test("parTupled runs both effects and pairs their results") {
    val out = runSync((Fx.sleep(60.millis) >> Fx.pure("a"), Fx.sleep(30.millis) >> Fx.pure(1)).parTupled)
    assertEquals(out, Right(("a", 1)))
  }

  test("Semaphore bounds concurrency and releases on error") {
    val current = new AtomicInteger(0)
    val peak    = new AtomicInteger(0)
    val program = for
      sem <- Semaphore(2)
      _   <- (1 to 6).toList.parTraverse { i =>
             sem
               .withPermit(tracked(current, peak)(Fx.sleep(60.millis) >> (if i == 3 then
                                                                            Fx.raiseError(new RuntimeException("x"))
                                                                          else Fx.unit)))
               .attempt
               .void
           }
      // If permits were leaked on the failing task, this final acquire would block forever.
      _ <- sem.acquire
      _ <- sem.acquire
    yield ()
    assertEquals(runSync(program).isRight, true)
    assert(peak.get() <= 2, s"concurrency should be capped at 2, but peaked at ${ peak.get() }")
  }
