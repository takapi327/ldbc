/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.*
import ldbc.dsl.codec.Encoder

/**
 * Trait for the syntax of expressions available in MySQL.
 *
 * SEE: https://dev.mysql.com/doc/refman/8.0/en/expressions.html
 */
sealed trait Expression:

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
  def parameter: List[Parameter.Dynamic]

  /**
   * Methods for combining expressions. Both conditions must be positive for the expression to be combined with this
   * method.
   *
   * @param other
   *   Right side of combined expression
   */
  def and(other: Expression): Expression =
    Expression.Pair(" AND ", this, other)
  def &&(other: Expression): Expression = and(other)

  /**
   * A method for combining expressions. The expressions combined with this method must have one of the conditions be
   * positive.
   *
   * @param other
   *   Right side of combined expression
   */
  def or(other: Expression): Expression =
    Expression.Pair(" OR ", this, other)
  def ||(other: Expression): Expression = or(other)

  /**
   * A method for combining expressions. The expressions combined with this method must be positive either individually
   * or in one of the combined conditions.
   *
   * @param other
   *   Right side of combined expression
   */
  def xor(other: Expression): Expression =
    Expression.Pair(" XOR ", this, other)

object Expression:

  private[ldbc] trait SingleValue[T] extends Expression:

    /**
     * Column name to be judged
     */
    def column: String

    /**
     * Value to be set for Statement.
     */
    def value: T

  private[ldbc] trait MultiValue[T] extends Expression:

    /**
     * List of values to be set for the Statement.
     */
    def values: Seq[T]

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
    left:  Expression,
    right: Expression
  ) extends Expression:

    override def statement: String =
      val result = (left, right) match
        case (l: Pair, r: Pair) =>
          l.left.statement + l.flag + l.right.statement + flag + r.left.statement + r.flag + r.right.statement
        case (l, r: Pair) => l.statement + flag + r.left.statement + r.flag + r.right.statement
        case (l: Pair, r) => l.left.statement + l.flag + l.right.statement + flag + r.statement
        case (l, r)       => l.statement + flag + r.statement
      s"($result)"

    override def parameter: List[Parameter.Dynamic] = left.parameter ++ right.parameter

  /**
   * Model for building sub queries.
   *
   * @param flag
   *   Sub query Conditional Expressions
   * @param column
   *   Name of the column for which the sub query is to be set
   * @param value
   *   Trait for constructing Statements that set conditions
   * @tparam T
   *   Scala types that match SQL DataType
   */
  private[ldbc] case class SubQuery[T](
    flag:   String,
    column: String,
    value:  SQL
  ) extends Expression:

    override def statement: String                  = s"$column $flag (${ value.statement })"
    override def parameter: List[Parameter.Dynamic] = value.params

  /**
   * Model for building join queries.
   *
   * @param flag
   * Join query Conditional Expressions
   * @param left
   * The left-hand column where the join join will be performed.
   * @param right
   * The right-hand column where the join join will be performed.
   * @tparam F
   * The effect type
   * @tparam T
   * Scala types that match SQL DataType
   */
  private[ldbc] case class JoinQuery[F[_], T](
    flag:  String,
    left:  Column[?],
    right: Column[?]
  ) extends Expression:

    override def statement = s"${ left.alias.getOrElse(left.name) } $flag ${ right.alias.getOrElse(right.name) }"
    override def parameter: List[Parameter.Dynamic] = List.empty

  /** comparison operator */
  private[ldbc] case class MatchCondition[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag:      String                  = "="
    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: MatchCondition[T] = MatchCondition[T](this.column, true, this.value)

  private[ldbc] case class OrMore[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = ">="

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: OrMore[T] = OrMore[T](this.column, true, this.value)

  private[ldbc] case class Over[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = ">"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Over[T] = Over[T](this.column, true, this.value)

  private[ldbc] case class LessThanOrEqualTo[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "<="

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LessThanOrEqualTo[T] = LessThanOrEqualTo[T](this.column, true, this.value)

  private[ldbc] case class LessThan[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "<"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LessThan[T] = LessThan[T](this.column, true, this.value)

  private[ldbc] case class NotEqual[T](flag: String, column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: NotEqual[T] = NotEqual[T](this.flag, this.column, true, this.value)

  private[ldbc] case class Is[T <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](
    column: String,
    isNot:  Boolean,
    value:  T
  ) extends SingleValue[T]:
    override def flag: String = "IS"

    override def parameter: List[Parameter.Dynamic] = List.empty

    override def statement: String =
      val not = if isNot then " NOT" else ""
      s"$column $flag$not $value"

    def NOT: Is[T] = Is[T](this.column, true, this.value)

  private[ldbc] case class NullSafeEqual[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "<=>"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: NullSafeEqual[T] = NullSafeEqual[T](this.column, true, this.value)

  private[ldbc] case class In[T](column: String, isNot: Boolean, values: T*)(using
    encoder: Encoder[T]
  ) extends MultiValue[T]:
    override def flag: String = "IN"

    override def parameter: List[Parameter.Dynamic] =
      values.flatMap(value => Parameter.Dynamic.many(encoder.encode(value))).toList

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$column $not$flag (${ values.map(_ => "?").mkString(", ") })"

    def NOT: In[T] = In[T](this.column, true, this.values*)

  private[ldbc] case class Between[T](column: String, isNot: Boolean, values: T*)(using
    encoder: Encoder[T]
  ) extends MultiValue[T]:
    override def flag: String = "BETWEEN"

    override def parameter: List[Parameter.Dynamic] =
      values.flatMap(value => Parameter.Dynamic.many(encoder.encode(value))).toList

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$column $not$flag ? AND ?"

    def NOT: Between[T] = Between[T](this.column, true, this.values*)

  private[ldbc] case class Like[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "LIKE"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Like[T] = Like[T](this.column, true, this.value)

  private[ldbc] case class LikeEscape[T](column: String, isNot: Boolean, values: T*)(using
    encoder: Encoder[T]
  ) extends MultiValue[T]:
    override def flag: String = "LIKE"

    override def parameter: List[Parameter.Dynamic] =
      values.flatMap(value => Parameter.Dynamic.many(encoder.encode(value))).toList

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? ESCAPE ?"

    def NOT: LikeEscape[T] = LikeEscape[T](this.column, true, this.values*)

  private[ldbc] case class Regexp[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "REGEXP"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Regexp[T] = Regexp[T](this.column, true, this.value)

  private[ldbc] case class LeftShift[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "<<"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LeftShift[T] = LeftShift[T](this.column, true, this.value)

  private[ldbc] case class RightShift[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = ">>"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: RightShift[T] = RightShift[T](this.column, true, this.value)

  private[ldbc] case class Div[T](column: String, isNot: Boolean, values: T*)(using
    encoder: Encoder[T]
  ) extends MultiValue[T]:
    override def flag: String = "DIV"

    override def parameter: List[Parameter.Dynamic] =
      values.flatMap(value => Parameter.Dynamic.many(encoder.encode(value))).toList

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? = ?"

    def NOT: Div[T] = Div[T](this.column, true, this.values*)

  private[ldbc] case class Mod[T](flag: String, column: String, isNot: Boolean, values: T*)(using
    encoder: Encoder[T]
  ) extends MultiValue[T]:
    override def parameter: List[Parameter.Dynamic] =
      values.flatMap(value => Parameter.Dynamic.many(encoder.encode(value))).toList

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? = ?"

    def NOT: Mod[T] = Mod[T](this.flag, this.column, true, this.values*)

  private[ldbc] case class BitXOR[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "^"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: BitXOR[T] = BitXOR[T](this.column, true, this.value)

  private[ldbc] case class BitFlip[T](column: String, isNot: Boolean, value: T)(using
    encoder: Encoder[T]
  ) extends SingleValue[T]:
    override def flag: String = "~"

    override def parameter: List[Parameter.Dynamic] = Parameter.Dynamic.many(encoder.encode(value))

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$flag$column = ?"

    def NOT: BitFlip[T] = BitFlip[T](this.column, true, this.value)
