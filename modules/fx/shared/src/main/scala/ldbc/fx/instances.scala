/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicReference }

import scala.concurrent.duration.FiniteDuration

import ldbc.effect.{ Concurrent, Fiber as EffectFiber }

/**
 * The `ldbc.effect.Concurrent` instance for [[Fx]] (design step 2). This is what keeps `Fx` as a
 * first-class effect after the tagless migration: the driver / pool, once written generically over
 * `F: Concurrent`, run natively at `F = Fx` (standalone use and the `Future` backend), and existing
 * `Fx` code stays green during the migration.
 *
 * Most operations delegate to their `Fx` counterparts. Two need bridging because `Fx`'s native async is
 * canceler-returning (`cb => Canceler`) rather than cats-effect-shaped (`cb => F[Option[F[Unit]]]`):
 *   - `async` runs the registration `k(cb)` on the current runtime and stores its optional finalizer as
 *     the canceler;
 *   - `race` (absent from `Fx`) is implemented with the same low-level "run both, first-to-finish wins,
 *     cancel the loser" pattern `Fx.timeout` uses.
 */
given concurrentFx: Concurrent[Fx] with
  def pure[A](a:                  A):                                         Fx[A] = Fx.pure(a)
  def flatMap[A, B](fa:           Fx[A])(f: A => Fx[B]):                      Fx[B] = fa.flatMap(f)
  override def map[A, B](fa:      Fx[A])(f: A => B):                          Fx[B] = fa.map(f)
  def raiseError[A](e:            Throwable):                                 Fx[A] = Fx.raiseError(e)
  def handleErrorWith[A](fa:      Fx[A])(f: Throwable => Fx[A]):              Fx[A] = fa.handleErrorWith(f)
  def delay[A](thunk:             => A):                                      Fx[A] = Fx.delay(thunk)
  override def blocking[A](thunk: => A):                                      Fx[A] = Fx.blocking(thunk)
  def bracket[A, B](acquire: Fx[A])(use: A => Fx[B])(release: A => Fx[Unit]): Fx[B] =
    Fx.bracket(acquire)(use)(release)
  def onCancel[A](fa:     Fx[A])(fin: Fx[Unit]): Fx[A]              = fa.onCancel(fin)
  def uncancelable[A](fa: Fx[A]):                Fx[A]              = Fx.uncancelable(fa)
  def monotonic:                                 Fx[FiniteDuration] = Fx.monotonic
  def realTime:                                  Fx[FiniteDuration] = Fx.realTime
  def sleep(duration:     FiniteDuration):       Fx[Unit]           = Fx.sleep(duration)
  override def timeout[A](fa: Fx[A], after: FiniteDuration)(onTimeout: => Throwable): Fx[A] =
    Fx.timeout(fa, after)(onTimeout)

  def start[A](fa: Fx[A]): Fx[EffectFiber[Fx, A]] =
    Fiber.start(fa).map { fx =>
      new EffectFiber[Fx, A]:
        def cancel: Fx[Unit] = fx.cancel
        def join:   Fx[A]    = fx.joinWithNever
    }

  def async[A](k: (Either[Throwable, A] => Unit) => Fx[Option[Fx[Unit]]]): Fx[A] =
    Fx.async[A] { cb =>
      given FxRuntime = FxRuntime.current
      val finRef      = new AtomicReference[Fx[Unit]](Fx.unit)
      k(cb).unsafeRun {
        case Right(opt) => opt.foreach(finRef.set)
        case Left(e)    => cb(Left(e))
      }
      new Fx.Canceler:
        override def cancel(): Unit = { finRef.get().unsafeRun(_ => ()); () }
    }

  def race[A, B](fa: Fx[A], fb: Fx[B]): Fx[Either[A, B]] =
    Fx.async[Either[A, B]] { cb =>
      given FxRuntime = FxRuntime.current
      val done        = new AtomicBoolean(false)
      val cancelA     = new AtomicReference[Fx.Canceler](Fx.Canceler.noop)
      val cancelB     = new AtomicReference[Fx.Canceler](Fx.Canceler.noop)
      def finish(r: Either[Throwable, Either[A, B]], other: AtomicReference[Fx.Canceler]): Unit =
        if done.compareAndSet(false, true) then { other.get().cancel(); cb(r) }
      cancelA.set(fa.unsafeRun(r => finish(r.map(Left(_)), cancelB)))
      cancelB.set(fb.unsafeRun(r => finish(r.map(Right(_)), cancelA)))
      new Fx.Canceler:
        override def cancel(): Unit = { cancelA.get().cancel(); cancelB.get().cancel() }
    }
