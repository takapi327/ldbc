/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.sql.*
import ldbc.query.builder.TableQuery

/** Trait for building Statements to be added.
  *
  * @tparam F
  *   The effect type
  * @tparam P
  *   Base trait for all products
  */
private[ldbc] trait Insert[F[_], P <: Product] extends Command[F]:

  /** Trait for generating SQL table information. */
  def tableQuery: TableQuery[F, P]

private[ldbc] object Insert:

  /** A model for constructing INSERT statements that insert single values in MySQL.
    *
    * @param tableQuery
    *   Trait for generating SQL table information.
    * @param tuple
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
  case class Single[F[_], P <: Product, T <: Tuple](
    tableQuery: TableQuery[F, P],
    tuple:      T,
    params:     Seq[ParameterBinder[F]]
  ) extends Insert[F, P]:

    override def statement: String =
      s"INSERT INTO ${ tableQuery.table._name } (${ tableQuery.table.all
          .mkString(", ") }) VALUES(${ tuple.toArray.map(_ => "?").mkString(", ") })"

  /** A model for constructing INSERT statements that insert multiple values in MySQL.
    *
    * @param tableQuery
    *   Trait for generating SQL table information.
    * @param tuples
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
  case class Multi[F[_], P <: Product, T <: Tuple](
    tableQuery: TableQuery[F, P],
    tuples:     List[T],
    params:     Seq[ParameterBinder[F]]
  ) extends Insert[F, P]:

    private val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")

    override def statement: String =
      s"INSERT INTO ${ tableQuery.table._name } (${ tableQuery.table.all.mkString(", ") }) VALUES${ values.mkString(", ") }"

  case class Select[F[_], P <: Product, T <: Tuple](
    _tableQuery: TableQuery[F, P],
    columns:     Tuple
  ):

    private def _statement: String =
      s"INSERT INTO ${ _tableQuery.table._name } (${ columns.toArray.mkString(", ") })"

    inline def values(tuples: T*): Insert[F, P] =

      val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")

      new Insert[F, P]:
        override def tableQuery: TableQuery[F, P] = _tableQuery
        override def statement:  String           = s"$_statement VALUES${ values.mkString(", ") }"
        override def params: Seq[ParameterBinder[F]] =
          tuples
            .map(Tuple.fromProduct)
            .flatMap(_.zip(Parameter.fold[F, T]).toArray.map {
              case (value: Any, parameter: Any) =>
                ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
            })
