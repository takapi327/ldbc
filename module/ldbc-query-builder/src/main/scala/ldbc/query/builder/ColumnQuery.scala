/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.annotation.targetName

import ldbc.core.*
import ldbc.core.attribute.Attribute
import ldbc.core.interpreter.Extract
import ldbc.sql.*
import ldbc.query.builder.statement.{ ExpressionSyntax, OrderBy, Query }
import ldbc.query.builder.statement.ExpressionSyntax.*

/**
 * Trait for retrieving data from a ResultSet using the column's information.
 *
 * @tparam T
 *   Scala types that match SQL DataType
 */
private[ldbc] trait ColumnQuery[T] extends Column[T]:

  private val noBagQuotLabel: String =
    alias.fold(label)(name => s"$name.$label")

  def count: Count = Count(label)

  def asc: OrderBy.Asc = OrderBy.Asc(this)

  def desc: OrderBy.Desc = OrderBy.Desc(this)

  def _equals(value: Extract[T])(using parameter: Parameter[Extract[T]]): MatchCondition[T] =
    MatchCondition[T](noBagQuotLabel, false, value)

  @targetName("matchCondition")
  def ===(value: Extract[T])(using parameter: Parameter[Extract[T]]): MatchCondition[T] = _equals(value)

  def orMore(value: Extract[T])(using parameter: Parameter[Extract[T]]): OrMore[T] =
    OrMore[T](noBagQuotLabel, false, value)

  @targetName("_orMore")
  def >=(value: Extract[T])(using parameter: Parameter[Extract[T]]): OrMore[T] = orMore(value)

  def over(value: Extract[T])(using parameter: Parameter[Extract[T]]): Over[T] =
    Over[T](noBagQuotLabel, false, value)

  @targetName("_over")
  def >(value: Extract[T])(using parameter: Parameter[Extract[T]]): Over[T] = over(value)

  def lessThanOrEqual(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThanOrEqualTo[T] =
    LessThanOrEqualTo[T](noBagQuotLabel, false, value)

  @targetName("_lessThanOrEqual")
  def <=(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThanOrEqualTo[T] = lessThanOrEqual(value)

  def lessThan(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThan[T] =
    LessThan[T](noBagQuotLabel, false, value)

  @targetName("_lessThan")
  def <(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThan[T] = lessThan(value)

  def notEqual(value: Extract[T])(using parameter: Parameter[Extract[T]]): NotEqual[T] =
    NotEqual[T]("<>", noBagQuotLabel, false, value)

  @targetName("_notEqual")
  def <>(value: Extract[T])(using parameter: Parameter[Extract[T]]): NotEqual[T] = notEqual(value)

  @targetName("_!equal")
  def !==(value: Extract[T])(using parameter: Parameter[Extract[T]]): NotEqual[T] =
    NotEqual[T]("!=", noBagQuotLabel, false, value)

  def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[A] =
    Is[A](noBagQuotLabel, false, value)

  def nullSafeEqual(value: Extract[T])(using parameter: Parameter[Extract[T]]): NullSafeEqual[T] =
    NullSafeEqual[T](noBagQuotLabel, false, value)

  @targetName("_nullSafeEqual")
  def <=>(value: Extract[T])(using parameter: Parameter[Extract[T]]): NullSafeEqual[T] = nullSafeEqual(value)

  def IN(value: Extract[T]*)(using parameter: Parameter[Extract[T]]): In[T] =
    In[T](noBagQuotLabel, false, value*)

  def BETWEEN(start: Extract[T], end: Extract[T])(using parameter: Parameter[Extract[T]]): Between[T] =
    Between[T](noBagQuotLabel, false, start, end)

  def LIKE(value: Extract[T])(using parameter: Parameter[Extract[T]]): Like[T] =
    Like[T](noBagQuotLabel, false, value)

  def LIKE_ESCAPE(like: Extract[T], escape: Extract[T])(using parameter: Parameter[Extract[T]]): LikeEscape[T] =
    LikeEscape[T](noBagQuotLabel, false, like, escape)

  def REGEXP(value: Extract[T])(using parameter: Parameter[Extract[T]]): Regexp[T] =
    Regexp[T](noBagQuotLabel, false, value)

  def leftShift(value: Extract[T])(using parameter: Parameter[Extract[T]]): LeftShift[T] =
    LeftShift[T](noBagQuotLabel, false, value)

  @targetName("_leftShift")
  def <<(value: Extract[T])(using parameter: Parameter[Extract[T]]): LeftShift[T] = leftShift(value)

  def rightShift(value: Extract[T])(using parameter: Parameter[Extract[T]]): RightShift[T] =
    RightShift[T](noBagQuotLabel, false, value)

  @targetName("_rightShift")
  def >>(value: Extract[T])(using parameter: Parameter[Extract[T]]): RightShift[T] = rightShift(value)

  def DIV(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Div[T] =
    Div[T](noBagQuotLabel, false, cond, result)

  def MOD(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Mod[T] =
    Mod[T]("MOD", noBagQuotLabel, false, cond, result)

  def mod(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Mod[T] =
    Mod[T]("%", noBagQuotLabel, false, cond, result)

  @targetName("_mod")
  def %(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Mod[T] = mod(cond, result)

  def bitXOR(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitXOR[T] =
    BitXOR[T](noBagQuotLabel, false, value)

  @targetName("_bitXOR")
  def ^(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitXOR[T] = bitXOR(value)

  def bitFlip(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitFlip[T] =
    BitFlip[T](noBagQuotLabel, false, value)

  @targetName("_bitFlip")
  def ~(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitFlip[T] = bitFlip(value)

  def combine(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = ColumnQuery.MultiColumn[T]("+", this, other)

  @targetName("_combine")
  def ++(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = combine(other)

  def deduct(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = ColumnQuery.MultiColumn[T]("-", this, other)

  @targetName("_deduct")
  def --(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = deduct(other)

  def multiply(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] =
    ColumnQuery.MultiColumn[T]("*", this, other)

  @targetName("_multiply")
  def *(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = multiply(other)

  def smash(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = ColumnQuery.MultiColumn[T]("/", this, other)

  @targetName("_smash")
  def /(other: ColumnQuery[T]): ColumnQuery.MultiColumn[T] = smash(other)

  /** List of sub query methods */
  def _equals(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T]("=", noBagQuotLabel, value)

  @targetName("subQueryEquals")
  def ===(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = _equals(value)

  def orMore(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T](">=", noBagQuotLabel, value)

  @targetName("subQueryOrMore")
  def >=(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = orMore(value)

  def over(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T](">", noBagQuotLabel, value)

  @targetName("subQueryOver")
  def >(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = over(value)

  def lessThanOrEqual(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T]("<=", noBagQuotLabel, value)

  @targetName("subQueryLessThanOrEqual")
  def <=(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = lessThanOrEqual(value)

  def lessThan(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T]("<", noBagQuotLabel, value)

  @targetName("subQueryLessThan")
  def <(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = lessThan(value)

  def notEqual(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T]("<>", noBagQuotLabel, value)

  @targetName("subQueryNotEqual")
  def <>(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = notEqual(value)

  def IN(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
    SubQuery[T]("IN", noBagQuotLabel, value)

  /** List of join query methods */
  def _equals(other: Column[?]): ExpressionSyntax = JoinQuery("=", this, other)

  @targetName("joinEqual")
  def ===(other: Column[?]): ExpressionSyntax = _equals(other)

  def orMore(other: Column[?]): ExpressionSyntax = JoinQuery(">=", this, other)

  @targetName("joinOrMore")
  def >=(other: Column[?]): ExpressionSyntax = orMore(other)

  def over(other: Column[?]): ExpressionSyntax = JoinQuery(">", this, other)

  @targetName("joinOver")
  def >(other: Column[?]): ExpressionSyntax = over(other)

  def lessThanOrEqual(other: Column[?]): ExpressionSyntax = JoinQuery("<=", this, other)

  @targetName("joinLessThanOrEqual")
  def <=(other: Column[?]): ExpressionSyntax = lessThanOrEqual(other)

  def lessThan(other: Column[?]): ExpressionSyntax = JoinQuery("<", this, other)

  @targetName("joinLessThan")
  def <(other: Column[?]): ExpressionSyntax = lessThan(other)

  def notEqual(other: Column[?]): ExpressionSyntax = JoinQuery("<>", this, other)

  @targetName("joinNotEqual")
  def <>(other: Column[?]): ExpressionSyntax = notEqual(other)

  @targetName("join!Equal")
  def !==(other: Column[?]): ExpressionSyntax = JoinQuery("!=", this, other)

object ColumnQuery:

  def apply[T](
    _label:      String,
    _dataType:   DataType[T],
    _attributes: Seq[Attribute[T]],
    _alias:      Option[String]
  ): ColumnQuery[T] =
    new ColumnQuery[T]:
      override def label:      String            = _label
      override def dataType:   DataType[T]       = _dataType
      override def attributes: Seq[Attribute[T]] = _attributes
      override def alias:      Option[String]    = _alias

  def fromColumn[T](column: Column[T]): ColumnQuery[T] =
    new ColumnQuery[T]:
      override def label:      String            = column.label
      override def dataType:   DataType[T]       = column.dataType
      override def attributes: Seq[Attribute[T]] = column.attributes
      override def alias:      Option[String]    = column.alias

  private[ldbc] case class MultiColumn[T](flag: String, left: ColumnQuery[T], right: ColumnQuery[T]):
    val label: String = s"${ left.noBagQuotLabel } $flag ${ right.noBagQuotLabel }"

    def _equals(value: Extract[T])(using parameter: Parameter[Extract[T]]): MatchCondition[T] =
      MatchCondition[T](label, false, value)

    @targetName("multiColumnEquals")
    def ===(value: Extract[T])(using parameter: Parameter[Extract[T]]): MatchCondition[T] = _equals(value)

    def orMore(value: Extract[T])(using parameter: Parameter[Extract[T]]): OrMore[T] =
      OrMore[T](label, false, value)

    @targetName("multiColumnOrMore")
    def >=(value: Extract[T])(using parameter: Parameter[Extract[T]]): OrMore[T] = orMore(value)

    def over(value: Extract[T])(using parameter: Parameter[Extract[T]]): Over[T] = Over[T](label, false, value)

    @targetName("multiColumnOver")
    def >(value: Extract[T])(using parameter: Parameter[Extract[T]]): Over[T] = over(value)

    def lessThanOrEqual(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThanOrEqualTo[T] =
      LessThanOrEqualTo[T](label, false, value)

    @targetName("multiColumnLessThanOrEqual")
    def <=(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThanOrEqualTo[T] = lessThanOrEqual(
      value
    )

    def lessThan(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThan[T] =
      LessThan[T](label, false, value)

    @targetName("multiColumnLessThan")
    def <(value: Extract[T])(using parameter: Parameter[Extract[T]]): LessThan[T] = lessThan(value)

    def notEqual(value: Extract[T])(using parameter: Parameter[Extract[T]]): NotEqual[T] =
      NotEqual[T]("<>", label, false, value)

    @targetName("multiColumnNotEqual")
    def <>(value: Extract[T])(using parameter: Parameter[Extract[T]]): NotEqual[T] = notEqual(value)

    @targetName("multiColumn!Equal")
    def !==(value: Extract[T])(using parameter: Parameter[Extract[T]]): NotEqual[T] =
      NotEqual[T]("!=", label, false, value)

    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[A] = Is[A](label, false, value)

    def nullSafeEqual(value: Extract[T])(using parameter: Parameter[Extract[T]]): NullSafeEqual[T] =
      NullSafeEqual[T](label, false, value)

    @targetName("multiColumnNullSafeEqual")
    def <=>(value: Extract[T])(using parameter: Parameter[Extract[T]]): NullSafeEqual[T] = nullSafeEqual(value)

    def IN(value: Extract[T]*)(using parameter: Parameter[Extract[T]]): In[T] = In[T](label, false, value*)

    def BETWEEN(start: Extract[T], end: Extract[T])(using parameter: Parameter[Extract[T]]): Between[T] =
      Between[T](label, false, start, end)

    def LIKE(value: Extract[T])(using parameter: Parameter[Extract[T]]): Like[T] = Like[T](label, false, value)

    def LIKE_ESCAPE(like: Extract[T], escape: Extract[T])(using parameter: Parameter[Extract[T]]): LikeEscape[T] =
      LikeEscape[T](label, false, like, escape)

    def REGEXP(value: Extract[T])(using parameter: Parameter[Extract[T]]): Regexp[T] =
      Regexp[T](label, false, value)

    def leftShift(value: Extract[T])(using parameter: Parameter[Extract[T]]): LeftShift[T] =
      LeftShift[T](label, false, value)

    @targetName("multiColumnLeftShift")
    def <<(value: Extract[T])(using parameter: Parameter[Extract[T]]): LeftShift[T] = leftShift(value)

    def rightShift(value: Extract[T])(using parameter: Parameter[Extract[T]]): RightShift[T] =
      RightShift[T](label, false, value)

    @targetName("multiColumnRightShift")
    def >>(value: Extract[T])(using parameter: Parameter[Extract[T]]): RightShift[T] = rightShift(value)

    def DIV(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Div[T] =
      Div[T](label, false, cond, result)

    def MOD(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Mod[T] =
      Mod[T]("MOD", label, false, cond, result)

    def mod(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Mod[T] =
      Mod[T]("%", label, false, cond, result)

    @targetName("multiColumnMOD")
    def %(cond: Extract[T], result: Extract[T])(using parameter: Parameter[Extract[T]]): Mod[T] =
      mod(cond, result)

    def bitXOR(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitXOR[T] =
      BitXOR[T](label, false, value)

    @targetName("multiColumnBitXOR")
    def ^(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitXOR[T] = bitXOR(value)

    def bitFlip(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitFlip[T] =
      BitFlip[T](label, false, value)

    @targetName("multiColumnBitFlip")
    def ~(value: Extract[T])(using parameter: Parameter[Extract[T]]): BitFlip[T] = bitFlip(value)

    /** List of sub query methods */
    def _equals(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
      SubQuery[T]("=", label, value)

    @targetName("subQueryMultiColumnEquals")
    def ===(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = _equals(value)

    def orMore(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
      SubQuery[T](">=", label, value)

    @targetName("subQueryMultiColumnOrMore")
    def >=(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = orMore(value)

    def over(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = SubQuery[T](">", label, value)

    @targetName("subQueryMultiColumnOver")
    def >(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = over(value)

    def lessThanOrEqual(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
      SubQuery[T]("<=", label, value)

    @targetName("subQueryMultiColumnLessThanOrEqual")
    def <=(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = lessThanOrEqual(value)

    def lessThan(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
      SubQuery[T]("<", label, value)

    @targetName("subQueryMultiColumnLessThan")
    def <(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = lessThan(value)

    def notEqual(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] =
      SubQuery[T]("<>", label, value)

    @targetName("subQueryMultiColumnNotEqual")
    def <>(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = notEqual(value)

    def IN(value: Query[ColumnQuery[T] & Column[T]]): SubQuery[T] = SubQuery[T]("IN", label, value)
