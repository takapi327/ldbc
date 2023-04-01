/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.core.syntax

import ldbc.core.{ Table, ForeignKey, Constraint }

/** Trait that provides for sorting an array of tables.
  */
trait OrderingTable:

  /** Methods for calculating specific weights according to external reference keys.
    *
    * @param table
    *   Trait for generating SQL table information.
    */
  private def calculateWeightByReference(table: Table[?]): Int =
    if table.keyDefinitions.nonEmpty then
      table.keyDefinitions.map {
        case _: ForeignKey => 1
        case constraint: Constraint =>
          constraint.key match
            case _: ForeignKey => 1
            case _             => 0
      }.sum
    else 0

  extension (table: Table[?]) def calculateWeight: Int = calculateWeightByReference(table)

  given Ordering[Table[?]] with

    override def compare(x: Table[?], y: Table[?]): Int =
      val calculateWeightX = calculateWeightByReference(x)
      val calculateWeightY = calculateWeightByReference(y)
      if calculateWeightX < calculateWeightY then -1
      else if calculateWeightY < calculateWeightX then 1
      else 0

object OrderingTable extends OrderingTable
