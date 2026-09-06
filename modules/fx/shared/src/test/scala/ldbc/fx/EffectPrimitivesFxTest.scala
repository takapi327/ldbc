/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

import ldbc.effect.{ Concurrent, Deferred, Ref, Resource, Semaphore }

/**
 * Step 2 check: the generic `ldbc.effect` primitives run at `F = Fx` through the `concurrentFx`
 * instance (in scope as a same-package top-level given).
 */
class EffectPrimitivesFxTest extends FxSuite:

  test("Ref at F=Fx") {
    val p =
      for
        r <- Ref.of[Fx, Int](0)
        _ <- r.update(_ + 1)
        b <- r.modify(a => (a + 10, a))
        v <- r.get
      yield (b, v)
    assertFx(p, (1, 11))
  }

  test("Deferred complete then get at F=Fx") {
    val p =
      for
        d <- Deferred[Fx, Int]
        _ <- d.complete(42)
        v <- d.get
      yield v
    assertFx(p, 42)
  }

  test("Deferred get suspends until a concurrent complete at F=Fx") {
    val p =
      for
        d   <- Deferred[Fx, String]
        fib <- Concurrent[Fx].start(Fx.sleep(30.millis).flatMap(_ => Concurrent[Fx].void(d.complete("ready"))))
        v   <- d.get
        _   <- fib.join
      yield v
    assertFx(p, "ready")
  }

  test("Semaphore withPermit at F=Fx (sequential)") {
    val p =
      for
        sem <- Semaphore[Fx](1)
        a   <- sem.withPermit(Fx.pure(1))
        b   <- sem.withPermit(Fx.pure(2))
      yield (a, b)
    assertFx(p, (1, 2))
  }

  test("Resource use releases in LIFO at F=Fx") {
    val log = new AtomicReference(List.empty[String])
    def res(name: String): Resource[Fx, Unit] =
      Resource.make(Fx.delay { log.updateAndGet(s"acq:$name" :: _); () })(_ =>
        Fx.delay { log.updateAndGet(s"rel:$name" :: _); () }
      )
    val p =
      res("a")
        .flatMap(_ => res("b"))
        .use(_ => Fx.delay { log.updateAndGet("use" :: _); () })
        .flatMap(_ => Fx.delay(log.get().reverse))
    assertFx(p, List("acq:a", "acq:b", "use", "rel:b", "rel:a"))
  }
