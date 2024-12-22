/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

/** A trait for building a table model. */
trait AbstractTable[T]:

  /** The name of the table. */
  def $name: String

  /** SQL statement string */
  def statement: String

  /** All columns that the table has.*/
  def * : Column[T]

object AbstractTable:

  /** A trait for building a table model with an Option type. */
  trait Opt[T] extends AbstractTable[Option[T]]
