/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

import scala.concurrent.duration.FiniteDuration

/**
 * ldbc's own minimal effect type-class hierarchy (`MonadThrow` ⊂ `Sync` ⊂ `Async` ⊂ `Temporal` ⊂
 * `Concurrent`), independent of cats-effect. The driver / net / pool are written generically against
 * these, and each effect (`IO` / `Task` / `Fx`) supplies an instance so it runs natively without any
 * cross-effect bridging. See `LDBC_EFFECT_TAGLESS_DESIGN.md`.
 *
 * The shapes deliberately mirror cats-effect 3 where it matters (notably `Async.async`, which is
 * signature-compatible with `cats.effect.Async.async`) so instances delegate directly.
 */

/** Sequencing plus error handling — the minimal monad. */
trait MonadThrow[F[_]]:
  def pure[A](a: A): F[A]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  /**
   * Default is `flatMap(a => pure(f(a)))`, which allocates. **Every instance must override `map` with the
   * effect's native implementation** (design §2.1) to avoid re-introducing the allocation the Fx `Map`
   * optimisation removed.
   */
  def map[A, B](fa: F[A])(f: A => B): F[B] = flatMap(fa)(a => pure(f(a)))

  def raiseError[A](e: Throwable): F[A]
  def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A]

  def unit: F[Unit]                            = pure(())
  def void[A](fa: F[A]): F[Unit]               = map(fa)(_ => ())
  def as[A, B](fa: F[A], b: B): F[B]           = map(fa)(_ => b)
  def productR[A, B](fa: F[A])(fb: F[B]): F[B] = flatMap(fa)(_ => fb)
  def flatTap[A, B](fa: F[A])(f: A => F[B]): F[A] = flatMap(fa)(a => map(f(a))(_ => a))
  def whenA(cond: Boolean)(fa: => F[Unit]): F[Unit] = if cond then fa else unit
  def attempt[A](fa: F[A]): F[Either[Throwable, A]] =
    handleErrorWith(map(fa)(Right(_): Either[Throwable, A]))(e => pure(Left(e)))

object MonadThrow:
  def apply[F[_]](using F: MonadThrow[F]): MonadThrow[F] = F

/** Suspension of synchronous side effects. */
trait Sync[F[_]] extends MonadThrow[F]:
  def delay[A](thunk: => A): F[A]
  def blocking[A](thunk: => A): F[A] = delay(thunk)

object Sync:
  def apply[F[_]](using F: Sync[F]): Sync[F] = F

/**
 * Asynchrony, resource-safety and cancellation masking. Network I/O and the concurrent primitives
 * require this.
 *
 * `async` is signature-compatible with `cats.effect.Async.async`: the registration returns
 * `F[Option[F[Unit]]]`, the `Some` case being the finalizer to run if cancelled while suspended.
 */
trait Async[F[_]] extends Sync[F]:
  def async[A](k: (Either[Throwable, A] => Unit) => F[Option[F[Unit]]]): F[A]
  def async_[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] =
    async(cb => map(delay(k(cb)))(_ => Option.empty[F[Unit]]))

  /**
   * Acquire → use → release. Release is guaranteed on success and error; on cancellation it is
   * best-effort per effect (IO/Task release, Future never cancels). See design §2.2.
   */
  def bracket[A, B](acquire: F[A])(use: A => F[B])(release: A => F[Unit]): F[B]

  /** Runs `fin` after `fa` on success, error, and (best-effort) cancellation. */
  def guarantee[A](fa: F[A])(fin: F[Unit]): F[A] = bracket(unit)(_ => fa)(_ => fin)

  /** Runs `fin` **only** if `fa` is cancelled. Needed by `Semaphore` to undo a queued waiter. */
  def onCancel[A](fa: F[A])(fin: F[Unit]): F[A]

  /** Runs `fa` as an uncancelable atomic region (poll-less). Required by `Exchange` (design §4.6). */
  def uncancelable[A](fa: F[A]): F[A]

  /** Monotonic clock (for timing; not wall-clock). */
  def monotonic: F[FiniteDuration]

  /** Wall-clock time. */
  def realTime: F[FiniteDuration]

object Async:
  def apply[F[_]](using F: Async[F]): Async[F] = F

/** Timers. `sleep` may be backed by an effect-agnostic scheduler; no native effect timer is required. */
trait Temporal[F[_]] extends Async[F]:
  def sleep(duration: FiniteDuration): F[Unit]

object Temporal:
  def apply[F[_]](using F: Temporal[F]): Temporal[F] = F

/** A running computation that can be cancelled or awaited. Far smaller than cats-effect's `Fiber`. */
trait Fiber[F[_], A]:
  def cancel: F[Unit]
  def join:   F[A]

/**
 * Concurrency: background fibers and racing. The pool (background tasks, `timeout`) and the driver
 * (handshake fiber, socket `timeout`) require this. **`Future` cannot satisfy it** (no cancellation),
 * so there is no `Concurrent[Future]`.
 */
trait Concurrent[F[_]] extends Temporal[F]:
  def start[A](fa: F[A]): F[Fiber[F, A]]
  def race[A, B](fa: F[A], fb: F[B]): F[Either[A, B]]

  /** Default: race the action against a timer; `race` cancels the loser. Instances may override. */
  def timeout[A](fa: F[A], after: FiniteDuration)(onTimeout: => Throwable): F[A] =
    flatMap(race(sleep(after), fa)) {
      case Right(a) => pure(a)
      case Left(_)  => raiseError(onTimeout)
    }

object Concurrent:
  def apply[F[_]](using F: Concurrent[F]): Concurrent[F] = F
