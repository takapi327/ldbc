/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ Executors, ScheduledExecutorService, ThreadFactory, TimeUnit }

/**
 * JVM platform-default [[FxRuntime]]: a scheduler thread for timers, a cached pool for blocking
 * calls, and a fixed pool (≈ cores) for auto-ceded continuations. Pools are lazy, so an idle process
 * pays nothing.
 */
private[fx] object PlatformFxRuntime:

  /** A daemon [[java.util.concurrent.ThreadFactory]] naming its threads `name`. */
  private def daemon(name: String): ThreadFactory = (r: Runnable) =>
    val t = new Thread(r, name)
    t.setDaemon(true)
    t

  /** Single-threaded scheduler backing `scheduleOnce`. */
  private lazy val scheduler: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(daemon("fx-scheduler"))

  /** Unbounded cached pool backing `executeBlocking`. */
  private lazy val blockingPool =
    Executors.newCachedThreadPool(daemon("fx-blocking"))

  /** Fixed pool (≈ cores) backing `executeCompute` — where auto-ceded continuations resume. */
  private lazy val computePool =
    Executors.newFixedThreadPool(math.max(2, Runtime.getRuntime.availableProcessors()), daemon("fx-compute"))

  /** The platform-default runtime instance. */
  val global: FxRuntime = new FxRuntime:

    override def executeCompute(task: () => Unit): Unit =
      computePool.execute(new Runnable { override def run(): Unit = task() })

    override def executeBlocking(task: () => Unit): Unit =
      blockingPool.execute(new Runnable { override def run(): Unit = task() })

    override def executeInterruptible(task: () => Unit): Fx.Canceler =
      val thread = new Thread(() => task(), "fx-interruptible")
      thread.setDaemon(true)
      thread.start()
      new Fx.Canceler { override def cancel(): Unit = thread.interrupt() }

    override def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler =
      val future =
        scheduler.schedule(new Runnable { override def run(): Unit = task() }, delayNanos, TimeUnit.NANOSECONDS)
      new Fx.Canceler { override def cancel(): Unit = { future.cancel(false); () } }

/** Holds the [[FxRuntime]] current on each thread during interpretation (JVM: a `ThreadLocal`). */
private[fx] object PlatformFxLocal:
  private val tl                    = new ThreadLocal[FxRuntime]
  def get():             FxRuntime  = tl.get()
  def set(rt: FxRuntime): Unit      = tl.set(rt)
