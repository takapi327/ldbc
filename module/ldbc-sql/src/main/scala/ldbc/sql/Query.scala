/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import cats.*

trait Query[F[_], T]:

  def run: Connection[F] => F[T]

  def map[B](f: T => B)(using Functor[F]): Query[F, B] =
    new Query[F, B]:
      override def run: Connection[F] => F[B] = conn => Functor[F].map(Query.this.run(conn))(f)
      override def readOnly(connection:    Connection[F]): F[B] = Functor[F].map(Query.this.readOnly(connection))(f)
      override def autoCommit(connection:  Connection[F]): F[B] = Functor[F].map(Query.this.autoCommit(connection))(f)
      override def transaction(connection: Connection[F]): F[B] = Functor[F].map(Query.this.transaction(connection))(f)
      override def rollback(connection:    Connection[F]): F[B] = Functor[F].map(Query.this.rollback(connection))(f)

  def flatMap[B](f: T => Query[F, B])(using Monad[F]): Query[F, B] =
    new Query[F, B]:
      override def run: Connection[F] => F[B] = conn => Monad[F].flatMap(Query.this.run(conn))(a => f(a).run(conn))
      override def readOnly(connection: Connection[F]): F[B] =
        Monad[F].flatMap(Query.this.readOnly(connection))(a => f(a).readOnly(connection))
      override def autoCommit(connection: Connection[F]): F[B] =
        Monad[F].flatMap(Query.this.autoCommit(connection))(a => f(a).autoCommit(connection))
      override def transaction(connection: Connection[F]): F[B] =
        Monad[F].flatMap(Query.this.transaction(connection))(a => f(a).transaction(connection))
      override def rollback(connection: Connection[F]): F[B] =
        Monad[F].flatMap(Query.this.rollback(connection))(a => f(a).rollback(connection))

  /**
   * Functions for managing the processing of connections in a read-only manner.
   */
  def readOnly(connection: Connection[F]): F[T]

  /**
   * Functions to manage the processing of connections for writing.
   */
  def autoCommit(connection: Connection[F]): F[T]

  /**
   * Functions to manage the processing of connections in a transaction.
   */
  def transaction(connection: Connection[F]): F[T]

  /**
   * Functions to manage the processing of connections, always rolling back.
   */
  def rollback(connection: Connection[F]): F[T]

object Query:

  given [F[_]: Functor]: Functor[[T] =>> Query[F, T]] with
    override def map[A, B](fa: Query[F, A])(f: A => B): Query[F, B] =
      new Query[F, B]:
        override def run: Connection[F] => F[B] = conn => Functor[F].map(fa.run(conn))(f)
        override def readOnly(connection:    Connection[F]): F[B] = Functor[F].map(fa.readOnly(connection))(f)
        override def autoCommit(connection:  Connection[F]): F[B] = Functor[F].map(fa.autoCommit(connection))(f)
        override def transaction(connection: Connection[F]): F[B] = Functor[F].map(fa.transaction(connection))(f)
        override def rollback(connection:    Connection[F]): F[B] = Functor[F].map(fa.rollback(connection))(f)

  given [F[_]: Monad]: Monad[[T] =>> Query[F, T]] with
    override def pure[A](x: A): Query[F, A] =
      new Query[F, A]:
        override def run: Connection[F] => F[A] = _ => Monad[F].pure(x)
        override def readOnly(connection:    Connection[F]): F[A] = Monad[F].pure(x)
        override def autoCommit(connection:  Connection[F]): F[A] = Monad[F].pure(x)
        override def transaction(connection: Connection[F]): F[A] = Monad[F].pure(x)
        override def rollback(connection:    Connection[F]): F[A] = Monad[F].pure(x)

    override def flatMap[A, B](fa: Query[F, A])(f: A => Query[F, B]): Query[F, B] =
      new Query[F, B]:
        override def run: Connection[F] => F[B] = conn => Monad[F].flatMap(fa.run(conn))(a => f(a).run(conn))
        override def readOnly(connection: Connection[F]): F[B] =
          Monad[F].flatMap(fa.readOnly(connection))(a => f(a).readOnly(connection))
        override def autoCommit(connection: Connection[F]): F[B] =
          Monad[F].flatMap(fa.autoCommit(connection))(a => f(a).autoCommit(connection))
        override def transaction(connection: Connection[F]): F[B] =
          Monad[F].flatMap(fa.transaction(connection))(a => f(a).transaction(connection))
        override def rollback(connection: Connection[F]): F[B] =
          Monad[F].flatMap(fa.rollback(connection))(a => f(a).rollback(connection))

    override def tailRecM[A, B](a: A)(f: A => Query[F, Either[A, B]]): Query[F, B] =
      new Query[F, B]:
        override def run: Connection[F] => F[B] = conn => Monad[F].tailRecM(a)(a => f(a).run(conn))
        override def readOnly(connection: Connection[F]): F[B] = Monad[F].tailRecM(a)(a => f(a).readOnly(connection))
        override def autoCommit(connection: Connection[F]): F[B] =
          Monad[F].tailRecM(a)(a => f(a).autoCommit(connection))
        override def transaction(connection: Connection[F]): F[B] =
          Monad[F].tailRecM(a)(a => f(a).transaction(connection))
        override def rollback(connection: Connection[F]): F[B] = Monad[F].tailRecM(a)(a => f(a).rollback(connection))
