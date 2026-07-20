/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.syntax

import cats.{ Foldable, Functor, Reducible }
import cats.data.NonEmptyList
import cats.syntax.all.*

import cats.effect.kernel.Sync

import ldbc.dsl.codec.Encoder

trait HelperFunctionsSyntax extends StringContextSyntax:

  /**
   * Function for setting parameters to be used as static strings.
   * {{{
   *   val table = sc("table")
   *   sql"SELECT * FROM $table WHERE id = ${1L}"
   *   // SELECT * FROM table WHERE id = ?
   * }}}
   *
   * @note This is not safe for user input. Use [[ident]] instead.
   */
  @deprecated("Use ident() for safe identifier escaping with backticks.", "0.7.0")
  def sc(value: String): ldbc.dsl.Parameter.Static = ldbc.dsl.Parameter.Static(value)

  /**
   * Function for safely embedding a SQL identifier (table name, column name, etc.) by escaping it
   * with backticks. Backtick characters in the name are escaped by doubling them (the only correct
   * escaping inside a MySQL backtick-quoted identifier), and NULL characters are removed.
   * Safe to use with user input, unlike [[sc]].
   * {{{
   *   sql"SELECT * FROM ${ident("users")}"
   *   // SELECT * FROM `users`
   *
   *   sql"SELECT ${ident("created_at")} FROM ${ident(tableName)}"
   *   // SELECT `created_at` FROM `user_table`
   * }}}
   */
  def ident(name: String): ldbc.dsl.Parameter.Static =
    val escaped = name.filter(_ != '\u0000').replace("`", "``")
    ldbc.dsl.Parameter.Static(s"`$escaped`")

  // The following helper functions for building SQL models are rewritten from doobie fragments for ldbc SQL models.
  // see: https://github.com/tpolecat/doobie/blob/main/modules/core/src/main/scala/doobie/util/fragments.scala

  /** Returns `VALUES (v0, v1), (v2, v3), ...`. */
  def values[M[_]: Reducible, T](vs: M[T])(using Encoder[T]): ldbc.dsl.Mysql =
    sql"VALUES" ++ comma(vs.toNonEmptyList.map(v => parentheses(values(v))))

  private def values[T](v: T)(using encoder: Encoder[T]): ldbc.dsl.Mysql =
    val params = ldbc.dsl.Parameter.Dynamic.many(encoder.encode(v))
    ldbc.dsl.Mysql(List.fill(params.size)("?").mkString(","), params)

  /** Returns `(sql IN (v0, v1, ...))`. */
  def in[T](s: ldbc.dsl.SQL, v0: T, v1: T, vs: T*)(using Encoder[T]): ldbc.dsl.Mysql =
    in(s, NonEmptyList(v0, v1 :: vs.toList))

  /** Returns `(sql IN (s0, s1, ...))`. */
  def in[M[_]: Reducible: Functor, T](s: ldbc.dsl.SQL, vs: M[T])(using Encoder[T]): ldbc.dsl.Mysql =
    parentheses(s ++ sql" IN " ++ parentheses(comma(vs.map(v => p"$v"))))

  def inOpt[M[_]: Foldable, T](s: ldbc.dsl.SQL, vs: M[T])(using Encoder[T]): Option[ldbc.dsl.Mysql] =
    NonEmptyList.fromFoldable(vs).map(nel => in(s, nel))

  /** Returns `(sql NOT IN (v0, v1, ...))`. */
  def notIn[T](s: ldbc.dsl.SQL, v0: T, v1: T, vs: T*)(using Encoder[T]): ldbc.dsl.Mysql =
    notIn(s, NonEmptyList(v0, v1 :: vs.toList))

  /** Returns `(sql NOT IN (v0, v1, ...))`, or `true` for empty `fs`. */
  def notIn[M[_]: Reducible: Functor, T](s: ldbc.dsl.SQL, vs: M[T])(using Encoder[T]): ldbc.dsl.Mysql =
    parentheses(s ++ sql" NOT IN " ++ parentheses(comma(vs.map(v => p"$v"))))

  def notInOpt[M[_]: Foldable, T](s: ldbc.dsl.SQL, vs: M[T])(using Encoder[T]): Option[ldbc.dsl.Mysql] =
    NonEmptyList.fromFoldable(vs).map(nel => notIn(s, nel))

  /** Returns `(s1 AND s2 AND ... sn)` for a non-empty collection. */
  def and[M[_]: Reducible](ss: M[ldbc.dsl.SQL], grouping: Boolean = true): ldbc.dsl.Mysql =
    val expr = ss.reduceLeftTo(s => parentheses(s))((s1, s2) => s1 ++ sql" AND " ++ parentheses(s2))
    if grouping then parentheses(expr) else expr

  /** Returns `(s1 AND s2 AND ... sn)`. */
  def and(s1: ldbc.dsl.SQL, s2: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    and(NonEmptyList(s1, s2 :: ss.toList))

  /** Returns `(s1 AND s2 AND ... sn)` for a non-empty collection. */
  def andOpt[M[_]: Foldable](vs: M[ldbc.dsl.SQL], grouping: Boolean = true): Option[ldbc.dsl.Mysql] =
    NonEmptyList.fromFoldable(vs).map(nel => and(nel, grouping))

  /** Returns `(s1 AND s2 AND ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
  def andOpt(s1: Option[ldbc.dsl.SQL], s2: Option[ldbc.dsl.SQL], ss: Option[ldbc.dsl.SQL]*): Option[ldbc.dsl.Mysql] =
    andOpt((s1 :: s2 :: ss.toList).flatten)

  /** Similar to andOpt, but defaults to TRUE if passed an empty collection */
  def andFallbackTrue[M[_]: Foldable](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    andOpt(ss).getOrElse(sql"TRUE")

  /** Returns `(s1 OR s2 OR ... sn)` for a non-empty collection. */
  def or[M[_]: Reducible](ss: M[ldbc.dsl.SQL], grouping: Boolean = true): ldbc.dsl.Mysql =
    val expr = ss.reduceLeftTo(s => parentheses(s))((s1, s2) => s1 ++ sql" OR " ++ parentheses(s2))
    if grouping then parentheses(expr) else expr

  /** Returns `(s1 OR s2 OR ... sn)`. */
  def or(s1: ldbc.dsl.SQL, s2: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    or(NonEmptyList(s1, s2 :: ss.toList))

  /** Returns `(s1 OR s2 OR ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
  def orOpt[M[_]: Foldable](vs: M[ldbc.dsl.SQL], grouping: Boolean = true): Option[ldbc.dsl.Mysql] =
    NonEmptyList.fromFoldable(vs).map(nel => or(nel, grouping))

  /** Returns `(s1 OR s2 OR ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
  def orOpt(s1: Option[ldbc.dsl.SQL], s2: Option[ldbc.dsl.SQL], ss: Option[ldbc.dsl.SQL]*): Option[ldbc.dsl.Mysql] =
    orOpt((s1 :: s2 :: ss.toList).flatten)

  /** Similar to orOpt, but defaults to FALSE if passed an empty collection */
  def orFallbackFalse[M[_]: Foldable](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    orOpt(ss).getOrElse(sql"FALSE")

  /** Returns `frag` if `cond` is true, otherwise the empty sql.
   * {{{
   *   sql"SELECT * FROM user" ++ when(limit > 0)(sql" LIMIT $limit")
   *   // if limit > 0: SELECT * FROM user LIMIT ?
   *   // if limit <= 0: SELECT * FROM user
   * }}}
   */
  def when(cond: Boolean)(frag: ldbc.dsl.Mysql): ldbc.dsl.Mysql =
    if cond then frag else ldbc.dsl.Mysql("", Nil)

  /** Returns `WHERE s1 AND s2 AND ... sn`. */
  def whereAnd(s1: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    whereAnd(NonEmptyList(s1, ss.toList))

  /** Returns `WHERE s1 AND s2 AND ... sn` or the empty sql if `ss` is empty. */
  def whereAnd[M[_]: Reducible](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    sql"WHERE " ++ and(ss, grouping = false)

  /** Returns `WHERE s1 AND s2 AND ... sn` for defined `s`, if any, otherwise the empty sql. */
  def whereAndOpt(s1: Option[ldbc.dsl.SQL], s2: Option[ldbc.dsl.SQL], ss: Option[ldbc.dsl.SQL]*): ldbc.dsl.Mysql =
    whereAndOpt((s1 :: s2 :: ss.toList).flatten)

  /** Returns `WHERE s1 AND s2 AND ... sn` if collection is not empty. If collection is empty returns an empty sql. */
  def whereAndOpt[M[_]: Foldable](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    NonEmptyList.fromFoldable(ss) match
      case Some(nel) => whereAnd(nel)
      case None      => sql""

  /** Returns `WHERE s1 OR s2 OR ... sn`. */
  def whereOr(s1: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    whereOr(NonEmptyList(s1, ss.toList))

  /** Returns `WHERE s1 OR s2 OR ... sn` or the empty sql if `ss` is empty. */
  def whereOr[M[_]: Reducible](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    sql"WHERE " ++ or(ss, grouping = false)

  /** Returns `WHERE s1 OR s2 OR ... sn` for defined `s`, if any, otherwise the empty sql. */
  def whereOrOpt(s1: Option[ldbc.dsl.SQL], s2: Option[ldbc.dsl.SQL], ss: Option[ldbc.dsl.SQL]*): ldbc.dsl.Mysql =
    whereOrOpt((s1 :: s2 :: ss.toList).flatten)

  /** Returns `WHERE s1 OR s2 OR ... sn` if collection is not empty. If collection is empty returns an empty sql. */
  def whereOrOpt[M[_]: Foldable](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    NonEmptyList.fromFoldable(ss) match
      case Some(nel) => whereOr(nel)
      case None      => sql""

  /** Returns `SET s1, s2, ... sn`. */
  def set(s1: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    set(NonEmptyList(s1, ss.toList))

  /** Returns `SET s1, s2, ... sn`. */
  def set[M[_]: Reducible](fs: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    sql"SET " ++ comma(fs)

  /** Returns `(sql)`. */
  def parentheses(s: ldbc.dsl.SQL): ldbc.dsl.Mysql =
    sql"(" ++ s ++ sql")"

  /** Returns `s1, s2, ... sn`. */
  def comma(s1: ldbc.dsl.SQL, s2: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    comma(NonEmptyList(s1, s2 :: ss.toList))

  /** Returns `s1, s2, ... sn`. */
  def comma[M[_]: Reducible](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    ss.reduceLeftTo(s => sql"" ++ s)((s1, s2) => s1 ++ sql"," ++ s2)

  /** Returns `ORDER BY s1, s2, ... sn`. */
  def orderBy(s1: ldbc.dsl.SQL, ss: ldbc.dsl.SQL*): ldbc.dsl.Mysql =
    orderBy(NonEmptyList(s1, ss.toList))

  def orderBy[M[_]: Reducible](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    sql"ORDER BY " ++ comma(ss)

  /** Returns `ORDER BY s1, s2, ... sn` or the empty sql if `ss` is empty. */
  def orderByOpt[M[_]: Foldable](ss: M[ldbc.dsl.SQL]): ldbc.dsl.Mysql =
    NonEmptyList.fromFoldable(ss) match
      case Some(nel) => orderBy(nel)
      case None      => sql""

  /** Returns `ORDER BY s1, s2, ... sn` for defined `s`, if any, otherwise the empty sql. */
  def orderByOpt(s1: Option[ldbc.dsl.SQL], s2: Option[ldbc.dsl.SQL], ss: Option[ldbc.dsl.SQL]*): ldbc.dsl.Mysql =
    orderByOpt((s1 :: s2 :: ss.toList).flatten)

  /** Returns `LIMIT limit OFFSET offset`, or `LIMIT limit` if offset is 0 or omitted.
   * {{{
   *   sql"SELECT * FROM user " ++ paginate(20, 40)
   *   // SELECT * FROM user LIMIT ? OFFSET ?
   *
   *   sql"SELECT * FROM user " ++ paginate(20)
   *   // SELECT * FROM user LIMIT ?
   * }}}
   *
   * @throws IllegalArgumentException if `limit` or `offset` is negative
   */
  def paginate(limit: Int, offset: Int = 0): ldbc.dsl.Mysql =
    require(limit >= 0, s"limit must be non-negative, but was $limit")
    require(offset >= 0, s"offset must be non-negative, but was $offset")
    if offset > 0 then
      ldbc.dsl.Mysql(
        "LIMIT ? OFFSET ?",
        List(ldbc.dsl.Parameter.Dynamic.Success(limit), ldbc.dsl.Parameter.Dynamic.Success(offset))
      )
    else ldbc.dsl.Mysql("LIMIT ?", List(ldbc.dsl.Parameter.Dynamic.Success(limit)))

  implicit def toDBIO[A](dbio: ldbc.DBIO[A]): ldbc.dsl.DBIO.Ops[A] = new ldbc.dsl.DBIO.Ops(dbio)

  implicit val syncDBIO: Sync[ldbc.DBIO] = ldbc.dsl.DBIO.syncDBIO
