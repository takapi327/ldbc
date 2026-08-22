/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import cats.effect.IO

import munit.CatsEffectSuite

/** Verifies the `ldbc.effect` primitives at `F = IO` via an in-test `Concurrent[IO]` instance. */
class PrimitivesTest extends CatsEffectSuite:

  private given Concurrent[IO] with
    def pure[A](a: A): IO[A]                                     = IO.pure(a)
    def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B]           = fa.flatMap(f)
    override def map[A, B](fa: IO[A])(f: A => B): IO[B]          = fa.map(f)
    def raiseError[A](e: Throwable): IO[A]                       = IO.raiseError(e)
    def handleErrorWith[A](fa: IO[A])(f: Throwable => IO[A]): IO[A] = fa.handleErrorWith(f)
    def delay[A](thunk: => A): IO[A]                            = IO(thunk)
    override def blocking[A](thunk: => A): IO[A]                = IO.blocking(thunk)
    def async[A](k: (Either[Throwable, A] => Unit) => IO[Option[IO[Unit]]]): IO[A] = IO.async(k)
    def bracket[A, B](acquire: IO[A])(use: A => IO[B])(release: A => IO[Unit]): IO[B] =
      acquire.bracket(use)(release)
    def onCancel[A](fa: IO[A])(fin: IO[Unit]): IO[A]            = fa.onCancel(fin)
    def uncancelable[A](fa: IO[A]): IO[A]                       = IO.uncancelable(_ => fa)
    def monotonic: IO[FiniteDuration]                          = IO.monotonic
    def realTime:  IO[FiniteDuration]                          = IO.realTime
    def sleep(duration: FiniteDuration): IO[Unit]              = IO.sleep(duration)
    def start[A](fa: IO[A]): IO[Fiber[IO, A]] =
      fa.start.map(fib =>
        new Fiber[IO, A]:
          def cancel: IO[Unit] = fib.cancel
          def join:   IO[A]    = fib.joinWithNever
      )
    def race[A, B](fa: IO[A], fb: IO[B]): IO[Either[A, B]] = IO.race(fa, fb)

  test("Ref: update / modify are atomic") {
    val p =
      for
        ref <- Ref.of[IO, Int](0)
        _   <- ref.update(_ + 1)
        b   <- ref.modify(a => (a + 10, a))
        v   <- ref.get
      yield (b, v)
    assertIO(p, (1, 11))
  }

  test("Deferred: complete then get resolves") {
    val p =
      for
        d  <- Deferred[IO, Int]
        ok <- d.complete(42)
        v  <- d.get
        t  <- d.tryGet
      yield (ok, v, t)
    assertIO(p, (true, 42, Some(42)))
  }

  test("Deferred: get suspends until a concurrent complete") {
    val p =
      for
        d     <- Deferred[IO, String]
        fiber <- Concurrent[IO].start(IO.sleep(30.millis) *> d.complete("ready").void)
        v     <- d.get // suspends until the fiber completes it
        _     <- fiber.join
      yield v
    assertIO(p, "ready")
  }

  test("Semaphore(1): withPermit serialises — max concurrency observed is 1") {
    val inFlight = new AtomicReference(0)
    val maxSeen  = new AtomicReference(0)
    def crit: IO[Unit] =
      IO {
        val n = inFlight.updateAndGet(_ + 1)
        maxSeen.updateAndGet(m => math.max(m, n))
      } *> IO.sleep(10.millis) *> IO(inFlight.updateAndGet(_ - 1)).void
    val p =
      for
        sem <- Semaphore[IO](1)
        _   <- List.fill(5)(sem.withPermit(crit)).parSequence_
      yield maxSeen.get()
    assertIO(p, 1)
  }

  test("Resource: use releases after the body (LIFO for nested)") {
    val log = new AtomicReference(List.empty[String])
    def res(name: String): Resource[IO, Unit] =
      Resource.make(IO(log.updateAndGet(s"acq:$name" :: _)).void)(_ => IO(log.updateAndGet(s"rel:$name" :: _)).void)
    val p =
      (res("a").flatMap(_ => res("b"))).use(_ => IO(log.updateAndGet("use" :: _)).void) *>
        IO(log.get().reverse)
    assertIO(p, List("acq:a", "acq:b", "use", "rel:b", "rel:a"))
  }
