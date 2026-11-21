/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.Ref
import ldbc.fx.concurrentFx
import ldbc.fx.syntax.*
import ldbc.fx.Fx
import ldbc.fx.FxSuite

class ConcurrentBagTest extends FxSuite:

  final class TestBagEntry(val id: String, stateRef: Ref[Fx, Int]) extends BagEntry[Fx]:
    override def getState:                                Fx[Int]     = stateRef.get
    override def setState(state: Int):                    Fx[Unit]    = stateRef.set(state)
    override def compareAndSet(expect: Int, update: Int): Fx[Boolean] =
      stateRef.modify(current => if current == expect then (update, true) else (current, false))

  def createTestEntry(id: String): Fx[TestBagEntry] =
    Ref.of(BagEntry.STATE_NOT_IN_USE).map(new TestBagEntry(id, _))

  test("ConcurrentBag should add and borrow items") {
    for
      bag       <- ConcurrentBag[Fx, TestBagEntry]()
      entry1    <- createTestEntry("item1")
      entry2    <- createTestEntry("item2")
      _         <- bag.add(entry1)
      _         <- bag.add(entry2)
      size1     <- bag.size
      borrowed1 <- bag.borrow(1.second)
      borrowed2 <- bag.borrow(1.second)
      borrowed3 <- bag.borrow(100.millis)
      state1    <- entry1.getState
      state2    <- entry2.getState
    yield
      assertEquals(size1, 2)
      assert(borrowed1.isDefined)
      assertEquals(borrowed1.get.id, "item2")
      assert(borrowed2.isDefined)
      assertEquals(borrowed2.get.id, "item1")
      assert(borrowed3.isEmpty)
      assertEquals(state1, BagEntry.STATE_IN_USE)
      assertEquals(state2, BagEntry.STATE_IN_USE)
  }

  test("ConcurrentBag should requite items back to bag") {
    for
      bag       <- ConcurrentBag[Fx, TestBagEntry]()
      entry     <- createTestEntry("item1")
      _         <- bag.add(entry)
      borrowed  <- bag.borrow(1.second)
      _         <- bag.requite(borrowed.get)
      borrowed2 <- bag.borrow(1.second)
      state     <- entry.getState
    yield
      assert(borrowed.isDefined)
      assert(borrowed2.isDefined)
      assertEquals(borrowed2.get.id, "item1")
      assertEquals(state, BagEntry.STATE_IN_USE)
  }

  test("ConcurrentBag should handle concurrent borrow and requite") {
    for
      bag     <- ConcurrentBag[Fx, TestBagEntry]()
      entries <- (1 to 10).toList.traverse(i => createTestEntry(s"item$i"))
      _       <- entries.traverse_(bag.add)
      _       <- (1 to 100).toList.parTraverse { _ =>
             bag.borrow(1.second).flatMap {
               case Some(item) => Fx.sleep(10.millis) >> bag.requite(item)
               case None       => Fx.raiseError(new Exception("Failed to borrow"))
             }
           }
      finalSize   <- bag.size
      allBorrowed <- entries.traverse(_ => bag.borrow(1.second))
    yield
      assertEquals(finalSize, 10)
      assert(allBorrowed.forall(_.isDefined))
  }

  test("ConcurrentBag should timeout when no items available") {
    for
      bag       <- ConcurrentBag[Fx, TestBagEntry]()
      startTime <- Fx.realTime
      result    <- bag.borrow(500.millis)
      endTime   <- Fx.realTime
      elapsed = endTime - startTime
    yield
      assert(result.isEmpty)
      assert(elapsed >= 500.millis)
      assert(elapsed < 1500.millis)
  }

  test("ConcurrentBag should remove items") {
    for
      bag      <- ConcurrentBag[Fx, TestBagEntry]()
      entry1   <- createTestEntry("item1")
      entry2   <- createTestEntry("item2")
      _        <- bag.add(entry1)
      _        <- bag.add(entry2)
      removed  <- bag.remove(entry1)
      size     <- bag.size
      borrowed <- bag.borrow(1.second)
      state1   <- entry1.getState
    yield
      assert(removed)
      assertEquals(size, 1)
      assert(borrowed.isDefined)
      assertEquals(borrowed.get.id, "item2")
      assertEquals(state1, BagEntry.STATE_REMOVED)
  }

  test("ConcurrentBag should handle remove while item is in use") {
    for
      bag       <- ConcurrentBag[Fx, TestBagEntry]()
      entry     <- createTestEntry("item1")
      _         <- bag.add(entry)
      borrowed  <- bag.borrow(1.second)
      removed   <- bag.remove(entry)
      state     <- entry.getState
      _         <- bag.requite(entry)
      borrowed2 <- bag.borrow(100.millis)
    yield
      assert(borrowed.isDefined)
      assert(removed)
      assertEquals(state, BagEntry.STATE_REMOVED)
      assert(borrowed2.isEmpty)
  }

  test("ConcurrentBag should handle close operation") {
    for
      bag      <- ConcurrentBag[Fx, TestBagEntry]()
      entry    <- createTestEntry("item1")
      _        <- bag.add(entry)
      _        <- bag.close
      borrowed <- bag.borrow(100.millis)
      entry2   <- createTestEntry("item2")
      _        <- bag.add(entry2)
      size     <- bag.size
    yield
      assert(borrowed.isEmpty)
      assertEquals(size, 1)
  }

  test("ConcurrentBag should handle waiting fibers") {
    for
      bag         <- ConcurrentBag[Fx, TestBagEntry]()
      entry       <- createTestEntry("item1")
      waiterFiber <- bag.borrow(5.seconds).start
      _           <- Fx.sleep(100.millis)
      _           <- bag.add(entry)
      result      <- waiterFiber.joinWithNever
    yield
      assert(result.isDefined)
      assertEquals(result.get.id, "item1")
  }

  test("ConcurrentBag should handle concurrent waiters") {
    for
      bag          <- ConcurrentBag[Fx, TestBagEntry]()
      waiterFibers <- (1 to 5).toList.traverse(_ => bag.borrow(5.seconds).start)
      _            <- Fx.sleep(100.millis)
      entries      <- (1 to 5).toList.traverse(i => createTestEntry(s"item$i"))
      _            <- entries.traverse_(entry => bag.add(entry) >> Fx.sleep(50.millis))
      results      <- waiterFibers.traverse(_.joinWithNever)
      states       <- entries.traverse(_.getState)
    yield
      assert(results.forall(_.isDefined))
      assert(states.forall(_ == BagEntry.STATE_IN_USE))
  }

  test("ConcurrentBag should handle requite of items not in shared list") {
    for
      bag       <- ConcurrentBag[Fx, TestBagEntry]()
      entry1    <- createTestEntry("item1")
      entry2    <- createTestEntry("item2")
      _         <- bag.add(entry1)
      _         <- bag.requite(entry2)
      size      <- bag.size
      borrowed1 <- bag.borrow(1.second)
      borrowed2 <- bag.borrow(1.second)
      ids = Set(borrowed1.get.id, borrowed2.get.id)
    yield
      assertEquals(size, 2)
      assert(borrowed1.isDefined)
      assert(borrowed2.isDefined)
      assertEquals(ids, Set("item1", "item2"))
  }

  test("ConcurrentBag should handle direct handoff to waiters") {
    for
      bag         <- ConcurrentBag[Fx, TestBagEntry]()
      entry       <- createTestEntry("item1")
      _           <- bag.add(entry)
      borrowed    <- bag.borrow(1.second)
      waiterFiber <- bag.borrow(5.seconds).start
      _           <- Fx.sleep(100.millis)
      _           <- bag.requite(entry)
      result      <- waiterFiber.joinWithNever
      size        <- bag.size
    yield
      assert(borrowed.isDefined)
      assert(result.isDefined)
      assertEquals(result.get.id, "item1")
      assertEquals(size, 1)
  }
