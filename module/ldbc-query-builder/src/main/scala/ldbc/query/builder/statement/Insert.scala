/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder.statement

import ldbc.core.Column
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
case class SingleInsert[F[_], P <: Product, T <: Tuple](
  tableQuery: TableQuery[F, P],
  tuple:      T,
  params:     Seq[ParameterBinder[F]]
) extends Insert[F, P]:

  override val statement: String =
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
case class MultiInsert[F[_], P <: Product, T <: Tuple](
  tableQuery: TableQuery[F, P],
  tuples:     List[T],
  params:     Seq[ParameterBinder[F]]
) extends Insert[F, P]:

  private val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")

  override val statement: String =
    s"INSERT INTO ${ tableQuery.table._name } (${ tableQuery.table.all.mkString(", ") }) VALUES${ values.mkString(", ") }"

/** A model for constructing INSERT statements that insert values into specified columns in MySQL.
  *
  * @param query
  *   Trait for generating SQL table information.
  * @param columns
  *   List of columns into which values are to be inserted.
  * @param parameter
  *   Parameters of the value to be inserted
  * @tparam F
  *   The effect type
  * @tparam P
  *   Base trait for all products
  * @tparam T
  *   Tuple type of the property with type parameter P
  */
case class SelectInsert[F[_], P <: Product, T](
  query:     TableQuery[F, P],
  columns:   T,
  parameter: Parameter.MapToTuple[F, Column.Extract[T]]
):

  private val columnStatement = columns match
    case v: Tuple => v.toArray.distinct.mkString(", ")
    case v        => v

  private val insertStatement: String =
    s"INSERT INTO ${ query.table._name } ($columnStatement)"

  def values(tuple: Column.Extract[T]): Insert[F, P] =
    new Insert[F, P]:
      override def tableQuery: TableQuery[F, P] = query
      override def statement: String = s"$insertStatement VALUES(${ tuple.toArray.map(_ => "?").mkString(", ") })"
      override def params: Seq[ParameterBinder[F]] =
        tuple.zip(parameter).toArray.toSeq.map {
          case (value: Any, parameter: Any) =>
            ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
        }

  def values(tuples: List[Column.Extract[T]]): Insert[F, P] =
    val values = tuples.map(tuple => s"(${ tuple.toArray.map(_ => "?").mkString(", ") })")
    new Insert[F, P]:
      override def tableQuery: TableQuery[F, P] = query
      override def statement:  String           = s"$insertStatement VALUES${ values.mkString(", ") }"
      override def params: Seq[ParameterBinder[F]] =
        tuples.flatMap(_.zip(parameter).toArray.map {
          case (value: Any, parameter: Any) =>
            ParameterBinder[F, Any](value)(using parameter.asInstanceOf[Parameter[F, Any]])
        })
