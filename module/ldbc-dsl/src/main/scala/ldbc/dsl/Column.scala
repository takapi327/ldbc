/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

/**
 * Trait for representing SQL Column
 *
 * @tparam T
 * Scala types that match SQL DataType
 */
sealed trait Column[T]:

  /** Column Field Name */
  def name: String

  /** Column alias name */
  def alias: Option[String] = None

  /** Functions for setting aliases on columns */
  def as(name: String): Column[T]

  override def toString: String = alias.fold(s"`$name`")(label => s"$label.`$name`")
