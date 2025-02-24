/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc

import java.time.*

import cats.{ Foldable, Functor, Reducible }
import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.MonadThrow

import cats.effect.*

import ldbc.sql.PreparedStatement

import ldbc.dsl.codec.Encoder
import ldbc.dsl.syntax.*

package object dsl:

  private[ldbc] trait ParamBinder:
    protected def paramBind[F[_]: MonadThrow](
      prepareStatement: PreparedStatement[F],
      params:           List[Parameter.Dynamic]
    ): F[Unit] =
      val encoded = params.foldLeft(MonadThrow[F].pure(List.empty[Encoder.Supported])) {
        case (acc, param) =>
          for
            acc$ <- acc
            value <- param match
                       case Parameter.Dynamic.Success(value) => MonadThrow[F].pure(value)
                       case Parameter.Dynamic.Failure(errors) =>
                         MonadThrow[F].raiseError(new IllegalArgumentException(errors.mkString(", ")))
          yield acc$ :+ value
      }
      encoded.flatMap(_.zipWithIndex.foldLeft(MonadThrow[F].unit) {
        case (acc, (value, index)) =>
          acc *> (value match
            case value: Boolean       => prepareStatement.setBoolean(index + 1, value)
            case value: Byte          => prepareStatement.setByte(index + 1, value)
            case value: Short         => prepareStatement.setShort(index + 1, value)
            case value: Int           => prepareStatement.setInt(index + 1, value)
            case value: Long          => prepareStatement.setLong(index + 1, value)
            case value: Float         => prepareStatement.setFloat(index + 1, value)
            case value: Double        => prepareStatement.setDouble(index + 1, value)
            case value: BigDecimal    => prepareStatement.setBigDecimal(index + 1, value)
            case value: String        => prepareStatement.setString(index + 1, value)
            case value: Array[Byte]   => prepareStatement.setBytes(index + 1, value)
            case value: LocalDate     => prepareStatement.setDate(index + 1, value)
            case value: LocalTime     => prepareStatement.setTime(index + 1, value)
            case value: LocalDateTime => prepareStatement.setTimestamp(index + 1, value)
            case None                 => prepareStatement.setNull(index + 1, ldbc.sql.Types.NULL))
      })

  private[ldbc] trait SyncSyntax[F[_]: MonadCancelThrow] extends StringContextSyntax[F]:

    /**
     * Function for setting parameters to be used as static strings.
     * {{{
     *   val table = sc("table")
     *   sql"SELECT * FROM $table WHERE id = ${1L}"
     *   // SELECT * FROM table WHERE id = ?
     * }}}
     */
    def sc(value: String): Parameter.Static = Parameter.Static(value)

    // The following helper functions for building SQL models are rewritten from doobie fragments for ldbc SQL models.
    // see: https://github.com/tpolecat/doobie/blob/main/modules/core/src/main/scala/doobie/util/fragments.scala

    /** Returns `VALUES (v0, v1), (v2, v3), ...`. */
    def values[M[_]: Reducible, T](vs: M[T])(using Encoder[T]): Mysql[F] =
      sql"VALUES" ++ comma(vs.toNonEmptyList.map(v => parentheses(values(v))))

    private def values[T](v: T)(using encoder: Encoder[T]): Mysql[F] =
      val params = Parameter.Dynamic.many(encoder.encode(v))
      Mysql[F](List.fill(params.size)("?").mkString(","), params)

    /** Returns `(sql IN (v0, v1, ...))`. */
    def in[T](s: SQL, v0: T, v1: T, vs: T*)(using Encoder[T]): Mysql[F] =
      in(s, NonEmptyList(v0, v1 :: vs.toList))

    /** Returns `(sql IN (s0, s1, ...))`. */
    def in[M[_]: Reducible: Functor, T](s: SQL, vs: M[T])(using Encoder[T]): Mysql[F] =
      parentheses(s ++ sql" IN " ++ parentheses(comma(vs.map(v => p"$v"))))

    def inOpt[M[_]: Foldable, T](s: SQL, vs: M[T])(using Encoder[T]): Option[Mysql[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => in(s, nel))

    /** Returns `(sql NOT IN (v0, v1, ...))`. */
    def notIn[T](s: SQL, v0: T, v1: T, vs: T*)(using Encoder[T]): Mysql[F] =
      notIn(s, NonEmptyList(v0, v1 :: vs.toList))

    /** Returns `(sql NOT IN (v0, v1, ...))`, or `true` for empty `fs`. */
    def notIn[M[_]: Reducible: Functor, T](s: SQL, vs: M[T])(using Encoder[T]): Mysql[F] =
      parentheses(s ++ sql" NOT IN " ++ parentheses(comma(vs.map(v => p"$v"))))

    def notInOpt[M[_]: Foldable, T](s: SQL, vs: M[T])(using Encoder[T]): Option[Mysql[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => notIn(s, nel))

    /** Returns `(s1 AND s2 AND ... sn)` for a non-empty collection. */
    def and[M[_]: Reducible](ss: M[SQL], grouping: Boolean = true): Mysql[F] =
      val expr = ss.reduceLeftTo(s => parentheses(s))((s1, s2) => s1 ++ sql" AND " ++ parentheses(s2))
      if grouping then parentheses(expr) else expr

    /** Returns `(s1 AND s2 AND ... sn)`. */
    def and(s1: SQL, s2: SQL, ss: SQL*): Mysql[F] =
      and(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `(s1 AND s2 AND ... sn)` for a non-empty collection. */
    def andOpt[M[_]: Foldable](vs: M[SQL], grouping: Boolean = true): Option[Mysql[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => and(nel, grouping))

    /** Returns `(s1 AND s2 AND ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
    def andOpt(s1: Option[SQL], s2: Option[SQL], ss: Option[SQL]*): Option[Mysql[F]] =
      andOpt((s1 :: s2 :: ss.toList).flatten)

    /** Similar to andOpt, but defaults to TRUE if passed an empty collection */
    def andFallbackTrue[M[_]: Foldable](ss: M[SQL]): Mysql[F] =
      andOpt(ss).getOrElse(sql"TRUE")

    /** Returns `(s1 OR s2 OR ... sn)` for a non-empty collection. */
    def or[M[_]: Reducible](ss: M[SQL], grouping: Boolean = true): Mysql[F] =
      val expr = ss.reduceLeftTo(s => parentheses(s))((s1, s2) => s1 ++ sql" OR " ++ parentheses(s2))
      if grouping then parentheses(expr) else expr

    /** Returns `(s1 OR s2 OR ... sn)`. */
    def or(s1: SQL, s2: SQL, ss: SQL*): SQL =
      or(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `(s1 OR s2 OR ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
    def orOpt[M[_]: Foldable](vs: M[SQL], grouping: Boolean = true): Option[Mysql[F]] =
      NonEmptyList.fromFoldable(vs).map(nel => or(nel, grouping))

    /** Returns `(s1 OR s2 OR ... sn)` for all defined sql, returning Empty SQL if there are no defined sql */
    def orOpt(s1: Option[SQL], s2: Option[SQL], ss: Option[SQL]*): Option[Mysql[F]] =
      orOpt((s1 :: s2 :: ss.toList).flatten)

    /** Similar to orOpt, but defaults to FALSE if passed an empty collection */
    def orFallbackFalse[M[_]: Foldable](ss: M[SQL]): Mysql[F] =
      orOpt(ss).getOrElse(sql"FALSE")

    /** Returns `WHERE s1 AND s2 AND ... sn`. */
    def whereAnd(s1: SQL, ss: SQL*): Mysql[F] =
      whereAnd(NonEmptyList(s1, ss.toList))

    /** Returns `WHERE s1 AND s2 AND ... sn` or the empty sql if `ss` is empty. */
    def whereAnd[M[_]: Reducible](ss: M[SQL]): Mysql[F] =
      sql"WHERE " ++ and(ss, grouping = false)

    /** Returns `WHERE s1 AND s2 AND ... sn` for defined `s`, if any, otherwise the empty sql. */
    def whereAndOpt(s1: Option[SQL], s2: Option[SQL], ss: Option[SQL]*): Mysql[F] =
      whereAndOpt((s1 :: s2 :: ss.toList).flatten)

    /** Returns `WHERE s1 AND s2 AND ... sn` if collection is not empty. If collection is empty returns an empty sql. */
    def whereAndOpt[M[_]: Foldable](ss: M[SQL]): Mysql[F] =
      NonEmptyList.fromFoldable(ss) match
        case Some(nel) => whereAnd(nel)
        case None      => sql""

    /** Returns `WHERE s1 OR s2 OR ... sn`. */
    def whereOr(s1: SQL, ss: SQL*): Mysql[F] =
      whereOr(NonEmptyList(s1, ss.toList))

    /** Returns `WHERE s1 OR s2 OR ... sn` or the empty sql if `ss` is empty. */
    def whereOr[M[_]: Reducible](ss: M[SQL]): Mysql[F] =
      sql"WHERE " ++ or(ss, grouping = false)

    /** Returns `WHERE s1 OR s2 OR ... sn` for defined `s`, if any, otherwise the empty sql. */
    def whereOrOpt(s1: Option[SQL], s2: Option[SQL], ss: Option[SQL]*): Mysql[F] =
      whereOrOpt((s1 :: s2 :: ss.toList).flatten)

    /** Returns `WHERE s1 OR s2 OR ... sn` if collection is not empty. If collection is empty returns an empty sql. */
    def whereOrOpt[M[_]: Foldable](ss: M[SQL]): Mysql[F] =
      NonEmptyList.fromFoldable(ss) match
        case Some(nel) => whereOr(nel)
        case None      => sql""

    /** Returns `SET s1, s2, ... sn`. */
    def set(s1: SQL, ss: SQL*): Mysql[F] =
      set(NonEmptyList(s1, ss.toList))

    /** Returns `SET s1, s2, ... sn`. */
    def set[M[_]: Reducible](fs: M[SQL]): Mysql[F] =
      sql"SET " ++ comma(fs)

    /** Returns `(sql)`. */
    def parentheses(s: SQL): Mysql[F] =
      sql"(" ++ s ++ sql")"

    /** Returns `s1, s2, ... sn`. */
    def comma(s1: SQL, s2: SQL, ss: SQL*): Mysql[F] =
      comma(NonEmptyList(s1, s2 :: ss.toList))

    /** Returns `s1, s2, ... sn`. */
    def comma[M[_]: Reducible](ss: M[SQL]): Mysql[F] =
      ss.reduceLeftTo(s => sql"" ++ s)((s1, s2) => s1 ++ sql"," ++ s2)

    /** Returns `ORDER BY s1, s2, ... sn`. */
    def orderBy(s1: SQL, ss: SQL*): SQL =
      orderBy(NonEmptyList(s1, ss.toList))

    def orderBy[M[_]: Reducible](ss: M[SQL]): Mysql[F] =
      sql"ORDER BY " ++ comma(ss)

    /** Returns `ORDER BY s1, s2, ... sn` or the empty sql if `ss` is empty. */
    def orderByOpt[M[_]: Foldable](ss: M[SQL]): Mysql[F] =
      NonEmptyList.fromFoldable(ss) match
        case Some(nel) => orderBy(nel)
        case None      => sql""

    /** Returns `ORDER BY s1, s2, ... sn` for defined `s`, if any, otherwise the empty sql. */
    def orderByOpt(s1: Option[SQL], s2: Option[SQL], ss: Option[SQL]*): Mysql[F] =
      orderByOpt((s1 :: s2 :: ss.toList).flatten)

    type DBIO[T] = ldbc.dsl.DBIO[F, T]
    val DBIO = ldbc.dsl.DBIO

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
