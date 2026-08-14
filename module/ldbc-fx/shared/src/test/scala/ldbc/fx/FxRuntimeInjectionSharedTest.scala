/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.*

/**
 * Cross-platform (incl. Scala.js) verification of runtime injection, written Future-first (no
 * threads, no blocking latch) so it runs on the single-threaded JS event loop. The adversarial focus
 * is JS-specific: `PlatformFxLocal` is a plain `var`, and JS resume points cross `setTimeout`
 * macrotask boundaries (`executeCompute = setTimeout(0)`, `scheduleOnce = setTimeout`). This checks
 * that `withRuntime` re-establishes the injected runtime on the far side of those boundaries — which
 * it must, because every resume re-wraps `loop()` in `withRuntime(rt)` with a closure-captured `rt`.
 */
class FxRuntimeInjectionSharedTest extends munit.FunSuite:

  import scala.concurrent.ExecutionContext.Implicits.global

  /** Counts how often the runtime is asked to compute / schedule; delegates to the global default so
   *  the work actually happens (on JS that means `setTimeout`). No threads — safe on Scala.js. */
  private final class CountingRuntime extends FxRuntime:
    val computeCount  = new AtomicInteger(0)
    val scheduleCount = new AtomicInteger(0)
    private val g     = FxRuntime.global

    override def executeCompute(task: () => Unit): Unit =
      computeCount.incrementAndGet()
      g.executeCompute(task)

    override def executeBlocking(task: () => Unit): Unit            = g.executeBlocking(task)
    override def executeInterruptible(task: () => Unit): Fx.Canceler = g.executeInterruptible(task)

    override def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
      scheduleCount.incrementAndGet()
      g.scheduleOnce(delayNanos, task)

  private def runFut[A](fx: Fx[A], rt: FxRuntime): Future[A] =
    val p = Promise[A]()
    fx.unsafeRun {
      case Right(a) => p.success(a)
      case Left(e)  => p.failure(e)
    }(using rt)
    p.future

  test("auto-cede re-establishes the injected runtime across the macrotask boundary") {
    val rt         = new CountingRuntime
    val endCurrent = new AtomicReference[FxRuntime](null)
    var fx: Fx[Int] = Fx.pure(0)
    for _ <- 0 until 1500 do fx = fx.map(_ + 1)
    runFut(fx.map { n => endCurrent.set(FxRuntime.current); n }, rt).map { n =>
      assertEquals(n, 1500)
      assert(rt.computeCount.get() >= 1, s"auto-cede must hit injected executeCompute, got ${rt.computeCount.get()}")
      assert(endCurrent.get() eq rt, "current after auto-cede (across setTimeout) must be the injected runtime")
    }
  }

  test("current survives an async (scheduleOnce/setTimeout) boundary, and a nested unsafeRun inherits it") {
    val rt         = new CountingRuntime
    val afterSleep = new AtomicReference[FxRuntime](null)
    val nested     = new AtomicReference[FxRuntime](null)
    val fx = Fx
      .sleep(5.millis)
      .flatMap(_ => Fx.delay(afterSleep.set(FxRuntime.current)))
      .flatMap(_ => Fx.delay(Fx.delay(nested.set(FxRuntime.current)).unsafeRun(_ => ())))
    runFut(fx, rt).map { _ =>
      assert(rt.scheduleCount.get() >= 1, "sleep must schedule on the injected runtime")
      assert(afterSleep.get() eq rt, "current after the async boundary must be the injected runtime")
      assert(nested.get() eq rt, "nested unsafeRun after the boundary must inherit the injected runtime")
    }
  }

  test("immediate (synchronous) async completion resumes inline, without an injected executeCompute hop") {
    val rt = new CountingRuntime
    val fx = Fx.async[Int] { cb => cb(Right(7)); Fx.Canceler.noop }.map(_ + 1)
    runFut(fx, rt).map { n =>
      assertEquals(n, 8)
      assertEquals(rt.computeCount.get(), 0, "inline async resume must not hop through the injected compute pool")
    }
  }

  test("the injected runtime does not leak past a run that crossed a setTimeout boundary (finally restores)") {
    val rt = new CountingRuntime
    // Crosses a setTimeout boundary (sleep), so `withRuntime`'s finally must restore across it.
    runFut(Fx.sleep(3.millis).map(_ => 1), rt).map { _ =>
      assert(
        FxRuntime.current eq FxRuntime.global,
        "after an injected run completes, current must be restored to the platform default, not leak the injected runtime"
      )
    }
  }
