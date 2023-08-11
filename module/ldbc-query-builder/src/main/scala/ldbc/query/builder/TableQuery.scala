/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.query.builder

import ldbc.core.Table
import ldbc.query.builder.statement.Select
import ldbc.query.builder.interpreter.Tuples

case class TableQuery[F[_], P <: Product](table: Table[P]):

  def select[T <: Tuple](
    func: Table[P] => Tuples.MapToColumn[T, F]
  ): Select[F, P, Tuples.MapToColumn[T, F]] =
    val columns = func(table)
    val statement = s"SELECT ${columns.toArray.distinct.mkString(", ")} FROM ${table._name}"
    Select[F, P, Tuples.MapToColumn[T, F]](table, statement, columns, Seq.empty)
