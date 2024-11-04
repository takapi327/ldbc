/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.annotation.targetName

import ldbc.dsl.codec.Decoder
import ldbc.query.builder.Column

trait Table[T](private[ldbc] val _name: String):
  
  private[ldbc] def alias: String = s"$$$_name"

  protected def column[A](name: String)(using Decoder.Elem[A]): Column[A] = Column.Impl[A](name, Some(alias))

  @targetName("all")
  def * : Column[T]

  def statement: String = _name

  override def toString: String = s"Table($_name)"
