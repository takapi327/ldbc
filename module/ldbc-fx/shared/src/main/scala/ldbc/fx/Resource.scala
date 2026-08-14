/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

/**
 * A composable acquire/release scope, the effect-agnostic counterpart of `cats.effect.Resource`.
 * A `Resource[A]` describes how to acquire an `A` together with the finalizer that releases it.
 * [[use]] acquires, runs the body, and releases — on success, error, or cancellation — via
 * [[Fx.bracket]]. [[flatMap]] nests resources, releasing them in LIFO order; if a later acquire
 * fails, the already-acquired resources are released before the error propagates.
 *
 * @param allocated an effect that acquires the value and returns it paired with its release action
 * @tparam A the acquired resource type
 */
final class Resource[A] private (private[fx] val allocated: Fx[(A, Fx[Unit])]):

  /**
   * Acquires the resource, runs `f` with it, and releases it afterwards (even on error/cancel).
   *
   * @param f the body that uses the resource
   * @tparam B the result type of the body
   */
  def use[B](f: A => Fx[B]): Fx[B] =
    Fx.bracket(allocated)((pair: (A, Fx[Unit])) => f(pair._1))((pair: (A, Fx[Unit])) => pair._2)

  /**
   * Sequences this resource with `f`, keeping both open for the duration and releasing in LIFO order.
   * If acquiring the second resource fails, this one is released before the error propagates.
   *
   * @param f produces the next resource from this one's value
   * @tparam B the next resource type
   */
  def flatMap[B](f: A => Resource[B]): Resource[B] =
    new Resource(
      allocated.flatMap { (a, releaseA) =>
        f(a).allocated
          .map { (b, releaseB) =>
            val release =
              releaseB
                .map(_ => Option.empty[Throwable])
                .handleErrorWith(e => Fx.pure(Some(e)))
                .flatMap(rbErr => releaseA.flatMap(_ => rbErr.fold(Fx.unit)(Fx.raiseError)))
            (b, release)
          }
          .handleErrorWith(error => releaseA.flatMap(_ => Fx.raiseError(error)))
      }
    )

  /**
   * Transforms the acquired value.
   *
   * @param f the mapping function
   * @tparam B the mapped value type
   */
  def map[B](f: A => B): Resource[B] = flatMap(a => Resource.pure(f(a)))

  /**
   * Acquires the resource and returns its value together with the release action to run later. The
   * caller becomes responsible for running the release; prefer [[use]] when possible.
   */
  def allocatedCase: Fx[(A, Fx[Unit])] = allocated

/** Constructors for [[Resource]]. */
object Resource:

  /**
   * A resource with an explicit acquire and release.
   *
   * @param acquire the effect that obtains the resource
   * @param release releases the resource
   * @tparam A the resource type
   */
  def make[A](acquire: Fx[A])(release: A => Fx[Unit]): Resource[A] =
    new Resource(acquire.map(a => (a, release(a))))

  /**
   * Lifts an effect into a resource with no release.
   *
   * @param fa the effect to evaluate on acquire
   * @tparam A the value type
   */
  def eval[A](fa: Fx[A]): Resource[A] = new Resource(fa.map(a => (a, Fx.unit)))

  /**
   * A resource that yields `a` with no release.
   *
   * @param a the value
   * @tparam A the value type
   */
  def pure[A](a: A): Resource[A] = new Resource(Fx.pure((a, Fx.unit)))

  /** The resource that yields `()` with no release. */
  val unit: Resource[Unit] = pure(())

  /**
   * A resource whose acquire and release are given as effects (no value threaded through).
   *
   * @param acquire the setup effect
   * @param release the teardown effect
   */
  def makeUnit(acquire: Fx[Unit])(release: Fx[Unit]): Resource[Unit] =
    make(acquire)(_ => release)
