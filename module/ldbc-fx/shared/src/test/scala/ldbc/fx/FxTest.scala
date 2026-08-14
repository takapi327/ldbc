/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.*

import ldbc.fx.syntax.*

/**
 * Cross-platform (JVM / JS / Native) tests for the core [[Fx]] semantics that do not depend on
 * real threads: stack safety, `async` call-once, `bracket` release on success/error and nested LIFO
 * order on cancel, `onCancel` running its finalizer only on cancellation, and error propagation (a
 * throwing handler / `flatMap`, `timeout`, the parallel combinators, and a failing `bracket`
 * release). Tests are `Future`-based so they run on the single-threaded JS runtime as well.
 * Thread-dependent behaviours (cancel/complete races, blocking-pool identity) live in the JVM-only
 * `FxConcurrencyTest`.
 */
class FxTest extends munit.FunSuite:

  given scala.concurrent.ExecutionContext = munitExecutionContext

  private def toFuture[A](fx: Fx[A]): Future[A] =
    val promise = Promise[A]()
    fx.unsafeRun(result => promise.complete(result.toTry))
    promise.future

  test("stack-safe: deep flatMap (1e6) does not overflow") {
    def deep(n: Int): Fx[Int] = if n == 0 then Fx.pure(0) else Fx.pure(n).flatMap(_ => deep(n - 1))
    toFuture(deep(1000000)).map(v => assertEquals(v, 0))
  }

  test("stack-safe: deep error-unwind (1e6)") {
    def deep(n: Int): Fx[Int] =
      if n == 0 then Fx.raiseError(new RuntimeException("boom")) else Fx.pure(n).flatMap(_ => deep(n - 1))
    toFuture(deep(1000000).handleErrorWith(_ => Fx.pure(-1))).map(v => assertEquals(v, -1))
  }

  test("async callback is call-once: first value wins and the continuation runs exactly once") {
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
  }

  test("bracket releases on success") {
    val released = new AtomicBoolean(false)
    val fx       = Fx.bracket(Fx.pure("r"))(_ => Fx.pure(1))(_ => Fx.delay { released.set(true); () })
    toFuture(fx).map { v =>
      assertEquals(v, 1)
      assert(released.get())
    }
  }

  test("bracket releases on error") {
    val released = new AtomicBoolean(false)
    val fx = Fx.bracket(Fx.pure("r"))(_ => Fx.raiseError[Int](new RuntimeException("x")))(_ =>
      Fx.delay { released.set(true); () }
    )
    toFuture(fx).failed.map(_ => assert(released.get()))
  }

  test("nested bracket finalizers run in LIFO order on cancel") {
    // On cancel the finalizers must unwind inner-before-outer, matching normal unwinding.
    val order = ListBuffer.empty[String]
    val program: Fx[Unit] =
      Fx.bracket(Fx.pure("outer"))(_ =>
        Fx.bracket(Fx.pure("inner"))(_ => Fx.async[Unit](_ => Fx.Canceler.noop))(_ => Fx.delay { order += "inner"; () })
      )(_ => Fx.delay { order += "outer"; () })
    val canceler = program.unsafeRun(_ => ())
    canceler.cancel()
    assertEquals(order.toList, List("inner", "outer"))
  }

  test("onCancel runs the finalizer when the effect is cancelled") {
    val ran     = new AtomicBoolean(false)
    val program = Fiber.start(Fx.never[Unit].onCancel(Fx.delay { ran.set(true); () })).flatMap(_.cancel)
    toFuture(program).map(_ => assert(ran.get()))
  }

  test("onCancel does not run the finalizer on success") {
    val ran = new AtomicBoolean(false)
    toFuture(Fx.pure(42).onCancel(Fx.delay { ran.set(true); () })).map { v =>
      assertEquals(v, 42)
      assert(!ran.get())
    }
  }

  test("onCancel does not run the finalizer on error") {
    val ran = new AtomicBoolean(false)
    val fx  = Fx.raiseError[Int](new RuntimeException("boom")).onCancel(Fx.delay { ran.set(true); () })
    toFuture(fx).failed.map(_ => assert(!ran.get()))
  }

  test("handleErrorWith surfaces an error thrown by the handler itself") {
    val bang = new RuntimeException("bang")
    toFuture(Fx.raiseError[Int](new RuntimeException("boom")).handleErrorWith(_ => throw bang)).failed
      .map(e => assertEquals(e.getMessage, "bang"))
  }

  test("a thrown exception inside flatMap is captured as an error, not propagated") {
    toFuture(Fx.pure(1).flatMap[Int](_ => throw new RuntimeException("boom"))).failed
      .map(e => assertEquals(e.getMessage, "boom"))
  }

  test("timeout propagates the wrapped effect's own error when it fails before the deadline") {
    toFuture(Fx.timeout(Fx.raiseError[Int](new RuntimeException("boom")), 10.seconds)(new RuntimeException("timeout"))).failed
      .map(e => assertEquals(e.getMessage, "boom"))
  }

  test("parTupled fails if either side fails") {
    toFuture((Fx.pure(1), Fx.raiseError[Int](new RuntimeException("boom"))).parTupled).failed
      .map(e => assertEquals(e.getMessage, "boom"))
  }

  test("parTraverse fails if any element fails") {
    toFuture(List(1, 2, 3).parTraverse(n => if n == 2 then Fx.raiseError[Int](new RuntimeException("boom")) else Fx.pure(n))).failed
      .map(e => assertEquals(e.getMessage, "boom"))
  }

  test("bracket surfaces an error raised by release on the success path") {
    toFuture(Fx.bracket(Fx.pure("r"))(_ => Fx.pure(1))(_ => Fx.raiseError[Unit](new RuntimeException("release-failed")))).failed
      .map(e => assertEquals(e.getMessage, "release-failed"))
  }
