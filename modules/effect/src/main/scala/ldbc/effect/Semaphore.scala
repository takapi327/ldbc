/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

/**
 * A counting semaphore with FIFO fairness, the effect-agnostic counterpart of `cats.effect.Semaphore`.
 * [[acquire]] takes a permit, suspending (not blocking a thread) until one is available; [[release]] hands
 * a permit to the longest-waiting acquirer if any. [[withPermit]] brackets an effect so the permit is
 * always returned. Generalised from `ldbc.fx.Semaphore` over `Async[F]` (uses `onCancel` to undo a queued
 * waiter when a suspended `acquire` is cancelled).
 *
 * @tparam F the effect type
 */
final class Semaphore[F[_]] private (stateRef: Ref[F, Semaphore.State[F]])(using F: Async[F]):

  def acquire: F[Unit] =
    F.flatMap(Deferred[F, Unit]) { waiter =>
      val cleanup: F[Unit] =
        F.flatMap(stateRef.modify { s =>
          if s.waiters.exists(_ eq waiter) then (s.copy(waiters = s.waiters.filterNot(_ eq waiter)), F.unit)
          else (s.copy(permits = s.permits + 1), F.unit)
        })(identity)
      F.flatMap(stateRef.modify { s =>
        if s.permits > 0 then (s.copy(permits = s.permits - 1), F.unit)
        else (s.copy(waiters = s.waiters :+ waiter), F.onCancel(waiter.get)(cleanup))
      })(identity)
    }

  def release: F[Unit] =
    F.flatMap(stateRef.modify { s =>
      s.waiters match
        case head +: tail => (s.copy(waiters = tail), F.map(head.complete(()))(_ => ()))
        case _            => (s.copy(permits = s.permits + 1), F.unit)
    })(identity)

  def withPermit[A](fa: F[A]): F[A] = F.bracket(acquire)(_ => fa)(_ => release)

object Semaphore:

  private case class State[F[_]](permits: Long, waiters: Vector[Deferred[F, Unit]])

  def apply[F[_]](permits: Long)(using F: Async[F]): F[Semaphore[F]] =
    F.map(Ref.of[F, State[F]](State(permits.max(0L), Vector.empty)))(new Semaphore(_))
