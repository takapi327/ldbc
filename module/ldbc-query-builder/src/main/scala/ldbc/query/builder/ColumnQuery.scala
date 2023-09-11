/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.query.builder

import scala.annotation.targetName

import cats.data.Kleisli

import ldbc.core.*
import ldbc.core.attribute.Attribute
import ldbc.core.interpreter.Extract
import ldbc.sql.*
import ldbc.query.builder.statement.{ ExpressionSyntax, OrderBy, Query }
import ldbc.query.builder.statement.ExpressionSyntax.*

private[ldbc] case class ColumnQuery[F[_], T](
  label: String,
  dataType: DataType[T],
  attributes: Seq[Attribute[T]],
  _alias: Option[String],
  reader: ResultSetReader[F, T],
) extends Column[T]:

  override private[ldbc] def alias = _alias

  val read: Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
    reader.read(resultSet, alias.fold(label)(name => s"$name.$label"))
  }

  def read(index: Int): Kleisli[F, ResultSet[F], T] = Kleisli { resultSet =>
    reader.read(resultSet, index)
  }

  private val noBagQuotLabel: String =
    alias.fold(label)(name => s"$name.$label")

  def count: ColumnQuery[F, Int] = this.copy(
    label = s"COUNT($label)",
    dataType = DataType.Integer(None, false),
    attributes = Seq.empty,
    _alias = alias,
    reader = ResultSetReader.given_ResultSetReader_F_Int
  )

  def asc: OrderBy.Asc = OrderBy.Asc(this)

  def desc: OrderBy.Desc = OrderBy.Desc(this)

  def _equals(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): MatchCondition[F, T] = MatchCondition[F, T](noBagQuotLabel, false, value)

  @targetName("matchCondition")
  def ===(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): MatchCondition[F, T] = _equals(value)

  def orMore(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): OrMore[F, T] = OrMore[F, T](noBagQuotLabel, false, value)

  @targetName("_orMore")
  def >=(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): OrMore[F, T] = orMore(value)

  def over(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Over[F, T] = Over[F, T](noBagQuotLabel, false, value)

  @targetName("_over")
  def >(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Over[F, T] = over(value)

  def lessThanOrEqual(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThanOrEqualTo[F, T] =
    LessThanOrEqualTo[F, T](noBagQuotLabel, false, value)

  @targetName("_lessThanOrEqual")
  def <=(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThanOrEqualTo[F, T] = lessThanOrEqual(value)

  def lessThan(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThan[F, T] = LessThan[F, T](noBagQuotLabel, false, value)

  @targetName("_lessThan")
  def <(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThan[F, T] = lessThan(value)

  def notEqual(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NotEqual[F, T] = NotEqual[F, T]("<>", noBagQuotLabel, false, value)

  @targetName("_notEqual")
  def <>(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NotEqual[F, T] = notEqual(value)

  @targetName("_!equal")
  def !==(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NotEqual[F, T] = NotEqual[F, T]("!=", noBagQuotLabel, false, value)

  def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] =
    Is[F, A](noBagQuotLabel, false, value)

  def nullSafeEqual(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NullSafeEqual[F, T] =
    NullSafeEqual[F, T](noBagQuotLabel, false, value)

  @targetName("_nullSafeEqual")
  def <=>(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NullSafeEqual[F, T] = nullSafeEqual(value)

  def IN(value: Extract[T]*)(using parameter: Parameter[F, Extract[T]]): In[F, T] = In[F, T](noBagQuotLabel, false, value: _*)

  def BETWEEN(start: Extract[T], end: Extract[T])(using parameter: Parameter[F, Extract[T]]): Between[F, T] =
    Between[F, T](noBagQuotLabel, false, start, end)

  def LIKE(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Like[F, T] = Like[F, T](noBagQuotLabel, false, value)

  def LIKE_ESCAPE(like: Extract[T], escape: Extract[T])(using parameter: Parameter[F, Extract[T]]): LikeEscape[F, T] =
    LikeEscape[F, T](noBagQuotLabel, false, like, escape)

  def REGEXP(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Regexp[F, T] = Regexp[F, T](noBagQuotLabel, false, value)

  def leftShift(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LeftShift[F, T] = LeftShift[F, T](noBagQuotLabel, false, value)

  @targetName("_leftShift")
  def <<(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LeftShift[F, T] = leftShift(value)

  def rightShift(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): RightShift[F, T] = RightShift[F, T](noBagQuotLabel, false, value)

  @targetName("_rightShift")
  def >>(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): RightShift[F, T] = rightShift(value)

  def DIV(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Div[F, T] = Div[F, T](noBagQuotLabel, false, cond, result)

  def MOD(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Mod[F, T] =
    Mod[F, T]("MOD", noBagQuotLabel, false, cond, result)

  def mod(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Mod[F, T] =
    Mod[F, T]("%", noBagQuotLabel, false, cond, result)

  @targetName("_mod")
  def %(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Mod[F, T] = mod(cond, result)

  def bitXOR(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitXOR[F, T] = BitXOR[F, T](noBagQuotLabel, false, value)

  @targetName("_bitXOR")
  def ^(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitXOR[F, T] = bitXOR(value)

  def bitFlip(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitFlip[F, T] = BitFlip[F, T](noBagQuotLabel, false, value)

  @targetName("_bitFlip")
  def ~(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitFlip[F, T] = bitFlip(value)

  def combine(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = ColumnQuery.MultiColumn[F, T]("+", this, other)

  @targetName("_combine")
  def ++(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = combine(other)

  def deduct(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = ColumnQuery.MultiColumn[F, T]("-", this, other)

  @targetName("_deduct")
  def --(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = deduct(other)

  def multiply(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = ColumnQuery.MultiColumn[F, T]("*", this, other)

  @targetName("_multiply")
  def *(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = multiply(other)

  def smash(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = ColumnQuery.MultiColumn[F, T]("/", this, other)

  @targetName("_smash")
  def /(other: ColumnQuery[F, T]): ColumnQuery.MultiColumn[F, T] = smash(other)

  /** List of sub query methods */
  def _equals(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T]("=", noBagQuotLabel, value)

  @targetName("subQueryEquals")
  def ===(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = _equals(value)

  def orMore(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T](">=", noBagQuotLabel, value)

  @targetName("subQueryOrMore")
  def >=(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = orMore(value)

  def over(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T](">", noBagQuotLabel, value)

  @targetName("subQueryOver")
  def >(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = over(value)

  def lessThanOrEqual(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T]("<=", noBagQuotLabel, value)

  @targetName("subQueryLessThanOrEqual")
  def <=(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = lessThanOrEqual(value)

  def lessThan(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T]("<", noBagQuotLabel, value)

  @targetName("subQueryLessThan")
  def <(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = lessThan(value)

  def notEqual(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T]("<>", noBagQuotLabel, value)

  @targetName("subQueryNotEqual")
  def <>(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = notEqual(value)

  def IN(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
    SubQuery[F, T]("IN", noBagQuotLabel, value)

  /** List of join query methods */
  def _equals(other: Column[?]): ExpressionSyntax[F] = JoinQuery("=", this, other)

  @targetName("joinEqual")
  def ===(other: Column[?]): ExpressionSyntax[F] = _equals(other)

  def orMore(other: Column[?]): ExpressionSyntax[F] = JoinQuery(">=", this, other)

  @targetName("joinOrMore")
  def >=(other: Column[?]): ExpressionSyntax[F] = orMore(other)

  def over(other: Column[?]): ExpressionSyntax[F] = JoinQuery(">", this, other)

  @targetName("joinOver")
  def >(other: Column[?]): ExpressionSyntax[F] = over(other)

  def lessThanOrEqual(other: Column[?]): ExpressionSyntax[F] = JoinQuery("<=", this, other)

  @targetName("joinLessThanOrEqual")
  def <=(other: Column[?]): ExpressionSyntax[F] = lessThanOrEqual(other)

  def lessThan(other: Column[?]): ExpressionSyntax[F] = JoinQuery("<", this, other)

  @targetName("joinLessThan")
  def <(other: Column[?]): ExpressionSyntax[F] = lessThan(other)

  def notEqual(other: Column[?]): ExpressionSyntax[F] = JoinQuery("<>", this, other)

  @targetName("joinNotEqual")
  def <>(other: Column[?]): ExpressionSyntax[F] = notEqual(other)

  @targetName("join!Equal")
  def !==(other: Column[?]): ExpressionSyntax[F] = JoinQuery("!=", this, other)

object ColumnQuery:

  def fromColumn[F[_], T](column: Column[T])(using reader: ResultSetReader[F, T]): ColumnQuery[F, T] =
    ColumnQuery[F, T](
      label = column.label,
      dataType = column.dataType,
      attributes = column.attributes,
      _alias = column.alias,
      reader = reader
    )

  private[ldbc] case class MultiColumn[F[_], T](flag: String, left: ColumnQuery[F, T], right: ColumnQuery[F, T]):
    val label: String = s"${left.noBagQuotLabel} $flag ${right.noBagQuotLabel}"

    def _equals(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): MatchCondition[F, T] = MatchCondition[F, T](label, false, value)

    @targetName("multiColumnEquals")
    def ===(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): MatchCondition[F, T] = _equals(value)

    def orMore(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): OrMore[F, T] = OrMore[F, T](label, false, value)

    @targetName("multiColumnOrMore")
    def >=(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): OrMore[F, T] = orMore(value)

    def over(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Over[F, T] = Over[F, T](label, false, value)

    @targetName("multiColumnOver")
    def >(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Over[F, T] = over(value)

    def lessThanOrEqual(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThanOrEqualTo[F, T] =
      LessThanOrEqualTo[F, T](label, false, value)

    @targetName("multiColumnLessThanOrEqual")
    def <=(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThanOrEqualTo[F, T] = lessThanOrEqual(value)

    def lessThan(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThan[F, T] = LessThan[F, T](label, false, value)

    @targetName("multiColumnLessThan")
    def <(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LessThan[F, T] = lessThan(value)

    def notEqual(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NotEqual[F, T] = NotEqual[F, T]("<>", label, false, value)

    @targetName("multiColumnNotEqual")
    def <>(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NotEqual[F, T] = notEqual(value)

    @targetName("multiColumn!Equal")
    def !==(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NotEqual[F, T] = NotEqual[F, T]("!=", label, false, value)

    def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[F, A] = Is[F, A](label, false, value)

    def nullSafeEqual(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NullSafeEqual[F, T] = NullSafeEqual[F, T](label, false, value)

    @targetName("multiColumnNullSafeEqual")
    def <=>(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): NullSafeEqual[F, T] = nullSafeEqual(value)

    def IN(value: Extract[T]*)(using parameter: Parameter[F, Extract[T]]): In[F, T] = In[F, T](label, false, value: _*)

    def BETWEEN(start: Extract[T], end: Extract[T])(using parameter: Parameter[F, Extract[T]]): Between[F, T] = Between[F, T](label, false, start, end)

    def LIKE(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Like[F, T] = Like[F, T](label, false, value)

    def LIKE_ESCAPE(like: Extract[T], escape: Extract[T])(using parameter: Parameter[F, Extract[T]]): LikeEscape[F, T] =
      LikeEscape[F, T](label, false, like, escape)

    def REGEXP(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): Regexp[F, T] = Regexp[F, T](label, false, value)

    def leftShift(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LeftShift[F, T] = LeftShift[F, T](label, false, value)

    @targetName("multiColumnLeftShift")
    def <<(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): LeftShift[F, T] = leftShift(value)

    def rightShift(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): RightShift[F, T] = RightShift[F, T](label, false, value)

    @targetName("multiColumnRightShift")
    def >>(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): RightShift[F, T] = rightShift(value)

    def DIV(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Div[F, T] = Div[F, T](label, false, cond, result)

    def MOD(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Mod[F, T] = Mod[F, T]("MOD", label, false, cond, result)

    def mod(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Mod[F, T] = Mod[F, T]("%", label, false, cond, result)

    @targetName("multiColumnMOD")
    def %(cond: Extract[T], result: Extract[T])(using parameter: Parameter[F, Extract[T]]): Mod[F, T] = mod(cond, result)

    def bitXOR(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitXOR[F, T] = BitXOR[F, T](label, false, value)

    @targetName("multiColumnBitXOR")
    def ^(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitXOR[F, T] = bitXOR(value)

    def bitFlip(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitFlip[F, T] = BitFlip[F, T](label, false, value)

    @targetName("multiColumnBitFlip")
    def ~(value: Extract[T])(using parameter: Parameter[F, Extract[T]]): BitFlip[F, T] = bitFlip(value)

    /** List of sub query methods */
    def _equals(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("=", label, value)

    @targetName("subQueryMultiColumnEquals")
    def ===(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = _equals(value)

    def orMore(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T](">=", label, value)

    @targetName("subQueryMultiColumnOrMore")
    def >=(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = orMore(value)

    def over(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = SubQuery[F, T](">", label, value)

    @targetName("subQueryMultiColumnOver")
    def >(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = over(value)

    def lessThanOrEqual(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<=", label, value)

    @targetName("subQueryMultiColumnLessThanOrEqual")
    def <=(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = lessThanOrEqual(value)

    def lessThan(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<", label, value)

    @targetName("subQueryMultiColumnLessThan")
    def <(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = lessThan(value)

    def notEqual(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] =
      SubQuery[F, T]("<>", label, value)

    @targetName("subQueryMultiColumnNotEqual")
    def <>(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = notEqual(value)

    def IN(value: Query[F, ColumnQuery[F, T] & Column[T]]): SubQuery[F, T] = SubQuery[F, T]("IN", label, value)
