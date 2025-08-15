/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.annotation.nowarn
import scala.concurrent.duration.*

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.*
import cats.effect.syntax.all.*

/**
 * A high-performance concurrent data structure for managing pooled connections.
 * 
 * Inspired by HikariCP's ConcurrentBag, this implementation is adapted for 
 * Cats Effect's fiber-based concurrency model. It provides:
 * 
 * - Fast connection acquisition through optimized data structures
 * - Lock-free operations using Ref and atomic operations
 * - Connection stealing across fibers for better utilization
 * - Direct handoff between fibers to minimize latency
 * 
 * Note: Unlike HikariCP which uses ThreadLocal for per-thread caching,
 * this implementation focuses on lock-free shared structures as Cats Effect
 * fibers can migrate between threads, making ThreadLocal unsuitable.
 * 
 * @tparam F the effect type
 */
trait ConcurrentBag[F[_], T <: BagEntry[F]]:

  /**
   * Borrow an item from the bag.
   * 
   * The algorithm:
   * 1. Check fiber-local storage first (fastest path)
   * 2. Scan the shared list for available items
   * 3. Wait for an item to be returned or timeout
   * 
   * @param timeout maximum time to wait for an item
   * @return the borrowed item, or None if timeout
   */
  def borrow(timeout: FiniteDuration): F[Option[T]]

  /**
   * Return an item to the bag.
   * 
   * The algorithm:
   * 1. Try to hand off directly to waiting fibers
   * 2. Add to fiber-local storage if no waiters
   * 3. Add to shared list as last resort
   * 
   * @param item the item to return
   */
  def requite(item: T): F[Unit]

  /**
   * Add a new item to the bag.
   * 
   * @param item the item to add
   */
  def add(item: T): F[Unit]

  /**
   * Remove an item from the bag permanently.
   * 
   * @param item the item to remove
   * @return true if the item was removed, false if not found
   */
  def remove(item: T): F[Boolean]

  /**
   * Get the current size of the bag.
   * 
   * @return the number of items in the bag
   */
  def size: F[Int]

  /**
   * Get all items currently in the bag.
   * 
   * @return all items in the bag
   */
  def values: F[List[T]]

  /**
   * Close the bag and prevent further operations.
   */
  def close: F[Unit]

/**
 * Entry interface for items stored in the ConcurrentBag.
 */
trait BagEntry[F[_]]:
  /**
   * Get the current state of the entry.
   */
  def getState: F[Int]

  /**
   * Set the state of the entry.
   */
  def setState(state: Int): F[Unit]

  /**
   * Atomically update the state if it matches the expected value.
   * 
   * @param expect the expected current state
   * @param update the new state to set
   * @return true if the update succeeded, false if the current state didn't match
   */
  def compareAndSet(expect: Int, update: Int): F[Boolean]

object BagEntry:
  val STATE_NOT_IN_USE: Int = 0
  val STATE_IN_USE:     Int = 1
  val STATE_REMOVED:    Int = -1
  val STATE_RESERVED:   Int = -2

object ConcurrentBag:

  /**
   * Create a new ConcurrentBag instance.
   * 
   * @param maxFiberLocalSize maximum number of items to store in fiber-local storage
   * @tparam F the effect type
   * @tparam T the type of items stored in the bag
   * @return a new ConcurrentBag instance
   */
  def apply[F[_]: Async, T <: BagEntry[F]](
    maxFiberLocalSize: Int = 16
  ): F[ConcurrentBag[F, T]] =
    for
      sharedList   <- Ref[F].of(List.empty[T])
      handoffQueue <- Queue.unbounded[F, T]
      waiters      <- Ref[F].of(0)
      closed       <- Ref[F].of(false)
      // Use a simple counter for round-robin distribution instead of fiber-local storage
      borrowCounter <- Ref[F].of(0L)
    yield new ConcurrentBagImpl[F, T](
      sharedList,
      handoffQueue,
      waiters,
      closed,
      borrowCounter,
      maxFiberLocalSize
    )

  private class ConcurrentBagImpl[F[_]: Temporal, T <: BagEntry[F]](
    sharedList:        Ref[F, List[T]],
    handoffQueue:      Queue[F, T],
    waiters:           Ref[F, Int],
    closed:            Ref[F, Boolean],
    borrowCounter:     Ref[F, Long],
    @nowarn maxFiberLocalSize: Int // Keep for API compatibility
  ) extends ConcurrentBag[F, T]:

    override def borrow(timeout: FiniteDuration): F[Option[T]] =
      closed.get.flatMap {
        case true  => Temporal[F].pure(None)
        case false =>
          // Track that we're waiting
          waiters.update(_ + 1) >>
            borrowInternal(timeout)
              .flatMap {
                case Some(item) =>
                  // Successfully borrowed, decrement waiters
                  waiters.update(_ - 1).as(Some(item))
                case None =>
                  // Timed out, decrement waiters
                  waiters.update(_ - 1).as(None)
              }
              .onCancel {
                // Ensure waiters is decremented on cancellation
                waiters.update(_ - 1)
              }
      }

    private def borrowInternal(timeout: FiniteDuration): F[Option[T]] =
      // Try shared list with optimized scanning
      tryBorrowFromShared.flatMap {
        case Some(item) =>
          checkAndNotifyWaiters.as(Some(item))
        case None =>
          // Wait on handoff queue with timeout
          handoffQueue.tryTake.flatMap {
            case Some(item) => Temporal[F].pure(Some(item))
            case None       =>
              Temporal[F]
                .race(
                  handoffQueue.take,
                  Temporal[F].sleep(timeout)
                )
                .map {
                  case Left(item) => Some(item)
                  case Right(_)   => None
                }
          }
      }

    // Removed tryBorrowFromFiberLocal as we're not using fiber-local storage

    private def tryBorrowFromShared: F[Option[T]] =
      // Use round-robin to distribute load evenly
      borrowCounter.getAndUpdate(_ + 1).flatMap { counter =>
        sharedList.get.flatMap { list =>
          if list.isEmpty then Temporal[F].pure(None)
          else
            // Start from different positions to reduce contention
            val startIdx = (counter % list.length).toInt
            val rotated = list.drop(startIdx) ++ list.take(startIdx)
            tryBorrowFromListShared(rotated)
        }
      }

    private def tryBorrowFromListShared(list: List[T]): F[Option[T]] =
      list match
        case Nil          => Temporal[F].pure(None)
        case head :: tail =>
          head.compareAndSet(BagEntry.STATE_NOT_IN_USE, BagEntry.STATE_IN_USE).flatMap {
            case true  => Temporal[F].pure(Some(head))
            case false => tryBorrowFromListShared(tail)
          }

    private def checkAndNotifyWaiters: F[Unit] =
      waiters.get.flatMap { waiting =>
        if waiting > 1 then
          // Signal that we may have stolen another waiter's connection
          // In the real implementation, this would trigger connection creation
          Temporal[F].unit
        else Temporal[F].unit
      }

    override def requite(item: T): F[Unit] =
      closed.get.flatMap {
        case true  => Temporal[F].unit
        case false =>
          // First check if item is removed
          item.getState.flatMap { state =>
            if state == BagEntry.STATE_REMOVED then Temporal[F].unit
            else
              // Reset state to not in use
              item.setState(BagEntry.STATE_NOT_IN_USE) >>
                // Check if item is in the shared list
                sharedList.get.flatMap { list =>
                  if list.exists(_ eq item) then
                    // Item is already in the list, handle waiters
                    waiters.get.flatMap { waiting =>
                      if waiting > 0 then
                        // Try to hand off directly to a waiter
                        handoffQueue.tryOffer(item).flatMap {
                          case true  => Temporal[F].unit
                          case false => addToFiberLocal(item)
                        }
                      else
                        // No waiters, item stays in shared list
                        Temporal[F].unit
                    }
                  else
                    // Item not in list, add it now
                    sharedList.update(item :: _) >>
                      waiters.get.flatMap { waiting =>
                        if waiting > 0 then
                          // Try to hand off directly to a waiter
                          handoffQueue.tryOffer(item).flatMap {
                            case true  => Temporal[F].unit
                            case false => Temporal[F].unit
                          }
                        else Temporal[F].unit
                      }
                }
          }
      }

    // Removed addToFiberLocal as we're not using fiber-local storage
    @nowarn private def addToFiberLocal(item: T): F[Unit] =
      // No-op, keep in shared list
      Temporal[F].unit

    override def add(item: T): F[Unit] =
      closed.get.flatMap {
        case true  => Temporal[F].unit
        case false =>
          // Add to shared list
          sharedList.update(item :: _) >>
            // Try to satisfy a waiter immediately
            waiters.get.flatMap { waiting =>
              if waiting > 0 then
                item.compareAndSet(BagEntry.STATE_NOT_IN_USE, BagEntry.STATE_IN_USE).flatMap {
                  case true  => handoffQueue.tryOffer(item).void
                  case false => Temporal[F].unit
                }
              else Temporal[F].unit
            }
      }

    override def remove(item: T): F[Boolean] =
      item.compareAndSet(BagEntry.STATE_NOT_IN_USE, BagEntry.STATE_REMOVED).flatMap {
        case true =>
          // Remove from shared list
          sharedList.update(_.filterNot(_ eq item)).as(true)
        case false =>
          item.compareAndSet(BagEntry.STATE_IN_USE, BagEntry.STATE_REMOVED).flatMap {
            case true =>
              // Item was in use, mark as removed but don't remove from list yet
              // It will be removed when requited
              Temporal[F].pure(true)
            case false =>
              // Item was already removed or in some other state
              Temporal[F].pure(false)
          }
      }

    override def size: F[Int] = sharedList.get.map(_.size)

    override def values: F[List[T]] = sharedList.get

    override def close: F[Unit] = closed.set(true)
