/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.core.Column
import ldbc.core.interpreter.Extract
import ldbc.sql.Parameter
import ldbc.query.builder.ColumnQuery

/**
 * Trait for the syntax of expressions available in MySQL.
 *
 * SEE: https://dev.mysql.com/doc/refman/8.0/en/expressions.html
 */
private[ldbc] trait ExpressionSyntax:

  /**
   * Formula to determine
   */
  def flag: String

  /**
   * Statement of the expression to be judged
   */
  def statement: String

  /**
   * Trait to allow values to be set in PreparedStatement with only index by generating them from Parameter.
   */
  def parameter: Seq[Parameter.DynamicBinder]

  /**
   * Methods for combining expressions. Both conditions must be positive for the expression to be combined with this
   * method.
   *
   * @param other
   *   Right side of combined expression
   */
  def and(other: ExpressionSyntax): ExpressionSyntax =
    ExpressionSyntax.Pair(" AND ", this, other)
  def &&(other: ExpressionSyntax): ExpressionSyntax = and(other)

  /**
   * A method for combining expressions. The expressions combined with this method must have one of the conditions be
   * positive.
   *
   * @param other
   *   Right side of combined expression
   */
  def or(other: ExpressionSyntax): ExpressionSyntax =
    ExpressionSyntax.Pair(" OR ", this, other)
  def ||(other: ExpressionSyntax): ExpressionSyntax = or(other)

  /**
   * A method for combining expressions. The expressions combined with this method must be positive either individually
   * or in one of the combined conditions.
   *
   * @param other
   *   Right side of combined expression
   */
  def xor(other: ExpressionSyntax): ExpressionSyntax =
    ExpressionSyntax.Pair(" XOR ", this, other)

object ExpressionSyntax:

  private[ldbc] trait SingleValue[F[_], T] extends ExpressionSyntax:

    /**
     * Column name to be judged
     */
    def column: String

    /**
     * Value to be set for Statement.
     */
    def value: Extract[T]

  private[ldbc] trait MultiValue[F[_], T] extends ExpressionSyntax:

    /**
     * List of values to be set for the Statement.
     */
    def values: Seq[Extract[T]]

  /**
   * A model for joining expressions together.
   *
   * @param flag
   *   Symbols for joining expressions.
   * @param left
   *   Left side of combined expression
   * @param right
   *   Right side of combined expression
   */
  private[ldbc] case class Pair(
    flag:  String,
    left:  ExpressionSyntax,
    right: ExpressionSyntax
  ) extends ExpressionSyntax:

    override def statement: String =
      val result = (left, right) match
        case (l: Pair, r: Pair) =>
          l.left.statement + l.flag + l.right.statement + flag + r.left.statement + r.flag + r.right.statement
        case (l, r: Pair) => l.statement + flag + r.left.statement + r.flag + r.right.statement
        case (l: Pair, r) => l.left.statement + l.flag + l.right.statement + flag + r.statement
        case (l, r)          => l.statement + flag + r.statement
      s"($result)"

    override def parameter: Seq[Parameter.DynamicBinder] = left.parameter ++ right.parameter

  /**
   * Model for building sub queries.
   *
   * @param flag
   *   Sub query Conditional Expressions
   * @param column
   *   Name of the column for which the sub query is to be set
   * @param value
   *   Trait for constructing Statements that set conditions
   * @tparam F
   *   The effect type
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class SubQuery[F[_], T](
    flag:   String,
    column: String,
    value:  Query[F, ColumnQuery[F, T] & Column[T]]
  ) extends ExpressionSyntax:

    override def statement: String                       = s"$column $flag (${ value.statement })"
    override def parameter: Seq[Parameter.DynamicBinder] = value.params

  /**
   * Model for building join queries.
   *
   * @param flag
   *   Join query Conditional Expressions
   * @param left
   *   The left-hand column where the join join will be performed.
   * @param right
   *   The right-hand column where the join join will be performed.
   * @tparam F
   *   The effect type
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class JoinQuery[F[_], T](
    flag:  String,
    left:  Column[?],
    right: Column[?]
  ) extends ExpressionSyntax:

    override def statement = s"${ left.alias.fold(left.label)(name => s"$name.${ left.label }") } $flag ${ right.alias
        .fold(right.label)(name => s"$name.${ right.label }") }"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq.empty

  /** comparison operator */
  private[ldbc] case class MatchCondition[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "="
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: MatchCondition[F, T] = MatchCondition[F, T](this.column, true, this.value)

  private[ldbc] case class OrMore[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = ">="
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: OrMore[F, T] = OrMore[F, T](this.column, true, this.value)

  private[ldbc] case class Over[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = ">"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Over[F, T] = Over[F, T](this.column, true, this.value)

  private[ldbc] case class LessThanOrEqualTo[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "<="
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LessThanOrEqualTo[F, T] = LessThanOrEqualTo[F, T](this.column, true, this.value)

  private[ldbc] case class LessThan[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "<"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LessThan[F, T] = LessThan[F, T](this.column, true, this.value)

  private[ldbc] case class NotEqual[F[_], T](flag: String, column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: NotEqual[F, T] = NotEqual[F, T](this.flag, this.column, true, this.value)

  private[ldbc] case class Is[F[_], T <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](
    column: String,
    isNot:  Boolean,
    value:  T
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "IS"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq.empty
    override def statement: String =
      val not = if isNot then " NOT" else ""
      s"$column $flag$not $value"

    def NOT: Is[F, T] = Is[F, T](this.column, true, this.value)

  private[ldbc] case class NullSafeEqual[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "<=>"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: NullSafeEqual[F, T] = NullSafeEqual[F, T](this.column, true, this.value)

  private[ldbc] case class In[F[_], T](column: String, isNot: Boolean, values: Extract[T]*)(using
    _parameter: Parameter[Extract[T]]
  ) extends MultiValue[F, T]:
    override def flag:      String                       = "IN"
    override def parameter: Seq[Parameter.DynamicBinder] = values.map(Parameter.DynamicBinder(_))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$column $not$flag (${ values.map(_ => "?").mkString(", ") })"

    def NOT: In[F, T] = In[F, T](this.column, true, this.values*)

  private[ldbc] case class Between[F[_], T](column: String, isNot: Boolean, values: Extract[T]*)(using
    _parameter: Parameter[Extract[T]]
  ) extends MultiValue[F, T]:
    override def flag:      String                       = "BETWEEN"
    override def parameter: Seq[Parameter.DynamicBinder] = values.map(Parameter.DynamicBinder(_))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$column $not$flag ? AND ?"

    def NOT: Between[F, T] = Between[F, T](this.column, true, this.values*)

  private[ldbc] case class Like[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "LIKE"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Like[F, T] = Like[F, T](this.column, true, this.value)

  private[ldbc] case class LikeEscape[F[_], T](column: String, isNot: Boolean, values: Extract[T]*)(using
    _parameter: Parameter[Extract[T]]
  ) extends MultiValue[F, T]:
    override def flag:      String                       = "LIKE"
    override def parameter: Seq[Parameter.DynamicBinder] = values.map(Parameter.DynamicBinder(_))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? ESCAPE ?"

    def NOT: LikeEscape[F, T] = LikeEscape[F, T](this.column, true, this.values*)

  private[ldbc] case class Regexp[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "REGEXP"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Regexp[F, T] = Regexp[F, T](this.column, true, this.value)

  private[ldbc] case class LeftShift[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "<<"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LeftShift[F, T] = LeftShift[F, T](this.column, true, this.value)

  private[ldbc] case class RightShift[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = ">>"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: RightShift[F, T] = RightShift[F, T](this.column, true, this.value)

  private[ldbc] case class Div[F[_], T](column: String, isNot: Boolean, values: Extract[T]*)(using
    _parameter: Parameter[Extract[T]]
  ) extends MultiValue[F, T]:
    override def flag:      String                       = "DIV"
    override def parameter: Seq[Parameter.DynamicBinder] = values.map(Parameter.DynamicBinder(_))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? = ?"

    def NOT: Div[F, T] = Div[F, T](this.column, true, this.values*)

  private[ldbc] case class Mod[F[_], T](flag: String, column: String, isNot: Boolean, values: Extract[T]*)(using
    _parameter: Parameter[Extract[T]]
  ) extends MultiValue[F, T]:
    override def parameter: Seq[Parameter.DynamicBinder] = values.map(Parameter.DynamicBinder(_))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? = ?"

    def NOT: Mod[F, T] = Mod[F, T](this.flag, this.column, true, this.values*)

  private[ldbc] case class BitXOR[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "^"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: BitXOR[F, T] = BitXOR[F, T](this.column, true, this.value)

  private[ldbc] case class BitFlip[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using
    _parameter: Parameter[Extract[T]]
  ) extends SingleValue[F, T]:
    override def flag:      String                       = "~"
    override def parameter: Seq[Parameter.DynamicBinder] = Seq(Parameter.DynamicBinder(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$flag$column = ?"

    def NOT: BitFlip[F, T] = BitFlip[F, T](this.column, true, this.value)
