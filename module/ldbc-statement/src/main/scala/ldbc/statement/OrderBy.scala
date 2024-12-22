/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import cats.Applicative

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.dsl.{ Parameter, SQL }

/**
 * A model for constructing ORDER BY statements in MySQL.
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
case class OrderBy[A, B](
  table:     A,
  columns:   Column[B],
  statement: String,
  params:    List[Parameter.Dynamic]
) extends Query[A, B],
          Limit.QueryProvider[A, B]:

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

object OrderBy:

  sealed trait Order[T]:
    def statement: String

  object Order extends TwiddleSyntax[Order]:

    private[ldbc] case class Impl[T](statement: String) extends Order[T]

    def asc[T](column:  Column[T]): Order[T] = Impl(s"${ column.alias.getOrElse(column.name) } ASC")
    def desc[T](column: Column[T]): Order[T] = Impl(s"${ column.alias.getOrElse(column.name) } DESC")

    given Applicative[Order] with
      override def pure[A](x: A): Order[A] = Impl("")
      override def ap[A, B](ff: Order[A => B])(fa: Order[A]): Order[B] =
        val statement = if ff.statement.isEmpty then fa.statement else s"${ ff.statement }, ${ fa.statement }"
        Impl(statement)

  /**
   * Transparent Trait to provide orderBy method.
   *
   * @tparam A
   *   The type of Table. in the case of Join, it is a Tuple of type Table.
   * @tparam B
   *   Scala types to be converted by Decoder
   */
  private[ldbc] transparent trait Provider[A, B]:
    self: Query[A, B] =>

    /**
     * A method for setting the ORDER BY condition in a statement.
     * 
     * {{{
     *  TableQuery[City]
     *    .select(_.population)
     *    .orderBy(_.population.desc)
     * }}}
     * 
     * @param func
     *   Function to construct an expression using the columns that Table has.
     */
    def orderBy[C](func: A => Order[C]): OrderBy[A, B] =
      OrderBy[A, B](
        table     = self.table,
        columns   = self.columns,
        statement = self.statement ++ s" ORDER BY ${ func(self.table).statement }",
        params    = self.params
      )
