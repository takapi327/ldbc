/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema.syntax

import ldbc.schema.{Constraint, ForeignKey, TableImpl}

/**
 * Trait that provides for sorting an array of tables.
 */
trait OrderingTable:

  /**
   * Methods for calculating specific weights according to external reference keys.
   *
   * @param table
   *   Trait for generating SQL table information.
   */
  private def calculateWeightByReference(table: TableImpl[?, ?, ?]): Int =
    if table.keyDefinitions.nonEmpty then
      table.keyDefinitions.map {
        case _: ForeignKey[?] => 1
        case constraint: Constraint =>
          constraint.key match
            case _: ForeignKey[?] => 1
            case _                => 0
        case _ => 0
      }.sum
    else 0

  given Ordering[TableImpl[?, ?, ?]] with

    override def compare(x: TableImpl[?, ?, ?], y: TableImpl[?, ?, ?]): Int =
      val calculateWeightX = calculateWeightByReference(x)
      val calculateWeightY = calculateWeightByReference(y)
      if calculateWeightX < calculateWeightY then -1
      else if calculateWeightY < calculateWeightX then 1
      else 0

object OrderingTable extends OrderingTable
