/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import ldbc.sql.Parameter
import ldbc.query.builder.TableQuery

/**
 * A model for constructing UPDATE statements in MySQL.
 *
 * @param tableQuery
 *   Trait for generating SQL table information.
 * @tparam F
 *   The effect type
 * @tparam P
 *   Base trait for all products
 */
case class Delete[F[_], P <: Product](
  tableQuery: TableQuery[F, P]
) extends Command[F],
          Command.LimitProvider[F]:

  override def params: Seq[Parameter.DynamicBinder] = Seq.empty

  override def statement: String = s"DELETE FROM ${ tableQuery.table._name }"

  /**
   * A method for setting the WHERE condition in a DELETE statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: TableQuery[F, P] => ExpressionSyntax[F]): Command.Where[F] =
    val expressionSyntax = func(tableQuery)
    Command.Where[F](
      _statement       = statement,
      expressionSyntax = expressionSyntax,
      params           = params ++ expressionSyntax.parameter
    )
