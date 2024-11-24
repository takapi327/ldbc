/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import scala.annotation.targetName

import ldbc.dsl.{ Parameter, SQL }
import ldbc.dsl.codec.Encoder

sealed trait Insert[A] extends Command:

  def table: A

  /** Methods for constructing INSERT ... ON DUPLICATE KEY UPDATE statements. */
  def onDuplicateKeyUpdate: Insert.DuplicateKeyUpdate[A] =
    Insert.DuplicateKeyUpdate(
      table,
      s"$statement ON DUPLICATE KEY UPDATE",
      params
    )

object Insert:

  case class Impl[A](table: A, statement: String, params: List[Parameter.Dynamic]) extends Insert[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

  case class DuplicateKeyUpdate[A](table: A, statement: String, params: List[Parameter.Dynamic]) extends Insert[A]:

    @targetName("combine")
    override def ++(sql: SQL): SQL = this.copy(statement = statement ++ sql.statement, params = params ++ sql.params)

    def set[B](func: A => Column[B], value: B)(using Encoder[B]): Insert.DuplicateKeyUpdate[A] =
      val columns = func(table)
      this.copy(
        statement = s"$statement ${ columns.name } = ?",
        params    = params :+ Parameter.Dynamic(value)
      )

    def setThis[B](func: A => Column[B]): Insert.DuplicateKeyUpdate[A] =
      val columns = func(table)
      this.copy(
        statement = s"$statement ${ columns.duplicateKeyUpdateStatement }"
      )
