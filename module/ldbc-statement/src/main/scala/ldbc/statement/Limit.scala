/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Encoder

/** A trait for constructing LIMIT statements in MySQL. */
sealed trait Limit:

  /** SQL statement string */
  def statement: String

  /** A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index only. */
  def params: List[Parameter.Dynamic]

object Limit:

  /**
   * A model for constructing read-only query LIMIT statements in MySQL.
   *
   * @param table
   *   Trait for generating SQL table information.
   * @param columns
   *   Union-type column list
   * @param statement
   *   SQL statement string
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
   *   only.
   * @tparam A
   *   The type of Table. in the case of Join, it is a Tuple of type Table.
   * @tparam B
   *   Scala types to be converted by Decoder
   */
  case class Q[A, B](
    table:     A,
    columns:   Column[B],
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Limit,
            Query[A, B]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    def offset(length: Long): Encoder[Long] ?=> Offset[A, B] =
      Offset(
        table     = table,
        columns   = columns,
        statement = statement ++ " OFFSET ?",
        params    = params ++ Parameter.Dynamic.many(summon[Encoder[Long]].encode(length))
      )

  transparent trait QueryProvider[A, B]:
    self: Query[A, B] =>

    /**
     * A method for setting the LIMIT condition in a SELECT statement.
     * 
     * {{{
     *   TableQuery[City]
     *     .select(_.population)
     *     .limit(10)
     * }}}
     *
     * @param length
     *   The number of rows to return.
     */
    def limit(length: Long): Encoder[Long] ?=> Limit.Q[A, B] =
      Limit.Q(
        table     = self.table,
        columns   = self.columns,
        statement = self.statement ++ " LIMIT ?",
        params    = self.params ++ Parameter.Dynamic.many(summon[Encoder[Long]].encode(length))
      )

  /**
   * A model for constructing write-only query LIMIT statements in MySQL.
   *
   * @param statement
   *   SQL statement string
   * @param params
   *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
   *   only.
   */
  case class C(
    statement: String,
    params:    List[Parameter.Dynamic]
  ) extends Limit,
            Command:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  transparent trait CommandProvider:
    self: Command =>

    /**
     * A method for setting the LIMIT condition in a UPDATE/DELETE statement.
     * 
     * {{{
     *   TableQuery[City]
     *     .delete
     *     .limit(10)
     * }}}
     *
     * @param length
     *   The number of rows to return.
     */
    def limit(length: Long): Encoder[Long] ?=> Limit.C =
      Limit.C(
        statement = statement ++ " LIMIT ?",
        params    = params ++ Parameter.Dynamic.many(summon[Encoder[Long]].encode(length))
      )
