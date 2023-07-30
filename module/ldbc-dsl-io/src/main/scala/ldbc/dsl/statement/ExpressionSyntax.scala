/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.statement

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
  def parameter: Option[Parameter[F, T]]

object ExpressionSyntax:

  private[ldbc] trait SingleValue[F[_], T] extends ExpressionSyntax[F, T]:
    def value: T

    def NOT: Not[F, T] = Not[F, T]("NOT", this)
    def ! : Not[F, T] = Not[F, T]("!", this)
    def |(expr: SingleValue[F, T]) : BitOr[F, T] = BitOr[F, T](this, expr)

  private[ldbc] trait MultiValue[F[_], T] extends ExpressionSyntax[F, T]:
    def value: Seq[T]

  /** comparison operator */
  private[ldbc] case class MatchCondition[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "="
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class OrMore[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends ExpressionSyntax[F, T]:
    override def flag: String = ">="
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s" $flag NULL"
      case _ => s" $flag ?"

  private[ldbc] case class Over[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = ">"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s" $flag NULL"
      case _ => s" $flag ?"

  private[ldbc] case class LessThanOrEqualTo[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "<="
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s" $flag NULL"
      case _ => s" $flag ?"

  private[ldbc] case class LessThan[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "<"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s" $flag NULL"
      case _ => s" $flag ?"

  private[ldbc] case class NotEqual[F[_], T](flag: String, column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s" $flag NULL"
      case _ => s" $flag ?"

  private[ldbc] case class Not[F[_], T](flag: String, expr: SingleValue[F, T]) extends SingleValue[F, T]:
    override def column: String = expr.column
    override def value: T = expr.value
    override def parameter: Option[Parameter[F, T]] = expr.parameter
    override def statement: String = s"$column NOT ${ expr.statement.replace(column, "") }"

  private[ldbc] case class Is[F[_], T <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](column: String, value: T) extends SingleValue[F, T]:
    override def flag: String = "IS"
    override def parameter: Option[Parameter[F, T]] = None
    override def statement: String = s"$column $flag $value"

  private[ldbc] case class IsNot[F[_], T <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](column: String, value: T) extends SingleValue[F, T]:
    override def flag: String = "IS NOT"
    override def parameter: Option[Parameter[F, T]] = None
    override def statement: String = s"$column $flag $value"

  private[ldbc] case class NullSafeEqual[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "<=>"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class In[F[_], T](column: String, value: T*)(using _parameter: Parameter[F, T]) extends MultiValue[F, T]:
    override def flag: String = "IN"
    override def parameter: Option[Parameter[F, T]] = Some(_parameter)
    override def statement: String = s"$column $flag (${ value.map(_ => "?").mkString(", ") })"

  private[ldbc] case class Between[F[_], T](column: String, value: T*)(using _parameter: Parameter[F, T]) extends MultiValue[F, T]:
    override def flag: String = "BETWEEN"
    override def parameter: Option[Parameter[F, T]] = Some(_parameter)
    override def statement: String = s"$column $flag ? AND ?"

  private[ldbc] case class Like[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "LIKE"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class LikeEscape[F[_], T](column: String, value: T*)(using _parameter: Parameter[F, T]) extends MultiValue[F, T]:
    override def flag: String = "LIKE"
    override def parameter: Option[Parameter[F, T]] = Some(_parameter)
    override def statement: String = s"$column $flag ? ESCAPE ?"

  private[ldbc] case class Regexp[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "REGEXP"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class LeftShift[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "<<"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class RightShift[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = ">>"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class Div[F[_], T](column: String, value: T*)(using _parameter: Parameter[F, T]) extends MultiValue[F, T]:
    override def flag: String = "DIV"
    override def parameter: Option[Parameter[F, T]] = Some(_parameter)
    override def statement: String = s"$column $flag ? = ?"

  private[ldbc] case class Mod[F[_], T](flag: String, column: String, value: T*)(using _parameter: Parameter[F, T]) extends MultiValue[F, T]:
    override def parameter: Option[Parameter[F, T]] = Some(_parameter)
    override def statement: String = s"$column $flag ? = ?"

  private[ldbc] case class BitXOR[F[_], T](column: String, value: T)(using _parameter: Parameter[F, T]) extends SingleValue[F, T]:
    override def flag: String = "^"
    override def parameter: Option[Parameter[F, T]] = value match
      case None => None
      case _ => Some(_parameter)
    override def statement: String = value match
      case None => s"$column $flag NULL"
      case _ => s"$column $flag ?"

  private[ldbc] case class BitOr[F[_], T](expr1: SingleValue[F, T], expr2: SingleValue[F, T]) extends MultiValue[F, T]:
    override def flag: String = "|"
    override def column: String = expr1.column
    override def value: Seq[T] = Seq(expr1.value, expr2.value)
    override def parameter: Option[Parameter[F, T]] = expr2.parameter

    override def statement: String = s"${ expr1.statement } $flag ${ expr2.statement }"
