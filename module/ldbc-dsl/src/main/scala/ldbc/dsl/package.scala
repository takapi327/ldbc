/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.{ Foldable, Functor, Reducible }
import cats.data.NonEmptyList
import cats.syntax.all.*

import cats.effect.*

import ldbc.sql.Parameter

import ldbc.dsl.syntax.*

package object dsl:

  private trait SyncSyntax[F[_]: Sync]
    extends StringContextSyntax[F],
            ConnectionSyntax[F],
            QuerySyntax[F],
            CommandSyntax[F]:

    // The following helper functions for building SQL models are rewritten from doobie fragments for ldbc SQL models.
    // see: https://github.com/tpolecat/doobie/blob/main/modules/core/src/main/scala/doobie/util/fragments.scala

    /** Returns `VALUES (v0), (v1), ...`. */
    def values[M[_]: Reducible, T](vs: M[T])(using Parameter[F, T]): SQL[F] =
      q"VALUES" ++ comma(vs.toNonEmptyList.map(v => parentheses(sql"$v")))

    /** Returns `VALUES (s0, s1, ...)`. */
    def values[M[_]: Reducible](vs: M[SQL[F]]): SQL[F] =
      q"VALUES" ++ parentheses(comma(vs.toNonEmptyList))

    /** Returns `(sql IN (v0, v1, ...))`. */
    def in[T](s: SQL[F], v0: T, v1: T, vs: T*)(using Parameter[F, T]): SQL[F] =
      in(s, NonEmptyList(v0, v1 :: vs.toList))

    /** Returns `(sql IN (s0, s1, ...))`. */
    def in[M[_]: Reducible: Functor, T](s: SQL[F], vs: M[T])(using Parameter[F, T]): SQL[F] =
      parentheses(s ++ q" IN " ++ parentheses(comma(vs.map(v => p"$v"))))

    def inOpt[M[_]: Foldable, T](s: SQL[F], vs: M[T])(using Parameter[F, T]): Option[SQL[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => in(s, nel))

    /** Returns `(sql NOT IN (v0, v1, ...))`. */
    def notIn[T](s: SQL[F], v0: T, v1: T, vs: T*)(using Parameter[F, T]): SQL[F] =
      notIn(s, NonEmptyList(v0, v1 :: vs.toList))

    /** Returns `(sql NOT IN (v0, v1, ...))`, or `true` for empty `fs`. */
    def notIn[M[_]: Reducible: Functor, T](s: SQL[F], vs: M[T])(using Parameter[F, T]): SQL[F] =
      parentheses(s ++ q" NOT IN " ++ parentheses(comma(vs.map(v => p"$v"))))

    def notInOpt[M[_]: Foldable, T](s: SQL[F], vs: M[T])(using Parameter[F, T]): Option[SQL[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => notIn(s, nel))

    /** Returns `(s1 AND s2 AND ... sn)` for a non-empty collection. */
    def and[M[_]: Reducible](ss: M[SQL[F]], grouping: Boolean = true): SQL[F] =
      val expr = ss.reduceLeftTo(s => parentheses(s))((s1, s2) => s1 ++ q" AND " ++ parentheses(s2))
      if grouping then parentheses(expr) else expr

    /** Returns `(s1 AND s2 AND ... sn)`. */
    def and(s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      and(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `(s1 AND s2 AND ... sn)` for a non-empty collection. */
    def andOpt[M[_]: Foldable](vs: M[SQL[F]], grouping: Boolean = true): Option[SQL[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => and(nel, grouping))

    /** Returns `(s1 AND s2 AND ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
    def andOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): Option[SQL[F]] =
      andOpt((s1 :: s2 :: ss.toList).flatten)

    /** Similar to andOpt, but defaults to TRUE if passed an empty collection */
    def andFallbackTrue[M[_]: Foldable](ss: M[SQL[F]]): SQL[F] =
      andOpt(ss).getOrElse(q"TRUE")

    /** Returns `(s1 OR s2 OR ... sn)` for a non-empty collection. */
    def or[M[_]: Reducible](ss: M[SQL[F]], grouping: Boolean = true): SQL[F] =
      val expr = ss.reduceLeftTo(s => parentheses(s))((s1, s2) => s1 ++ q" OR " ++ parentheses(s2))
      if grouping then parentheses(expr) else expr

    /** Returns `(s1 OR s2 OR ... sn)`. */
    def or(s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      or(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `(s1 OR s2 OR ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
    def orOpt[M[_]: Foldable](vs: M[SQL[F]], grouping: Boolean = true): Option[SQL[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => or(nel, grouping))

    /** Returns `(s1 OR s2 OR ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
    def orOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): Option[SQL[F]] =
      orOpt((s1 :: s2 :: ss.toList).flatten)

    /** Similar to orOpt, but defaults to FALSE if passed an empty collection */
    def orFallbackFalse[M[_]: Foldable](ss: M[SQL[F]]): SQL[F] =
      orOpt(ss).getOrElse(q"FALSE")

    /** Returns `WHERE s1 AND s2 AND ... sn`. */
    def whereAnd(s1: SQL[F], ss: SQL[F]*): SQL[F] =
      whereAnd(NonEmptyList(s1, ss.toList))

    /** Returns `WHERE s1 AND s2 AND ... sn` or the empty sql if `ss` is empty. */
    def whereAnd[M[_]: Reducible](ss: M[SQL[F]]): SQL[F] =
      q"WHERE " ++ and(ss, grouping = false)

    /** Returns `WHERE s1 AND s2 AND ... sn` for defined `s`, if any, otherwise the empty sql. */
    def whereAndOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): SQL[F] =
      whereAndOpt((s1 :: s2 :: ss.toList).flatten)

    /** Returns `WHERE s1 AND s2 AND ... sn` if collection is not empty. If collection is empty returns an empty sql. */
    def whereAndOpt[M[_]: Foldable](ss: M[SQL[F]]): SQL[F] =
      NonEmptyList.fromFoldable(ss) match
        case Some(nel) => whereAnd(nel)
        case None      => q""

    /** Returns `WHERE s1 OR s2 OR ... sn`. */
    def whereOr(s1: SQL[F], ss: SQL[F]*): SQL[F] =
      whereOr(NonEmptyList(s1, ss.toList))

    /** Returns `WHERE s1 OR s2 OR ... sn` or the empty sql if `ss` is empty. */
    def whereOr[M[_]: Reducible](ss: M[SQL[F]]): SQL[F] =
      q"WHERE " ++ or(ss, grouping = false)

    /** Returns `WHERE s1 OR s2 OR ... sn` for defined `s`, if any, otherwise the empty sql. */
    def whereOrOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): SQL[F] =
      whereOrOpt((s1 :: s2 :: ss.toList).flatten)

    /** Returns `WHERE s1 OR s2 OR ... sn` if collection is not empty. If collection is empty returns an empty sql. */
    def whereOrOpt[M[_]: Foldable](ss: M[SQL[F]]): SQL[F] =
      NonEmptyList.fromFoldable(ss) match
        case Some(nel) => whereOr(nel)
        case None      => q""

    /** Returns `SET s1, s2, ... sn`. */
    def set(s1: SQL[F], ss: SQL[F]*): SQL[F] =
      set(NonEmptyList(s1, ss.toList))

    /** Returns `SET s1, s2, ... sn`. */
    def set[M[_]: Reducible](fs: M[SQL[F]]): SQL[F] =
      q"SET " ++ comma(fs)

    /** Returns `(sql)`. */
    def parentheses(s: SQL[F]): SQL[F] =
      q"(" ++ s ++ q")"

    /** Returns `s1, s2, ... sn`. */
    def comma(s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      comma(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `s1, s2, ... sn`. */
    def comma[M[_]: Reducible](ss: M[SQL[F]]): SQL[F] =
      ss.nonEmptyIntercalate(q",")

    /** Returns `ORDER BY s1, s2, ... sn`. */
    def orderBy(s1: SQL[F], ss: SQL[F]*): SQL[F] =
      orderBy(NonEmptyList(s1, ss.toList))

    def orderBy[M[_]: Reducible](ss: M[SQL[F]]): SQL[F] =
      q"ORDER BY " ++ comma(ss)

    /** Returns `ORDER BY s1, s2, ... sn` or the empty sql if `ss` is empty. */
    def orderByOpt[M[_]: Foldable](ss: M[SQL[F]]): SQL[F] =
      NonEmptyList.fromFoldable(ss) match
        case Some(nel) => orderBy(nel)
        case None      => q""

    /** Returns `ORDER BY s1, s2, ... sn` for defined `s`, if any, otherwise the empty sql. */
    def orderByOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): SQL[F] =
      orderByOpt((s1 :: s2 :: ss.toList).flatten)

  /**
   * Top-level imports provide aliases for the most commonly used types and modules. A typical starting set of imports
   * might look something like this.
   *
   * example:
   * {{{
   *   import ldbc.dsl.io.*
   * }}}
   */
  val io: SyncSyntax[IO] = new SyncSyntax[IO] {}
