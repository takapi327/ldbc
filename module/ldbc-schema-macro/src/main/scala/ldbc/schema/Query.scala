/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.annotation.targetName

import ldbc.dsl.{Parameter, SQL}
import ldbc.dsl.codec.Decoder
import ldbc.query.builder.Column
import ldbc.query.builder.statement.Expression
import ldbc.query.builder.statement.Query as QuerySQL

sealed trait Query[A, B] extends QuerySQL[B]:

  def table: A
  
  def columns: Column[B]

  def params: List[Parameter.Dynamic]
  
  override def decoder: Decoder[B] = columns.decoder
  
object Query:
  
  case class Select[A, B](
                           table: A,
                           columns: Column[B],
                           statement: String,
                           params: List[Parameter.Dynamic]
                         ) extends Query[A, B]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
      
    def where(func: A => Expression): Where[A, B] =
      val expression = func(table)
      Where[A, B](
        table = table,
        columns = columns,
        statement = statement ++ s" WHERE ${ expression.statement }",
        params = params ++ expression.parameter
      )

  case class Where[A, B](
                          table: A,
                          columns: Column[B],
                          statement: String,
                          params: List[Parameter.Dynamic]
                        ) extends Query[A, B]:

    @targetName("combine")
    override def ++(sql: SQL): SQL =
      this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)
