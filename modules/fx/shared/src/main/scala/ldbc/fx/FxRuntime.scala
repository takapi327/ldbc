/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

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

/** Companion providing the platform-default runtime and the dynamic "current runtime" scope. */
object FxRuntime:

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
