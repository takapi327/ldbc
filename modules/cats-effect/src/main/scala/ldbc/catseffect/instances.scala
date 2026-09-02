/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.catseffect

import scala.concurrent.duration.FiniteDuration

import cats.effect.IO

import ldbc.effect.{ Concurrent, Fiber }

/**
 * The `ldbc.effect.Concurrent` instance for Cats Effect `IO`, built purely by delegating to cats-effect.
 *
 * This is what lets the effect-generic driver / net / pool run **natively** at `F = IO` — the whole
 * `DBIO` program is interpreted directly in `IO` with no cross-effect bridging (design §3, step 7). `map`
 * is delegated to `IO.map` (native) so the type-class layer never re-introduces the `flatMap + pure`
 * allocation, and `guarantee` / `timeout` use `IO`'s native combinators rather than the default
 * `bracket` / `race` derivations.
 */
given concurrentIO: Concurrent[IO] with
  override def pure[A](a:             A):                            IO[A] = IO.pure(a)
  override def flatMap[A, B](fa:      IO[A])(f: A => IO[B]):         IO[B] = fa.flatMap(f)
  override def map[A, B](fa:          IO[A])(f: A => B):             IO[B] = fa.map(f)
  override def raiseError[A](e:       Throwable):                    IO[A] = IO.raiseError(e)
  override def handleErrorWith[A](fa: IO[A])(f: Throwable => IO[A]): IO[A] = fa.handleErrorWith(f)
  override def delay[A](thunk:        => A):                         IO[A] = IO(thunk)
  override def blocking[A](thunk:     => A):                         IO[A] = IO.blocking(thunk)
  override def async[A](k: (Either[Throwable, A] => Unit) => IO[Option[IO[Unit]]]):    IO[A] = IO.async(k)
  override def bracket[A, B](acquire: IO[A])(use: A => IO[B])(release: A => IO[Unit]): IO[B] =
    acquire.bracket(use)(release)
  override def guarantee[A](fa:    IO[A])(fin: IO[Unit]): IO[A]              = fa.guarantee(fin)
  override def onCancel[A](fa:     IO[A])(fin: IO[Unit]): IO[A]              = fa.onCancel(fin)
  override def uncancelable[A](fa: IO[A]):                IO[A]              = IO.uncancelable(_ => fa)
  override def monotonic:                                 IO[FiniteDuration] = IO.monotonic
  override def realTime:                                  IO[FiniteDuration] = IO.realTime
  override def sleep(duration:     FiniteDuration):       IO[Unit]           = IO.sleep(duration)
  override def start[A](fa: IO[A]):                       IO[Fiber[IO, A]]   =
    fa.start.map { fib =>
      new Fiber[IO, A]:
        override def cancel: IO[Unit] = fib.cancel
        override def join:   IO[A]    = fib.joinWithNever
    }
  override def race[A, B](fa: IO[A], fb: IO[B]):                                      IO[Either[A, B]] = IO.race(fa, fb)
  override def timeout[A](fa: IO[A], after: FiniteDuration)(onTimeout: => Throwable): IO[A]            =
    fa.timeoutTo(after, IO.raiseError(onTimeout))
