/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

/**
 * A counting semaphore over [[Fx]] with FIFO fairness. [[acquire]] takes a permit, blocking (by
 * suspending, not by holding a thread) until one is available; [[release]] returns a permit, handing
 * it directly to the longest-waiting acquirer if any. Use [[withPermit]] to bracket an effect so the
 * permit is always returned, even on error or cancellation.
 *
 * @param state the permit count paired with the FIFO queue of waiters
 */
final class Semaphore private (state: Ref[Semaphore.State]):

  /** Acquires a single permit, suspending until one is available. */
  def acquire: Fx[Unit] =
    Deferred[Unit].flatMap { waiter =>
      val cleanup: Fx[Unit] =
        state
          .modify { s =>
            if s.waiters.exists(_ eq waiter) then (s.copy(waiters = s.waiters.filterNot(_ eq waiter)), Fx.unit)
            else (s.copy(permits = s.permits + 1), Fx.unit)
          }
          .flatMap(identity)
      state
        .modify { s =>
          if s.permits > 0 then (s.copy(permits = s.permits - 1), Fx.unit)
          else (s.copy(waiters = s.waiters :+ waiter), waiter.get.onCancel(cleanup))
        }
        .flatMap(identity)
    }

  /** Releases a single permit, handing it to the longest-waiting acquirer if any. */
  def release: Fx[Unit] =
    state
      .modify { s =>
        s.waiters match
          case head +: tail => (s.copy(waiters = tail), head.complete(()).map(_ => ()))
          case _            => (s.copy(permits = s.permits + 1), Fx.unit)
      }
      .flatMap(identity)

  /**
   * Runs `fa` while holding a permit, releasing it afterwards on success, error, and cancellation.
   *
   * @param fa the effect to run under a permit
   * @tparam A the result type
   */
  def withPermit[A](fa: Fx[A]): Fx[A] = Fx.bracket(acquire)(_ => fa)(_ => release)

object Semaphore:

  /** The mutable state of a [[Semaphore]]: available permits and the FIFO waiter queue. */
  private case class State(permits: Long, waiters: Vector[Deferred[Unit]])

  /**
   * Creates a semaphore with `permits` initial permits.
   *
   * @param permits the initial permit count (negative values are clamped to 0)
   * @return an effect producing the semaphore
   */
  def apply(permits: Long): Fx[Semaphore] =
    Ref.of(State(permits.max(0L), Vector.empty)).map(new Semaphore(_))
