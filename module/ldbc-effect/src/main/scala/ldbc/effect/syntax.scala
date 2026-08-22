/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

import scala.concurrent.duration.FiniteDuration

/**
 * Generic combinator syntax over the effect type-classes, mirroring `ldbc.fx.syntax` so the pool /
 * driver read the same after being written against `F` instead of `Fx`. Import with
 * `import ldbc.effect.syntax.*`.
 */
object syntax:

  extension [F[_], A](fa: F[A])
    /** Method-style `flatMap`/`map`/`handleErrorWith` so code written for `Fx` reads unchanged over `F`
      * (also drives `for`-comprehensions). On concrete effects the native member wins over this extension. */
    def flatMap[B](f: A => F[B])(using F: MonadThrow[F]): F[B]     = F.flatMap(fa)(f)
    def map[B](f: A => B)(using F: MonadThrow[F]): F[B]           = F.map(fa)(f)
    def handleErrorWith(f: Throwable => F[A])(using F: MonadThrow[F]): F[A] = F.handleErrorWith(fa)(f)
    def >>[B](fb: => F[B])(using F: MonadThrow[F]): F[B]        = F.flatMap(fa)(_ => fb)
    def *>[B](fb: => F[B])(using F: MonadThrow[F]): F[B]        = F.flatMap(fa)(_ => fb)
    def <*[B](fb: => F[B])(using F: MonadThrow[F]): F[A]        = F.flatMap(fa)(a => F.map(fb)(_ => a))
    def void(using F:  MonadThrow[F]): F[Unit]                 = F.map(fa)(_ => ())
    def as[B](b:  B)(using F: MonadThrow[F]): F[B]             = F.map(fa)(_ => b)
    def flatTap[B](f: A => F[B])(using F: MonadThrow[F]): F[A] = F.flatMap(fa)(a => F.map(f(a))(_ => a))
    def attempt(using F: MonadThrow[F]): F[Either[Throwable, A]] = F.attempt(fa)
    def handleError[B >: A](h: Throwable => B)(using F: MonadThrow[F]): F[B] =
      F.handleErrorWith(F.map(fa)(a => (a: B)))(e => F.pure(h(e)))
    def onError(pf: PartialFunction[Throwable, F[Unit]])(using F: MonadThrow[F]): F[A] =
      F.handleErrorWith(fa)(e => F.flatMap(pf.applyOrElse(e, (_: Throwable) => F.unit))(_ => F.raiseError(e)))
    def guarantee(finalizer: F[Unit])(using F: Async[F]): F[A] = F.guarantee(fa)(finalizer)
    def start(using F: Concurrent[F]): F[Fiber[F, A]]         = F.start(fa)
    def timeout(duration: FiniteDuration)(using F: Concurrent[F]): F[A] =
      F.timeout(fa, duration)(new java.util.concurrent.TimeoutException(s"timed out after $duration"))

  extension [F[_]](fb: F[Boolean])
    def ifM[A](ifTrue: => F[A], ifFalse: => F[A])(using F: MonadThrow[F]): F[A] =
      F.flatMap(fb)(cond => if cond then ifTrue else ifFalse)

  extension [F[_], A](xs: Iterable[A])
    def traverse_[B](f: A => F[B])(using F: MonadThrow[F]): F[Unit] =
      xs.foldLeft(F.unit)((acc, a) => F.flatMap(acc)(_ => F.map(f(a))(_ => ())))
    def traverse[B](f: A => F[B])(using F: MonadThrow[F]): F[List[B]] =
      F.map(xs.foldLeft(F.pure(Vector.empty[B]))((acc, a) => F.flatMap(acc)(bs => F.map(f(a))(b => bs :+ b))))(_.toList)
    def filterA(f: A => F[Boolean])(using F: MonadThrow[F]): F[List[A]] =
      F.map(
        xs.foldLeft(F.pure(Vector.empty[A]))((acc, a) =>
          F.flatMap(acc)(as => F.map(f(a))(keep => if keep then as :+ a else as))
        )
      )(_.toList)
    def parTraverse[B](f: A => F[B])(using F: Concurrent[F]): F[List[B]] =
      F.flatMap(xs.toList.traverse(a => F.start(f(a))))(fibers => fibers.traverse(_.join))
    def parTraverse_[B](f: A => F[B])(using F: Concurrent[F]): F[Unit] =
      F.flatMap(xs.toList.traverse(a => F.start(f(a))))(fibers => fibers.traverse_(fib => F.map(fib.join)(_ => ())))
    def parTraverseN[B](n: Int)(f: A => F[B])(using F: Concurrent[F]): F[List[B]] =
      F.flatMap(Semaphore[F](n.max(1).toLong)) { semaphore =>
        F.flatMap(xs.toList.traverse(a => F.start(semaphore.withPermit(f(a)))))(fibers => fibers.traverse(_.join))
      }

  extension [F[_], A, B](pair: (F[A], F[B]))
    def parTupled(using F: Concurrent[F]): F[(A, B)] =
      F.flatMap(F.start(pair._1)) { fiberA =>
        F.flatMap(F.start(pair._2))(fiberB => F.flatMap(fiberA.join)(a => F.map(fiberB.join)(b => (a, b))))
      }
