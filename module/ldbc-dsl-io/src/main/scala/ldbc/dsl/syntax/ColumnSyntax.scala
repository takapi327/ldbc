/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.syntax

import cats.data.Kleisli

import ldbc.core.Column
import ldbc.sql.{ ResultSet, ResultSetReader }
import ldbc.dsl.Parameter
import ldbc.dsl.statement.ExpressionSyntax
import ldbc.dsl.statement.ExpressionSyntax.*

trait ColumnSyntax[F[_]]:

  private[ldbc] case class MultiColumn[T](flag: String, column1: Column[T], column2: Column[T]):
    val label: String = s"${ column1.label } $flag ${ column2.label }"

  extension [T] (column: Column[T])(using Parameter[F, T])

    def get(using reader: ResultSetReader[F, T]): Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      reader.read(resultSet, column.label)
    }

    def ===(value: T): MatchCondition[F, T] = MatchCondition[F, T](column.label, value)
    def >=(value: T): OrMore[F, T] = OrMore[F, T](column.label, value)
    def >(value: T): Over[F, T] = Over[F, T](column.label, value)
    def <=(value: T): LessThanOrEqualTo[F, T] = LessThanOrEqualTo[F, T](column.label, value)
    def <(value: T): LessThan[F, T] = LessThan[F, T](column.label, value)
    def <>(value: T): NotEqual[F, T] = NotEqual[F, T]("<>", column.label, value)
    def !==(value: T): NotEqual[F, T] = NotEqual[F, T]("!=", column.label, value)
    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] = Is[F, A](column.label, value)
    def IS_NOT[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): IsNot[F, A] = IsNot[F, A](column.label, value)
    def <=>(value: T): NullSafeEqual[F, T] = NullSafeEqual[F, T](column.label, value)
    def IN(value: T*): In[F, T] = In[F, T](column.label, value: _*)
    def BETWEEN(start: T, end: T): In[F, T] = In[F, T](column.label, start, end)
    def LIKE(value: T): Like[F, T] = Like[F, T](column.label, value)
    def LIKE_ESCAPE(like: T, escape: T): LikeEscape[F, T] = LikeEscape[F, T](column.label, like, escape)
    def REGEXP(value: T): Regexp[F, T] = Regexp[F, T](column.label, value)
    def <<(value: T): LeftShift[F, T] = LeftShift[F, T](column.label, value)
    def >>(value: T): RightShift[F, T] = RightShift[F, T](column.label, value)
    def DIV(cond: T, result: T): Div[F, T] = Div[F, T](column.label, cond, result)
    def MOD(cond: T, result: T): Mod[F, T] = Mod[F, T]("MOD", column.label, cond, result)
    def %(cond: T, result: T): Mod[F, T] = Mod[F, T]("%", column.label, cond, result)
    def ^(value: T): BitXOR[F, T] = BitXOR[F, T](column.label, value)

    def ++(other: Column[T]): MultiColumn[T] = MultiColumn[T]("+", column, other)
    def --(other: Column[T]): MultiColumn[T] = MultiColumn[T]("-", column, other)
    def *(other: Column[T]): MultiColumn[T] = MultiColumn[T]("*", column, other)
    def /(other: Column[T]): MultiColumn[T] = MultiColumn[T]("/", column, other)

  extension [T] (column: MultiColumn[T])(using Parameter[F, T])

    def ===(value: T): MatchCondition[F, T] = MatchCondition[F, T](column.label, value)
    def >=(value: T): OrMore[F, T] = OrMore[F, T](column.label, value)
    def >(value: T): Over[F, T] = Over[F, T](column.label, value)
    def <=(value: T): LessThanOrEqualTo[F, T] = LessThanOrEqualTo[F, T](column.label, value)
    def <(value: T): LessThan[F, T] = LessThan[F, T](column.label, value)
    def <>(value: T): NotEqual[F, T] = NotEqual[F, T]("<>", column.label, value)
    def !==(value: T): NotEqual[F, T] = NotEqual[F, T]("!=", column.label, value)
    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] = Is[F, A](column.label, value)
    def IS_NOT[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): IsNot[F, A] = IsNot[F, A](column.label, value)
    def <=>(value: T): NullSafeEqual[F, T] = NullSafeEqual[F, T](column.label, value)
    def IN(value: T*): In[F, T] = In[F, T](column.label, value: _*)
    def BETWEEN(start: T, end: T): In[F, T] = In[F, T](column.label, start, end)
    def LIKE(value: T): Like[F, T] = Like[F, T](column.label, value)
    def LIKE_ESCAPE(like: T, escape: T): LikeEscape[F, T] = LikeEscape[F, T](column.label, like, escape)
    def REGEXP(value: T): Regexp[F, T] = Regexp[F, T](column.label, value)
    def <<(value: T): LeftShift[F, T] = LeftShift[F, T](column.label, value)
    def >>(value: T): RightShift[F, T] = RightShift[F, T](column.label, value)
    def DIV(cond: T, result: T): Div[F, T] = Div[F, T](column.label, cond, result)
    def MOD(cond: T, result: T): Mod[F, T] = Mod[F, T]("MOD", column.label, cond, result)
    def %(cond: T, result: T): Mod[F, T] = Mod[F, T]("%", column.label, cond, result)
    def ^(value: T): BitXOR[F, T] = BitXOR[F, T](column.label, value)
