/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.codec.Decoder

trait Column[T]:

  /** Column Field Name */
  def name: String

  /** Column alias name */
  def alias: Option[String]

  /** Function to get a value of type T from a ResultSet */
  def decoder: Decoder[T]
