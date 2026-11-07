/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.AtomicReference

import scala.collection.immutable.Queue

/**
 * A fair (FIFO) mutual-exclusion lock, the effect-agnostic counterpart of `cats.effect.std.Mutex`.
 * Backed by a lock-free [[java.util.concurrent.atomic.AtomicReference]] state machine so it works on
 * every platform without `Async`. The supported API is [[surround]], which acquires the lock, runs
 * the body, and releases it — even on error or cancellation — using [[Fx.bracket]] (whose acquire is
 * uncancelable, so the lock can never be leaked by a cancellation between acquire and release).
 */
final class Mutex private (private val state: AtomicReference[Mutex.State]):

  /**
   * Runs `fa` while holding the lock, releasing it afterwards even on error or cancellation. If the
   * lock is held, the caller suspends (FIFO) until it is its turn.
   *
   * @param fa the body to run under mutual exclusion
   * @tparam A the result type of the body
   */
  def surround[A](fa: Fx[A]): Fx[A] = Fx.bracket(acquire)(_ => fa)(_ => release)

  /** Acquires the lock, suspending in FIFO order until it is granted. */
  private def acquire: Fx[Unit] = Fx.async { cb =>
    @annotation.tailrec
    def loop(): Fx.Canceler =
      state.get match
        case Mutex.Free =>
          if state.compareAndSet(Mutex.Free, Mutex.Locked(Queue.empty)) then
            cb(Right(()))
            Fx.Canceler.noop
          else loop()
        case locked @ Mutex.Locked(waiters) =>
          if state.compareAndSet(locked, Mutex.Locked(waiters.enqueue(cb))) then
            new Fx.Canceler { override def cancel(): Unit = remove(cb) }
          else loop()
    loop()
  }

  /** Releases the lock, granting it to the next FIFO waiter if any. */
  private def release: Fx[Unit] = Fx.delay {
    @annotation.tailrec
    def loop(): Unit =
      state.get match
        case Mutex.Free                     => ()
        case locked @ Mutex.Locked(waiters) =>
          if waiters.isEmpty then { if !state.compareAndSet(locked, Mutex.Free) then loop() }
          else
            val (next, rest) = waiters.dequeue
            if state.compareAndSet(locked, Mutex.Locked(rest)) then next(Right(())) else loop()
    loop()
  }

  /** Removes a still-queued waiter (used by the cancellation of a suspended acquire). */
  private def remove(cb: Either[Throwable, Unit] => Unit): Unit =
    @annotation.tailrec
    def loop(): Unit =
      state.get match
        case Mutex.Free                     => ()
        case locked @ Mutex.Locked(waiters) =>
          if !state.compareAndSet(locked, Mutex.Locked(waiters.filterNot(_ eq cb))) then loop()
    loop()

/** Constructors and internal state for [[Mutex]]. */
object Mutex:

  /** Internal lock state. */
  private sealed trait State

  /** Unlocked with no waiters. */
  private case object Free extends State

  /** Locked, holding the FIFO queue of pending acquirer callbacks. */
  private final case class Locked(waiters: Queue[Either[Throwable, Unit] => Unit]) extends State

  /** Creates a new, unlocked mutex. */
  def create: Fx[Mutex] = Fx.delay(new Mutex(new AtomicReference(Free)))
