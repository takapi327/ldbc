/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.{ Future, Promise }
import scala.concurrent.duration.*

/**
 * Cross-platform (JVM / JS / Native) tests for the platform [[FxRuntime]] behind `Fx.sleep`
 * (`scheduleOnce`) and `Fx.blocking` (`executeBlocking`). Tests are `Future`-based so they run on
 * the single-threaded JS runtime as well.
 */
class FxRuntimeTest extends munit.FunSuite:

  given scala.concurrent.ExecutionContext = munitExecutionContext

  private def toFuture[A](fx: Fx[A]): Future[A] =
    val promise = Promise[A]()
    fx.unsafeRun(result => promise.complete(result.toTry))
    promise.future

  test("scheduleOnce: sleep completes with unit") {
    toFuture(Fx.sleep(20.millis)).map(u => assertEquals(u, ()))
  }

  test("scheduleOnce: sleep then flatMap sequences") {
    toFuture(Fx.sleep(10.millis).flatMap(_ => Fx.pure(7))).map(v => assertEquals(v, 7))
  }

  test("executeBlocking: blocking returns the computed value") {
    toFuture(Fx.blocking(6 * 7)).map(v => assertEquals(v, 42))
  }

  test("executeBlocking: blocking failure propagates") {
    toFuture(Fx.blocking[Int](throw new RuntimeException("boom"))).failed.map(e => assertEquals(e.getMessage, "boom"))
  }
