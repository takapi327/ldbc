/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import cats.InvariantSemigroupal

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.sql.ResultSet
import ldbc.dsl.*
import ldbc.dsl.codec.{ Encoder, Decoder }
import ldbc.statement.interpreter.Extract
import ldbc.statement.Expression.*

/**
 * Trait for representing SQL Column
 *
 * @tparam A
 *   Scala types that match SQL DataType
 */
trait Column[A]:
  self =>

  /** Column Field Name */
  def name: String

  /** Column alias name */
  def alias: Option[String]

  /** Functions for setting aliases on columns */
  def as(name: String): Column[A]

  /** Function to get a value of type T from a ResultSet */
  def decoder: Decoder[A]

  /** Function to set a value of type T to a PreparedStatement */
  def encoder: Encoder[A]

  /** Indicator of how many columns are specified */
  def values: Int = 1

  /** Used in select statement `Column, Column` used in the Insert statement */
  def statement: String = name

  /** Used in Insert statement `(Column, Column) VALUES (?,?)` used in the Insert statement */
  def insertStatement: String = s"($name) VALUES (${ List.fill(values)("?").mkString(",") })"

  /** Used in Update statement `Column = ?, Column = ?` used in the Update statement */
  def updateStatement: String

  /** Used in Update statement `Column = VALUES(Column), Column = VALUES(Column)` used in the Duplicate Key Update statement */
  def duplicateKeyUpdateStatement: String

  def opt: Column[Option[A]] = Column.Opt[A](name, alias, decoder, encoder)

  def count(using Decoder[Int]): Column.Count = Column.Count(name, alias)

  def asc:  OrderBy.Order[A] = OrderBy.Order.asc(this)
  def desc: OrderBy.Order[A] = OrderBy.Order.desc(this)

  private lazy val noBagQuotLabel: String = alias.getOrElse(name)

  private[ldbc] def list: List[Column[?]] = List(this)

  def imap[B](f: A => B)(g: B => A): Column[B] =
    new Column[B]:
      override def name:             String         = self.name
      override def alias:            Option[String] = self.alias
      override def as(name: String): Column[B]      = this
      override def decoder: Decoder[B] = self.decoder.map(f)
      override def encoder: Encoder[B] = (value: B) => self.encoder.encode(g(value))
      override def updateStatement:             String          = self.updateStatement
      override def duplicateKeyUpdateStatement: String          = self.duplicateKeyUpdateStatement
      override def values:                      Int             = self.values
      override private[ldbc] def list:          List[Column[?]] = self.list

  def product[B](fb: Column[B]): Column[(A, B)] =
    new Column[(A, B)]:
      override def name: String = s"${ self.name }, ${ fb.name }"
      override def alias: Option[String] = (self.alias, fb.alias) match
        case (Some(a), Some(b)) => Some(s"$a, $b")
        case (Some(a), None)    => Some(a)
        case (None, Some(b))    => Some(b)
        case (None, None)       => None
      override def as(name: String): Column[(A, B)]  = this
      override def decoder:          Decoder[(A, B)] = self.decoder.product(fb.decoder)
      override def encoder:          Encoder[(A, B)] = self.encoder.product(fb.encoder)
      override def updateStatement:  String          = s"${ self.updateStatement }, ${ fb.updateStatement }"
      override def duplicateKeyUpdateStatement: String =
        s"${ self.duplicateKeyUpdateStatement }, ${ fb.duplicateKeyUpdateStatement }"
      override def values: Int = self.values + fb.values
      override def opt: Column[Option[(A, B)]] =
        val decoder = new Decoder[Option[(A, B)]]:
          override def offset: Int = self.decoder.offset + fb.decoder.offset
          override def decode(resultSet: ResultSet, index: Int): Option[(A, B)] =
            for
              v1 <- self.opt.decoder.decode(resultSet, index)
              v2 <- fb.opt.decoder.decode(resultSet, index + self.decoder.offset)
            yield (v1, v2)

        val encoder: Encoder[Option[(A, B)]] = {
          case Some((v1, v2)) => self.opt.encoder.encode(Some(v1)).product(fb.opt.encoder.encode(Some(v2)))
          case None           => self.opt.encoder.encode(None).product(fb.opt.encoder.encode(None))
        }
        Column.Impl[Option[(A, B)]](
          name,
          alias,
          decoder,
          encoder,
          Some(values),
          Some(updateStatement)
        )
      override private[ldbc] def list: List[Column[?]] = self.list ++ fb.list

  def _equals(value: Extract[A])(using Encoder[Extract[A]]): MatchCondition[A] =
    MatchCondition[A](noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values match in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id === 1L)
   *   // SELECT name, age FROM user WHERE id = ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values match in a Where statement
   */
  @targetName("matchCondition")
  def ===(value: Extract[A])(using Encoder[Extract[A]]): MatchCondition[A] = _equals(value)

  def orMore(value: Extract[A])(using Encoder[Extract[A]]): OrMore[A] =
    OrMore[A](noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are greater than or equal to in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id >= 1L)
   *   // SELECT name, age FROM user WHERE id >= ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are greater than or equal to in a Where statement
   */
  @targetName("_orMore")
  def >=(value: Extract[A])(using Encoder[Extract[A]]): OrMore[A] = orMore(value)

  def over(value: Extract[A])(using Encoder[Extract[A]]): Over[A] =
    Over[A](noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are greater than in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id > 1L)
   *   // SELECT name, age FROM user WHERE id > ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are greater than in a Where statement
   */
  @targetName("_over")
  def >(value: Extract[A])(using Encoder[Extract[A]]): Over[A] = over(value)

  def lessThanOrEqual(value: Extract[A])(using Encoder[Extract[A]]): LessThanOrEqualTo[A] =
    LessThanOrEqualTo[A](noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are less than or equal to in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id <= 1L)
   *   // SELECT name, age FROM user WHERE id <= ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are less than or equal to in a Where statement
   */
  @targetName("_lessThanOrEqual")
  def <=(value: Extract[A])(using Encoder[Extract[A]]): LessThanOrEqualTo[A] = lessThanOrEqual(value)

  def lessThan(value: Extract[A])(using Encoder[Extract[A]]): LessThan[A] =
    LessThan[A](noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are less than in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id < 1L)
   *   // SELECT name, age FROM user WHERE id < ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are less than in a Where statement
   */
  @targetName("_lessThan")
  def <(value: Extract[A])(using Encoder[Extract[A]]): LessThan[A] = lessThan(value)

  def notEqual(value: Extract[A])(using Encoder[Extract[A]]): NotEqual[A] =
    NotEqual[A]("<>", noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are not equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id <> 1L)
   *   // SELECT name, age FROM user WHERE id <> ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are not equal in a Where statement
   */
  @targetName("_notEqual")
  def <>(value: Extract[A])(using Encoder[Extract[A]]): NotEqual[A] = notEqual(value)

  /**
   * A function that sets a WHERE condition to check whether the values are not equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id !== 1L)
   *   // SELECT name, age FROM user WHERE id != ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are not equal in a Where statement
   */
  @targetName("_!equal")
  def !==(value: Extract[A])(using Encoder[Extract[A]]): NotEqual[A] =
    NotEqual[A]("!=", noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id IS "NULL")
   *   // SELECT name, age FROM user WHERE id IS NULL
   * }}}
   *
   * @param value
   *   Value to compare
   * @tparam A
   *   Type of value to compare
   * @return
   *   Query to check whether values are equal in a Where statement.
   */
  def IS[A <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: A): Is[A] =
    Is[A](noBagQuotLabel, false, value)

  def nullSafeEqual(value: Extract[A])(using Encoder[Extract[A]]): NullSafeEqual[A] =
    NullSafeEqual[A](noBagQuotLabel, false, value)

  /**
   * A function that sets a WHERE condition to check whether the values are equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id <=> 1L)
   *   // SELECT name, age FROM user WHERE id <=> ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are equal in a Where statement
   */
  @targetName("_nullSafeEqual")
  def <=>(value: Extract[A])(using Encoder[Extract[A]]): NullSafeEqual[A] = nullSafeEqual(value)

  /**
   * A function that sets a WHERE condition to check whether the values are equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id IN (1L, 2L, 3L))
   *   // SELECT name, age FROM user WHERE id IN (?, ?, ?)
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are equal in a Where statement
   */
  def IN(value: Extract[A]*)(using Encoder[Extract[A]]): In[A] =
    In[A](noBagQuotLabel, false, value*)

  /**
   * A function that sets a WHERE condition to check whether a value is included in a specified range in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.updateAt BETWEEN (LocalDateTime.now(), LocalDateTime.now()))
   *   // SELECT name, age FROM user WHERE update_at BETWEEN ? AND ?
   * }}}
   *
   * @param start
   *   Start value
   * @param end
   *   End value
   * @return
   *   A query to check whether the value is included in a specified range in a Where statement
   */
  def BETWEEN(start: Extract[A], end: Extract[A])(using Encoder[Extract[A]]): Between[A] =
    Between[A](noBagQuotLabel, false, start, end)

  /**
   * A function to set a WHERE condition to check a value with an ambiguous search in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.name LIKE "Tak%")
   *   // SELECT name, age FROM user WHERE name LIKE ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check a value with an ambiguous search in a Where statement
   */
  def LIKE(value: Extract[A])(using Encoder[Extract[A]]): Like[A] =
    Like[A](noBagQuotLabel, false, value)

  /**
   * A function to set a WHERE condition to check a value with an ambiguous search in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.name LIKE "Tak%" ESCAPE "!")
   *   // SELECT name, age FROM user WHERE name LIKE ? ESCAPE ?
   * }}}
   *
   * @param like
   *   Value to compare
   * @param escape
   *   Value to escape
   * @return
   *   A query to check a value with an ambiguous search in a Where statement
   */
  def LIKE_ESCAPE(like: Extract[A], escape: Extract[A])(using Encoder[Extract[A]]): LikeEscape[A] =
    LikeEscape[A](noBagQuotLabel, false, like, escape)

  /**
   * A function to set a WHERE condition to check values in a regular expression in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.name REGEXP "Tak.*")
   *   // SELECT name, age FROM user WHERE name REGEXP ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check values in a regular expression in a Where statement
   */
  def REGEXP(value: Extract[A])(using Encoder[Extract[A]]): Regexp[A] =
    Regexp[A](noBagQuotLabel, false, value)

  def leftShift(value: Extract[A])(using Encoder[Extract[A]]): LeftShift[A] =
    LeftShift[A](noBagQuotLabel, false, value)

  /**
   * A function to set a WHERE condition to check whether the values are shifted to the left in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id << 1L)
   *   // SELECT name, age FROM user WHERE id << ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are shifted to the left in a Where statement
   */
  @targetName("_leftShift")
  def <<(value: Extract[A])(using Encoder[Extract[A]]): LeftShift[A] = leftShift(value)

  def rightShift(value: Extract[A])(using Encoder[Extract[A]]): RightShift[A] =
    RightShift[A](noBagQuotLabel, false, value)

  /**
   * A function to set a WHERE condition to check whether the values are shifted to the right in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id >> 1L)
   *   // SELECT name, age FROM user WHERE id >> ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check whether the values are shifted to the right in a Where statement
   */
  @targetName("_rightShift")
  def >>(value: Extract[A])(using Encoder[Extract[A]]): RightShift[A] = rightShift(value)

  /**
   * A function to set a WHERE condition to check whether the values are added in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id DIV(1L, 2L))
   *   // SELECT name, age FROM user WHERE id DIV ? = ?
   * }}}
   *
   * @param cond
   *   Condition to check
   * @param result
   *   Result to compare
   * @return
   *   A query to check whether the values are added in a Where statement
   */
  def DIV(cond: Extract[A], result: Extract[A])(using Encoder[Extract[A]]): Div[A] =
    Div[A](noBagQuotLabel, false, cond, result)

  /**
   * A function to set the WHERE condition for modulo operations in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id MOD(1L, 2L))
   *   // SELECT name, age FROM user WHERE id MOD ? = ?
   * }}}
   *
   * @param cond
   *   Condition to check
   * @param result
   *   Result to compare
   * @return
   *   A query to check the modulo operation in a Where statement
   */
  def MOD(cond: Extract[A], result: Extract[A])(using Encoder[Extract[A]]): Mod[A] =
    Mod[A]("MOD", noBagQuotLabel, false, cond, result)

  def mod(cond: Extract[A], result: Extract[A])(using Encoder[Extract[A]]): Mod[A] =
    Mod[A]("%", noBagQuotLabel, false, cond, result)

  /**
   * A function to set the WHERE condition for modulo operations in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id %(1L, 2L))
   *   // SELECT name, age FROM user WHERE id % ? = ?
   * }}}
   *
   * @param cond
   *   Condition to check
   * @param result
   *   Result to compare
   * @return
   *   A query to check the modulo operation in a Where statement
   */
  @targetName("_mod")
  def %(cond: Extract[A], result: Extract[A])(using Encoder[Extract[A]]): Mod[A] = mod(cond, result)

  def bitXOR(value: Extract[A])(using Encoder[Extract[A]]): BitXOR[A] =
    BitXOR[A](noBagQuotLabel, false, value)

  /**
   * A function to set the WHERE condition for bitwise XOR operations in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id ^ 1L)
   *   // SELECT name, age FROM user WHERE id ^ ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check the bitwise XOR operation in a Where statement
   */
  @targetName("_bitXOR")
  def ^(value: Extract[A])(using Encoder[Extract[A]]): BitXOR[A] = bitXOR(value)

  def bitFlip(value: Extract[A])(using Encoder[Extract[A]]): BitFlip[A] =
    BitFlip[A](noBagQuotLabel, false, value)

  /**
   * A function to set the WHERE condition for bitwise NOT operations in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id ~ 1L)
   *   // SELECT name, age FROM user WHERE id ~ ?
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to check the bitwise NOT operation in a Where statement
   */
  @targetName("_bitFlip")
  def ~(value: Extract[A])(using Encoder[Extract[A]]): BitFlip[A] = bitFlip(value)

  def combine(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] =
    Column.MultiColumn[A]("+", this, other)

  /**
   * A function to combine columns in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name ++ user.age)
   *   // SELECT name + age FROM user
   * }}}
   *
   * @param other
   *   Column to combine
   * @return
   *   A query to combine columns in a SELECT statement
   */
  @targetName("_combine")
  def ++(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] = combine(other)

  def deduct(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] =
    Column.MultiColumn[A]("-", this, other)

  /**
   * A function to subtract columns in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name -- user.age)
   *   // SELECT name - age FROM user
   * }}}
   *
   * @param other
   *   Column to subtract
   * @return
   *   A query to subtract columns in a SELECT statement
   */
  @targetName("_deduct")
  def --(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] = deduct(other)

  def multiply(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] =
    Column.MultiColumn[A]("*", this, other)

  /**
   * A function to multiply columns in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name * user.age)
   *   // SELECT name * age FROM user
   * }}}
   *
   * @param other
   *   Column to multiply
   * @return
   *   A query to multiply columns in a SELECT statement
   */
  @targetName("_multiply")
  def *(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] = multiply(other)

  def smash(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] =
    Column.MultiColumn[A]("/", this, other)

  /**
   * A function to divide columns in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name / user.age)
   *   // SELECT name / age FROM user
   * }}}
   *
   * @param other
   *   Column to divide
   * @return
   *   A query to divide columns in a SELECT statement
   */
  @targetName("_smash")
  def /(other: Column[A])(using Decoder[A], Encoder[A]): Column.MultiColumn[A] = smash(other)

  /** List of sub query methods */
  def _equals(value: SQL): SubQuery[A] =
    SubQuery[A]("=", noBagQuotLabel, value)

  /**
   * A function to perform a comparison with the column of interest using a subquery.
   *
   * {{{
   *   val sub: SQL = ???
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id === sub)
   *   // SELECT name, age FROM user WHERE id = (SELECT ...)
   * }}}
   *
   * @param value
   *   Value to compare
   * @return
   *   A query to compare with the column of interest using a subquery
   */
  @targetName("subQueryEquals")
  def ===(value: SQL): SubQuery[A] = _equals(value)

  def orMore(value: SQL): SubQuery[A] =
    SubQuery[A](">=", noBagQuotLabel, value)

  @targetName("subQueryOrMore")
  def >=(value: SQL): SubQuery[A] = orMore(value)

  def over(value: SQL): SubQuery[A] =
    SubQuery[A](">", noBagQuotLabel, value)

  @targetName("subQueryOver")
  def >(value: SQL): SubQuery[A] = over(value)

  def lessThanOrEqual(value: SQL): SubQuery[A] =
    SubQuery[A]("<=", noBagQuotLabel, value)

  @targetName("subQueryLessThanOrEqual")
  def <=(value: SQL): SubQuery[A] = lessThanOrEqual(value)

  def lessThan(value: SQL): SubQuery[A] =
    SubQuery[A]("<", noBagQuotLabel, value)

  @targetName("subQueryLessThan")
  def <(value: SQL): SubQuery[A] = lessThan(value)

  def notEqual(value: SQL): SubQuery[A] =
    SubQuery[A]("<>", noBagQuotLabel, value)

  @targetName("subQueryNotEqual")
  def <>(value: SQL): SubQuery[A] = notEqual(value)

  def IN(value: SQL): SubQuery[A] =
    SubQuery[A]("IN", noBagQuotLabel, value)

  /** List of join query methods */
  def _equals(other: Column[?]): Expression = JoinQuery("=", this, other)

  @targetName("joinEqual")
  def ===(other: Column[?]): Expression = _equals(other)

  def orMore(other: Column[?]): Expression = JoinQuery(">=", this, other)

  @targetName("joinOrMore")
  def >=(other: Column[?]): Expression = orMore(other)

  def over(other: Column[?]): Expression = JoinQuery(">", this, other)

  @targetName("joinOver")
  def >(other: Column[?]): Expression = over(other)

  def lessThanOrEqual(other: Column[?]): Expression = JoinQuery("<=", this, other)

  @targetName("joinLessThanOrEqual")
  def <=(other: Column[?]): Expression = lessThanOrEqual(other)

  def lessThan(other: Column[?]): Expression = JoinQuery("<", this, other)

  @targetName("joinLessThan")
  def <(other: Column[?]): Expression = lessThan(other)

  def notEqual(other: Column[?]): Expression = JoinQuery("<>", this, other)

  @targetName("joinNotEqual")
  def <>(other: Column[?]): Expression = notEqual(other)

  @targetName("join!Equal")
  def !==(other: Column[?]): Expression = JoinQuery("!=", this, other)

  override def toString: String = name

object Column extends TwiddleSyntax[Column]:

  type Extract[A] <: Tuple = A match
    case Column[t]               => t *: EmptyTuple
    case Column[t] *: EmptyTuple => t *: EmptyTuple
    case Column[t] *: ts         => t *: Extract[ts]

  given InvariantSemigroupal[Column] with
    override def imap[A, B](fa:    Column[A])(f:  A => B)(g: B => A): Column[B]      = fa.imap(f)(g)
    override def product[A, B](fa: Column[A], fb: Column[B]):         Column[(A, B)] = fa product fb

  case class Pure[A](value: A) extends Column[A]:
    override def name:             String         = ""
    override def alias:            Option[String] = None
    override def as(name: String): Column[A]      = this
    override def decoder: Decoder[A] = new Decoder[A]:
      override def offset: Int = 0
      override def decode(resultSet: ResultSet, index: Int): A = value
    override def encoder:                     Encoder[A]      = (value: A) => Encoder.Encoded.success(List.empty)
    override def insertStatement:             String          = ""
    override def updateStatement:             String          = ""
    override def duplicateKeyUpdateStatement: String          = ""
    override def values:                      Int             = 0
    override private[ldbc] def list:          List[Column[?]] = List.empty

  def apply[A](name: String)(using Decoder[A], Encoder[A]): Column[A] =
    Impl[A](name)

  def apply[A](name: String, alias: String)(using Decoder[A], Encoder[A]): Column[A] =
    Impl[A](name, s"$alias.$name")

  private[ldbc] case class Impl[A](
    name:    String,
    alias:   Option[String],
    decoder: Decoder[A],
    encoder: Encoder[A],
    length:  Option[Int]    = None,
    update:  Option[String] = None
  ) extends Column[A]:
    override def as(name: String): Column[A] =
      this.copy(alias = Some(name))
    override def values:                      Int    = length.getOrElse(1)
    override def updateStatement:             String = update.getOrElse(s"$name = ?")
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

  object Impl:
    def apply[A](name: String)(using decoder: Decoder[A], encoder: Encoder[A]): Column[A] =
      Impl[A](name, None, decoder, encoder)

    def apply[A](name: String, alias: String)(using decoder: Decoder[A], encoder: Encoder[A]): Column[A] =
      Impl[A](name, Some(alias), decoder, encoder)

  private[ldbc] case class Opt[A](
    name:     String,
    alias:    Option[String],
    _decoder: Decoder[A],
    _encoder: Encoder[A]
  ) extends Column[Option[A]]:
    override def as(name: String): Column[Option[A]]  = this.copy(alias = Some(s"$name.${ this.name }"))
    override def decoder:          Decoder[Option[A]] = _decoder.opt
    override def encoder: Encoder[Option[A]] = {
      case Some(v) => _encoder.encode(v)
      case None    => Encoder.Encoded.success(List(None))
    }
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

  private[ldbc] case class MultiColumn[A](
    flag:  String,
    left:  Column[A],
    right: Column[A]
  )(using _decoder: Decoder[A], _encoder: Encoder[A])
    extends Column[A]:
    override def name: String = s"${ left.noBagQuotLabel } $flag ${ right.noBagQuotLabel }"
    override def alias: Option[String] = Some(
      s"${ left.alias.getOrElse(left.name) } $flag ${ right.alias.getOrElse(right.name) }"
    )
    override def as(name: String): Column[A] = this
    override def decoder: Decoder[A] = _decoder
    override def encoder:                     Encoder[A] = _encoder
    override def insertStatement:             String     = ""
    override def updateStatement:             String     = ""
    override def duplicateKeyUpdateStatement: String     = ""

  private[ldbc] case class Count(_name: String, _alias: Option[String])(using
    _decoder:     Decoder[Int],
    _encoder: Encoder[Int]
  ) extends Column[Int]:
    override def name:             String         = s"COUNT($_name)"
    override def alias:            Option[String] = _alias.map(a => s"COUNT($a)")
    override def as(name: String): Column[Int]    = this.copy(s"$name.${ _name }")
    override def decoder: Decoder[Int] = _decoder
    override def encoder:                     Encoder[Int] = _encoder
    override def toString:                    String       = name
    override def insertStatement:             String       = ""
    override def updateStatement:             String       = ""
    override def duplicateKeyUpdateStatement: String       = ""
