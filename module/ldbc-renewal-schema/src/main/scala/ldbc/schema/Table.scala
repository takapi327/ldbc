/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.dsl.codec.Decoder
import ldbc.statement.AbstractTable

trait Table[T](val $name: String) extends AbstractTable[T]:

  type Column[A] = ldbc.statement.Column[A]

  protected final def column[A](name: String)(using Decoder.Elem[A]): Column[A] =
    ldbc.statement.Column[A](name, $name)

  override final def statement: String = $name

  override def toString: String = s"Table($$name)"
