/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.core.*
import ldbc.sql.ParameterBinder

/** A model for constructing INSERT statements in MySQL.
  *
  * @param table
  *   Trait for generating SQL table information.
  * @param value
  *   Tuple type value of the property with type parameter P.
  * @param params
  *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
  *   only.
  * @tparam F
  *   The effect type
  * @tparam P
  *   Base trait for all products
  * @tparam T
  *   Tuple type of the property with type parameter P
  */
private[ldbc] case class Insert[F[_], P <: Product, T <: Tuple](
  table:  Table[P],
  value:  T,
  params: Seq[ParameterBinder[F]]
) extends Command[F]:

  override def statement: String =
    s"INSERT INTO ${ table._name } (${ table.all.mkString(", ") }) VALUES(${ value.toArray.map(_ => "?").mkString(", ") })"
