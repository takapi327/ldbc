/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.effect

import java.util.concurrent.atomic.AtomicReference

/**
 * A mutable reference updated atomically inside `F`, the effect-agnostic counterpart of
 * `cats.effect.Ref`. Backed by [[java.util.concurrent.atomic.AtomicReference]] so updates are lock-free
 * and safe under JVM/Native multithreading. Generalised from `ldbc.fx.Ref` over `Sync[F]`.
 *
 * @tparam F the effect type
 * @tparam A the referenced value type
 */
final class Ref[F[_], A] private (underlying: AtomicReference[A])(using F: Sync[F]):

  def get: F[A] = F.delay(underlying.get)

  def set(a: A): F[Unit] = F.delay(underlying.set(a))

  def update(f: A => A): F[Unit] = F.delay {
    @annotation.tailrec
    def loop(): Unit =
      val cur = underlying.get
      if !underlying.compareAndSet(cur, f(cur)) then loop()
    loop()
  }

  def modify[B](f: A => (A, B)): F[B] = F.delay {
    @annotation.tailrec
    def loop(): B =
      val cur     = underlying.get
      val (na, b) = f(cur)
      if underlying.compareAndSet(cur, na) then b else loop()
    loop()
  }

  def getAndUpdate(f: A => A): F[A] = modify(a => (f(a), a))

  def updateAndGet(f: A => A): F[A] = modify { a =>
    val na = f(a); (na, na)
  }

  def getAndSet(a: A): F[A] = modify(old => (a, old))

object Ref:

  /** Creates a new [[Ref]] initialised to `a`. */
  def of[F[_], A](a: A)(using F: Sync[F]): F[Ref[F, A]] = F.delay(new Ref(new AtomicReference(a)))

  /** Eager, effect-free constructor (mirrors `cats.effect.Ref.unsafe`). Prefer [[of]]. */
  def unsafe[F[_], A](a: A)(using Sync[F]): Ref[F, A] = new Ref(new AtomicReference(a))
