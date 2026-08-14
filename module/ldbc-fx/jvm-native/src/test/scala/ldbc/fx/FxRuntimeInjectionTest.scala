/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ CountDownLatch, Executors, ThreadFactory, TimeUnit }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicReference }

import scala.concurrent.duration.*

/**
 * Verifies runtime injection ([[FxRuntime]] threaded through [[Fx.unsafeRun]]) and the hybrid resume
 * routing decided by the Gate-1 / Gate-2 measurements:
 *   - auto-ceded continuations resume on the injected runtime (so a host can place them on its own
 *     compute pool),
 *   - `Async` completions resume INLINE on the completing thread (no hop through the injected
 *     runtime — the synchronization-heavy path must stay cheap),
 *   - `FxRuntime.current` is the injected runtime during interpretation, and a nested `unsafeRun`
 *     started then inherits it.
 */
class FxRuntimeInjectionTest extends munit.FunSuite:

  private def named(name: String): ThreadFactory = (r: Runnable) =>
    val t = new Thread(r, name)
    t.setDaemon(true)
    t

  /** A [[FxRuntime]] that runs compute continuations on a single, distinctly-named thread and counts
   *  how often it is asked to. Blocking / scheduler / interruptible delegate to the global default. */
  private final class RecordingRuntime extends FxRuntime:
    val computeCount                      = new AtomicInteger(0)
    val scheduleCount                     = new AtomicInteger(0)
    val lastComputeThread                 = new AtomicReference[Thread](null)
    private val exec                      = Executors.newSingleThreadExecutor(named("INJECTED-COMPUTE"))
    private val g                         = FxRuntime.global

    override def executeCompute(task: () => Unit): Unit =
      computeCount.incrementAndGet()
      exec.execute(() => { lastComputeThread.set(Thread.currentThread); task() })

    override def executeBlocking(task: () => Unit): Unit             = g.executeBlocking(task)
    override def executeInterruptible(task: () => Unit): Fx.Canceler = g.executeInterruptible(task)

    override def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
      scheduleCount.incrementAndGet()
      g.scheduleOnce(delayNanos, task)

  private def runWith[A](fx: Fx[A], rt: FxRuntime): Either[Throwable, A] =
    val latch  = new CountDownLatch(1)
    val holder = new AtomicReference[Either[Throwable, A]](null)
    fx.unsafeRun { r => holder.set(r); latch.countDown() }(using rt)
    assert(latch.await(10, TimeUnit.SECONDS), "run did not complete within 10s")
    holder.get()

  test("auto-ceded continuation resumes on the injected runtime") {
    val rt = new RecordingRuntime
    val endThread = new AtomicReference[Thread](null)
    // A long chain of pure steps exceeds the auto-cede threshold (1024), forcing a re-schedule.
    var fx: Fx[Int] = Fx.pure(0)
    for _ <- 0 until 1500 do fx = fx.map(_ + 1)
    val result = runWith(fx.map { n => endThread.set(Thread.currentThread); n }, rt)

    assertEquals(result, Right(1500))
    assert(rt.computeCount.get() >= 1, s"auto-cede should hit injected executeCompute, got ${rt.computeCount.get()}")
    assert(
      endThread.get() eq rt.lastComputeThread.get(),
      "post-auto-cede continuation must run on the injected runtime's thread"
    )
  }

  test("Async completion resumes inline, not through the injected runtime") {
    val rt              = new RecordingRuntime
    val completer       = Executors.newSingleThreadExecutor(named("COMPLETER"))
    val completerThread = new AtomicReference[Thread](null)
    val resumeThread    = new AtomicReference[Thread](null)
    val eff = Fx
      .async[Int] { cb => completer.execute(() => { completerThread.set(Thread.currentThread); cb(Right(1)) }); Fx.Canceler.noop }
      .map { n => resumeThread.set(Thread.currentThread); n }

    val result = runWith(eff, rt)

    assertEquals(result, Right(1))
    assert(resumeThread.get() eq completerThread.get(), "async resume must be inline on the completing thread")
    assertEquals(rt.computeCount.get(), 0, "async resume must not hop through the injected compute pool")
    completer.shutdownNow()
  }

  test("FxRuntime.current is the injected runtime, and a nested unsafeRun inherits it") {
    val rt     = new RecordingRuntime
    val outer  = new AtomicReference[FxRuntime](null)
    val nested = new AtomicReference[FxRuntime](null)
    val eff = Fx
      .delay(outer.set(FxRuntime.current))
      .flatMap(_ => Fx.delay(Fx.delay(nested.set(FxRuntime.current)).unsafeRun(_ => ())))

    runWith(eff, rt)

    assert(outer.get() eq rt, "current runtime during interpretation must be the injected one")
    assert(nested.get() eq rt, "nested unsafeRun started during interpretation must inherit the injected runtime")
  }

  test("bracket finalizer runs under the captured injected runtime on an external-thread cancel (§4-B)") {
    val rt             = new RecordingRuntime
    val releaseRuntime = new AtomicReference[FxRuntime](null)
    val releaseDone    = new CountDownLatch(1)
    // use = `never`, so `unsafeRun` returns only after the run has suspended (executing = false);
    // cancelling then drains finalizers on THIS (external) thread, where the ThreadLocal is empty.
    // Only §4-B's explicit `(using rt)` capture makes the release see the injected runtime.
    val program: Fx[Unit] =
      Fx.bracket(Fx.delay(()))(_ => Fx.never[Unit]) { _ =>
        Fx.delay { releaseRuntime.set(FxRuntime.current); releaseDone.countDown() }
      }

    val canceler = program.unsafeRun(_ => ())(using rt)
    canceler.cancel()

    assert(releaseDone.await(10, TimeUnit.SECONDS), "release did not run on cancel")
    assert(releaseRuntime.get() eq rt, "release must run under the captured injected runtime")
    assert(releaseRuntime.get() ne FxRuntime.global, "release must not fall back to the global runtime")
  }

  test("timeout's nested runs schedule on the injected runtime (§4-A propagation)") {
    val rt        = new RecordingRuntime
    val completer = Executors.newSingleThreadExecutor(named("TIMEOUT-COMPLETER"))
    // `fa` completes asynchronously after a short delay, so the timeout's internal `sleep` timer is
    // actually scheduled — via `FxRuntime.current.scheduleOnce`, which must be the injected runtime.
    val fa = Fx.async[Int] { cb =>
      completer.execute(() => { Thread.sleep(50); cb(Right(42)) })
      Fx.Canceler.noop
    }
    val result = runWith(Fx.timeout(fa, 5.seconds)(new RuntimeException("timeout")), rt)

    assertEquals(result, Right(42))
    assert(rt.scheduleCount.get() >= 1, "timeout's inner sleep must schedule on the injected runtime")
    completer.shutdownNow()
  }
