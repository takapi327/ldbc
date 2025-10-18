/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import cats.data.NonEmptyList
import cats.InvariantSemigroupal

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.dsl.*
import ldbc.dsl.codec.*

import ldbc.statement.Expression.*

import ldbc.free.ResultSetIO

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

  def count(using Codec[Int]): Column.Count = Column.Count(name, alias)

  def asc:  OrderBy.Order[A] = OrderBy.Order.asc(this)
  def desc: OrderBy.Order[A] = OrderBy.Order.desc(this)

  private lazy val noBagQuotLabel: String = alias.getOrElse(name)

  private[ldbc] def list: List[Column[?]] = List(this)

  def imap[B](f: A => B)(g: B => A): Column[B] =
    new Column[B]:
      override def name:                        String          = self.name
      override def alias:                       Option[String]  = self.alias
      override def as(name: String):            Column[B]       = this
      override def decoder:                     Decoder[B]      = self.decoder.map(f)
      override def encoder:                     Encoder[B]      = (value: B) => self.encoder.encode(g(value))
      override def updateStatement:             String          = self.updateStatement
      override def duplicateKeyUpdateStatement: String          = self.duplicateKeyUpdateStatement
      override def values:                      Int             = self.values
      override private[ldbc] def list:          List[Column[?]] = self.list

  def product[B](fb: Column[B]): Column[(A, B)] =
    new Column[(A, B)]:
      override def name:  String         = s"${ self.name }, ${ fb.name }"
      override def alias: Option[String] = (self.alias, fb.alias) match
        case (Some(a), Some(b)) => Some(s"$a, $b")
        case (Some(a), None)    => Some(a)
        case (None, Some(b))    => Some(b)
        case (None, None)       => None
      override def as(name: String):            Column[(A, B)]  = this
      override def decoder:                     Decoder[(A, B)] = self.decoder.product(fb.decoder)
      override def encoder:                     Encoder[(A, B)] = self.encoder.product(fb.encoder)
      override def updateStatement:             String          = s"${ self.updateStatement }, ${ fb.updateStatement }"
      override def duplicateKeyUpdateStatement: String          =
        s"${ self.duplicateKeyUpdateStatement }, ${ fb.duplicateKeyUpdateStatement }"
      override def values: Int                    = self.values + fb.values
      override def opt:    Column[Option[(A, B)]] =
        val decoder = new Decoder[Option[(A, B)]]:
          override def offset: Int = self.decoder.offset + fb.decoder.offset
          override def decode(index: Int, statement: String): ResultSetIO[Either[Decoder.Error, Option[(A, B)]]] =
            for
              v1E <- self.opt.decoder.decode(index, statement)
              v2E <- fb.opt.decoder.decode(index + self.decoder.offset, statement)
            yield v1E.flatMap(ap => v2E.map(bp => ap.flatMap(a => bp.map(b => (a, b)))))

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

  def _equals[B](value: B)(using Encoder[B], A =:= Option[B]): MatchCondition[B] =
    MatchCondition(noBagQuotLabel, false, value)

  def _equals(value: A)(using Encoder[A]): MatchCondition[A] =
    MatchCondition(noBagQuotLabel, false, value)

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
  def ===(value: A)(using Encoder[A]): MatchCondition[A] = _equals(value)
  @targetName("matchCondition")
  def ===[B](value: B)(using Encoder[B], A =:= Option[B]): MatchCondition[B] = _equals(value)

  def orMore(value:    A)(using Encoder[A]):                  OrMore[A] = OrMore(noBagQuotLabel, false, value)
  def orMore[B](value: B)(using Encoder[B], A =:= Option[B]): OrMore[B] = OrMore(noBagQuotLabel, false, value)

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
  def >=(value: A)(using Encoder[A]): OrMore[A] = orMore(value)
  @targetName("_orMore")
  def >=[B](value: B)(using Encoder[B], A =:= Option[B]): OrMore[B] = orMore(value)

  def over(value:    A)(using Encoder[A]):                  Over[A] = Over(noBagQuotLabel, false, value)
  def over[B](value: B)(using Encoder[B], A =:= Option[B]): Over[B] = Over(noBagQuotLabel, false, value)

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
  def >(value: A)(using Encoder[A]): Over[A] = over(value)
  @targetName("_over")
  def >[B](value: B)(using Encoder[B], A =:= Option[B]): Over[B] = over(value)

  def lessThanOrEqual(value: A)(using Encoder[A]): LessThanOrEqualTo[A] =
    LessThanOrEqualTo(noBagQuotLabel, false, value)
  def lessThanOrEqual[B](value: B)(using Encoder[B], A =:= Option[B]): LessThanOrEqualTo[B] =
    LessThanOrEqualTo(noBagQuotLabel, false, value)

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
  def <=(value: A)(using Encoder[A]): LessThanOrEqualTo[A] = lessThanOrEqual(value)
  @targetName("_lessThanOrEqual")
  def <=[B](value: B)(using Encoder[B], A =:= Option[B]): LessThanOrEqualTo[B] = lessThanOrEqual(value)

  def lessThan(value: A)(using Encoder[A]): LessThan[A] =
    LessThan(noBagQuotLabel, false, value)
  def lessThan[B](value: B)(using Encoder[B], A =:= Option[B]): LessThan[B] =
    LessThan(noBagQuotLabel, false, value)

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
  def <(value: A)(using Encoder[A]): LessThan[A] = lessThan(value)
  @targetName("_lessThan")
  def <[B](value: B)(using Encoder[B], A =:= Option[B]): LessThan[B] = lessThan(value)

  def notEqual(value: A)(using Encoder[A]): NotEqual[A] =
    NotEqual("<>", noBagQuotLabel, false, value)
  def notEqual[B](value: B)(using Encoder[B], A =:= Option[B]): NotEqual[B] =
    NotEqual("<>", noBagQuotLabel, false, value)

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
  def <>(value: A)(using Encoder[A]): NotEqual[A] = notEqual(value)
  @targetName("_notEqual")
  def <>[B](value: B)(using Encoder[B], A =:= Option[B]): NotEqual[B] = notEqual(value)

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
  def !==(value: A)(using Encoder[A]): NotEqual[A] =
    NotEqual("!=", noBagQuotLabel, false, value)
  @targetName("_!equal")
  def !==[B](value: B)(using Encoder[B], A =:= Option[B]): NotEqual[B] =
    NotEqual("!=", noBagQuotLabel, false, value)

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
   * @tparam B
   *   Type of value to compare
   * @return
   *   Query to check whether values are equal in a Where statement.
   */
  def IS[B <: "TRUE" | "FALSE" | "UNKNOWN" | "NULL"](value: B): Is[B] =
    Is[B](noBagQuotLabel, false, value)

  def nullSafeEqual(value: A)(using Encoder[A]): NullSafeEqual[A] =
    NullSafeEqual(noBagQuotLabel, false, value)
  def nullSafeEqual[B](value: B)(using Encoder[B], A =:= Option[B]): NullSafeEqual[B] =
    NullSafeEqual(noBagQuotLabel, false, value)

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
  def <=>(value: A)(using Encoder[A]): NullSafeEqual[A] = nullSafeEqual(value)
  @targetName("_nullSafeEqual")
  def <=>[B](value: B)(using Encoder[B], A =:= Option[B]): NullSafeEqual[B] = nullSafeEqual(value)

  /**
   * A function that sets a WHERE condition to check whether the values are equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id IN NonEmptyList.of(1L, 2L, 3L))
   *   // SELECT name, age FROM user WHERE id IN (?, ?, ?)
   * }}}
   *
   * @param values
   *   Value to compare
   * @return
   *   A query to check whether the values are equal in a Where statement
   */
  def IN(values: NonEmptyList[A])(using Encoder[A]): In[A] =
    In(noBagQuotLabel, false, values.toList*)
  def IN[B](values: NonEmptyList[B])(using Encoder[B], A =:= Option[B]): In[B] =
    In(noBagQuotLabel, false, values.toList*)

  /**
   * A function that sets a WHERE condition to check whether the values are equal in a SELECT statement.
   *
   * {{{
   *   TableQuery[User].select(user => user.name *: user.age).where(_.id IN (1L, 2L, 3L))
   *   // SELECT name, age FROM user WHERE id IN (?, ?, ?)
   * }}}
   *
   * @param head
   *   Value to compare
   * @param tail
   *   Value to compare
   * @return
   * A query to check whether the values are equal in a Where statement
   */
  def IN(head:    A, tail: A*)(using Encoder[A]):                  In[A] = self.IN(NonEmptyList.of(head, tail*))
  def IN[B](head: B, tail: B*)(using Encoder[B], A =:= Option[B]): In[B] = self.IN(NonEmptyList.of(head, tail*))

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
  def BETWEEN(start: A, end: A)(using Encoder[A]): Between[A] =
    Between(noBagQuotLabel, false, start, end)
  def BETWEEN[B](start: B, end: B)(using Encoder[B], A =:= Option[B]): Between[B] =
    Between(noBagQuotLabel, false, start, end)

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
  def LIKE(value: A)(using Encoder[A]): Like[A] =
    Like(noBagQuotLabel, false, value)
  def LIKE[B](value: B)(using Encoder[B], A =:= Option[B]): Like[B] =
    Like(noBagQuotLabel, false, value)

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
  def LIKE_ESCAPE(like: A, escape: A)(using Encoder[A]): LikeEscape[A] =
    LikeEscape(noBagQuotLabel, false, like, escape)
  def LIKE_ESCAPE[B](like: B, escape: B)(using Encoder[B], A =:= Option[B]): LikeEscape[B] =
    LikeEscape(noBagQuotLabel, false, like, escape)

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
  def REGEXP(value: A)(using Encoder[A]): Regexp[A] =
    Regexp(noBagQuotLabel, false, value)
  def REGEXP[B](value: B)(using Encoder[B], A =:= Option[B]): Regexp[B] =
    Regexp(noBagQuotLabel, false, value)

  def leftShift(value: A)(using Encoder[A]): LeftShift[A] =
    LeftShift(noBagQuotLabel, false, value)
  def leftShift[B](value: B)(using Encoder[B], A =:= Option[B]): LeftShift[B] =
    LeftShift(noBagQuotLabel, false, value)

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
  def <<(value: A)(using Encoder[A]): LeftShift[A] = leftShift(value)
  @targetName("_leftShift")
  def <<[B](value: B)(using Encoder[B], A =:= Option[B]): LeftShift[B] = leftShift(value)

  def rightShift(value: A)(using Encoder[A]): RightShift[A] =
    RightShift[A](noBagQuotLabel, false, value)
  def rightShift[B](value: B)(using Encoder[B], A =:= Option[B]): RightShift[B] =
    RightShift(noBagQuotLabel, false, value)

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
  def >>(value: A)(using Encoder[A]): RightShift[A] = rightShift(value)
  @targetName("_rightShift")
  def >>[B](value: B)(using Encoder[B], A =:= Option[B]): RightShift[B] = rightShift(value)

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
  def DIV(cond: A, result: A)(using Encoder[A]): Div[A] =
    Div(noBagQuotLabel, false, cond, result)
  def DIV[B](cond: B, result: B)(using Encoder[B], A =:= Option[B]): Div[B] =
    Div(noBagQuotLabel, false, cond, result)

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
  def MOD(cond: A, result: A)(using Encoder[A]): Mod[A] =
    Mod("MOD", noBagQuotLabel, false, cond, result)
  def MOD[B](cond: B, result: B)(using Encoder[B], A =:= Option[B]): Mod[B] =
    Mod("MOD", noBagQuotLabel, false, cond, result)

  def mod(cond: A, result: A)(using Encoder[A]): Mod[A] =
    Mod("%", noBagQuotLabel, false, cond, result)
  def mod[B](cond: B, result: B)(using Encoder[B], A =:= Option[B]): Mod[B] =
    Mod("%", noBagQuotLabel, false, cond, result)

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
  def %(cond: A, result: A)(using Encoder[A]): Mod[A] = mod(cond, result)
  @targetName("_mod")
  def %[B](cond: B, result: B)(using Encoder[B], A =:= Option[B]): Mod[B] = mod(cond, result)

  def bitXOR(value: A)(using Encoder[A]): BitXOR[A] =
    BitXOR(noBagQuotLabel, false, value)
  def bitXOR[B](value: B)(using Encoder[B], A =:= Option[B]): BitXOR[B] =
    BitXOR(noBagQuotLabel, false, value)

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
  def ^(value: A)(using Encoder[A]): BitXOR[A] = bitXOR(value)
  @targetName("_bitXOR")
  def ^[B](value: B)(using Encoder[B], A =:= Option[B]): BitXOR[B] = bitXOR(value)

  def bitFlip(value: A)(using Encoder[A]): BitFlip[A] =
    BitFlip(noBagQuotLabel, false, value)
  def bitFlip[B](value: B)(using Encoder[B], A =:= Option[B]): BitFlip[B] =
    BitFlip(noBagQuotLabel, false, value)

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
  def ~(value: A)(using Encoder[A]): BitFlip[A] = bitFlip(value)
  @targetName("_bitFlip")
  def ~[B](value: B)(using Encoder[B], A =:= Option[B]): BitFlip[B] = bitFlip(value)

  def combine(other: Column[A])(using Codec[A]): Column.MultiColumn[A] =
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
  def ++(other: Column[A])(using Codec[A]): Column.MultiColumn[A] = combine(other)

  def deduct(other: Column[A])(using Codec[A]): Column.MultiColumn[A] =
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
  def --(other: Column[A])(using Codec[A]): Column.MultiColumn[A] = deduct(other)

  def multiply(other: Column[A])(using Codec[A]): Column.MultiColumn[A] =
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
  def *(other: Column[A])(using Codec[A]): Column.MultiColumn[A] = multiply(other)

  def smash(other: Column[A])(using Codec[A]): Column.MultiColumn[A] =
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
  def /(other: Column[A])(using Codec[A]): Column.MultiColumn[A] = smash(other)

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
    override def decoder:          Decoder[A]     = new Decoder[A]:
      override def offset:                                Int                                   = 0
      override def decode(index: Int, statement: String): ResultSetIO[Either[Decoder.Error, A]] =
        ResultSetIO.pure(Right(value))
    override def encoder:                     Encoder[A]      = (_: A) => Encoder.Encoded.success(List.empty)
    override def insertStatement:             String          = ""
    override def updateStatement:             String          = ""
    override def duplicateKeyUpdateStatement: String          = ""
    override def values:                      Int             = 0
    override private[ldbc] def list:          List[Column[?]] = List.empty

  def apply[A](name: String)(using Decoder[A], Encoder[A]): Column[A] =
    Impl[A](name)

  def apply[A](name: String, alias: String)(using Decoder[A], Encoder[A]): Column[A] =
    Impl[A](name, s"$alias.`$name`")

  /**
   * Function to construct a function that is registered as reserved in MySQL.
   * 
   * @param name
   *   Name of the function
   * @tparam A
   *   Type of the function
   */
  def function[A](name: String)(using decoder: Decoder[A], encoder: Encoder[A]): Column[A] =
    Impl[A](s"$name", None, decoder, encoder)

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
      Impl[A](s"`$name`", None, decoder, encoder)

    def apply[A](name: String, alias: String)(using decoder: Decoder[A], encoder: Encoder[A]): Column[A] =
      Impl[A](s"`$name`", Some(alias), decoder, encoder)

  private[ldbc] case class Opt[A](
    name:     String,
    alias:    Option[String],
    _decoder: Decoder[A],
    _encoder: Encoder[A]
  ) extends Column[Option[A]]:
    override def as(name: String): Column[Option[A]]  = this.copy(alias = Some(s"$name.${ this.name }"))
    override def decoder:          Decoder[Option[A]] = _decoder.opt
    override def encoder:          Encoder[Option[A]] = {
      case Some(v) => _encoder.encode(v)
      case None    => Encoder.Encoded.success(List(None))
    }
    override def updateStatement:             String = s"$name = ?"
    override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"

  private[ldbc] case class MultiColumn[A](
    flag:  String,
    left:  Column[A],
    right: Column[A]
  )(using codec: Codec[A])
    extends Column[A]:
    override def name:  String         = s"${ left.noBagQuotLabel } $flag ${ right.noBagQuotLabel }"
    override def alias: Option[String] = Some(
      s"${ left.alias.getOrElse(left.name) } $flag ${ right.alias.getOrElse(right.name) }"
    )
    override def as(name: String):            Column[A]  = this
    override def decoder:                     Decoder[A] = codec.asDecoder
    override def encoder:                     Encoder[A] = codec.asEncoder
    override def insertStatement:             String     = ""
    override def updateStatement:             String     = ""
    override def duplicateKeyUpdateStatement: String     = ""

  private[ldbc] case class Count(_name: String, _alias: Option[String])(using codec: Codec[Int]) extends Column[Int]:
    override def name:                        String         = s"COUNT($_name)"
    override def alias:                       Option[String] = _alias.map(a => s"COUNT($a)")
    override def as(name: String):            Column[Int]    = this.copy(s"$name.${ _name }")
    override def decoder:                     Decoder[Int]   = codec.asDecoder
    override def encoder:                     Encoder[Int]   = codec.asEncoder
    override def toString:                    String         = name
    override def insertStatement:             String         = ""
    override def updateStatement:             String         = ""
    override def duplicateKeyUpdateStatement: String         = ""
