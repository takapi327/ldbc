/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.free

import cats.{ MonadError, StackSafeMonad }

import ldbc.fx.Fx

/**
 * Cats type class instances for [[ldbc.fx.Fx]].
 *
 * These live in ldbc-core rather than ldbc-fx because ldbc-fx does not depend on cats.
 * The interpreter's `Free.foldMap` into `Kleisli[Fx, J, *]` requires a `cats.Monad[Fx]`;
 * a full `MonadError[Fx, Throwable]` is provided since `Fx` natively supports error handling.
 */
object FxInstances:

  /**
   * A [[cats.MonadError]] instance for [[ldbc.fx.Fx]]. `Fx` is trampolined, so
   * [[cats.StackSafeMonad]] supplies a stack-safe `tailRecM` in terms of `flatMap`.
   */
  given catsMonadErrorForFx: MonadError[Fx, Throwable] =
    new MonadError[Fx, Throwable] with StackSafeMonad[Fx]:
      override def pure[A](a: A): Fx[A] = Fx.pure(a)
      override def flatMap[A, B](fa: Fx[A])(f: A => Fx[B]): Fx[B] = fa.flatMap(f)
      override def raiseError[A](e: Throwable): Fx[A] = Fx.raiseError(e)
      override def handleErrorWith[A](fa: Fx[A])(f: Throwable => Fx[A]): Fx[A] = fa.handleErrorWith(f)
