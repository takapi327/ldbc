/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS }

/**
 * Effect-agnostic execution substrate for [[Fx]]: the compute / blocking / scheduler / interruptible
 * facilities the run loop schedules onto.
 *
 * The platform default ([[FxRuntime.global]]) owns its own `fx-*` pools. Frontends (cats-effect /
 * ZIO) can instead supply an implementation backed by the host runtime, so `Fx` runs on the host's
 * threads rather than duplicating them. Which runtime a run uses is threaded through
 * [[Fx.unsafeRun]] and defaults to [[FxRuntime.current]].
 */
trait FxRuntime:

  /**
   * Runs a CPU-bound continuation. This is the auto-cede target — where a long synchronous
   * continuation is moved so it cannot monopolise the thread that completed an async step.
   *
   * @param task the continuation to run
   */
  def executeCompute(task: () => Unit): Unit

  /**
   * Runs a blocking action off the run loop so it does not stall the interpreter.
   *
   * @param task the blocking action to run
   */
  def executeBlocking(task: () => Unit): Unit

  /**
   * Runs an interruptible blocking action, returning a [[Fx.Canceler]] that interrupts it.
   *
   * @param task the interruptible blocking action
   * @return a [[Fx.Canceler]] that interrupts the running action
   */
  def executeInterruptible(task: () => Unit): Fx.Canceler

  /**
   * Schedules `task` to run once after the given delay.
   *
   * @param delayNanos the delay in nanoseconds
   * @param task       the action to run once the delay elapses
   * @return a [[Fx.Canceler]] that cancels the pending timer
   */
  def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler

  /**
   * How many consecutive synchronous run-loop steps may execute before the loop re-schedules the
   * remaining continuation onto [[executeCompute]] and returns, freeing the current thread.
   *
   * This keeps a long synchronous chain from monopolising a thread that resumes continuations inline —
   * typically an I/O poller or selector thread. Lower it to yield sooner on a runtime whose threads are
   * latency-sensitive; raise it to cut hand-offs on a runtime dedicated to compute.
   *
   * Must be at least [[FxRuntime.minAutoCedeThreshold]]: a smaller value would cede before executing
   * anything, so the loop would re-schedule forever without making progress. The run loop clamps to that
   * minimum rather than trusting the value, so an out-of-range override degrades to frequent ceding
   * instead of a hang.
   */
  def autoCedeThreshold: Int = FxRuntime.defaultAutoCedeThreshold

  /**
   * The upper bound applied to each cancel-path release, so a release that never settles (a rollback to
   * a dead peer, say) cannot make [[Fx.CancelToken.cancel]] hang forever.
   *
   * It belongs to the runtime rather than the process because the right bound depends on what the
   * programs on that runtime release — a short-lived request runtime wants to give up long before a
   * background one does.
   *
   * Must be positive. Zero or a negative duration means every release is abandoned before it can run,
   * and because a release's error is suppressed the resources it would have freed are simply not freed;
   * there is no way to express "wait forever", since the bound exists to stop `cancel` hanging.
   */
  def finalizerTimeout: FiniteDuration = FxRuntime.defaultFinalizerTimeout

/** Companion providing the platform-default runtime and the dynamic "current runtime" scope. */
object FxRuntime:

  /** The [[FxRuntime.autoCedeThreshold]] a runtime gets unless it overrides it. */
  val defaultAutoCedeThreshold: Int = 1024

  /**
   * The smallest workable [[FxRuntime.autoCedeThreshold]]. At 1 the loop would cede before running a
   * single step, so the run loop clamps anything lower to this.
   */
  val minAutoCedeThreshold: Int = 2

  /** The [[FxRuntime.finalizerTimeout]] a runtime gets unless it overrides it. */
  val defaultFinalizerTimeout: FiniteDuration = FiniteDuration(30000, MILLISECONDS)

  /**
   * The platform-default runtime (owns the `fx-*` pools). Used by the Future / direct-`Fx`
   * frontends on every platform, and as the fallback when no runtime is injected.
   */
  def global: FxRuntime = PlatformFxRuntime.global

  /**
   * The runtime in effect on the current thread, or [[global]] when none is set. Nested `unsafeRun`
   * calls started during interpretation inherit it (see [[withRuntime]]).
   */
  def current: FxRuntime = Option(PlatformFxLocal.get()).getOrElse(global)

  /**
   * Runs `body` with `rt` installed as [[current]] for the duration, restoring the previous value
   * afterwards. Every resume point re-establishes it, so a thread hop does not lose the runtime.
   *
   * @param rt   the runtime to install
   * @param body the action to run with `rt` current
   * @tparam A the result type
   * @return the result of `body`
   */
  private[fx] def withRuntime[A](rt: FxRuntime)(body: => A): A =
    val prev = PlatformFxLocal.get()
    PlatformFxLocal.set(rt)
    try body
    finally PlatformFxLocal.set(prev)
