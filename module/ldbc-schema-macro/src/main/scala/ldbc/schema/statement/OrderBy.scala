/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.statement

import scala.annotation.targetName

import cats.Applicative

import org.typelevel.twiddles.TwiddleSyntax

import ldbc.dsl.{Parameter, SQL}
import ldbc.schema.Column

case class OrderBy[A, B](
                          table: A,
                          columns: Column[B],
                          statement: String,
                          params: List[Parameter.Dynamic]
                        ) extends Query[A, B], Limit.QueryProvider[A, B]:
  
  @targetName("combine")
  override def ++(sql: SQL): SQL =
    this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

object OrderBy:
  
  trait Order[T]:
    def statement: String
    
  object Order extends TwiddleSyntax[Order]:
    
    private[ldbc] case class Impl[T](statement: String) extends Order[T]
      
    def asc[T](column: Column[T]): Order[T] = Impl(s"${ column.toString } ASC")
    def desc[T](column: Column[T]): Order[T] = Impl(s"${ column.toString } DESC")

    given Applicative[Order] with 
      override def pure[A](x: A): Order[A] = Impl("")
      override def ap[A, B](ff: Order[A => B])(fa: Order[A]): Order[B] =
        val statement = if ff.statement.isEmpty then fa.statement else s"${ ff.statement }, ${ fa.statement }"
        Impl(statement)

  private[ldbc] transparent trait Provider[A, B]:
    self: Query[A, B] =>

    def orderBy[C](func: A => Order[C]): OrderBy[A, B] =
      OrderBy[A, B](
        table = self.table,
        columns = self.columns,
        statement = self.statement ++ s" ORDER BY ${ func(self.table).statement }",
        params = self.params
      )
