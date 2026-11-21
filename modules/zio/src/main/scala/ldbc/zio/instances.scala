/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.zio

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.{ MonadError, StackSafeMonad }

import ldbc.effect.{ Concurrent, Fiber }

import zio.{ Clock, Duration as ZDuration, Promise, Task, Unsafe, ZIO }

/**
 * A [[cats.MonadError]] instance for ZIO's `Task`, provided here (the consumer) rather than in the
 * effect-agnostic core so the core keeps no cats/ZIO coupling. `Free.foldMap` into `Kleisli[Task, J, *]`
 * inside the `DBIO` interpreter needs a `cats.Monad[Task]`; a full `MonadError` is supplied since `Task`
 * handles errors natively, and [[cats.StackSafeMonad]] derives a stack-safe `tailRecM` from ZIO's `flatMap`.
 */
given catsMonadErrorForTask: MonadError[Task, Throwable] =
  new MonadError[Task, Throwable] with StackSafeMonad[Task]:
    override def pure[A](a:             A):                                Task[A] = ZIO.succeed(a)
    override def flatMap[A, B](fa:      Task[A])(f: A => Task[B]):         Task[B] = fa.flatMap(f)
    override def raiseError[A](e:       Throwable):                        Task[A] = ZIO.fail(e)
    override def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.catchAll(f)

/**
 * The `ldbc.effect.Concurrent` instance for ZIO's `Task` (`ZIO[Any, Throwable, *]`), built purely by
 * delegating to ZIO's native combinators — no `zio-interop-cats` bridge.
 *
 * This is what lets the effect-generic driver / net / pool run **natively** on `Task`: the whole `DBIO`
 * program is interpreted directly in `Task`. Only [[async]] needs care, because `ldbc.effect.Async`'s
 * callback is effectful and yields an optional finalizer (cats-effect shape) whereas `ZIO.async`'s
 * registration is synchronous; it is bridged with a `Promise` completed from the callback and an
 * interruptible await that runs the finalizer on interruption.
 */
given concurrentTask: Concurrent[Task] with
  override def pure[A](a:             A):                                Task[A] = ZIO.succeed(a)
  override def flatMap[A, B](fa:      Task[A])(f: A => Task[B]):         Task[B] = fa.flatMap(f)
  override def map[A, B](fa:          Task[A])(f: A => B):               Task[B] = fa.map(f)
  override def raiseError[A](e:       Throwable):                        Task[A] = ZIO.fail(e)
  override def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.catchAll(f)
  override def delay[A](thunk:        => A):                             Task[A] = ZIO.attempt(thunk)
  override def blocking[A](thunk:     => A):                             Task[A] = ZIO.attemptBlocking(thunk)

  override def async[A](k: (Either[Throwable, A] => Unit) => Task[Option[Task[Unit]]]): Task[A] =
    ZIO.uninterruptibleMask { restore =>
      for
        promise <- Promise.make[Throwable, A]
        runtime <- ZIO.runtime[Any]
        cb = (either: Either[Throwable, A]) =>
               val complete = either.fold(promise.fail, promise.succeed)
               Unsafe.unsafe { implicit unsafe =>
                 val _ = runtime.unsafe.run(complete)
               }
        finalizer <- k(cb)
        result    <- restore(promise.await).onInterrupt(finalizer.getOrElse(ZIO.unit).orDie)
      yield result
    }

  override def bracket[A, B](acquire: Task[A])(use: A => Task[B])(release: A => Task[Unit]): Task[B] =
    ZIO.acquireReleaseWith(acquire)((a: A) => release(a).orDie)(use)

  override def guarantee[A](fa:    Task[A])(fin: Task[Unit]): Task[A] = fa.ensuring(fin.orDie)
  override def onCancel[A](fa:     Task[A])(fin: Task[Unit]): Task[A] = fa.onInterrupt(fin.orDie)
  override def uncancelable[A](fa: Task[A]):                  Task[A] = ZIO.uninterruptible(fa)

  override def monotonic: Task[FiniteDuration] = Clock.nanoTime.map(FiniteDuration(_, TimeUnit.NANOSECONDS))
  override def realTime:  Task[FiniteDuration] =
    Clock.currentTime(TimeUnit.MILLISECONDS).map(FiniteDuration(_, TimeUnit.MILLISECONDS))
  override def sleep(duration: FiniteDuration): Task[Unit] = ZIO.sleep(ZDuration.fromScala(duration))

  override def start[A](fa: Task[A]): Task[Fiber[Task, A]] =
    fa.fork.map { fiber =>
      new Fiber[Task, A]:
        override def cancel: Task[Unit] = fiber.interrupt.unit
        override def join:   Task[A]    = fiber.join
    }

  override def race[A, B](fa: Task[A], fb: Task[B]): Task[Either[A, B]] = fa.raceEither(fb)

  override def timeout[A](fa: Task[A], after: FiniteDuration)(onTimeout: => Throwable): Task[A] =
    fa.timeoutFail(onTimeout)(ZDuration.fromScala(after))
