/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

/**
 * The terminal state of a [[Fiber]]: it either produced a value, failed with an error, or was
 * cancelled before completing. Mirrors cats-effect's `Outcome`.
 *
 * @tparam A the result type of the fiber
 */
enum Outcome[+A]:
  /** The fiber completed successfully with `value`. */
  case Succeeded(value: A)

  /** The fiber failed with `error`. */
  case Errored(error: Throwable)

  /** The fiber was cancelled before completing. */
  case Canceled

/**
 * A handle to an [[Fx]] running concurrently in the background, started with [[Fx.start]]. It can be
 * awaited with [[join]] / [[joinWithNever]] or interrupted with [[cancel]].
 *
 * Cancellation is race-safe: [[cancel]] both interrupts the running effect and settles the fiber's
 * outcome, so a `join` never hangs waiting for an already-cancelled fiber. If the effect had already
 * completed, `cancel` is a no-op with respect to the outcome.
 *
 * @param result   the settled outcome of the background effect
 * @param canceler the handle that interrupts the background effect
 * @tparam A the result type
 */
final class Fiber[A] private[fx] (result: Deferred[Outcome[A]], canceler: Fx.Canceler):

  /** Awaits the fiber and returns its [[Outcome]] (never fails). */
  def join: Fx[Outcome[A]] = result.get

  /**
   * Awaits the fiber, returning its value or re-raising its error. If the fiber was cancelled this
   * never completes, matching cats-effect's `joinWithNever`.
   */
  def joinWithNever: Fx[A] = result.get.flatMap {
    case Outcome.Succeeded(a) => Fx.pure(a)
    case Outcome.Errored(e)   => Fx.raiseError(e)
    case Outcome.Canceled     => Fx.never
  }

  /**
   * Awaits the fiber, returning its value, re-raising its error, or running `onCancel` if it was
   * cancelled.
   *
   * @param onCancel the effect to run if the fiber was cancelled
   */
  def joinWith(onCancel: => Fx[A]): Fx[A] = result.get.flatMap {
    case Outcome.Succeeded(a) => Fx.pure(a)
    case Outcome.Errored(e)   => Fx.raiseError(e)
    case Outcome.Canceled     => onCancel
  }

  /**
   * Requests cancellation of the background effect and settles its outcome as
   * [[Outcome.Canceled]] (unless it had already completed). Idempotent.
   */
  def cancel: Fx[Unit] =
    Fx.delay(canceler.cancel()).flatMap(_ => result.complete(Outcome.Canceled)).map(_ => ())

object Fiber:

  /**
   * Starts `fa` running concurrently in the background and returns a [[Fiber]] handle to it.
   *
   * @param fa the effect to run concurrently
   * @tparam A the result type
   * @return an effect producing the fiber handle
   */
  def start[A](fa: Fx[A]): Fx[Fiber[A]] =
    Deferred[Outcome[A]].flatMap { result =>
      Fx.delay {
        val canceler =
          fa.map(a => Outcome.Succeeded(a): Outcome[A])
            .handleErrorWith(error => Fx.pure(Outcome.Errored(error)))
            .flatMap(outcome => result.complete(outcome))
            .map(_ => ())
            .unsafeRun(_ => ())
        new Fiber(result, canceler)
      }
    }
