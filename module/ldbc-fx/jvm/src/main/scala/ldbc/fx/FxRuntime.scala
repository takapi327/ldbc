/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ Executors, ScheduledExecutorService, ThreadFactory, TimeUnit }

/** JVM runtime for [[Fx]]: a scheduler thread for timers and a cached pool for blocking calls. */
private[fx] object FxRuntime:

  /** A daemon [[java.util.concurrent.ThreadFactory]] naming its threads `name`. */
  private def daemon(name: String): ThreadFactory = (r: Runnable) =>
    val t = new Thread(r, name)
    t.setDaemon(true)
    t

  /** Single-threaded scheduler backing [[scheduleOnce]]. */
  private lazy val scheduler: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(daemon("fx-scheduler"))

  /** Unbounded cached pool backing [[executeBlocking]]. */
  private lazy val blockingPool =
    Executors.newCachedThreadPool(daemon("fx-blocking"))

  /** Fixed pool (≈ cores) backing [[executeCompute]] — where auto-ceded continuations resume. */
  private lazy val computePool =
    Executors.newFixedThreadPool(math.max(2, Runtime.getRuntime.availableProcessors()), daemon("fx-compute"))

  /**
   * Re-schedules `task` onto the compute pool. Used by the run loop's auto-cede to move a long
   * synchronous continuation off the current thread (e.g. an I/O poller thread), so it cannot
   * monopolise it.
   *
   * @param task the continuation to resume elsewhere
   */
  def executeCompute(task: () => Unit): Unit =
    computePool.execute(new Runnable { override def run(): Unit = task() })

  /**
   * Schedules `task` to run once after the given delay.
   *
   * @param delayNanos the delay in nanoseconds
   * @param task       the action to run once the delay elapses
   * @return a [[Fx.Canceler]] that cancels the pending timer
   */
  def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
    val future = scheduler.schedule(new Runnable { override def run(): Unit = task() }, delayNanos, TimeUnit.NANOSECONDS)
    new Fx.Canceler { override def cancel(): Unit = { future.cancel(false); () } }

  /**
   * Runs `task` on the blocking pool so it does not block the run loop.
   *
   * @param task the blocking action to run
   */
  def executeBlocking(task: () => Unit): Unit =
    blockingPool.execute(new Runnable { override def run(): Unit = task() })

  /**
   * Runs `task` on a fresh interruptible daemon thread and returns a [[Fx.Canceler]] that interrupts
   * it. A dedicated thread (not the shared blocking pool) ensures the interrupt cannot leak to other
   * work reusing a pooled thread.
   *
   * @param task the interruptible blocking action
   * @return a [[Fx.Canceler]] that interrupts the running thread
   */
  def executeInterruptible(task: () => Unit): Fx.Canceler =
    val thread = new Thread(() => task(), "fx-interruptible")
    thread.setDaemon(true)
    thread.start()
    new Fx.Canceler { override def cancel(): Unit = thread.interrupt() }
