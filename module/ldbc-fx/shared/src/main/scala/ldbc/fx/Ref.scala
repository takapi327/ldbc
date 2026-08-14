/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.fx

import java.util.concurrent.atomic.AtomicReference

/**
 * A mutable reference to a value of type `A`, updated atomically inside [[Fx]]. This is the
 * effect-agnostic counterpart of `cats.effect.Ref`, backed by an [[java.util.concurrent.atomic.AtomicReference]]
 * so that updates are lock-free and safe under JVM/Native multithreading.
 *
 * @tparam A the type of the referenced value
 */
final class Ref[A] private (private val underlying: AtomicReference[A]):

  /** Reads the current value. */
  def get: Fx[A] = Fx.delay(underlying.get)

  /** Sets the value to `a`, discarding the previous one. */
  def set(a: A): Fx[Unit] = Fx.delay(underlying.set(a))

  /**
   * Atomically updates the value with `f`, retrying on contention.
   *
   * @param f the transformation to apply to the current value
   */
  def update(f: A => A): Fx[Unit] = Fx.delay {
    @annotation.tailrec
    def loop(): Unit =
      val cur = underlying.get
      if !underlying.compareAndSet(cur, f(cur)) then loop()
    loop()
  }

  /**
   * Atomically updates the value and returns a derived result, retrying on contention.
   *
   * @param f a function returning the next value and a result to return
   * @tparam B the type of the returned result
   */
  def modify[B](f: A => (A, B)): Fx[B] = Fx.delay {
    @annotation.tailrec
    def loop(): B =
      val cur     = underlying.get
      val (na, b) = f(cur)
      if underlying.compareAndSet(cur, na) then b else loop()
    loop()
  }

  /**
   * Atomically updates the value with `f`, returning the previous value.
   *
   * @param f the update function
   */
  def getAndUpdate(f: A => A): Fx[A] = modify(a => (f(a), a))

  /**
   * Atomically updates the value with `f`, returning the updated value.
   *
   * @param f the update function
   */
  def updateAndGet(f: A => A): Fx[A] = modify { a =>
    val na = f(a); (na, na)
  }

  /**
   * Atomically sets the value to `a`, returning the previous value.
   *
   * @param a the new value
   */
  def getAndSet(a: A): Fx[A] = modify(old => (a, old))

/** Constructors for [[Ref]]. */
object Ref:

  /**
   * Creates a new [[Ref]] initialised to `a`.
   *
   * @param a the initial value
   * @tparam A the type of the referenced value
   */
  def of[A](a: A): Fx[Ref[A]] = Fx.delay(new Ref(new AtomicReference(a)))

  /**
   * Creates a new [[Ref]] initialised to `a` eagerly, outside of any [[Fx]] context.
   *
   * Unlike [[of]], this allocates the mutable cell immediately as a side effect, mirroring
   * cats-effect's `Ref.unsafe`. Prefer [[of]] wherever an `Fx` is available; reach for this only
   * when a `Ref` must be produced as a plain value (for example a field initialiser).
   *
   * @param a the initial value
   * @tparam A the type of the referenced value
   */
  def unsafe[A](a: A): Ref[A] = new Ref(new AtomicReference(a))
