/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import cats.{Foldable, Functor, Reducible}
import cats.data.NonEmptyList
import cats.syntax.all.*

import cats.effect.{ IO, Sync }

import ldbc.sql.*
import ldbc.dsl.syntax.*

package object dsl:

  private trait SyncSyntax[F[_]: Sync]
    extends StringContextSyntax[F],
            ConnectionSyntax[F],
            QuerySyntax[F],
            CommandSyntax[F]:

    /** Returns `VALUES (sql0), (sql1), ...`. */
    def values[M[_]: Reducible, T](vs: M[T])(using param: Parameter[F, T]): SQL[F] =
      q"VALUES" ++ comma(vs.toNonEmptyList.map(v => parentheses(sql"$v")))

    /** Returns `VALUES (sql0, sql1, ...)`. */
    def values[M[_] : Reducible](vs: M[SQL[F]]): SQL[F] =
      q"VALUES" ++ parentheses(comma(vs.toNonEmptyList))

    /** Returns `(sql IN (sql0, sql1, ...))`. */
    def in[M[_]: Reducible: Functor, T](vs: M[T])(using param: Parameter[F, T]): SQL[F] =
      q"IN" ++ parentheses(comma(vs.toNonEmptyList.map(v => sql"$v")))

    /** Returns `(sql1 AND sql2 AND ... sql*)` for a non-empty collection. */
    def and[M[_]: Reducible](vs: M[SQL[F]], grouping: Boolean = true): SQL[F] =
      val expr = vs.nonEmptyIntercalate(q" AND")
      if grouping then parentheses(expr) else expr

    /** Returns `(sql1 AND sql2 AND ... sql*)`. */
    def and(s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      and(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `(sql1 AND sql2 AND ... sql*)` for a non-empty collection. */
    def andOpt[M[_]: Foldable](vs: M[SQL[F]], grouping: Boolean = true): SQL[F] =
      NonEmptyList.fromFoldable(vs).map(nel => and(nel, grouping)).getOrElse(q"")

    /** Returns `(sql1 AND sql2 AND ... sql*)` for all defined sql, returning Empty SQL if there are no defined sql */
    def andOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): SQL[F] =
      andOpt((s1 :: s2 :: ss.toList).flatten)

    /** Returns `(sql1 OR sql2 OR ... sql*)` for a non-empty collection. */
    def or[M[_]: Reducible](vs: M[SQL[F]], grouping: Boolean = true): SQL[F] =
      val expr = vs.nonEmptyIntercalate(q" OR")
      if grouping then parentheses(expr) else expr

    /** Returns `(sql1 OR sql2 OR ... sql*)`. */
    def or(s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      or(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `(sql1 OR sql2 OR ... sql*)` for all defined sql, returning Empty SQL if there are no defined sql */
    def orOpt[M[_]: Foldable](vs: M[SQL[F]], grouping: Boolean = true): SQL[F] =
      NonEmptyList.fromFoldable(vs).map(nel => or(nel, grouping)).getOrElse(q"")

    /** Returns `(sql1 OR sql2 OR ... sql*)` for all defined sql, returning Empty SQL if there are no defined sql */
    def orOpt(s1: Option[SQL[F]], s2: Option[SQL[F]], ss: Option[SQL[F]]*): SQL[F] =
      orOpt((s1 :: s2 :: ss.toList).flatten)

    /** Returns `WHERE sql1, sql2, ... sql*`. */
    def where(bind: "AND" | "OR", s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      q"WHERE " ++ NonEmptyList(s1, s2 :: ss.toList).nonEmptyIntercalate(q" $bind ")

    def where(bind: "AND" | "OR", s1: SQL[F], s2: Option[SQL[F]], ss: Option[SQL[F]]*): SQL[F] =
      q"WHERE " ++ NonEmptyList.fromFoldable((Some(s1) :: s2 :: ss.toList).flatten).map(_.nonEmptyIntercalate(q" $bind ")).getOrElse(q"TRUE")

    /** Returns `SET sql1, sql2, ... sql*`. */
    def set(s1: SQL[F], ss: SQL[F]*): SQL[F] =
      set(NonEmptyList(s1, ss.toList))

    /** Returns `SET sql1, sql2, ... sql*`. */
    def set[M[_]: Reducible](fs: M[SQL[F]]): SQL[F] =
      q"SET" ++ comma(fs)

    /** Returns `(sql)`. */
    def parentheses(s: SQL[F]): SQL[F] =
      q"(" ++ s ++ q")"

    /** Returns `sql1, sql2, ... sql*`. */
    def comma(s1: SQL[F], s2: SQL[F], ss: SQL[F]*): SQL[F] =
      comma(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `sql1, sql2, ... sql*`. */
    def comma[M[_]:  Reducible](ss: M[SQL[F]]): SQL[F] =
      ss.nonEmptyIntercalate(q",")

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
