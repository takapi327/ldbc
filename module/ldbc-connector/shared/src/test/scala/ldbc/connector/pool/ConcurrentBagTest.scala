/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.*

import munit.CatsEffectSuite

class ConcurrentBagTest extends CatsEffectSuite:

  // Test implementation of BagEntry
  case class TestBagEntry[F[_]: Sync](id: String, stateRef: Ref[F, Int]) extends BagEntry[F]:
    def getState:                                F[Int]     = stateRef.get
    def setState(state: Int):                    F[Unit]    = stateRef.set(state)
    def compareAndSet(expect: Int, update: Int): F[Boolean] =
      stateRef.modify { current =>
        if current == expect then (update, true)
        else (current, false)
      }

  def createTestEntry[F[_]: Sync](id: String): F[TestBagEntry[F]] =
    Ref.of[F, Int](BagEntry.STATE_NOT_IN_USE).map(TestBagEntry(id, _))

  test("ConcurrentBag should add and borrow items") {
    for {
      bag    <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry1 <- createTestEntry[IO]("item1")
      entry2 <- createTestEntry[IO]("item2")

      // Add items
      _ <- bag.add(entry1)
      _ <- bag.add(entry2)

      // Check size
      size1 <- bag.size

      // Borrow items
      borrowed1 <- bag.borrow(1.second)
      borrowed2 <- bag.borrow(1.second)

      // Bag should be empty now
      borrowed3 <- bag.borrow(100.millis)

      // Check states
      state1 <- entry1.getState
      state2 <- entry2.getState
    } yield {
      assertEquals(size1, 2)
      assert(borrowed1.isDefined)
      assertEquals(borrowed1.get.id, "item2") // LIFO order
      assert(borrowed2.isDefined)
      assertEquals(borrowed2.get.id, "item1")
      assert(borrowed3.isEmpty)
      assertEquals(state1, BagEntry.STATE_IN_USE)
      assertEquals(state2, BagEntry.STATE_IN_USE)
    }
  }

  test("ConcurrentBag should requite items back to bag") {
    for {
      bag   <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry <- createTestEntry[IO]("item1")

      // Add and borrow
      _        <- bag.add(entry)
      borrowed <- bag.borrow(1.second)

      // Return to bag
      _ <- bag.requite(borrowed.get)

      // Should be able to borrow again
      borrowed2 <- bag.borrow(1.second)

      // Check state
      state <- entry.getState
    } yield {
      assert(borrowed.isDefined)
      assert(borrowed2.isDefined)
      assertEquals(borrowed2.get.id, "item1")
      assertEquals(state, BagEntry.STATE_IN_USE)
    }
  }

  test("ConcurrentBag should handle concurrent borrow and requite") {
    for {
      bag     <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entries <- (1 to 10).toList.traverse(i => createTestEntry[IO](s"item$i"))

      // Add all entries
      _ <- entries.traverse(bag.add)

      // Concurrent borrow and requite
      _ <- (1 to 100).toList.parTraverse { _ =>
             bag.borrow(1.second).flatMap {
               case Some(item) =>
                 // Simulate some work
                 IO.sleep(10.millis) >> bag.requite(item)
               case None =>
                 IO.raiseError(new Exception("Failed to borrow"))
             }
           }

      // All items should be back in the bag
      finalSize <- bag.size

      // All items should be borrowable
      allBorrowed <- entries.traverse(_ => bag.borrow(1.second))
    } yield {
      assertEquals(finalSize, 10)
      assert(allBorrowed.forall(_.isDefined))
    }
  }

  test("ConcurrentBag should timeout when no items available") {
    for {
      bag <- ConcurrentBag[IO, TestBagEntry[IO]]()

      // Try to borrow from empty bag
      startTime <- IO.realTime
      result    <- bag.borrow(500.millis)
      endTime   <- IO.realTime
      elapsed = endTime - startTime
    } yield {
      assert(result.isEmpty)
      assert(elapsed >= 500.millis)
      assert(elapsed < 600.millis) // Allow some overhead
    }
  }

  test("ConcurrentBag should remove items") {
    for {
      bag    <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry1 <- createTestEntry[IO]("item1")
      entry2 <- createTestEntry[IO]("item2")

      // Add items
      _ <- bag.add(entry1)
      _ <- bag.add(entry2)

      // Remove one
      removed <- bag.remove(entry1)

      // Check size
      size <- bag.size

      // Should only be able to borrow entry2
      borrowed <- bag.borrow(1.second)

      // entry1 should be marked as removed
      state1 <- entry1.getState
    } yield {
      assert(removed)
      assertEquals(size, 1)
      assert(borrowed.isDefined)
      assertEquals(borrowed.get.id, "item2")
      assertEquals(state1, BagEntry.STATE_REMOVED)
    }
  }

  test("ConcurrentBag should handle remove while item is in use") {
    for {
      bag   <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry <- createTestEntry[IO]("item1")

      // Add and borrow
      _        <- bag.add(entry)
      borrowed <- bag.borrow(1.second)

      // Try to remove while in use
      removed <- bag.remove(entry)

      // Item should be marked as removed
      state <- entry.getState

      // Return should not add it back
      _ <- bag.requite(entry)

      // Should not be borrowable
      borrowed2 <- bag.borrow(100.millis)
    } yield {
      assert(borrowed.isDefined)
      assert(removed)
      assertEquals(state, BagEntry.STATE_REMOVED)
      assert(borrowed2.isEmpty)
    }
  }

  test("ConcurrentBag should handle close operation") {
    for {
      bag   <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry <- createTestEntry[IO]("item1")

      // Add item
      _ <- bag.add(entry)

      // Close bag
      _ <- bag.close

      // Should not be able to borrow
      borrowed <- bag.borrow(100.millis)

      // Should not be able to add
      entry2 <- createTestEntry[IO]("item2")
      _      <- bag.add(entry2)
      size   <- bag.size
    } yield {
      assert(borrowed.isEmpty)
      assertEquals(size, 1) // Still only has entry1
    }
  }

  test("ConcurrentBag should handle waiting fibers") {
    for {
      bag   <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry <- createTestEntry[IO]("item1")

      // Start a fiber that will wait for an item
      waiterFiber <- bag.borrow(5.seconds).start

      // Give the waiter time to start waiting
      _ <- IO.sleep(100.millis)

      // Add an item
      _ <- bag.add(entry)

      // Waiter should get the item
      result <- waiterFiber.joinWithNever
    } yield {
      assert(result.isDefined)
      assertEquals(result.get.id, "item1")
    }
  }

  test("ConcurrentBag should handle concurrent waiters") {
    for {
      bag <- ConcurrentBag[IO, TestBagEntry[IO]]()

      // Start multiple waiters
      waiterFibers <- (1 to 5).toList.traverse(_ => bag.borrow(5.seconds).start)

      // Give waiters time to start
      _ <- IO.sleep(100.millis)

      // Add items one by one
      entries <- (1 to 5).toList.traverse(i => createTestEntry[IO](s"item$i"))
      _       <- entries.traverse { entry =>
             bag.add(entry) >> IO.sleep(50.millis)
           }

      // All waiters should get items
      results <- waiterFibers.traverse(_.joinWithNever)

      // All items should be in use
      states <- entries.traverse(_.getState)
    } yield {
      assert(results.forall(_.isDefined))
      assert(states.forall(_ == BagEntry.STATE_IN_USE))
    }
  }

  test("ConcurrentBag should handle requite of items not in shared list") {
    for {
      bag    <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry1 <- createTestEntry[IO]("item1")
      entry2 <- createTestEntry[IO]("item2")

      // Add only entry1 to bag
      _ <- bag.add(entry1)

      // Requite entry2 (not previously in bag)
      _ <- bag.requite(entry2)

      // Both should now be borrowable
      size <- bag.size

      borrowed1 <- bag.borrow(1.second)
      borrowed2 <- bag.borrow(1.second)

      ids = Set(borrowed1.get.id, borrowed2.get.id)
    } yield {
      assertEquals(size, 2)
      assert(borrowed1.isDefined)
      assert(borrowed2.isDefined)
      assertEquals(ids, Set("item1", "item2"))
    }
  }

  test("ConcurrentBag should handle direct handoff to waiters") {
    for {
      bag   <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entry <- createTestEntry[IO]("item1")

      // Add and borrow the item
      _        <- bag.add(entry)
      borrowed <- bag.borrow(1.second)

      // Start a waiter
      waiterFiber <- bag.borrow(5.seconds).start

      // Give waiter time to start
      _ <- IO.sleep(100.millis)

      // Return the item - should go directly to waiter
      _ <- bag.requite(entry)

      // Waiter should get it immediately
      result <- waiterFiber.joinWithNever

      // Bag should be empty (item went directly to waiter)
      size <- bag.size
    } yield {
      assert(borrowed.isDefined)
      assert(result.isDefined)
      assertEquals(result.get.id, "item1")
      assertEquals(size, 1) // Item is still in shared list but in use
    }
  }

  test("ConcurrentBag should maintain LIFO order") {
    for {
      bag     <- ConcurrentBag[IO, TestBagEntry[IO]]()
      entries <- (1 to 5).toList.traverse(i => createTestEntry[IO](s"item$i"))

      // Add in order
      _ <- entries.traverse(bag.add)

      // Borrow should be in reverse order (LIFO)
      borrowed <- (1 to 5).toList.traverse(_ => bag.borrow(1.second))

      ids = borrowed.flatten.map(_.id)
    } yield {
      assertEquals(ids, List("item5", "item4", "item3", "item2", "item1"))
    }
  }
