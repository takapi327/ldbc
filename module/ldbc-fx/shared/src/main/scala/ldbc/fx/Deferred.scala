/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.AtomicReference

/**
 * A write-once, read-many asynchronous value, the effect-agnostic counterpart of
 * `cats.effect.Deferred`. [[get]] suspends until [[complete]] is called (or returns immediately if
 * already completed); [[complete]] succeeds at most once and wakes every pending getter. Backed by
 * an [[java.util.concurrent.atomic.AtomicReference]] state machine so completion may be observed
 * from any thread.
 *
 * @tparam A the type of the deferred value
 */
final class Deferred[A] private (private val state: AtomicReference[Deferred.State[A]]):

  /**
   * Suspends until the value is available. If already completed, resumes immediately; otherwise the
   * caller is registered as a waiter and the returned cancellation removes that registration.
   */
  def get: Fx[A] = Fx.async { cb =>
    @annotation.tailrec
    def loop(): Fx.Canceler =
      state.get match
        case Deferred.Done(value) =>
          cb(Right(value))
          Fx.Canceler.noop
        case w @ Deferred.Waiting(cbs) =>
          if state.compareAndSet(w, Deferred.Waiting(cb :: cbs)) then
            new Fx.Canceler { override def cancel(): Unit = remove(cb) }
          else loop()
    loop()
  }

  /**
   * Completes the value with `a`, waking all pending getters. Returns `true` if this call set the
   * value, or `false` if it was already completed.
   *
   * @param a the value to publish
   */
  def complete(a: A): Fx[Boolean] = Fx.delay {
    @annotation.tailrec
    def loop(): Boolean =
      state.get match
        case Deferred.Done(_)          => false
        case w @ Deferred.Waiting(cbs) =>
          if state.compareAndSet(w, Deferred.Done(a)) then
            cbs.foreach(cb => cb(Right(a)))
            true
          else loop()
    loop()
  }

  /** Returns the value if already completed, or `None` otherwise, without suspending. */
  def tryGet: Fx[Option[A]] = Fx.delay {
    state.get match
      case Deferred.Done(value)   => Some(value)
      case _: Deferred.Waiting[A] => None
  }

  /** Removes a still-pending waiter callback (used by the cancellation of [[get]]). */
  private def remove(cb: Either[Throwable, A] => Unit): Unit =
    @annotation.tailrec
    def loop(): Unit =
      state.get match
        case _: Deferred.Done[A]       => ()
        case w @ Deferred.Waiting(cbs) =>
          if !state.compareAndSet(w, Deferred.Waiting(cbs.filterNot(_ eq cb))) then loop()
    loop()

/** Constructors for [[Deferred]]. */
object Deferred:

  /** Internal completion state of a [[Deferred]]. */
  private sealed trait State[A]

  /** The not-yet-completed state, holding the list of pending getter callbacks. */
  private final case class Waiting[A](cbs: List[Either[Throwable, A] => Unit]) extends State[A]

  /** The completed state, holding the published value. */
  private final case class Done[A](value: A) extends State[A]

  /**
   * Creates a new, empty [[Deferred]].
   *
   * @tparam A the type of the deferred value
   */
  def apply[A]: Fx[Deferred[A]] = Fx.delay(new Deferred(new AtomicReference(Waiting[A](Nil))))
