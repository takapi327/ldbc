/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Encoder

trait Update[A] extends Command:

  def table: A

  def set[B](column: A => Column[B], value: B)(using Encoder[B]): Update[A]

  def set[B](column: A => Column[B], value: Option[B])(using Encoder[B]): Update[A]

  def set[B](column: A => Column[B], value: B, bool: Boolean)(using Encoder[B]): Update[A]

  def where(func: A => Expression): Where.C[A]

object Update:

  case class Impl[A](
    table:     A,
    statement: String,
    params:    List[Parameter.Dynamic],
    isFirst:   Boolean = true
  ) extends Update[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override def set[B](column: A => Column[B], value: B)(using Encoder[B]): Update[A] =
      val set = if isFirst then " SET" else ","
      this.copy(
        statement = statement ++ s"$set ${ column(table).name } = ?",
        params    = params :+ Parameter.Dynamic(value),
        isFirst   = false
      )

    override def set[B](column: A => Column[B], value: Option[B])(using Encoder[B]): Update[A] =
      value.fold(this)(v => set(column, v))

    override def set[B](column: A => Column[B], value: B, bool: Boolean)(using Encoder[B]): Update[A] =
      if bool then set(column, value) else this

    override def where(func: A => Expression): Where.C[A] =
      val expression = func(table)
      Where.C[A](
        table     = table,
        statement = statement ++ s" WHERE ${ expression.statement }",
        params    = params ++ expression.parameter
      )

  case class Join[A](
    table:     A,
    statement: String,
    params:    List[Parameter.Dynamic],
    isFirst:   Boolean = true
  ) extends Update[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    override def set[B](column: A => Column[B], value: B)(using Encoder[B]): Update[A] =
      val set = if isFirst then " SET" else ","
      val col = column(table)
      this.copy(
        statement = statement ++ s"$set ${ col.alias.getOrElse(col.name) } = ?",
        params    = params :+ Parameter.Dynamic(value),
        isFirst   = false
      )

    override def set[B](column: A => Column[B], value: Option[B])(using Encoder[B]): Update[A] =
      value.fold(this)(v => set(column, v))

    override def set[B](column: A => Column[B], value: B, bool: Boolean)(using Encoder[B]): Update[A] =
      if bool then set(column, value) else this

    override def where(func: A => Expression): Where.C[A] =
      val expression = func(table)
      Where.C[A](
        table     = table,
        statement = statement ++ s" WHERE ${ expression.statement }",
        params    = params ++ expression.parameter
      )
