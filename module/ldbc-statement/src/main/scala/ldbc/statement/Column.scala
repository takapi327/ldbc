/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import cats.Applicative

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.sql.ResultSet
import ldbc.dsl.*
import ldbc.dsl.codec.{ Encoder, Decoder }
import ldbc.statement.interpreter.Extract
import ldbc.statement.Expression.*

/**
 * Trait for representing SQL Column
 *
 * @tparam T
 *   Scala types that match SQL DataType
 */
trait Column[T]:

  /** Column Field Name */
  def name: String

  /** Column alias name */
  def alias: Option[String]

  /** Functions for setting aliases on columns */
  def as(name: String): Column[T]

  /** Function to get a value of type T from a ResultSet */
  def decoder: Decoder[T]

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

  def opt: Column[Option[T]] = Column.Opt[T](name, alias, decoder)

  def count(using decoder: Decoder.Elem[Int]): Column.Count = Column.Count(name, alias)

  def asc:  OrderBy.Order[T] = OrderBy.Order.asc(this)
  def desc: OrderBy.Order[T] = OrderBy.Order.desc(this)

  private lazy val noBagQuotLabel: String = alias.getOrElse(name)

  private[ldbc] def list: List[Column[?]] = List(this)

  def _equals(value: Extract[T])(using Encoder[Extract[T]]): MatchCondition[T] =
    MatchCondition[T](noBagQuotLabel, false, value)

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
  def ===(value: Extract[T])(using Encoder[Extract[T]]): MatchCondition[T] = _equals(value)

  def orMore(value: Extract[T])(using Encoder[Extract[T]]): OrMore[T] =
    OrMore[T](noBagQuotLabel, false, value)

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
  def >=(value: Extract[T])(using Encoder[Extract[T]]): OrMore[T] = orMore(value)

  def over(value: Extract[T])(using Encoder[Extract[T]]): Over[T] =
    Over[T](noBagQuotLabel, false, value)

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
  def >(value: Extract[T])(using Encoder[Extract[T]]): Over[T] = over(value)

  def lessThanOrEqual(value: Extract[T])(using Encoder[Extract[T]]): LessThanOrEqualTo[T] =
    LessThanOrEqualTo[T](noBagQuotLabel, false, value)

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
  def <=(value: Extract[T])(using Encoder[Extract[T]]): LessThanOrEqualTo[T] = lessThanOrEqual(value)

  def lessThan(value: Extract[T])(using Encoder[Extract[T]]): LessThan[T] =
    LessThan[T](noBagQuotLabel, false, value)

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
  def <(value: Extract[T])(using Encoder[Extract[T]]): LessThan[T] = lessThan(value)

  def notEqual(value: Extract[T])(using Encoder[Extract[T]]): NotEqual[T] =
    NotEqual[T]("<>", noBagQuotLabel, false, value)

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
  def <>(value: Extract[T])(using Encoder[Extract[T]]): NotEqual[T] = notEqual(value)

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
  def !==(value: Extract[T])(using Encoder[Extract[T]]): NotEqual[T] =
    NotEqual[T]("!=", noBagQuotLabel, false, value)

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

  def nullSafeEqual(value: Extract[T])(using Encoder[Extract[T]]): NullSafeEqual[T] =
    NullSafeEqual[T](noBagQuotLabel, false, value)

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
  def <=>(value: Extract[T])(using Encoder[Extract[T]]): NullSafeEqual[T] = nullSafeEqual(value)

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
  def IN(value: Extract[T]*)(using Encoder[Extract[T]]): In[T] =
    In[T](noBagQuotLabel, false, value*)

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
  def BETWEEN(start: Extract[T], end: Extract[T])(using Encoder[Extract[T]]): Between[T] =
    Between[T](noBagQuotLabel, false, start, end)

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
  def LIKE(value: Extract[T])(using Encoder[Extract[T]]): Like[T] =
    Like[T](noBagQuotLabel, false, value)

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
  def LIKE_ESCAPE(like: Extract[T], escape: Extract[T])(using Encoder[Extract[T]]): LikeEscape[T] =
    LikeEscape[T](noBagQuotLabel, false, like, escape)

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
  def REGEXP(value: Extract[T])(using Encoder[Extract[T]]): Regexp[T] =
    Regexp[T](noBagQuotLabel, false, value)

  def leftShift(value: Extract[T])(using Encoder[Extract[T]]): LeftShift[T] =
    LeftShift[T](noBagQuotLabel, false, value)

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
  def <<(value: Extract[T])(using Encoder[Extract[T]]): LeftShift[T] = leftShift(value)

  def rightShift(value: Extract[T])(using Encoder[Extract[T]]): RightShift[T] =
    RightShift[T](noBagQuotLabel, false, value)

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
  def >>(value: Extract[T])(using Encoder[Extract[T]]): RightShift[T] = rightShift(value)

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
  def DIV(cond: Extract[T], result: Extract[T])(using Encoder[Extract[T]]): Div[T] =
    Div[T](noBagQuotLabel, false, cond, result)

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
  def MOD(cond: Extract[T], result: Extract[T])(using Encoder[Extract[T]]): Mod[T] =
    Mod[T]("MOD", noBagQuotLabel, false, cond, result)

  def mod(cond: Extract[T], result: Extract[T])(using Encoder[Extract[T]]): Mod[T] =
    Mod[T]("%", noBagQuotLabel, false, cond, result)

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
  def %(cond: Extract[T], result: Extract[T])(using Encoder[Extract[T]]): Mod[T] = mod(cond, result)

  def bitXOR(value: Extract[T])(using Encoder[Extract[T]]): BitXOR[T] =
    BitXOR[T](noBagQuotLabel, false, value)

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
  def ^(value: Extract[T])(using Encoder[Extract[T]]): BitXOR[T] = bitXOR(value)

  def bitFlip(value: Extract[T])(using Encoder[Extract[T]]): BitFlip[T] =
    BitFlip[T](noBagQuotLabel, false, value)

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
  def ~(value: Extract[T])(using Encoder[Extract[T]]): BitFlip[T] = bitFlip(value)

  def combine(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = Column.MultiColumn[T]("+", this, other)

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
  def ++(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = combine(other)

  def deduct(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = Column.MultiColumn[T]("-", this, other)

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
  def --(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = deduct(other)

  def multiply(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] =
    Column.MultiColumn[T]("*", this, other)

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
  def *(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = multiply(other)

  def smash(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = Column.MultiColumn[T]("/", this, other)

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
  def /(other: Column[T])(using Decoder.Elem[T]): Column.MultiColumn[T] = smash(other)

  /** List of sub query methods */
  def _equals(value: SQL): SubQuery[T] =
    SubQuery[T]("=", noBagQuotLabel, value)

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
  def ===(value: SQL): SubQuery[T] = _equals(value)

  def orMore(value: SQL): SubQuery[T] =
    SubQuery[T](">=", noBagQuotLabel, value)

  @targetName("subQueryOrMore")
  def >=(value: SQL): SubQuery[T] = orMore(value)

  def over(value: SQL): SubQuery[T] =
    SubQuery[T](">", noBagQuotLabel, value)

  @targetName("subQueryOver")
  def >(value: SQL): SubQuery[T] = over(value)

  def lessThanOrEqual(value: SQL): SubQuery[T] =
    SubQuery[T]("<=", noBagQuotLabel, value)

  @targetName("subQueryLessThanOrEqual")
  def <=(value: SQL): SubQuery[T] = lessThanOrEqual(value)

  def lessThan(value: SQL): SubQuery[T] =
    SubQuery[T]("<", noBagQuotLabel, value)

  @targetName("subQueryLessThan")
  def <(value: SQL): SubQuery[T] = lessThan(value)

  def notEqual(value: SQL): SubQuery[T] =
    SubQuery[T]("<>", noBagQuotLabel, value)

  @targetName("subQueryNotEqual")
  def <>(value: SQL): SubQuery[T] = notEqual(value)

  def IN(value: SQL): SubQuery[T] =
    SubQuery[T]("IN", noBagQuotLabel, value)

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

  type Extract[T] <: Tuple = T match
    case Column[t]               => t *: EmptyTuple
    case Column[t] *: EmptyTuple => t *: EmptyTuple
    case Column[t] *: ts         => t *: Extract[ts]

  given Applicative[Column] with
    override def pure[A](x: A): Column[A] = Pure(x)
    override def ap[A, B](ff: Column[A => B])(fa: Column[A]): Column[B] =
      new Column[B]:
        override def name: String = if ff.name.isEmpty then fa.name else s"${ ff.name }, ${ fa.name }"
        override def alias: Option[String] =
          (ff.alias, fa.alias) match
            case (Some(ff), Some(fa)) => Some(s"$ff, $fa")
            case (Some(ff), None)     => Some(s"$ff")
            case (None, Some(fa))     => Some(s"$fa")
            case (None, None)         => None
        override def as(name: String): Column[B] = this
        override def decoder: Decoder[B] = new Decoder[B]((resultSet: ResultSet, prefix: Option[String]) =>
          ff.decoder.decode(resultSet, prefix)(fa.decoder.decode(resultSet, prefix))
        )
        override def updateStatement: String =
          if ff.name.isEmpty then fa.updateStatement else s"${ ff.updateStatement }, ${ fa.updateStatement }"
        override def duplicateKeyUpdateStatement: String =
          if ff.name.isEmpty then fa.duplicateKeyUpdateStatement
          else s"${ ff.duplicateKeyUpdateStatement }, ${ fa.duplicateKeyUpdateStatement }"
        override def values: Int = ff.values + fa.values
        override def opt: Column[Option[B]] =
          val decoder = new Decoder[Option[B]]((resultSet: ResultSet, prefix: Option[String]) =>
            for
              v1 <- ff.opt.decoder.decode(resultSet, prefix)
              v2 <- fa.opt.decoder.decode(resultSet, prefix)
            yield v1(v2)
          )
          Impl[Option[B]](name, alias, decoder, Some(values), Some(updateStatement))
        override private[ldbc] def list: List[Column[?]] =
          if ff.name.isEmpty then fa.list else ff.list ++ fa.list

  case class Pure[T](value: T) extends Column[T]:
    override def name:             String         = ""
    override def alias:            Option[String] = None
    override def as(name: String): Column[T]      = this
    override def decoder: Decoder[T] =
      new Decoder[T]((resultSet: ResultSet, prefix: Option[String]) => value)
    override def insertStatement:             String          = ""
    override def updateStatement:             String          = ""
    override def duplicateKeyUpdateStatement: String          = ""
    override def values:                      Int             = 0
    override private[ldbc] def list:          List[Column[?]] = List.empty

  def apply[T](name: String)(using elem: Decoder.Elem[T]): Column[T] =
    Impl[T](name)

  def apply[T](name: String, alias: String)(using elem: Decoder.Elem[T]): Column[T] =
    Impl[T](name, s"$alias.$name")

  private[ldbc] case class Impl[T](
    name:    String,
    alias:   Option[String],
    decoder: Decoder[T],
    length:  Option[Int]    = None,
    update:  Option[String] = None
  ) extends Column[T]:
    override def as(name: String): Column[T] =
      this.copy(
        alias = Some(name),
        decoder =
          new Decoder[T]((resultSet: ResultSet, prefix: Option[String]) => decoder.decode(resultSet, Some(name)))
      )
    override def values:                      Int    = length.getOrElse(1)
    override def updateStatement:             String = update.getOrElse(s"$name = ?")
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

  object Impl:
    def apply[T](name: String)(using elem: Decoder.Elem[T]): Column[T] =
      val decoder = new Decoder[T]((resultSet: ResultSet, prefix: Option[String]) =>
        val column = prefix.map(_ + ".").getOrElse("") + name
        elem.decode(resultSet, column)
      )
      Impl[T](name, None, decoder)

    def apply[T](name: String, alias: String)(using elem: Decoder.Elem[T]): Column[T] =
      val decoder = new Decoder[T]((resultSet: ResultSet, prefix: Option[String]) =>
        elem.decode(resultSet, prefix.getOrElse(alias))
      )
      Impl[T](name, Some(alias), decoder)

  private[ldbc] case class Opt[T](
    name:     String,
    alias:    Option[String],
    _decoder: Decoder[T]
  ) extends Column[Option[T]]:
    override def as(name: String): Column[Option[T]] = Opt[T](this.name, Some(s"$name.${ this.name }"), _decoder)
    override def decoder: Decoder[Option[T]] =
      new Decoder[Option[T]]((resultSet: ResultSet, prefix: Option[String]) =>
        Option(_decoder.decode(resultSet, prefix.orElse(alias)))
      )
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

  private[ldbc] case class MultiColumn[T](
    flag:  String,
    left:  Column[T],
    right: Column[T]
  )(using elem: Decoder.Elem[T])
    extends Column[T]:
    override def name: String = s"${ left.noBagQuotLabel } $flag ${ right.noBagQuotLabel }"
    override def alias: Option[String] = Some(
      s"${ left.alias.getOrElse(left.name) } $flag ${ right.alias.getOrElse(right.name) }"
    )
    override def as(name: String): Column[T] = this
    override def decoder: Decoder[T] =
      new Decoder[T]((resultSet: ResultSet, prefix: Option[String]) =>
        elem.decode(resultSet, prefix.map(_ + ".").getOrElse("") + name)
      )
    override def insertStatement:             String = ""
    override def updateStatement:             String = ""
    override def duplicateKeyUpdateStatement: String = ""

  private[ldbc] case class Count(_name: String, _alias: Option[String])(using elem: Decoder.Elem[Int])
    extends Column[Int]:
    override def name:             String         = s"COUNT($_name)"
    override def alias:            Option[String] = _alias.map(a => s"COUNT($a)")
    override def as(name: String): Column[Int]    = this.copy(s"$name.${ _name }")
    override def decoder: Decoder[Int] = new Decoder[Int]((resultSet: ResultSet, prefix: Option[String]) =>
      elem.decode(resultSet, alias.getOrElse(name))
    )
    override def toString:                    String = name
    override def insertStatement:             String = ""
    override def updateStatement:             String = ""
    override def duplicateKeyUpdateStatement: String = ""
