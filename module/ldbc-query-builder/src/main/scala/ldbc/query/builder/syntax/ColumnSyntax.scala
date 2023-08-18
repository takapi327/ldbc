/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.syntax

import scala.annotation.targetName

import cats.data.Kleisli

import ldbc.core.Column
import ldbc.core.interpreter.Extract
import ldbc.sql.*
import ldbc.query.builder.{ ColumnReader, Count }
import ldbc.query.builder.statement.{ ExpressionSyntax, OrderBy, Query }
import ldbc.query.builder.statement.ExpressionSyntax.*

trait ColumnSyntax[F[_]]:

  private def buildColumnName[T](column: Column[T]): String =
    column.alias.fold(column.label)(name => s"$name.${ column.label }")

  private[ldbc] case class MultiColumn[T](flag: String, column1: Column[T], column2: Column[T]):
    val label: String = s"${ buildColumnName(column1) } $flag ${ buildColumnName(column2) }"

  given [T](using reader: ResultSetReader[F, T]): Conversion[Column[T], Kleisli[F, ResultSet[F], T]] with
    override def apply(x: Column[T]): Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
      reader.read(resultSet, x.alias.fold(x.label)(name => s"$name.${ x.label }"))
    }

  given [T](using reader: ResultSetReader[F, T]): Conversion[Column[T], ColumnReader[F, T]] with
    override def apply(x: Column[T]): ColumnReader[F, T] = ColumnReader(x, reader)

  /** When implementing a method with the same method name but different arguments, an implementation using extension
    * will result in a compile error on the user side. Therefore, an implementation using implicit class is used.
    *
    * In addition, if a method is implemented with a union type, the method is implemented individually because pattern
    * matching of the received type is not possible.
    *
    * example:
    * {{{
    *   [error]  |value === is not a member of ldbc.core.Column[Option[String]].
    *   [error]  |An extension method was tried, but could not be fully constructed:
    *   [error]  |
    *   [error]  |    ldbc.dsl.io.===()
    *   [error]  |
    *   [error]  |    failed with:
    *   [error]  |
    *   [error]  |        value ===: <overloaded ldbc.dsl.io.===> does not take parameters
    * }}}
    */
  implicit class ColumnStatement[T](column: Column[T])(using Parameter[F, Extract[T]]):

    def count = Count[F](column.label)

    def asc: OrderBy.Asc = OrderBy.Asc(column)

    def desc: OrderBy.Desc = OrderBy.Desc(column)

    def _equals(value: Extract[T]): MatchCondition[F, T] = MatchCondition[F, T](buildColumnName(column), false, value)
    @targetName("matchCondition")
    def ===(value: Extract[T]): MatchCondition[F, T] = _equals(value)

    def orMore(value: Extract[T]): OrMore[F, T] = OrMore[F, T](buildColumnName(column), false, value)
    @targetName("_orMore")
    def >=(value: Extract[T]): OrMore[F, T] = orMore(value)

    def over(value: Extract[T]): Over[F, T] = Over[F, T](buildColumnName(column), false, value)
    @targetName("_over")
    def >(value: Extract[T]): Over[F, T] = over(value)

    def lessThanOrEqual(value: Extract[T]): LessThanOrEqualTo[F, T] =
      LessThanOrEqualTo[F, T](buildColumnName(column), false, value)
    @targetName("_lessThanOrEqual")
    def <=(value: Extract[T]): LessThanOrEqualTo[F, T] = lessThanOrEqual(value)

    def lessThan(value: Extract[T]): LessThan[F, T] = LessThan[F, T](buildColumnName(column), false, value)
    @targetName("_lessThan")
    def <(value: Extract[T]): LessThan[F, T] = lessThan(value)

    def notEqual(value: Extract[T]): NotEqual[F, T] = NotEqual[F, T]("<>", buildColumnName(column), false, value)
    @targetName("_notEqual")
    def <>(value: Extract[T]): NotEqual[F, T] = notEqual(value)

    @targetName("_!equal")
    def !==(value: Extract[T]): NotEqual[F, T] = NotEqual[F, T]("!=", buildColumnName(column), false, value)

    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] =
      Is[F, A](buildColumnName(column), false, value)

    def nullSafeEqual(value: Extract[T]): NullSafeEqual[F, T] =
      NullSafeEqual[F, T](buildColumnName(column), false, value)
    @targetName("_nullSafeEqual")
    def <=>(value: Extract[T]): NullSafeEqual[F, T] = nullSafeEqual(value)

    def IN(value: Extract[T]*): In[F, T] = In[F, T](buildColumnName(column), false, value: _*)

    def BETWEEN(start: Extract[T], end: Extract[T]): Between[F, T] =
      Between[F, T](buildColumnName(column), false, start, end)

    def LIKE(value: Extract[T]): Like[F, T] = Like[F, T](buildColumnName(column), false, value)

    def LIKE_ESCAPE(like: Extract[T], escape: Extract[T]): LikeEscape[F, T] =
      LikeEscape[F, T](buildColumnName(column), false, like, escape)

    def REGEXP(value: Extract[T]): Regexp[F, T] = Regexp[F, T](buildColumnName(column), false, value)

    def leftShift(value: Extract[T]): LeftShift[F, T] = LeftShift[F, T](buildColumnName(column), false, value)
    @targetName("_leftShift")
    def <<(value: Extract[T]): LeftShift[F, T] = leftShift(value)

    def rightShift(value: Extract[T]): RightShift[F, T] = RightShift[F, T](buildColumnName(column), false, value)
    @targetName("_rightShift")
    def >>(value: Extract[T]): RightShift[F, T] = rightShift(value)

    def DIV(cond: Extract[T], result: Extract[T]): Div[F, T] = Div[F, T](buildColumnName(column), false, cond, result)

    def MOD(cond: Extract[T], result: Extract[T]): Mod[F, T] =
      Mod[F, T]("MOD", buildColumnName(column), false, cond, result)

    def mod(cond: Extract[T], result: Extract[T]): Mod[F, T] =
      Mod[F, T]("%", buildColumnName(column), false, cond, result)
    @targetName("_mod")
    def %(cond: Extract[T], result: Extract[T]): Mod[F, T] = mod(cond, result)

    def bitXOR(value: Extract[T]): BitXOR[F, T] = BitXOR[F, T](buildColumnName(column), false, value)
    @targetName("_bitXOR")
    def ^(value: Extract[T]): BitXOR[F, T] = bitXOR(value)

    def bitFlip(value: Extract[T]): BitFlip[F, T] = BitFlip[F, T](buildColumnName(column), false, value)
    @targetName("_bitFlip")
    def ~(value: Extract[T]): BitFlip[F, T] = bitFlip(value)

    def combine(other: Column[T]): MultiColumn[T] = MultiColumn[T]("+", column, other)
    @targetName("_combine")
    def ++(other: Column[T]): MultiColumn[T] = combine(other)

    def deduct(other: Column[T]): MultiColumn[T] = MultiColumn[T]("-", column, other)
    @targetName("_deduct")
    def --(other: Column[T]): MultiColumn[T] = deduct(other)

    def multiply(other: Column[T]): MultiColumn[T] = MultiColumn[T]("*", column, other)
    @targetName("_multiply")
    def *(other: Column[T]): MultiColumn[T] = multiply(other)

    def smash(other: Column[T]): MultiColumn[T] = MultiColumn[T]("/", column, other)
    @targetName("_smash")
    def /(other: Column[T]): MultiColumn[T] = smash(other)

    /** List of sub query methods */
    def _equals(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("=", buildColumnName(column), value)
    @targetName("subQueryEquals")
    def ===(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = _equals(value)

    def orMore(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T](">=", buildColumnName(column), value)
    @targetName("subQueryOrMore")
    def >=(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = orMore(value)

    def over(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T](">", buildColumnName(column), value)
    @targetName("subQueryOver")
    def >(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = over(value)

    def lessThanOrEqual(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<=", buildColumnName(column), value)
    @targetName("subQueryLessThanOrEqual")
    def <=(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = lessThanOrEqual(value)

    def lessThan(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<", buildColumnName(column), value)
    @targetName("subQueryLessThan")
    def <(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = lessThan(value)

    def notEqual(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<>", buildColumnName(column), value)
    @targetName("subQueryNotEqual")
    def <>(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = notEqual(value)

    def IN(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("IN", buildColumnName(column), value)

    /** List of join query methods */
    def _equals(other: Column[?]): ExpressionSyntax[F] = JoinQuery("=", column, other)
    @targetName("joinEqual")
    def ===(other: Column[?]): ExpressionSyntax[F] = _equals(other)

    def orMore(other: Column[?]): ExpressionSyntax[F] = JoinQuery(">=", column, other)
    @targetName("joinOrMore")
    def >=(other: Column[?]): ExpressionSyntax[F] = orMore(other)

    def over(other: Column[?]): ExpressionSyntax[F] = JoinQuery(">", column, other)
    @targetName("joinOver")
    def >(other: Column[?]): ExpressionSyntax[F] = over(other)

    def lessThanOrEqual(other: Column[?]): ExpressionSyntax[F] = JoinQuery("<=", column, other)
    @targetName("joinLessThanOrEqual")
    def <=(other: Column[?]): ExpressionSyntax[F] = lessThanOrEqual(other)

    def lessThan(other: Column[?]): ExpressionSyntax[F] = JoinQuery("<", column, other)
    @targetName("joinLessThan")
    def <(other: Column[?]): ExpressionSyntax[F] = lessThan(other)

    def notEqual(other: Column[?]): ExpressionSyntax[F] = JoinQuery("<>", column, other)
    @targetName("joinNotEqual")
    def <>(other: Column[?]): ExpressionSyntax[F] = notEqual(other)

    @targetName("join!Equal")
    def !==(other: Column[?]): ExpressionSyntax[F] = JoinQuery("!=", column, other)

  implicit class MultiColumnStatement[T](column: MultiColumn[T])(using Parameter[F, Extract[T]]):

    def _equals(value: Extract[T]): MatchCondition[F, T] = MatchCondition[F, T](column.label, false, value)
    @targetName("multiColumnEquals")
    def ===(value: Extract[T]): MatchCondition[F, T] = _equals(value)

    def orMore(value: Extract[T]): OrMore[F, T] = OrMore[F, T](column.label, false, value)
    @targetName("multiColumnOrMore")
    def >=(value: Extract[T]): OrMore[F, T] = orMore(value)

    def over(value: Extract[T]): Over[F, T] = Over[F, T](column.label, false, value)
    @targetName("multiColumnOver")
    def >(value: Extract[T]): Over[F, T] = over(value)

    def lessThanOrEqual(value: Extract[T]): LessThanOrEqualTo[F, T] =
      LessThanOrEqualTo[F, T](column.label, false, value)
    @targetName("multiColumnLessThanOrEqual")
    def <=(value: Extract[T]): LessThanOrEqualTo[F, T] = lessThanOrEqual(value)

    def lessThan(value: Extract[T]): LessThan[F, T] = LessThan[F, T](column.label, false, value)
    @targetName("multiColumnLessThan")
    def <(value: Extract[T]): LessThan[F, T] = lessThan(value)

    def notEqual(value: Extract[T]): NotEqual[F, T] = NotEqual[F, T]("<>", column.label, false, value)
    @targetName("multiColumnNotEqual")
    def <>(value: Extract[T]): NotEqual[F, T] = notEqual(value)

    @targetName("multiColumn!Equal")
    def !==(value: Extract[T]): NotEqual[F, T] = NotEqual[F, T]("!=", column.label, false, value)

    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] = Is[F, A](column.label, false, value)

    def nullSafeEqual(value: Extract[T]): NullSafeEqual[F, T] = NullSafeEqual[F, T](column.label, false, value)
    @targetName("multiColumnNullSafeEqual")
    def <=>(value: Extract[T]): NullSafeEqual[F, T] = nullSafeEqual(value)

    def IN(value: Extract[T]*): In[F, T] = In[F, T](column.label, false, value: _*)

    def BETWEEN(start: Extract[T], end: Extract[T]): Between[F, T] = Between[F, T](column.label, false, start, end)

    def LIKE(value: Extract[T]): Like[F, T] = Like[F, T](column.label, false, value)

    def LIKE_ESCAPE(like: Extract[T], escape: Extract[T]): LikeEscape[F, T] =
      LikeEscape[F, T](column.label, false, like, escape)

    def REGEXP(value: Extract[T]): Regexp[F, T] = Regexp[F, T](column.label, false, value)

    def leftShift(value: Extract[T]): LeftShift[F, T] = LeftShift[F, T](column.label, false, value)
    @targetName("multiColumnLeftShift")
    def <<(value: Extract[T]): LeftShift[F, T] = leftShift(value)

    def rightShift(value: Extract[T]): RightShift[F, T] = RightShift[F, T](column.label, false, value)
    @targetName("multiColumnRightShift")
    def >>(value: Extract[T]): RightShift[F, T] = rightShift(value)

    def DIV(cond: Extract[T], result: Extract[T]): Div[F, T] = Div[F, T](column.label, false, cond, result)

    def MOD(cond: Extract[T], result: Extract[T]): Mod[F, T] = Mod[F, T]("MOD", column.label, false, cond, result)

    def mod(cond: Extract[T], result: Extract[T]): Mod[F, T] = Mod[F, T]("%", column.label, false, cond, result)
    @targetName("multiColumnMOD")
    def %(cond: Extract[T], result: Extract[T]): Mod[F, T] = mod(cond, result)

    def bitXOR(value: Extract[T]): BitXOR[F, T] = BitXOR[F, T](column.label, false, value)
    @targetName("multiColumnBitXOR")
    def ^(value: Extract[T]): BitXOR[F, T] = bitXOR(value)

    def bitFlip(value: Extract[T]): BitFlip[F, T] = BitFlip[F, T](column.label, false, value)
    @targetName("multiColumnBitFlip")
    def ~(value: Extract[T]): BitFlip[F, T] = bitFlip(value)

    /** List of sub query methods */
    def _equals(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("=", column.label, value)
    @targetName("subQueryMultiColumnEquals")
    def ===(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = _equals(value)

    def orMore(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T](">=", column.label, value)
    @targetName("subQueryMultiColumnOrMore")
    def >=(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = orMore(value)

    def over(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = SubQuery[F, T](">", column.label, value)
    @targetName("subQueryMultiColumnOver")
    def >(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = over(value)

    def lessThanOrEqual(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<=", column.label, value)
    @targetName("subQueryMultiColumnLessThanOrEqual")
    def <=(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = lessThanOrEqual(value)

    def lessThan(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<", column.label, value)
    @targetName("subQueryMultiColumnLessThan")
    def <(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = lessThan(value)

    def notEqual(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<>", column.label, value)
    @targetName("subQueryMultiColumnNotEqual")
    def <>(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = notEqual(value)

    def IN(value: Query[F, ColumnReader[F, T] & Column[T]]): SubQuery[F, T] = SubQuery[F, T]("IN", column.label, value)
