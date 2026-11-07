/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.duration.FiniteDuration

/**
 * Convenience syntax for [[Fx]], mirroring the small cats/cats-effect combinators used pervasively by
 * the connector code (`>>`, `*>`, `void`, `as`, `attempt`, `flatTap`) plus effectful collection
 * traversals (`traverse_`, `traverse`, `filterA`) and their concurrent counterparts (`parTraverse`,
 * `parTraverse_`, `parTraverseN`, `parTupled`, `start`, `timeout`). Import with
 * `import ldbc.fx.syntax.*`.
 */
object syntax:

  extension [A](fa: Fx[A])

    /** Runs `fa`, then `fb`, keeping `fb`'s result (cats `>>`). */
    def >>[B](fb: => Fx[B]): Fx[B] = fa.flatMap(_ => fb)

    /** Runs `fa`, then `fb`, keeping `fb`'s result (cats `*>`). */
    def *>[B](fb: => Fx[B]): Fx[B] = fa.flatMap(_ => fb)

    /** Runs `fa`, then `fb`, keeping `fa`'s result (cats `<*`). */
    def <*[B](fb: => Fx[B]): Fx[A] = fa.flatMap(a => fb.map(_ => a))

    /** Discards `fa`'s result. */
    def void: Fx[Unit] = fa.map(_ => ())

    /** Replaces `fa`'s result with `b`. */
    def as[B](b: B): Fx[B] = fa.map(_ => b)

    /** Runs `fa` for its effect, keeping its own result (cats `flatTap`). */
    def flatTap[B](f: A => Fx[B]): Fx[A] = fa.flatMap(a => f(a).map(_ => a))

    /** Materialises success/failure into an `Either` (cats `attempt`). */
    def attempt: Fx[Either[Throwable, A]] =
      fa.map(a => Right(a)).handleErrorWith(error => Fx.pure(Left(error)))

    /** Recovers from any error by mapping it to a pure value (cats `handleError`). */
    def handleError[B >: A](h: Throwable => B): Fx[B] =
      fa.handleErrorWith(error => Fx.pure(h(error)))

    /** Runs `finalizer` on success, error, and cancellation, keeping `fa`'s result (cats `guarantee`). */
    def guarantee(finalizer: Fx[Unit]): Fx[A] =
      Fx.bracket(Fx.unit)(_ => fa)(_ => finalizer)

    /**
     * Runs `pf` for its effect if `fa` fails with a matching error, then re-raises the original error;
     * non-matching errors and successes pass through unchanged (cats `onError`).
     */
    def onError(pf: PartialFunction[Throwable, Fx[Unit]]): Fx[A] =
      fa.handleErrorWith(error => pf.applyOrElse(error, (_: Throwable) => Fx.unit).flatMap(_ => Fx.raiseError(error)))

    /** Starts `fa` running concurrently in the background (cats-effect `start`). */
    def start: Fx[Fiber[A]] = Fiber.start(fa)

    /**
     * Bounds `fa` by `duration`, failing with a [[java.util.concurrent.TimeoutException]] if it does
     * not complete in time; the losing side is cancelled (cats-effect `timeout`).
     */
    def timeout(duration: FiniteDuration): Fx[A] =
      Fx.timeout(fa, duration)(new java.util.concurrent.TimeoutException(s"timed out after $duration"))

  extension (fb: Fx[Boolean])

    /** Branches on a boolean effect, running `ifTrue` or `ifFalse` accordingly (cats `ifM`). */
    def ifM[A](ifTrue: => Fx[A], ifFalse: => Fx[A]): Fx[A] =
      fb.flatMap(cond => if cond then ifTrue else ifFalse)

  extension [A](xs: Iterable[A])

    /** Runs `f` on each element in order, discarding the results (cats `traverse_`). */
    def traverse_(f: A => Fx[Any]): Fx[Unit] =
      xs.foldLeft(Fx.unit)((acc, a) => acc.flatMap(_ => f(a).map(_ => ())))

    /** Runs `f` on each element in order, collecting the results (cats `traverse`). */
    def traverse[B](f: A => Fx[B]): Fx[List[B]] =
      xs.foldLeft(Fx.pure(List.empty[B]))((acc, a) => acc.flatMap(bs => f(a).map(b => bs :+ b))).map(_.toList)

    /** Keeps the elements for which `f` yields `true` (cats `filterA`). */
    def filterA(f: A => Fx[Boolean]): Fx[List[A]] =
      xs.foldLeft(Fx.pure(List.empty[A])) { (acc, a) =>
        acc.flatMap(as => f(a).map(keep => if keep then as :+ a else as))
      }.map(_.toList)

    /**
     * Runs `f` over every element concurrently, collecting the results in order (cats-effect
     * `parTraverse`). All elements are started as fibers; on error the failure is surfaced but
     * already-started fibers still run to completion (no automatic sibling cancellation).
     */
    def parTraverse[B](f: A => Fx[B]): Fx[List[B]] =
      xs.toList.traverse(a => Fiber.start(f(a))).flatMap(fibers => fibers.traverse(_.joinWithNever))

    /** Like [[parTraverse]] but discards the results (cats-effect `parTraverse_`). */
    def parTraverse_(f: A => Fx[Any]): Fx[Unit] =
      xs.toList.traverse(a => Fiber.start(f(a))).flatMap(fibers => fibers.traverse_(_.joinWithNever))

    /**
     * Runs `f` over every element with at most `n` running concurrently, collecting the results in
     * order (cats-effect `parTraverseN`). Concurrency is bounded by a [[Semaphore]], giving a true
     * sliding window rather than fixed batches.
     *
     * @param n the maximum number of concurrently-running effects (values below 1 are treated as 1)
     */
    def parTraverseN[B](n: Int)(f: A => Fx[B]): Fx[List[B]] =
      Semaphore(n.max(1).toLong).flatMap { semaphore =>
        xs.toList
          .traverse(a => Fiber.start(semaphore.withPermit(f(a))))
          .flatMap(fibers => fibers.traverse(_.joinWithNever))
      }

  extension [A, B](pair: (Fx[A], Fx[B]))

    /** Runs both effects concurrently and pairs their results (cats-effect `parTupled`). */
    def parTupled: Fx[(A, B)] =
      Fiber.start(pair._1).flatMap { fiberA =>
        Fiber.start(pair._2).flatMap { fiberB =>
          fiberA.joinWithNever.flatMap(a => fiberB.joinWithNever.map(b => (a, b)))
        }
      }
