/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.statement

import ldbc.dsl.SQL

/**
 * Trait represents a read-only query in MySQL.
 *
 * @tparam A
 *   The type of Table. in the case of Join, it is a Tuple of type Table.
 * @tparam B
 *   Scala types to be converted by Decoder
 */
trait Query[A, B] extends SQL:

  /** Trait for generating SQL table information. */
  def table: A

  /** Union-type column list */
  def columns: Column[B]
