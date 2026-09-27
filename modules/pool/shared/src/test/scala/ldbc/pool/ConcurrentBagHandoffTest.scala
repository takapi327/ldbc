/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.Ref as EffectRef
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

/**
 * Covers the two windows a borrower passes through between announcing itself and parking.
 *
 * `borrow` raises the waiter count first and only enqueues a handoff slot once its non-blocking
 * retries are exhausted, roughly 7ms later. Anything that consults the waiter count in between sees a
 * waiter it cannot reach, which is where both an arriving item and a shutdown can go missing.
 */
class ConcurrentBagHandoffTest extends FxSuite:

  private final class Entry(val id: String, state: EffectRef[Fx, Int]) extends BagEntry[Fx]:
    override def getState:                                Fx[Int]     = state.get
    override def setState(newState: Int):                 Fx[Unit]    = state.set(newState)
    override def compareAndSet(expect: Int, update: Int): Fx[Boolean] =
      state.modify(current => if current == expect then (update, true) else (current, false))

  private def entry(id: String): Fx[Entry] =
    EffectRef.of[Fx, Int](BagEntry.STATE_NOT_IN_USE).map(new Entry(id, _))

  test("an item added while a borrower is announced but not yet parked is not stranded") {
    for
      bag  <- ConcurrentBag[Fx, Entry]()
      item <- entry("added-in-the-window")
      borrower <- bag.borrow(5.seconds).start
      _        <- Fx.sleep(2.millis)
      waiting  <- bag.waiting
      _        <- bag.add(item)
      borrowed <- borrower.joinWithNever
      state    <- item.getState
      leftover <- bag.tryBorrow
    yield
      assertEquals(waiting, 1, "the borrower had announced itself before the item was added")
      assert(
        borrowed.exists(_.id == item.id) || leftover.exists(_.id == item.id),
        "the item reached neither the borrower nor the shared list, so nothing can ever use it"
      )
      assert(
        state != BagEntry.STATE_IN_USE || borrowed.isDefined,
        s"the item was left marked as in use with no holder (state: $state)"
      )
  }

  test("closing the bag wakes a borrower that has already announced itself") {
    for
      bag      <- ConcurrentBag[Fx, Entry]()
      borrower <- bag.borrow(30.seconds).start
      _        <- Fx.sleep(2.millis)
      _        <- bag.close
      start    <- Fx.monotonic
      borrowed <- borrower.joinWithNever
      elapsed  <- Fx.monotonic.map(_ - start)
    yield
      assertEquals(borrowed, None)
      assert(elapsed < 5.seconds, s"the borrower waited out its timeout instead of being woken ($elapsed)")
  }

  test("closing the bag wakes a borrower that is already parked") {
    for
      bag      <- ConcurrentBag[Fx, Entry]()
      borrower <- bag.borrow(30.seconds).start
      _        <- Fx.sleep(50.millis)
      _        <- bag.close
      start    <- Fx.monotonic
      borrowed <- borrower.joinWithNever
      elapsed  <- Fx.monotonic.map(_ - start)
    yield
      assertEquals(borrowed, None)
      assert(elapsed < 5.seconds, s"the borrower waited out its timeout instead of being woken ($elapsed)")
  }

  test("tryBorrow takes an available item and reports emptiness without waiting") {
    for
      bag   <- ConcurrentBag[Fx, Entry]()
      empty <- bag.tryBorrow
      item  <- entry("available")
      _     <- bag.add(item)
      taken <- bag.tryBorrow
      again <- bag.tryBorrow
    yield
      assertEquals(empty, None)
      assertEquals(taken.map(_.id), Some("available"))
      assertEquals(again, None, "the item was already taken")
  }

  test("waiting reports the number of announced borrowers") {
    for
      bag    <- ConcurrentBag[Fx, Entry]()
      idle   <- bag.waiting
      first  <- bag.borrow(1.second).start
      second <- bag.borrow(1.second).start
      _      <- Fx.sleep(20.millis)
      parked <- bag.waiting
      _      <- first.joinWithNever
      _      <- second.joinWithNever
      after  <- bag.waiting
    yield
      assertEquals(idle, 0)
      assertEquals(parked, 2)
      assertEquals(after, 0)
  }
