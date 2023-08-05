/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl.syntax

import cats.data.Kleisli

import ldbc.core.Column
import ldbc.core.interpreter.Extract
import ldbc.sql.{ ResultSet, ResultSetReader }
import ldbc.dsl.Parameter
import ldbc.dsl.statement.*
import ldbc.dsl.statement.ExpressionSyntax.*

trait ColumnSyntax[F[_]]:

  private[ldbc] case class MultiColumn[T](flag: String, column1: Column[T], column2: Column[T]):
    val label: String = s"${ column1.label } $flag ${ column2.label }"

  given [T](using reader: ResultSetReader[F, T]): Conversion[Column[T], Kleisli[F, ResultSet[F], T]] with
    override def apply(x: Column[T]): Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      reader.read(resultSet, x.label)
    }

  extension [T](column: Column[T])(using Parameter[F, Extract[T]])

    def asc:  OrderBy.Asc  = OrderBy.Asc(column)
    def desc: OrderBy.Desc = OrderBy.Desc(column)

    def ===(value: Extract[T]): MatchCondition[F, T]    = MatchCondition[F, T](column.label, false, value)
    def >=(value: Extract[T]):  OrMore[F, T]            = OrMore[F, T](column.label, false, value)
    def >(value: Extract[T]):   Over[F, T]              = Over[F, T](column.label, false, value)
    def <=(value: Extract[T]):  LessThanOrEqualTo[F, T] = LessThanOrEqualTo[F, T](column.label, false, value)
    def <(value: Extract[T]):   LessThan[F, T]          = LessThan[F, T](column.label, false, value)
    def <>(value: Extract[T]):  NotEqual[F, T]          = NotEqual[F, T]("<>", column.label, false, value)
    def !==(value: Extract[T]): NotEqual[F, T]          = NotEqual[F, T]("!=", column.label, false, value)
    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] = Is[F, A](column.label, false, value)
    def <=>(value: Extract[T]): NullSafeEqual[F, T] = NullSafeEqual[F, T](column.label, false, value)
    def IN(value: Extract[T]*): In[F, T]            = In[F, T](column.label, false, value: _*)
    def BETWEEN(start: Extract[T], end: Extract[T]): Between[F, T] = Between[F, T](column.label, false, start, end)
    def LIKE(value: Extract[T]):                     Like[F, T]    = Like[F, T](column.label, false, value)
    def LIKE_ESCAPE(like: Extract[T], escape: Extract[T]): LikeEscape[F, T] =
      LikeEscape[F, T](column.label, false, like, escape)
    def REGEXP(value: Extract[T]):                 Regexp[F, T]     = Regexp[F, T](column.label, false, value)
    def <<(value: Extract[T]):                     LeftShift[F, T]  = LeftShift[F, T](column.label, false, value)
    def >>(value: Extract[T]):                     RightShift[F, T] = RightShift[F, T](column.label, false, value)
    def DIV(cond: Extract[T], result: Extract[T]): Div[F, T]        = Div[F, T](column.label, false, cond, result)
    def MOD(cond: Extract[T], result: Extract[T]): Mod[F, T]     = Mod[F, T]("MOD", column.label, false, cond, result)
    def %(cond: Extract[T], result: Extract[T]):   Mod[F, T]     = Mod[F, T]("%", column.label, false, cond, result)
    def ^(value: Extract[T]):                      BitXOR[F, T]  = BitXOR[F, T](column.label, false, value)
    def ~(value: Extract[T]):                      BitFlip[F, T] = BitFlip[F, T](column.label, false, value)

    def ++(other: Column[T]): MultiColumn[T] = MultiColumn[T]("+", column, other)
    def --(other: Column[T]): MultiColumn[T] = MultiColumn[T]("-", column, other)
    def *(other: Column[T]):  MultiColumn[T] = MultiColumn[T]("*", column, other)
    def /(other: Column[T]):  MultiColumn[T] = MultiColumn[T]("/", column, other)

  extension [T](column: MultiColumn[T])(using Parameter[F, Extract[T]])

    def ===(value: Extract[T]): MatchCondition[F, T]    = MatchCondition[F, T](column.label, false, value)
    def >=(value: Extract[T]):  OrMore[F, T]            = OrMore[F, T](column.label, false, value)
    def >(value: Extract[T]):   Over[F, T]              = Over[F, T](column.label, false, value)
    def <=(value: Extract[T]):  LessThanOrEqualTo[F, T] = LessThanOrEqualTo[F, T](column.label, false, value)
    def <(value: Extract[T]):   LessThan[F, T]          = LessThan[F, T](column.label, false, value)
    def <>(value: Extract[T]):  NotEqual[F, T]          = NotEqual[F, T]("<>", column.label, false, value)
    def !==(value: Extract[T]): NotEqual[F, T]          = NotEqual[F, T]("!=", column.label, false, value)
    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] = Is[F, A](column.label, false, value)
    def <=>(value: Extract[T]): NullSafeEqual[F, T] = NullSafeEqual[F, T](column.label, false, value)
    def IN(value: Extract[T]*): In[F, T]            = In[F, T](column.label, false, value: _*)
    def BETWEEN(start: Extract[T], end: Extract[T]): Between[F, T] = Between[F, T](column.label, false, start, end)
    def LIKE(value: Extract[T]):                     Like[F, T]    = Like[F, T](column.label, false, value)
    def LIKE_ESCAPE(like: Extract[T], escape: Extract[T]): LikeEscape[F, T] =
      LikeEscape[F, T](column.label, false, like, escape)
    def REGEXP(value: Extract[T]):                 Regexp[F, T]     = Regexp[F, T](column.label, false, value)
    def <<(value: Extract[T]):                     LeftShift[F, T]  = LeftShift[F, T](column.label, false, value)
    def >>(value: Extract[T]):                     RightShift[F, T] = RightShift[F, T](column.label, false, value)
    def DIV(cond: Extract[T], result: Extract[T]): Div[F, T]        = Div[F, T](column.label, false, cond, result)
    def MOD(cond: Extract[T], result: Extract[T]): Mod[F, T]     = Mod[F, T]("MOD", column.label, false, cond, result)
    def %(cond: Extract[T], result: Extract[T]):   Mod[F, T]     = Mod[F, T]("%", column.label, false, cond, result)
    def ^(value: Extract[T]):                      BitXOR[F, T]  = BitXOR[F, T](column.label, false, value)
    def ~(value: Extract[T]):                      BitFlip[F, T] = BitFlip[F, T](column.label, false, value)
