/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

import java.util.concurrent.atomic.AtomicReference

/**
 * A write-once, read-many asynchronous value, the effect-agnostic counterpart of `cats.effect.Deferred`.
 * [[get]] suspends until [[complete]] (or resumes immediately if already completed); [[complete]] succeeds
 * at most once and wakes every pending getter. Generalised from `ldbc.fx.Deferred` over `Async[F]`: the
 * `get` suspension uses the generic `async` finalizer (`Some(remove(cb))`) to deregister on cancellation.
 *
 * @tparam F the effect type
 * @tparam A the deferred value type
 */
final class Deferred[F[_], A] private (state: AtomicReference[Deferred.State[A]])(using F: Async[F]):

  def get: F[A] = F.async { cb =>
    F.delay {
      @annotation.tailrec
      def loop(): Option[F[Unit]] =
        state.get match
          case Deferred.Done(value) =>
            cb(Right(value)); Option.empty[F[Unit]]
          case w @ Deferred.Waiting(cbs) =>
            if state.compareAndSet(w, Deferred.Waiting(cb :: cbs)) then Some(F.delay(remove(cb)))
            else loop()
      loop()
    }
  }

  def complete(a: A): F[Boolean] = F.delay {
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

  def tryGet: F[Option[A]] = F.delay {
    state.get match
      case Deferred.Done(value)   => Some(value)
      case _: Deferred.Waiting[A] => None
  }

  private def remove(cb: Either[Throwable, A] => Unit): Unit =
    @annotation.tailrec
    def loop(): Unit =
      state.get match
        case _: Deferred.Done[A]       => ()
        case w @ Deferred.Waiting(cbs) =>
          if !state.compareAndSet(w, Deferred.Waiting(cbs.filterNot(_ eq cb))) then loop()
    loop()

object Deferred:

  private sealed trait State[A]
  private final case class Waiting[A](cbs: List[Either[Throwable, A] => Unit]) extends State[A]
  private final case class Done[A](value: A)                                   extends State[A]

  def apply[F[_], A](using F: Async[F]): F[Deferred[F, A]] =
    F.delay(new Deferred(new AtomicReference(Waiting[A](Nil))))
