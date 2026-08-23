/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.tests

import scala.concurrent.Future
import scala.reflect.ClassTag

import cats.MonadThrow

import cats.effect.IO

import ldbc.fx.Fx

/**
 * Effect-agnostic munit base suite for the shared connector integration bodies.
 *
 * A test body may return an `F[A]` for any effect `F` that has a `cats.MonadThrow`: the per-effect
 * mixin ([[IODatabaseSuite]] / [[FxDatabaseSuite]] / [[FutureDatabaseSuite]]) supplies the munit value
 * transform that runs `F` to a `Future`, and this trait supplies [[assertF]] / [[interceptF]] as the
 * `assertIO` / `interceptIO` analogues in terms of `MonadThrow[F].map`/`attempt`. Because the DSL's
 * `readOnly`/`commit`/`transaction` carry no effect constraint (they only delegate to `Connector.run`),
 * a body built from `sql"…".query.….readOnly(connector)` runs on `IO`, `Fx`, and `Future` alike.
 */
trait DatabaseSuite[F[_]] extends munit.FunSuite:

  protected given monad: MonadThrow[F]

  /** Runs `obtained` and asserts its result equals `returns` (the `assertIO` analogue). */
  def assertF[A, B](obtained: F[A], returns: B, clue: => Any = "values are not the same")(using
    munit.Location,
    B <:< A
  ): F[Unit] =
    monad.map(obtained)(a => assertEquals(a, returns, clue))

  /** Runs `obtained` and asserts its boolean result is `true` (the `assertIOBoolean` analogue). */
  def assertFBoolean(obtained: F[Boolean], clue: => Any = "assertion failed")(using munit.Location): F[Unit] =
    monad.map(obtained)(b => assert(b, clue))

  /** Runs `fa`, expecting it to fail with a `T`, and yields that throwable (the `interceptIO` analogue). */
  def interceptF[T <: Throwable](using ct: ClassTag[T], loc: munit.Location): InterceptFApplied[T] =
    new InterceptFApplied[T]

  /**
   * An effect-agnostic suite-level fixture: `setup` runs once in `beforeAll`, `teardown` once in
   * `afterAll`, each as an `F[Unit]` that munit awaits through the per-effect value transform. The
   * analogue of munit-cats-effect's `ResourceSuiteLocalFixture` over any `F`.
   */
  def suiteFixture(name: String, setup: => F[Unit], teardown: => F[Unit]): munit.AnyFixture[Unit] =
    new munit.AnyFixture[Unit](name):
      def apply():              Unit    = ()
      override def beforeAll(): F[Unit] = setup
      override def afterAll():  F[Unit] = teardown

  /** Partial application of [[interceptF]]: fixes the expected throwable `T`, infers the body's `A`. */
  final class InterceptFApplied[T <: Throwable](using ct: ClassTag[T], loc: munit.Location):
    def apply[A](fa: F[A]): F[T] =
      monad.map(monad.attempt(fa)) {
        case Left(error) if ct.runtimeClass.isInstance(error) => error.asInstanceOf[T]
        case Left(error)                                      =>
          throw munit.FailException(
            s"intercept failed, exception '${ error.getClass.getName }' is not a subtype of '${ ct.runtimeClass.getName }'",
            loc
          )
        case Right(_) =>
          throw munit.FailException(
            s"expected exception of type '${ ct.runtimeClass.getName }' but body evaluated successfully",
            loc
          )
      }

/** `F = IO` leaf harness: reuses munit-cats-effect's IO value transform. */
trait IODatabaseSuite extends munit.CatsEffectSuite with DatabaseSuite[IO]:
  protected given monad: MonadThrow[IO] = cats.effect.IO.asyncForIO

/** `F = Fx` leaf harness: reuses `ldbc.fx.FxSuite`'s Fx value transform. */
trait FxDatabaseSuite extends DatabaseSuite[Fx] with ldbc.fx.FxSuite:
  protected given monad: MonadThrow[Fx] = ldbc.future.FxInstances.catsMonadErrorForFx

/** `F = Future` leaf harness: munit transforms `Future` natively; `MonadThrow[Future]` comes from cats. */
trait FutureDatabaseSuite extends DatabaseSuite[Future]:
  import scala.concurrent.ExecutionContext.Implicits.global
  protected given monad: MonadThrow[Future] = cats.instances.future.catsStdInstancesForFuture

/**
 * `F = IO` leaf harness that additionally supplies `ldbc.effect.Async[IO]`, needed by bodies that use
 * `datasource.use` (`ldbc.mysql.syntax`) or `Async`-level primitives. `Future` has no `ldbc.effect.Async`,
 * so those bodies are `IO`/`Fx` only.
 */
trait IOAsyncDatabaseSuite extends IODatabaseSuite:
  protected given concurrent: ldbc.effect.Concurrent[IO] = ldbc.catseffect.concurrentIO

/** `F = Fx` counterpart of [[IOAsyncDatabaseSuite]]. */
trait FxAsyncDatabaseSuite extends FxDatabaseSuite:
  protected given concurrent: ldbc.effect.Concurrent[Fx] = ldbc.fx.concurrentFx
