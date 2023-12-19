/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
 * distributed with this source code.
 */

package ldbc.query.builder.statement

import ldbc.core.builder.TableQueryBuilder
import ldbc.sql.ParameterBinder
import ldbc.query.builder.TableQuery

/**
 * A model for constructing Drop Table statements in MySQL.
 * 
 * @param tableQuery
 *   Trait for generating SQL table information.
 * @tparam F
 *   The effect type
 * @tparam P
 *   Base trait for all products
 */
class Drop[F[_], P <: Product](
  tableQuery: TableQuery[F, P]
) extends Command[F]:

  override def params: Seq[ParameterBinder[F]] = Seq.empty

  override def statement: String = TableQueryBuilder(tableQuery.table).dropStatement
