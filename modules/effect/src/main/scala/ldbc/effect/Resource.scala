/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

/**
 * A composable acquire/release scope, the effect-agnostic counterpart of `cats.effect.Resource`.
 * [[use]] acquires, runs the body, and releases (on success, error, or cancellation) via `Async.bracket`.
 * [[flatMap]] nests resources, releasing in LIFO order; if a later acquire fails the already-acquired
 * resources are released before the error propagates. Generalised from `ldbc.fx.Resource` over `Async[F]`.
 *
 * @tparam F the effect type
 * @tparam A the acquired resource type
 */
final class Resource[F[_], A] private (private[effect] val allocated: F[(A, F[Unit])])(using F: Async[F]):

  def use[B](f: A => F[B]): F[B] =
    F.bracket(allocated)((pair: (A, F[Unit])) => f(pair._1))((pair: (A, F[Unit])) => pair._2)

  def flatMap[B](f: A => Resource[F, B]): Resource[F, B] =
    new Resource(
      F.flatMap(allocated) { (a, releaseA) =>
        F.handleErrorWith(
          F.map(f(a).allocated) { (b, releaseB) =>
            val release =
              F.flatMap(
                F.handleErrorWith(F.map(releaseB)(_ => Option.empty[Throwable]))(e => F.pure(Some(e)))
              )(rbErr => F.flatMap(releaseA)(_ => rbErr.fold(F.unit)(F.raiseError)))
            (b, release)
          }
        )(error => F.flatMap(releaseA)(_ => F.raiseError(error)))
      }
    )

  def map[B](f: A => B): Resource[F, B] = flatMap(a => Resource.pure(f(a)))

  def allocatedCase: F[(A, F[Unit])] = allocated

object Resource:

  def make[F[_], A](acquire: F[A])(release: A => F[Unit])(using F: Async[F]): Resource[F, A] =
    new Resource(F.map(acquire)(a => (a, release(a))))

  def eval[F[_], A](fa: F[A])(using F: Async[F]): Resource[F, A] =
    new Resource(F.map(fa)(a => (a, F.unit)))

  def pure[F[_], A](a: A)(using F: Async[F]): Resource[F, A] =
    new Resource(F.pure((a, F.unit)))

  def unit[F[_]](using Async[F]): Resource[F, Unit] = pure(())

  def makeUnit[F[_]](acquire: F[Unit])(release: F[Unit])(using Async[F]): Resource[F, Unit] =
    make(acquire)(_ => release)
