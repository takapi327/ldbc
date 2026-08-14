/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import scala.concurrent.Promise
import scala.reflect.ClassTag

import ldbc.fx.syntax.*

/**
 * A munit base suite for [[Fx]]-valued tests, shared across every effect-agnostic module (fx, net,
 * pool, mysql). Like `munit-cats-effect`'s `CatsEffectSuite`, a test body may return an `Fx[Unit]`
 * (or any `Fx[A]`): the value transform runs it to completion and fails the test if it errors. This
 * lets the connector's tests be ported by swapping `IO` for `Fx` while keeping their
 * `for`-comprehension shape, and provides [[assertFx]]/[[interceptFx]] as the `assertIO`/`interceptIO`
 * analogues. Cross-platform: the transform yields a `Future`, so it works on JVM, JS, and Native
 * without blocking.
 */
trait FxSuite extends munit.FunSuite:

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform(
      "Fx",
      { case fx: Fx[?] =>
        val promise = Promise[Any]()
        fx.unsafeRun { result =>
          scala.concurrent.ExecutionContext.global.execute { () =>
            result match
              case Right(a) => promise.success(a)
              case Left(e)  => promise.failure(e)
          }
        }
        promise.future
      }
    )

  /**
   * Runs `obtained` and asserts its result equals `returns` (the `assertIO` analogue).
   *
   * @param obtained the effect whose result is checked
   * @param returns  the expected value
   * @param clue     a message shown on failure
   */
  def assertFx[A, B](obtained: Fx[A], returns: B, clue: => Any = "values are not the same")(using
    loc: munit.Location,
    ev:  B <:< A
  ): Fx[Unit] =
    obtained.map(a => assertEquals(a, returns, clue))

  /**
   * Runs `obtained` and asserts its boolean result is `true` (the `assertIOBoolean` analogue).
   *
   * @param obtained the effect whose boolean result must be `true`
   * @param clue     a message shown on failure
   */
  def assertFxBoolean(obtained: Fx[Boolean], clue: => Any = "assertion failed")(using
    loc: munit.Location
  ): Fx[Unit] =
    obtained.map(b => assert(b, clue))

  /**
   * Adapts an [[Fx]] [[Resource]] into a per-test munit fixture — the analogue of munit-cats-effect's
   * `ResourceTestLocalFixture`. Following munit-cats-effect's `IOFixture`, this extends
   * [[munit.AnyFixture]] and its lifecycle hooks RETURN an `Fx[Unit]` rather than blocking: munit runs
   * each returned effect through [[munitValueTransforms]] and awaits it asynchronously. That keeps the
   * fixture non-blocking and therefore cross-platform — it works on single-threaded Scala.js, where a
   * blocking latch cannot. The resource is allocated fresh in `beforeEach` and released in `afterEach`.
   *
   * @param name     the fixture name reported by munit
   * @param resource the resource whose lifecycle brackets each test
   */
  def resourceFixture[A](name: String, resource: Resource[A]): munit.AnyFixture[A] =
    new munit.AnyFixture[A](name):
      private var state: Option[(A, Fx[Unit])] = None

      def apply(): A =
        state.getOrElse(throw new IllegalStateException(s"fixture '$name' was not initialised"))._1

      override def beforeEach(context: BeforeEach): Fx[Unit] =
        resource.allocatedCase.map(acquired => state = Some(acquired))

      override def afterEach(context: AfterEach): Fx[Unit] =
        state match
          case Some((_, release)) => release.map(_ => { state = None })
          case None               => Fx.unit

  /**
   * Runs `fx`, expecting it to fail with a throwable of type `T`, and yields that throwable (the
   * `interceptIO` analogue). Fails the test if `fx` succeeds or fails with a different type.
   *
   * @tparam T the expected throwable type
   * @param fx the effect expected to fail
   */

  def interceptFx[T <: Throwable](fx: Fx[Any])(using T: ClassTag[T], loc: munit.Location): Fx[T] =
    fx.attempt.map {
      case Left(error) =>
        if T.runtimeClass.isInstance(error) then error.asInstanceOf[T]
        else
          throw munit.FailException(
            s"intercept failed, exception '${ error.getClass.getName }' is not a subtype of '${ T.runtimeClass.getName }'",
            loc
          )
      case Right(value) =>
        throw munit.FailException(
          s"expected exception of type '${ T.runtimeClass.getName }' but body evaluated successfully",
          loc
        )
    }
