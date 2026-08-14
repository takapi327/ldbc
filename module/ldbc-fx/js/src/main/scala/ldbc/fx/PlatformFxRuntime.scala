/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.scalajs.js.timers.{ clearTimeout, setTimeout }

/**
 * Scala.js platform-default [[FxRuntime]]. Single-threaded (event loop): timers use `setTimeout`,
 * there is no thread offload so `blocking` runs inline, and `executeCompute` yields to the event
 * loop via `setTimeout(0)`.
 */
private[fx] object PlatformFxRuntime:

  /** The platform-default runtime instance. */
  val global: FxRuntime = new FxRuntime:

    override def executeCompute(task: () => Unit): Unit =
      setTimeout(0.0) { task() }
      ()

    override def executeBlocking(task: () => Unit): Unit = task()

    override def executeInterruptible(task: () => Unit): Fx.Canceler =
      task()
      Fx.Canceler.noop

    override def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
      val handle = setTimeout(delayNanos.toDouble / 1e6) { task() }
      new Fx.Canceler { override def cancel(): Unit = clearTimeout(handle) }

/**
 * Holds the [[FxRuntime]] current during interpretation. The single-threaded JS runtime needs no
 * `ThreadLocal`, so a plain `var` suffices.
 */
private[fx] object PlatformFxLocal:
  private var value:      FxRuntime = null
  def get():              FxRuntime = value
  def set(rt: FxRuntime): Unit      = value = rt
