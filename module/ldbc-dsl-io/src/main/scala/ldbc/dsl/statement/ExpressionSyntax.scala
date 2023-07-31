/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.statement

import ldbc.core.interpreter.Extract
import ldbc.dsl.Parameter

/**
 * Trait for the syntax of expressions available in MySQL.
 *
 * SEE: https://dev.mysql.com/doc/refman/8.0/en/expressions.html
 */
private[ldbc] trait ExpressionSyntax[F[_], T]:

  /**
   * Formula to determine
   */
  def flag: String

  /**
   * Column name to be judged
   */
  def column: String

  /**
   * Statement of the expression to be judged
   */
  def statement: String

  /**
   * Trait for setting Scala and Java values to PreparedStatement.
   */
  def parameter: Option[Parameter[F, Extract[T]]]

object ExpressionSyntax:

  private[ldbc] trait SingleValue[F[_], T] extends ExpressionSyntax[F, T]:
    def value: Extract[T]

  private[ldbc] trait MultiValue[F[_], T] extends ExpressionSyntax[F, T]:
    def value: Seq[Extract[T]]

  /** comparison operator */
  private[ldbc] case class MatchCondition[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "="
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: MatchCondition[F, T] = MatchCondition[F, T](this.column, true, this.value)

  private[ldbc] case class OrMore[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = ">="
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: OrMore[F, T] = OrMore[F, T](this.column, true, this.value)

  private[ldbc] case class Over[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = ">"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Over[F, T] = Over[F, T](this.column, true, this.value)

  private[ldbc] case class LessThanOrEqualTo[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "<="
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LessThanOrEqualTo[F, T] = LessThanOrEqualTo[F, T](this.column, true, this.value)

  private[ldbc] case class LessThan[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "<"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LessThan[F, T] = LessThan[F, T](this.column, true, this.value)

  private[ldbc] case class NotEqual[F[_], T](flag: String, column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: NotEqual[F, T] = NotEqual[F, T](this.flag, this.column, true, this.value)

  private[ldbc] case class Is[F[_], T <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](column: String, isNot: Boolean, value: T) extends SingleValue[F, T]:
    override def flag: String = "IS"
    override def parameter: Option[Parameter[F, T]] = None
    override def statement: String =
      val not = if isNot then " NOT" else ""
      s"$column $flag$not $value"

    def NOT: Is[F, T] = Is[F, T](this.column, true, this.value)

  private[ldbc] case class NullSafeEqual[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "<=>"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: NullSafeEqual[F, T] = NullSafeEqual[F, T](this.column, true, this.value)

  private[ldbc] case class In[F[_], T](column: String, isNot: Boolean, value: Extract[T]*)(using _parameter: Parameter[F, Extract[T]]) extends MultiValue[F, T]:
    override def flag: String = "IN"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$column $not$flag (${value.map(_ => "?").mkString(", ")})"

    def NOT: In[F, T] = In[F, T](this.column, true, this.value: _*)

  private[ldbc] case class Between[F[_], T](column: String, isNot: Boolean, value: Extract[T]*)(using _parameter: Parameter[F, Extract[T]]) extends MultiValue[F, T]:
    override def flag: String = "BETWEEN"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$column $not$flag ? AND ?"

    def NOT: Between[F, T] = Between[F, T](this.column, true, this.value: _*)

  private[ldbc] case class Like[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "LIKE"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Like[F, T] = Like[F, T](this.column, true, this.value)


  private[ldbc] case class LikeEscape[F[_], T](column: String, isNot: Boolean, value: Extract[T]*)(using _parameter: Parameter[F, Extract[T]]) extends MultiValue[F, T]:
    override def flag: String = "LIKE"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? ESCAPE ?"

    def NOT: LikeEscape[F, T] = LikeEscape[F, T](this.column, true, this.value: _*)

  private[ldbc] case class Regexp[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "REGEXP"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: Regexp[F, T] = Regexp[F, T](this.column, true, this.value)

  private[ldbc] case class LeftShift[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "<<"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: LeftShift[F, T] = LeftShift[F, T](this.column, true, this.value)

  private[ldbc] case class RightShift[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = ">>"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: RightShift[F, T] = RightShift[F, T](this.column, true, this.value)

  private[ldbc] case class Div[F[_], T](column: String, isNot: Boolean, value: Extract[T]*)(using _parameter: Parameter[F, Extract[T]]) extends MultiValue[F, T]:
    override def flag: String = "DIV"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? = ?"

    def NOT: Div[F, T] = Div[F, T](this.column, true, this.value: _*)

  private[ldbc] case class Mod[F[_], T](flag: String, column: String, isNot: Boolean, value: Extract[T]*)(using _parameter: Parameter[F, Extract[T]]) extends MultiValue[F, T]:
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ? = ?"

    def NOT: Mod[F, T] = Mod[F, T](this.flag, this.column, true, this.value: _*)

  private[ldbc] case class BitXOR[F[_], T](column: String, isNot: Boolean, value: Extract[T])(using _parameter: Parameter[F, Extract[T]]) extends SingleValue[F, T]:
    override def flag: String = "^"
    override def parameter: Option[Parameter[F, Extract[T]]] = Some(_parameter)

    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not$column $flag ?"

    def NOT: BitXOR[F, T] = BitXOR[F, T](this.column, true, this.value)

  private[ldbc] case class BitOr[F[_], T](isNot: Boolean, expr1: SingleValue[F, T], expr2: SingleValue[F, T]) extends MultiValue[F, T]:
    override def flag: String = "|"
    override def column: String = expr1.column
    override def value: Seq[Extract[T]] = Seq(expr1.value, expr2.value)
    override def parameter: Option[Parameter[F, Extract[T]]] = expr2.parameter
    override def statement: String =
      val not = if isNot then "NOT " else ""
      s"$not${ expr1.statement } $flag ${ expr2.statement }"

    def NOT: BitOr[F, T] = BitOr[F, T](true, this.expr1, this.expr2)
