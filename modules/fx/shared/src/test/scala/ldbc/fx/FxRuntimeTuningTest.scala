/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.{ Duration, DurationInt, FiniteDuration }
import scala.concurrent.Promise

/**
 * The run loop's two tuning knobs — the auto-cede threshold and the cancel-path finalizer timeout —
 * belong to the [[FxRuntime]] a program runs on, not to the process.
 *
 * Written cross-platform (no threads, no blocking) so it also runs on the single-threaded JS event loop.
 */
class FxRuntimeTuningTest extends munit.FunSuite:

  import scala.concurrent.ExecutionContext.Implicits.global

  /** A runtime that delegates to the platform default while overriding the tuning knobs. */
  private final class TunedRuntime(
    override val autoCedeThreshold: Int,
    override val finalizerTimeout:  FiniteDuration
  ) extends FxRuntime:
    val computeCount: AtomicInteger = new AtomicInteger(0)
    private val delegate = FxRuntime.global

    override def executeCompute(task: () => Unit): Unit =
      computeCount.incrementAndGet()
      delegate.executeCompute(task)

    override def executeBlocking(task:      () => Unit):           Unit        = delegate.executeBlocking(task)
    override def executeInterruptible(task: () => Unit):           Fx.Canceler = delegate.executeInterruptible(task)
    override def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
      delegate.scheduleOnce(delayNanos, task)

  /**
   * A runtime that drops re-scheduled work after `maxReschedules`, so a threshold that would otherwise
   * livelock the run loop fails the test instead of hanging the suite.
   */
  private final class BoundedComputeRuntime(override val autoCedeThreshold: Int, maxReschedules: Int) extends FxRuntime:
    val reschedules: AtomicInteger = new AtomicInteger(0)

    override def executeCompute(task: () => Unit): Unit =
      if reschedules.incrementAndGet() <= maxReschedules then task()

    override def executeBlocking(task: () => Unit):      Unit        = task()
    override def executeInterruptible(task: () => Unit): Fx.Canceler =
      task()
      Fx.Canceler.noop
    override def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
      FxRuntime.global.scheduleOnce(delayNanos, task)

  /** A chain of `length` synchronous `map`s, which the run loop walks without ever suspending. */
  private def syntheticChain(length: Int): Fx[Int] =
    (0 until length).foldLeft(Fx.pure(0))((acc, _) => acc.map(_ + 1))

  test("the runtime's autoCedeThreshold decides when the loop cedes") {
    val runtime = new TunedRuntime(autoCedeThreshold = 8, finalizerTimeout = 30.seconds)
    syntheticChain(64).unsafeRun(_ => ())(using runtime)
    assert(
      runtime.computeCount.get() >= 1,
      "a 64-step chain must cede at a threshold of 8, but executeCompute was never called"
    )
  }

  test("a high autoCedeThreshold keeps a short chain on the calling thread") {
    val runtime = new TunedRuntime(autoCedeThreshold = 4096, finalizerTimeout = 30.seconds)
    syntheticChain(64).unsafeRun(_ => ())(using runtime)
    assertEquals(
      runtime.computeCount.get(),
      0,
      "a 64-step chain must not cede at a threshold of 4096"
    )
  }

  test("the runtime's finalizerTimeout bounds a cancel-path release that never settles") {
    val runtime = new TunedRuntime(autoCedeThreshold = 1024, finalizerTimeout = 150.millis)

    val neverSettles: Fx[Unit] = Fx.async(_ => Fx.Canceler.noop)
    val program = Fx.bracket(Fx.pure("resource"))(_ => Fx.async[Unit](_ => Fx.Canceler.noop))(_ => neverSettles)

    val token          = program.unsafeRunCancelable(_ => ())(using runtime)
    val cancelFinished = new AtomicInteger(0)
    token.cancel.unsafeRun(_ => cancelFinished.incrementAndGet())(using runtime)

    val waited = Promise[Unit]()
    Fx.sleep(1.second).unsafeRun(_ => waited.success(()))(using runtime)
    waited.future.map { _ =>
      assertEquals(
        cancelFinished.get(),
        1,
        "cancel must unblock once the runtime's 150ms finalizer timeout fires, not the 30s default"
      )
    }
  }

  test("a pathological autoCedeThreshold still makes progress instead of livelocking") {
    for threshold <- List(Int.MinValue, 0, 1) do
      val runtime   = new BoundedComputeRuntime(threshold, maxReschedules = 50)
      val completed = new AtomicInteger(0)
      syntheticChain(3).unsafeRun(_ => completed.incrementAndGet())(using runtime)
      assertEquals(
        completed.get(),
        1,
        s"a threshold of $threshold must not stop the run loop from executing any step " +
          s"(gave up after ${ runtime.reschedules.get() } re-schedules)"
      )
  }

  test("a non-positive finalizerTimeout abandons the cancel-path release rather than waiting") {
    val runtime      = new TunedRuntime(autoCedeThreshold = 1024, finalizerTimeout = Duration.Zero)
    val releaseBegan = new AtomicInteger(0)

    val neverSettles: Fx[Unit] = Fx.delay(releaseBegan.incrementAndGet()).flatMap(_ => Fx.async(_ => Fx.Canceler.noop))
    val program = Fx.bracket(Fx.pure("resource"))(_ => Fx.async[Unit](_ => Fx.Canceler.noop))(_ => neverSettles)

    val token          = program.unsafeRunCancelable(_ => ())(using runtime)
    val cancelFinished = new AtomicInteger(0)
    token.cancel.unsafeRun(_ => cancelFinished.incrementAndGet())(using runtime)

    val waited = Promise[Unit]()
    Fx.sleep(1.second).unsafeRun(_ => waited.success(()))(using runtime)
    waited.future.map { _ =>
      assertEquals(cancelFinished.get(), 1, "cancel must not hang when the timeout is zero")
      assertEquals(releaseBegan.get(), 1, "the release is started, then abandoned by the immediate timeout")
    }
  }
