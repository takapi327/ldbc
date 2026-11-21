/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.future

import scala.concurrent.{ Future, Promise }

import ldbc.fx.{ Fx, FxRuntime }

/**
 * Runs an [[ldbc.fx.Fx]] as a `scala.concurrent.Future`.
 *
 * The whole `Fx` program is executed once through `unsafeRun`, and its result completes a `Promise`.
 * This is the single boundary conversion: a `DBIO` interpreted natively in `Fx` becomes a `Future` here
 * with one bridge per run (not per operation).
 *
 * Unlike the cats-effect / ZIO bridges, `Future` has no cancellation, so the `Fx` canceler is dropped:
 * cancelling the surrounding computation does not stop the underlying `Fx` program. `Future` also has no
 * host runtime to borrow and no reason to swap one, so the program always runs on the platform-default
 * [[ldbc.fx.FxRuntime.global]] pools.
 *
 * @param fa the effect to run
 */
def toFuture[A](fa: Fx[A]): Future[A] =
  val promise = Promise[A]()
  fa.unsafeRun {
    case Right(a) => promise.success(a)
    case Left(e)  => promise.failure(e)
  }(using FxRuntime.global)
  promise.future

/**
 * Allocates an [[ldbc.fx.Resource]] into an allocated-form `Future`.
 *
 * The resource is acquired through [[toFuture]] and the caller receives both the acquired value and a
 * thunk that, when invoked, releases it (again via [[toFuture]]). This mirrors the allocated form used by
 * [[ldbc.sql.DataSource]] on the `Future` side, where there is no `Resource` type to return.
 *
 * @param r the `Fx` resource to allocate
 */
def allocated[A](r: ldbc.effect.Resource[Fx, A]): Future[(A, () => Future[Unit])] =
  toFuture(r.allocatedCase.map { case (a, release) => (a, () => toFuture(release)) })
