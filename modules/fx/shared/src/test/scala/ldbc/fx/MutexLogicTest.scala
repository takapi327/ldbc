/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.duration.*

import ldbc.fx.syntax.*

/**
 * Cross-platform (JVM / JS / Native) tests for [[Mutex]]'s acquire/release logic, driven
 * cooperatively by fibers (no real threads) so they run on the single-threaded JS event loop. The
 * real-thread contention test (mutual exclusion under preemption) lives in the jvm-native `MutexTest`;
 * this suite verifies the ordering contract: a second acquirer waits until the first releases, and a
 * held critical section is never entered concurrently.
 */
class MutexLogicTest extends FxSuite:

  test("surround serializes: a second acquirer waits until the first releases") {
    assertFx(
      for
        m       <- Mutex.create
        gate    <- Deferred[Unit] // holder 1 stays in the critical section until this completes
        entered <- Ref.of(List.empty[String])
        f1      <- Fiber.start(m.surround(entered.update("in-1" :: _) >> gate.get >> entered.update("out-1" :: _)))
        // f1 now holds the lock, suspended on `gate.get`
        f2 <- Fiber.start(m.surround(entered.update("in-2" :: _)))
        // f2 is suspended waiting for the lock and must not have entered
        before <- entered.get
        _      <- gate.complete(()) // release holder 1 → the lock is handed to f2
        _      <- f1.joinWithNever
        _      <- f2.joinWithNever
        after  <- entered.get
      yield (before.reverse, after.reverse),
      (List("in-1"), List("in-1", "out-1", "in-2"))
    )
  }

  test("a second fiber cannot enter the critical section while the first holds it") {
    // `gate` keeps holder 1 *inside* the critical section (suspended) while holder 2 attempts entry,
    // so this observes genuine overlap: a broken mutex lets holder 2 in and `maxSeen` reaches 2.
    assertFx(
      for
        m       <- Mutex.create
        gate    <- Deferred[Unit]
        inside  <- Ref.of(0)
        maxSeen <- Ref.of(0)
        f1      <- Fiber.start(
                m.surround(
                  inside.updateAndGet(_ + 1).flatMap(n => maxSeen.update(mx => if n > mx then n else mx))
                    >> gate.get
                    >> inside.update(_ - 1)
                )
              )
        // f1 is now inside the critical section, suspended on `gate.get`, still holding the lock
        f2 <- Fiber.start(
                m.surround(
                  inside.updateAndGet(_ + 1).flatMap(n => maxSeen.update(mx => if n > mx then n else mx))
                    >> inside.update(_ - 1)
                )
              )
        _  <- gate.complete(()) // let f1 leave; only now may f2 enter
        _  <- f1.joinWithNever
        _  <- f2.joinWithNever
        mx <- maxSeen.get
      yield mx,
      1
    )
  }

  test("surround releases the lock when the body is cancelled") {
    assertFx(
      for
        m <- Mutex.create
        f <- Fiber.start(m.surround(Fx.never[Unit])) // takes the lock, then blocks
        _ <- f.cancel                                // bracket must release the lock on cancel
        r <- Fx.timeout(m.surround(Fx.pure("ok")), 3.seconds)(new RuntimeException("lock not released on cancel"))
      yield r,
      "ok"
    )
  }
