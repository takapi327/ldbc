/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.pool

import scala.concurrent.duration.*

import ldbc.effect.{ Concurrent, Deferred, Ref }
import ldbc.effect.syntax.*

/** An item that can live in a [[ConcurrentBag]], tracking its own borrow/return state. */
trait BagEntry[F[_]]:
  def getState:                                F[Int]
  def setState(state:       Int):              F[Unit]
  def compareAndSet(expect: Int, update: Int): F[Boolean]

object BagEntry:
  val STATE_NOT_IN_USE: Int = 0
  val STATE_IN_USE:     Int = 1
  val STATE_REMOVED:    Int = -1
  val STATE_RESERVED:   Int = -2

/**
 * A lock-free-ish concurrent bag for pooled items, inspired by HikariCP's `ConcurrentBag` and adapted to
 * the effect-generic concurrency model. Items live in a shared list scanned with a rotating start index;
 * a handoff queue hands a returned item directly to a waiter. Acquisition timeout is a `sleep` raced
 * against the handoff via `Concurrent.race`.
 */
trait ConcurrentBag[F[_], T <: BagEntry[F]]:
  def borrow(timeout: FiniteDuration): F[Option[T]]

  /** Attempts to take an available item without waiting. */
  def tryBorrow: F[Option[T]]

  def requite(item: T): F[Unit]
  def add(item:     T): F[Unit]
  def remove(item:  T): F[Boolean]
  def size:             F[Int]
  def values:           F[List[T]]

  /** The number of callers currently parked in [[borrow]]. */
  def waiting: F[Int]

  def close: F[Unit]

object ConcurrentBag:

  def apply[F[_], T <: BagEntry[F]]()(using F: Concurrent[F]): F[ConcurrentBag[F, T]] =
    for
      sharedList    <- Ref.of[F, List[T]](List.empty)
      handoff       <- Ref.of[F, Vector[Deferred[F, Option[T]]]](Vector.empty)
      waiters       <- Ref.of[F, Int](0)
      closed        <- Ref.of[F, Boolean](false)
      borrowCounter <- Ref.of[F, Long](0L)
    yield new ConcurrentBagImpl[F, T](sharedList, handoff, waiters, closed, borrowCounter)

  private final class ConcurrentBagImpl[F[_], T <: BagEntry[F]](
    sharedList:    Ref[F, List[T]],
    handoff:       Ref[F, Vector[Deferred[F, Option[T]]]],
    waiters:       Ref[F, Int],
    closed:        Ref[F, Boolean],
    borrowCounter: Ref[F, Long]
  )(using F: Concurrent[F])
    extends ConcurrentBag[F, T]:

    override def borrow(timeout: FiniteDuration): F[Option[T]] =
      closed.get.flatMap {
        case true  => F.pure(None)
        case false =>
          waiters
            .update(_ + 1)
            .flatMap(_ => borrowInternal(timeout))
            .flatMap(result => waiters.update(_ - 1).map(_ => result))
      }

    private def borrowInternal(timeout: FiniteDuration): F[Option[T]] =
      borrowWithBackoff(0).flatMap {
        case Some(item) => F.pure(Some(item))
        case None       => waitForHandoff(timeout)
      }

    private def borrowWithBackoff(retries: Int): F[Option[T]] =
      tryBorrowFromShared.flatMap {
        case Some(item)          => F.pure(Some(item))
        case None if retries < 3 =>
          F.sleep((1 << retries).millis).flatMap(_ => borrowWithBackoff(retries + 1))
        case None => F.pure(None)
      }

    override def tryBorrow: F[Option[T]] =
      closed.get.flatMap {
        case true  => F.pure(None)
        case false => tryBorrowFromShared
      }

    private def tryBorrowFromShared: F[Option[T]] =
      borrowCounter.modify(c => (c + 1, c)).flatMap { counter =>
        sharedList.get.flatMap { list =>
          if list.isEmpty then F.pure(None)
          else tryBorrowFromListShared(list, (counter % list.length).toInt, 0, list.length)
        }
      }

    private def tryBorrowFromListShared(list: List[T], startIdx: Int, offset: Int, size: Int): F[Option[T]] =
      if offset >= size then F.pure(None)
      else
        val item = list((startIdx + offset) % size)
        item.compareAndSet(BagEntry.STATE_NOT_IN_USE, BagEntry.STATE_IN_USE).flatMap {
          case true  => F.pure(Some(item))
          case false => tryBorrowFromListShared(list, startIdx, offset + 1, size)
        }

    /**
     * Parks until an item is handed off or the timeout elapses.
     *
     * The `closed` check after enqueuing is not redundant with the one in [[borrow]]. A caller that
     * passed that check before [[close]] ran would otherwise enqueue into a queue that has already
     * been drained, and nothing would ever wake it. Re-checking here covers that ordering, while
     * `close` covers the opposite one.
     */
    private def waitForHandoff(timeout: FiniteDuration): F[Option[T]] =
      Deferred[F, Option[T]].flatMap { deferred =>
        handoff.update(_ :+ deferred).flatMap { _ =>
          closed.get.flatMap {
            case true  => drainWaiters.flatMap(_ => removeWaiter(deferred).map(_ => None))
            case false =>
              F.race(deferred.get, F.sleep(timeout)).flatMap {
                case Left(item) => F.pure(item)                          // handed off (Some(item) or None)
                case Right(_)   => removeWaiter(deferred).map(_ => None) // timed out; loser (get) cancelled by race
              }
          }
        }
      }

    /** Completes every parked waiter with `None`. */
    private def drainWaiters: F[Unit] =
      handoff.getAndSet(Vector.empty).flatMap(_.traverse_(_.complete(None).attempt))

    private def removeWaiter(deferred: Deferred[F, Option[T]]): F[Unit] =
      handoff.update(_.filterNot(_ eq deferred))

    private def offerToWaiter(item: T): F[Boolean] =
      handoff
        .modify {
          case head +: rest => (rest, Some(head))
          case _            => (Vector.empty, None)
        }
        .flatMap {
          case None           => F.pure(false)
          case Some(deferred) =>
            deferred.complete(Some(item)).flatMap {
              case true  => F.pure(true)
              case false => offerToWaiter(item) // waiter already timed out; try the next
            }
        }

    override def requite(item: T): F[Unit] =
      closed.get.flatMap {
        case true  => F.unit
        case false =>
          item.getState.flatMap { state =>
            if state == BagEntry.STATE_REMOVED then F.unit
            else
              item.compareAndSet(BagEntry.STATE_IN_USE, BagEntry.STATE_NOT_IN_USE).flatMap {
                case true  => continueRequiteProcess(item)
                case false =>
                  item.getState.flatMap {
                    case BagEntry.STATE_NOT_IN_USE => continueRequiteProcess(item)
                    case BagEntry.STATE_REMOVED    => F.unit
                    case _                         => handleStateConflict(item)
                  }
              }
          }
      }

    /**
     * Publishes an item and hands it straight to a waiter when there is one.
     *
     * `waiters` is incremented at the start of [[borrow]] but the waiter only enqueues itself once the
     * non-blocking retries are exhausted, so `waiting > 0` does not imply that [[offerToWaiter]] will
     * find a target. When it does not, the item must be put back into the borrowable state: leaving it
     * `STATE_IN_USE` while nobody holds it makes it unreachable to both [[tryBorrow]] and [[requite]].
     */
    override def add(item: T): F[Unit] =
      closed.get.flatMap {
        case true  => F.unit
        case false =>
          sharedList.update(list => distributeItem(item, list)).flatMap { _ =>
            waiters.get.flatMap { waiting =>
              if waiting > 0 then
                item.compareAndSet(BagEntry.STATE_NOT_IN_USE, BagEntry.STATE_IN_USE).flatMap {
                  case true =>
                    offerToWaiter(item).flatMap {
                      case true  => F.unit
                      case false => item.setState(BagEntry.STATE_NOT_IN_USE)
                    }
                  case false => F.unit
                }
              else F.unit
            }
          }
      }

    override def remove(item: T): F[Boolean] =
      item.compareAndSet(BagEntry.STATE_NOT_IN_USE, BagEntry.STATE_REMOVED).flatMap {
        case true  => sharedList.update(_.filterNot(_ eq item)).map(_ => true)
        case false =>
          item.compareAndSet(BagEntry.STATE_IN_USE, BagEntry.STATE_REMOVED).flatMap {
            case true  => F.pure(true)
            case false => F.pure(false)
          }
      }

    override def size: F[Int] = sharedList.get.map(_.size)

    override def values: F[List[T]] = sharedList.get

    override def waiting: F[Int] = waiters.get

    override def close: F[Unit] = closed.set(true).flatMap(_ => drainWaiters)

    private def distributeItem(item: T, list: List[T]): List[T] =
      if list.isEmpty then item :: list
      else
        val idx = Math.abs(item.hashCode()) % (list.length + 1)
        if idx == 0 then item :: list
        else
          val (front, back) = list.splitAt(idx - 1)
          front ++ (item :: back)

    private def handleStateConflict(item: T): F[Unit] =
      item.getState.flatMap {
        case BagEntry.STATE_REMOVED    => F.unit
        case BagEntry.STATE_NOT_IN_USE => F.unit
        case BagEntry.STATE_RESERVED   =>
          F.sleep(1.milli).flatMap { _ =>
            item.compareAndSet(BagEntry.STATE_RESERVED, BagEntry.STATE_NOT_IN_USE).flatMap {
              case true  => continueRequiteProcess(item)
              case false => handleStateConflict(item)
            }
          }
        case BagEntry.STATE_IN_USE =>
          item.setState(BagEntry.STATE_NOT_IN_USE).flatMap(_ => continueRequiteProcess(item))
        case unknownState =>
          F.raiseError(new IllegalStateException(s"Unknown bag entry state: $unknownState"))
      }

    private def continueRequiteProcess(item: T): F[Unit] =
      sharedList.get.flatMap { list =>
        if list.exists(_ eq item) then
          waiters.get.flatMap { waiting =>
            if waiting > 0 then offerToWaiter(item).map(_ => ()) else F.unit
          }
        else sharedList.update(l => distributeItem(item, l))
      }
