/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.{ ConcurrentLinkedQueue, TimeoutException }
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import scala.concurrent.duration.{ FiniteDuration, MILLISECONDS, NANOSECONDS }
import scala.util.control.NonFatal

/**
 * Effect-agnostic, cancelable, lazy async effect. This is the internal core effect of the driver;
 * end users never see it. It is bridged to cats-effect `IO`, ZIO `Task`, and `Future` at the
 * frontend boundary, so a single effect-agnostic program runs natively on any of them.
 *
 * Key properties (validated empirically before landing):
 *   - lazy: side effects run only in `unsafeRun`, so CE/ZIO can wrap it lawfully.
 *   - stack-safe: the run loop is trampolined (deep flatMap / error-unwind chains do not overflow).
 *   - cancelable: `unsafeRun` returns a [[Fx.Canceler]]; `bracket` releases on success/error/cancel.
 *   - the `async` completion callback is call-once and safe under cancel/complete races.
 *
 * @tparam A the produced value type
 */
sealed trait Fx[+A]:

  /**
   * Transforms the successful result of this effect with `f`.
   *
   * @param f the function applied to the produced value
   * @tparam B the transformed result type
   * @return an effect that produces `f(a)` when this produces `a`
   */
  def map[B](f: A => B): Fx[B] = flatMap(a => Fx.Pure(f(a)))

  /**
   * Sequences this effect with `f`: runs `f` on the successful result to produce the next effect.
   *
   * @param f the continuation invoked with the produced value
   * @tparam B the result type of the continuation
   * @return the effect returned by `f`, run after this one succeeds
   */
  def flatMap[B](f: A => Fx[B]): Fx[B] = Fx.FlatMap(this, f)

  /**
   * Recovers from a failure of this effect by running `h` with the error.
   *
   * @param h the handler invoked with the thrown error
   * @tparam B a supertype of `A` produced by the recovery effect
   * @return an effect that falls back to `h(t)` when this fails with `t`
   */
  def handleErrorWith[B >: A](h: Throwable => Fx[B]): Fx[B] = Fx.Handle(this, h)

  /**
   * Runs `fin` if and only if this effect is cancelled before completing. Unlike `bracket`'s release,
   * `fin` does NOT run on normal success or error — only on cancellation. Useful for deregistering a
   * pending registration (e.g. removing a waiter from a queue) when a suspended effect is interrupted.
   *
   * @param fin the finalizer to run only on cancellation
   * @return an effect equivalent to this one, with `fin` attached to the cancellation path
   */
  def onCancel(fin: Fx[Unit]): Fx[A] = Fx.OnCancel(this, fin)

  /**
   * Runs this effect. This is the unsafe boundary where side effects actually happen; the effect
   * frontends ([[ldbc.fx.Fx]] bridged to cats-effect / ZIO / Future) call it internally.
   *
   * @param cb invoked at most once with `Right(a)` on success or `Left(t)` on failure
   * @param rt the runtime the continuations run on; defaults to [[FxRuntime.current]] so a nested run
   *           started during interpretation inherits the enclosing run's runtime
   * @return a [[Fx.Canceler]] that requests interruption and runs any open `bracket` finalizers
   */
  def unsafeRun(cb: Either[Throwable, A] => Unit)(using rt: FxRuntime = FxRuntime.current): Fx.Canceler =
    val handle = Fx.run(this, cb, rt, cancelable = false)
    new Fx.Canceler:
      override def cancel(): Unit = handle.requestCancel()

  /**
   * Like [[unsafeRun]] but returns a [[Fx.CancelToken]] whose `cancel: Fx[Unit]` completes only
   * after the run's cancel-path finalizers have drained. Frontend bridges (CE/ZIO) use this so a
   * cancelled fiber backpressures until rollback / resource release actually finishes.
   *
   * @param cb invoked at most once with the outcome
   * @param rt the runtime the continuations run on
   * @return a [[Fx.CancelToken]] whose completion tracks finalizer draining
   */
  def unsafeRunCancelable(cb: Either[Throwable, A] => Unit)(using rt: FxRuntime = FxRuntime.current): Fx.CancelToken =
    val handle = Fx.run(this, cb, rt, cancelable = true)
    new Fx.CancelToken:
      val cancel: Fx[Unit] =
        Fx.delay(handle.requestCancel()).flatMap { _ =>
          if handle.cancelDone != null then handle.cancelDone.get else Fx.unit
        }

/** Constructors and the runtime for [[Fx]]. */
object Fx:

  /** An idempotent cancellation handle. Cancelling requests interruption and runs open `bracket` finalizers. */
  trait Canceler:
    /** Requests cancellation. Safe to call more than once. */
    def cancel(): Unit

  /** Companion for [[Canceler]]. */
  object Canceler:
    /** A [[Canceler]] that does nothing. */
    val noop: Canceler = new Canceler:
      override def cancel(): Unit = ()

  /**
   * A completion-aware cancel handle returned by [[Fx.unsafeRunCancelable]]. Unlike [[Canceler]]
   * (fire-and-forget), [[cancel]] is an `Fx[Unit]` that completes only after the run's cancel-path
   * finalizers have drained, so a frontend bridge (CE/ZIO) can backpressure on cancellation.
   */
  trait CancelToken:
    /** Requests cancellation and completes when the run's finalizers have finished draining. */
    def cancel: Fx[Unit]

  /** Pairs an in-flight async/interruptible canceler with whether it was suspended inside a mask. */
  private[fx] final case class Cancelable(canceler: Canceler, masked: Boolean)

  /**
   * Internal result of [[run]]: the fire-and-forget cancel request and (for cancelable runs) the
   * `Deferred` that closes when cancellation finalizers have drained. [[unsafeRun]] wraps only the
   * former into a [[Canceler]]; [[unsafeRunCancelable]] wraps both into a [[CancelToken]].
   */
  private[fx] final class RunHandle(val requestCancel: () => Unit, val cancelDone: Deferred[Unit] | Null)

  private[fx] final case class Pure[A](a: A)                                           extends Fx[A]
  private[fx] final case class Err(t: Throwable)                                       extends Fx[Nothing]
  private[fx] final case class Delay[A](thunk: () => A)                                extends Fx[A]
  private[fx] final case class Blocking[A](thunk: () => A)                             extends Fx[A]
  private[fx] final case class Interruptible[A](thunk: () => A)                        extends Fx[A]
  private[fx] final case class Async[A](k: (Either[Throwable, A] => Unit) => Canceler) extends Fx[A]
  private[fx] final case class FlatMap[A, B](fa: Fx[A], f: A => Fx[B])                 extends Fx[B]
  private[fx] final case class Handle[A](fa: Fx[A], h: Throwable => Fx[A])             extends Fx[A]
  private[fx] final case class Bracket[A, B](acquire: Fx[A], use: A => Fx[B], release: A => Fx[Unit]) extends Fx[B]
  private[fx] final case class Uncancelable[A](body: Fx[A])                                           extends Fx[A]
  private[fx] final case class OnCancel[A](fa: Fx[A], fin: Fx[Unit])                                  extends Fx[A]

  /**
   * Lifts an already-computed value into an effect.
   *
   * @param a the value to produce
   * @tparam A the value type
   * @return an effect that immediately succeeds with `a`
   */
  def pure[A](a: A): Fx[A] = Pure(a)

  /** The effect that immediately produces `()`. */
  def unit: Fx[Unit] = Pure(())

  /**
   * An effect that immediately fails with `t`.
   *
   * @param t the error to raise
   * @tparam A the (never produced) result type
   * @return a failed effect
   */
  def raiseError[A](t: Throwable): Fx[A] = Err(t)

  /**
   * Suspends a synchronous side effect so it runs when the effect is executed rather than now.
   * The thunk runs on the calling thread; use [[blocking]] for calls that may block.
   *
   * @param thunk the side effect to suspend (evaluated on each run)
   * @tparam A the result type
   * @return an effect that evaluates `thunk` when run
   */
  def delay[A](thunk: => A): Fx[A] = Delay(() => thunk)

  /**
   * Suspends a blocking synchronous call, executing it off the IO/event thread (on a dedicated
   * pool on JVM/Native; inline on the single-threaded JS runtime).
   *
   * The computation is cancelable at the boundary — on cancel the result is discarded and the
   * continuation does not run — but the thunk itself always runs to completion; it is not
   * interrupted. Use [[interruptible]] when the blocking call must actually be aborted on cancel.
   *
   * @param thunk the blocking side effect to suspend
   * @tparam A the result type
   * @return an effect that evaluates `thunk` on the blocking executor when run
   */
  def blocking[A](thunk: => A): Fx[A] = Blocking(() => thunk)

  /**
   * Like [[blocking]], but the executing thread is interrupted (`Thread.interrupt()`) when the effect
   * is cancelled, so a thunk that honours interruption (e.g. `Thread.sleep`, an interruptible channel,
   * or code polling `Thread.interrupted()`) can abort early. Runs on a dedicated thread on JVM/Native
   * so the interrupt cannot leak to other work; on the single-threaded JS runtime there is no thread
   * to interrupt, so it behaves like [[blocking]].
   *
   * @param thunk the interruptible blocking side effect to suspend
   * @tparam A the result type
   * @return an effect that evaluates `thunk`, interrupting it on cancellation
   */
  def interruptible[A](thunk: => A): Fx[A] = Interruptible(() => thunk)

  /**
   * Bridges a callback-based asynchronous API into an [[Fx]]. The IO engine drives the completion.
   *
   * @param k registers the completion callback and returns a [[Canceler]] for the pending operation
   * @tparam A the result type
   * @return an effect that suspends until the callback is invoked
   * @note the completion callback is call-once: only the first invocation takes effect.
   */
  def async[A](k: (Either[Throwable, A] => Unit) => Canceler): Fx[A] = Async(k)

  /**
   * Acquires a resource, uses it, and guarantees release. `release` runs on success, error, and
   * cancellation, so the resource never leaks.
   *
   * @param acquire the effect that obtains the resource
   * @param use     produces the effect that uses the resource
   * @param release releases the resource; run exactly once
   * @tparam A the resource type
   * @tparam B the result type of `use`
   * @return an effect that yields the result of `use` with `release` guaranteed
   */
  def bracket[A, B](acquire: Fx[A])(use: A => Fx[B])(release: A => Fx[Unit]): Fx[B] =
    Bracket(acquire, use, release)

  /**
   * Runs `body` with cancellation deferred: while inside, a `cancel()` is recorded but does not
   * interrupt `body`; it takes effect only once the region exits. This makes a `bracket` acquire
   * uninterruptible, so an acquired resource is never leaked by a racing cancel. The whole `body` is
   * masked — there is no partial `poll`.
   *
   * @param body the effect to run uninterruptibly
   * @tparam A the result type
   * @return an effect equivalent to `body` with cancellation deferred across it
   */
  def uncancelable[A](body: Fx[A]): Fx[A] = Uncancelable(body)

  /**
   * An effect that completes after the given delay, driven by the platform scheduler.
   *
   * @param d the delay before completion
   * @return an effect that produces `()` after `d`
   */
  def sleep(d: FiniteDuration): Fx[Unit] = Async[Unit] { cb =>
    FxRuntime.current.scheduleOnce(d.toNanos, () => cb(Right(())))
  }

  /**
   * An effect that never completes. Useful as the cancellation branch of a fiber join, mirroring
   * cats-effect's `joinWithNever`.
   *
   * @tparam A the (never-produced) result type
   * @return an effect that suspends forever
   */
  def never[A]: Fx[A] = Async[A](_ => Canceler.noop)

  /**
   * Runs `fa` only when `cond` is true, otherwise does nothing (cats `whenA`).
   *
   * @param cond the guard condition
   * @param fa   the effect to run when `cond` is true (evaluated lazily)
   * @return `fa` when `cond`, otherwise a no-op
   */
  def whenA(cond: Boolean)(fa: => Fx[Unit]): Fx[Unit] = if cond then fa else unit

  /**
   * Runs `fa` only when `cond` is false, otherwise does nothing (cats `unlessA`).
   *
   * @param cond the guard condition
   * @param fa   the effect to run when `cond` is false (evaluated lazily)
   * @return `fa` when `!cond`, otherwise a no-op
   */
  def unlessA(cond: Boolean)(fa: => Fx[Unit]): Fx[Unit] = if cond then unit else fa

  /**
   * The current value of a monotonic clock, suitable for measuring elapsed time. Unlike [[realTime]]
   * it is not affected by wall-clock adjustments, so differences between two readings are reliable.
   *
   * @return an effect producing the monotonic time as a [[FiniteDuration]] since an arbitrary origin
   */
  def monotonic: Fx[FiniteDuration] = Delay(() => FiniteDuration(System.nanoTime(), NANOSECONDS))

  /**
   * The current wall-clock time since the Unix epoch. Subject to clock adjustments, so use
   * [[monotonic]] for measuring durations.
   *
   * @return an effect producing the wall-clock time as a [[FiniteDuration]] since the epoch
   */
  def realTime: Fx[FiniteDuration] = Delay(() => FiniteDuration(System.currentTimeMillis(), MILLISECONDS))

  /**
   * Bounds `fa` by `duration`, racing it against a timer. If `fa` completes first its result is used
   * and the timer is cancelled; if the timer fires first `fa` is cancelled and the effect fails with
   * `onTimeout`. The loser is always cancelled, so no work is left running.
   *
   * @param fa        the effect to bound
   * @param duration  the maximum time to wait for `fa`
   * @param onTimeout the error to raise if `duration` elapses first (evaluated only on timeout)
   * @tparam A the result type
   * @return an effect equivalent to `fa` but failing with `onTimeout` if it does not complete in time
   */
  def timeout[A](fa: Fx[A], duration: FiniteDuration)(onTimeout: => Throwable): Fx[A] =
    Async[A] { cb =>
      val settled  = new AtomicBoolean(false)
      val timerRef = new AtomicReference[Canceler](Canceler.noop)
      val mainRef  = new AtomicReference[Canceler](Canceler.noop)
      def finish(result: Either[Throwable, A], cancelOther: () => Unit): Unit =
        if settled.compareAndSet(false, true) then
          cancelOther()
          cb(result)
      val mainCanceler = fa.unsafeRun(result => finish(result, () => timerRef.get().cancel()))
      mainRef.set(mainCanceler)
      if !settled.get() then
        val timerCanceler = sleep(duration).unsafeRun(_ => finish(Left(onTimeout), () => mainRef.get().cancel()))
        timerRef.set(timerCanceler)
      new Canceler:
        override def cancel(): Unit =
          if settled.compareAndSet(false, true) then
            mainRef.get().cancel()
            timerRef.get().cancel()
    }

  private sealed trait Frame
  private final case class Bind(f: Any => Fx[Any])          extends Frame
  private final case class HandleF(h: Throwable => Fx[Any]) extends Frame
  private case object Unmask                                extends Frame

  private val SUSPENDED: AnyRef = new AnyRef

  /**
   * Test-only seam invoked on the running thread immediately after an `async` publishes its
   * suspension (i.e. after the `SUSPENDED` CAS wins), so concurrency tests can deterministically
   * open the suspend/cancel race window. It is a no-op in production and must stay `private[fx]`.
   */
  @volatile private[fx] var suspendHook: () => Unit = () => ()

  /**
   * Auto-cede threshold: after this many consecutive synchronous run-loop steps without suspending,
   * the loop re-schedules the remaining continuation onto [[FxRuntime.executeCompute]] and returns,
   * freeing the current thread. This prevents a long synchronous chain from monopolising an I/O
   * poller/selector thread (which resumes continuations inline). Overridable `private[fx]` for tests.
   */
  @volatile private[fx] var autoCedeThreshold: Int = 1024

  /**
   * Upper bound applied per cancel-path release so a release that never settles (e.g. a rollback to
   * a dead peer) cannot make [[CancelToken.cancel]] hang forever. Global `private[fx] var` for now;
   * a per-runtime value would require extending [[FxRuntime]] (and every platform impl + test double).
   */
  @volatile private[fx] var finalizerTimeout: FiniteDuration = FiniteDuration(30000, MILLISECONDS)

  private def fromResult(r: Either[Throwable, Any]): Fx[Any] = r match
    case Right(a) => Pure(a)
    case Left(t)  => Err(t)

  private def run[A](start: Fx[A], cb: Either[Throwable, A] => Unit, rt: FxRuntime, cancelable: Boolean): RunHandle =
    val cancelled        = new AtomicBoolean(false)
    val done             = new AtomicBoolean(false)
    val drained          = new AtomicBoolean(false)
    val current          = new AtomicReference[Cancelable](Cancelable(Canceler.noop, false))
    val cancelFinalizers = new ConcurrentLinkedQueue[() => Fx[Unit]]()

    /**
     * Present only on the cancelable entry point; closed (run-independently) at every run termination
     * point so a waiting `CancelToken.cancel` unblocks. Null on the fire-and-forget `unsafeRun` path.
     */
    val cancelDone: Deferred[Unit] | Null = if cancelable then Deferred.unsafe[Unit] else null

    def finish(r: Either[Throwable, Any]): Unit =
      if done.compareAndSet(false, true) then
        cb(r.asInstanceOf[Either[Throwable, A]])
        if cancelDone != null then { cancelDone.unsafeComplete(()); () }

    var cur:   Fx[Any]     = start.asInstanceOf[Fx[Any]]
    var stack: List[Frame] = Nil

    /**
     * Current uncancelable-mask depth; atomic so [[Canceler.cancel]] can consult it. Cancellation
     * (including finalizer draining) is deferred while it is positive.
     */
    val maskDepth = new java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Whether user code is currently in flight — the run loop executing a step, or a
     * blocking/interruptible thunk running on its executor. While `true`, [[Canceler.cancel]] must
     * NOT drain finalizers (a release running concurrently with `use` could, e.g., free native
     * memory an in-flight call still touches); draining is instead performed by whichever side
     * observes the cancellation once the in-flight step has finished.
     */
    val executing = new AtomicBoolean(true)

    /**
     * Runs the cancel-path finalizers exactly once (guarded by `drained` so concurrent callers do not
     * split the queue), sequentially in LIFO order, each bounded by `finalizerTimeout` and with its
     * error suppressed. When the chain settles it closes `cancelDone` (run-independently) so a waiting
     * `CancelToken.cancel` unblocks. May be called from the loop, from `requestCancel`, or from the
     * Async self-cancel return.
     */
    def drainFinalizers(): Unit =
      if drained.compareAndSet(false, true) then
        var fs: List[() => Fx[Unit]] = Nil
        var f = cancelFinalizers.poll()
        while f != null do { fs = f :: fs; f = cancelFinalizers.poll() }
        val chain = fs.foldLeft(Fx.unit) { (acc, rel) =>
          acc.flatMap(_ =>
            timeout(rel(), finalizerTimeout)(new TimeoutException("finalizer timed out")).handleErrorWith(_ => Fx.unit)
          )
        }
        chain.unsafeRun(_ => if cancelDone != null then { cancelDone.unsafeComplete(()); () })(using rt)

    /**
     * Resumes `task` on `rt`'s compute pool with `rt` re-installed as [[FxRuntime.current]]. Used
     * by the resume points that must leave their current thread — auto-cede (off the completing
     * thread), `Blocking` / `Interruptible` (off their dedicated threads). The `Async` completion
     * resumes inline instead (see its case): routing every async resume through a pool hop regresses
     * synchronization-heavy paths (pool `Deferred`/`Mutex`) with no benefit, so only the points that
     * genuinely need to move threads hop.
     */
    def resumeOn(task: () => Unit): Unit =
      rt.executeCompute(() => FxRuntime.withRuntime(rt)(task()))

    /**
     * Trampolined interpreter. Suspends (returns) on `Async`/`Blocking`; the completion callback
     * resumes by re-invoking `loop()`. Only one thread drives `loop()` at a time — the hand-off
     * across threads is arbitrated by the CAS on `cbState`.
     */
    def loop(): Unit =
      var iters  = 0
      val cedeAt = autoCedeThreshold // read the volatile once per loop entry, not per iteration
      while true do
        if cancelled.get() && maskDepth.get() == 0 then
          drainFinalizers()
          executing.set(false)
          return
        iters += 1
        if iters >= cedeAt then
          resumeOn(() => loop())
          return
        cur match
          case Pure(a) =>
            var s   = stack
            var brk = false
            while !brk do
              s match
                case Bind(f) :: rest    => cur = safeApply(f, a); stack = rest; brk = true
                case HandleF(_) :: rest => s = rest
                case Unmask :: rest     => maskDepth.decrementAndGet(); s = rest
                case Nil                => finish(Right(a)); executing.set(false); return
          case Err(t) =>
            var s   = stack
            var brk = false
            while !brk do
              s match
                case HandleF(h) :: rest => cur = safeApply1(h, t); stack = rest; brk = true
                case Bind(_) :: rest    => s = rest
                case Unmask :: rest     => maskDepth.decrementAndGet(); s = rest
                case Nil                => finish(Left(t)); executing.set(false); return
          case Delay(th) =>
            cur = try Pure(th())
            catch { case NonFatal(e) => Err(e) }
          case FlatMap(fa, f) =>
            stack = Bind(f.asInstanceOf[Any => Fx[Any]]) :: stack; cur = fa
          case Handle(fa, h) =>
            stack = HandleF(h.asInstanceOf[Throwable => Fx[Any]]) :: stack; cur = fa
          case Uncancelable(body) =>
            maskDepth.incrementAndGet()
            stack = Unmask :: stack
            cur   = body.asInstanceOf[Fx[Any]]
          case Bracket(acquire, use, release) =>
            val u = use.asInstanceOf[Any => Fx[Any]]
            val r = release.asInstanceOf[Any => Fx[Unit]]
            cur = Uncancelable(
              acquire.asInstanceOf[Fx[Any]].map { res =>
                val ran = new AtomicBoolean(false)
                val rel: () => Fx[Unit] =
                  () => if ran.compareAndSet(false, true) then r(res) else Fx.unit
                cancelFinalizers.add(rel)
                (res, rel, ran)
              }
            ).flatMap {
              case (res, rel, ran) =>
                // Deregister the cancel-path finalizer, claim the single run, then run `release`
                // UNCANCELABLY so its error is surfaced rather than swallowed. Runs exactly once per
                // path (success XOR error), so evaluating this description in both arms below is safe.
                val runRelease: Fx[Unit] =
                  Fx.delay { cancelFinalizers.remove(rel); ran.set(true); () }.flatMap(_ => Uncancelable(r(res)))
                // Fast path (use succeeds, release succeeds): no `attempt`/`Either` boxing, no match —
                // `handleErrorWith` only wraps `u(res)`, so a release error from the trailing `flatMap`
                // is NOT re-caught (no double release). On a use error, release runs with its own error
                // suppressed and the use error (primary) is re-raised.
                u(res)
                  .handleErrorWith(ue => runRelease.handleErrorWith(_ => Fx.unit).flatMap(_ => Fx.raiseError(ue)))
                  .flatMap(a => runRelease.map(_ => a))
            }
          case OnCancel(fa, fin) =>
            val f   = fin.asInstanceOf[Fx[Unit]]
            val ran = new AtomicBoolean(false)
            val rel: () => Fx[Unit] = () => if ran.compareAndSet(false, true) then f else Fx.unit
            cancelFinalizers.add(rel)
            // On normal completion (success or error) deregister WITHOUT running `fin`; only the
            // cancellation path (drainFinalizers) runs it. The `executing` flag arbitrates so a cancel
            // cannot race the deregister within an interpretation burst.
            val deregister: Fx[Unit] = Fx.delay { cancelFinalizers.remove(rel); () }
            cur = fa
              .asInstanceOf[Fx[Any]]
              .flatMap(a => deregister.map(_ => a))
              .handleErrorWith(e => deregister.flatMap(_ => Fx.raiseError(e)))
          case Blocking(th) =>
            val once          = new AtomicBoolean(false)
            val cbState       = new AtomicReference[AnyRef](null)
            val capturedStack = stack
            val maskedHere    = maskDepth.get() > 0
            rt.executeBlocking { () =>
              val result: Either[Throwable, Any] = try Right(th())
              catch { case NonFatal(e) => Left(e) }
              if once.compareAndSet(false, true) then
                if !cbState.compareAndSet(null, result.asInstanceOf[AnyRef]) then
                  if !cancelled.get() || maskedHere then
                    cur = fromResult(result); stack = capturedStack; resumeOn(() => loop())
                  else
                    executing.set(false)
                    drainFinalizers()
            }
            if cbState.compareAndSet(null, SUSPENDED) then return
            else cur = fromResult(cbState.get().asInstanceOf[Either[Throwable, Any]])
          case Interruptible(th) =>
            val once              = new AtomicBoolean(false)
            val cbState           = new AtomicReference[AnyRef](null)
            val capturedStack     = stack
            val maskedHere        = maskDepth.get() > 0
            val cellSlot          = new AtomicReference[Cancelable](Cancelable(Canceler.noop, false))
            val interruptCanceler = rt.executeInterruptible { () =>
              val result: Either[Throwable, Any] = try Right(th())
              catch { case NonFatal(e) => Left(e) }
              if once.compareAndSet(false, true) then
                if !cbState.compareAndSet(null, result.asInstanceOf[AnyRef]) then
                  current.compareAndSet(cellSlot.get(), Cancelable(Canceler.noop, false))
                  if !cancelled.get() || maskedHere then
                    cur = fromResult(result); stack = capturedStack; resumeOn(() => loop())
                  else
                    executing.set(false)
                    drainFinalizers()
            }
            val cell = Cancelable(interruptCanceler, maskedHere)
            cellSlot.set(cell)
            current.set(cell)
            if cbState.compareAndSet(null, SUSPENDED) then
              suspendHook()
              if cancelled.get() && !maskedHere then interruptCanceler.cancel()
              return
            else
              current.compareAndSet(cell, Cancelable(Canceler.noop, false))
              cur = fromResult(cbState.get().asInstanceOf[Either[Throwable, Any]])
          case Async(k) =>
            val once          = new AtomicBoolean(false)
            val cbState       = new AtomicReference[AnyRef](null)
            val capturedStack = stack
            val cellSlot      = new AtomicReference[Cancelable](Cancelable(Canceler.noop, false))
            val maskedHere    = maskDepth.get() > 0
            def complete(result: Either[Throwable, Any]): Unit =
              if once.compareAndSet(false, true) then
                if !cbState.compareAndSet(null, result.asInstanceOf[AnyRef]) then
                  current.compareAndSet(cellSlot.get(), Cancelable(Canceler.noop, false))
                  executing.set(true)
                  if !cancelled.get() || maskedHere then
                    cur = fromResult(result); stack = capturedStack
                    FxRuntime.withRuntime(rt)(loop())
                  else
                    executing.set(false)
                    drainFinalizers()
            val canceler =
              try k(result => complete(result))
              catch
                case NonFatal(error) =>
                  complete(Left(error))
                  Canceler.noop
            val cell = Cancelable(canceler, maskedHere)
            cellSlot.set(cell)
            current.set(cell)
            executing.set(false)
            if cbState.compareAndSet(null, SUSPENDED) then
              suspendHook()
              if cancelled.get() && !maskedHere then
                canceler.cancel()
                drainFinalizers()
              return
            else
              executing.set(true)
              current.compareAndSet(cell, Cancelable(Canceler.noop, false))
              cur = fromResult(cbState.get().asInstanceOf[Either[Throwable, Any]])
      end while

    FxRuntime.withRuntime(rt)(loop())

    /** Fire-and-forget cancel request (mask-aware): does not wait for finalizer draining. */
    def requestCancel(): Unit =
      if cancelled.compareAndSet(false, true) then
        val c = current.getAndSet(Cancelable(Canceler.noop, false))
        if !c.masked then c.canceler.cancel()
        if !executing.get() && maskDepth.get() == 0 then drainFinalizers()

    new RunHandle(() => requestCancel(), cancelDone)

  private def safeApply(f: Any => Fx[Any], a: Any): Fx[Any] =
    try f(a)
    catch { case NonFatal(e) => Err(e) }
  private def safeApply1(h: Throwable => Fx[Any], t: Throwable): Fx[Any] =
    try h(t)
    catch { case NonFatal(e) => Err(e) }
