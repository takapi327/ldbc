/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

import scala.concurrent.{ Future, Promise }

/**
 * Cross-platform (JVM / JS / Native) tests for the core [[Fx]] semantics that do not depend on
 * real threads: stack safety, `async` call-once, and `bracket` release on success/error. Tests are
 * `Future`-based so they run on the single-threaded JS runtime as well. Thread-dependent behaviours
 * (cancel/complete races, blocking-pool identity) live in the JVM-only `FxConcurrencyTest`.
 */
class FxTest extends munit.FunSuite:

  given scala.concurrent.ExecutionContext = munitExecutionContext

  private def toFuture[A](fx: Fx[A]): Future[A] =
    val promise = Promise[A]()
    fx.unsafeRun(result => promise.complete(result.toTry))
    promise.future

  test("stack-safe: deep flatMap (1e6) does not overflow"):
    def deep(n: Int): Fx[Int] = if n == 0 then Fx.pure(0) else Fx.pure(n).flatMap(_ => deep(n - 1))
    toFuture(deep(1000000)).map(v => assertEquals(v, 0))

  test("stack-safe: deep error-unwind (1e6)"):
    def deep(n: Int): Fx[Int] =
      if n == 0 then Fx.raiseError(new RuntimeException("boom")) else Fx.pure(n).flatMap(_ => deep(n - 1))
    toFuture(deep(1000000).handleErrorWith(_ => Fx.pure(-1))).map(v => assertEquals(v, -1))

  test("async callback is call-once: first value wins and the continuation runs exactly once"):
    // The `.map` is the continuation after the async; counting its runs catches a broken call-once
    // that resumes the loop for every callback invocation (which the value alone would not reveal,
    // since a second `Promise.complete` is silently ignored).
    val runs = new AtomicInteger(0)
    val fx = Fx
      .async[Int] { cb => cb(Right(1)); cb(Right(2)); cb(Right(3)); Fx.Canceler.noop }
      .map { v => runs.incrementAndGet(); v }
    toFuture(fx).map { v =>
      assertEquals(v, 1, "the first callback value must win")
      assertEquals(runs.get(), 1, "the continuation after an async must run exactly once")
    }

  test("bracket releases on success"):
    val released = new AtomicBoolean(false)
    val fx       = Fx.bracket(Fx.pure("r"))(_ => Fx.pure(1))(_ => Fx.delay { released.set(true); () })
    toFuture(fx).map { v =>
      assertEquals(v, 1)
      assert(released.get())
    }

  test("bracket releases on error"):
    val released = new AtomicBoolean(false)
    val fx = Fx.bracket(Fx.pure("r"))(_ => Fx.raiseError[Int](new RuntimeException("x")))(_ =>
      Fx.delay { released.set(true); () }
    )
    toFuture(fx).failed.map(_ => assert(released.get()))
