/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.dsl.statement

/**
 * Trait for the syntax of expressions available in MySQL.
 *
 * SEE: https://dev.mysql.com/doc/refman/8.0/en/expressions.html
 */
private[ldbc] trait ExpressionSyntax:
  def flag: String
  val statement: String

object ExpressionSyntax:
  
  private[ldbc] trait WithValue extends ExpressionSyntax:
    override val statement: String = s" $flag ?"
  object WithValue:
    def apply(_flag: String): WithValue = new WithValue:
      override def flag: String = _flag

  private[ldbc] trait NoValue extends ExpressionSyntax:
    override val statement: String = s" $flag"
  object NoValue:
    def apply(_flag: String): NoValue = new NoValue:
      override def flag: String = _flag

  val OR = WithValue("OR")
  val || = WithValue("||")
  val AND = WithValue("AND")
  val && = WithValue("&&")
  val NOT = NoValue("NOT")
  val ! = NoValue("!")
  val IS_NULL = NoValue("IS NULL")
  val IS_NOT_NULL = NoValue("IS NOT NULL")
  val <=> = WithValue("<=>")

  /** comparison operator */
  val === = WithValue("=")
  val >= = WithValue(">=")
  val > = WithValue(">")
  val <= = WithValue("<=")
  val < = WithValue("<")
  val <> = WithValue("<>")
  val !== = WithValue("!=")

  /** predicate */
  val IN = WithValue("IN")
  val NOT_IN = WithValue("NOT IN")
  val BETWEEN = WithValue("BETWEEN")
  val NOT_BETWEEN = WithValue("NOT BETWEEN")
  val SOUNDS_LIKE = WithValue("SOUNDS LIKE")
  val LIKE = WithValue("LIKE")
  val NOT_LIKE = WithValue("NOT LIKE")
  val LIKE_ESCAPE = WithValue("LIKE")
  val NOT_LIKE_ESCAPE = WithValue("NOT LIKE")
  val REGEXP = WithValue("REGEXP")
  val NOT_REGEXP = WithValue("NOT REGEXP")

  /** bit expr */
  val | = WithValue("|")
  val & = WithValue("&")
  val << = WithValue("<<")
  val >> = WithValue(">>")
  val + = WithValue("+")
  val - = WithValue("-")
  val * = WithValue("*")
  val / = WithValue("/")
  val DIV = WithValue("DIV")
  val MOD = WithValue("MOD")
  val % = WithValue("%")
  val ^ = WithValue("^")
