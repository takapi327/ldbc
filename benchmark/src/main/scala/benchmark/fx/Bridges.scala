/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package benchmark.fx

import scala.concurrent.{ Future, Promise }

import cats.effect.IO

import ldbc.fx.Fx

import zio.{ Task, ZIO }

/**
 * Effect-frontend bridges (`Fx ~> IO / ZIO / Future`) used by the Fx overhead benchmarks. These
 * mirror the intended `ldbc-cats` / `ldbc-zio` / `ldbc-future` modules, kept local until those exist.
 */
object Bridges:

  /** Bridges an [[ldbc.fx.Fx]] to a cats-effect `IO`, wiring cancellation into the finalizer. */
  def toIO[A](fx: Fx[A]): IO[A] =
    IO.async[A] { cb =>
      IO {
        val canceler = fx.unsafeRun(cb)
        Some(IO(canceler.cancel()))
      }
    }

  /** Bridges an [[ldbc.fx.Fx]] to a ZIO `Task`, wiring cancellation into the interrupt canceler. */
  def toZIO[A](fx: Fx[A]): Task[A] =
    ZIO.asyncInterrupt[Any, Throwable, A] { k =>
      val canceler = fx.unsafeRun(result => k(ZIO.fromEither(result)))
      Left(ZIO.succeed(canceler.cancel()))
    }

  /** Bridges an [[ldbc.fx.Fx]] to a `Future` (no cancellation, as is native to `Future`). */
  def toFuture[A](fx: Fx[A]): Future[A] =
    val promise = Promise[A]()
    fx.unsafeRun(result => promise.complete(result.toTry))
    promise.future
