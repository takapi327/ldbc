/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.query.builder

import scala.language.dynamics
import scala.deriving.Mirror
import scala.annotation.targetName

import ldbc.core.*
import ldbc.core.interpreter.Tuples as CoreTuples
import ldbc.sql.*
import ldbc.query.builder.statement.*
import ldbc.query.builder.interpreter.Tuples

/** A model for generating queries from Table information.
  *
  * @param table
  *   Trait for generating SQL table information.
  * @tparam F
  *   The effect type
  * @tparam P
  *   A class that implements a [[Product]] that is one-to-one with the table definition.
  */
case class TableQuery[F[_], P <: Product](table: Table[P]) extends Dynamic:

  private[ldbc] val alias: Table[P] = table.as(s"${ table._name }_alias")

  transparent inline def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    reader: ResultSetReader[F, Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
  ): ColumnQuery[F, Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    ColumnQuery.fromColumn[F, Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]](
      table.selectDynamic[Tag](tag)
    )

  /** Type alias for ParameterBinder. Mainly for use with Tuple.map. */
  private type ParamBind[A] = ParameterBinder[F]

  /** A method to build a query model to retrieve all columns defined in the Table.
    *
    * @param mirror
    *   product isomorphism map
    */
  inline def selectAll(using mirror: Mirror.ProductOf[P]): Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]] =
    val columns = table.*.zip(ResultSetReader.fold[F, mirror.MirroredElemTypes])
      .map(
        [t] =>
          (x: t) =>
            val (column, reader) = x.asInstanceOf[(Column[t], ResultSetReader[F, t])]
            ColumnQuery.fromColumn(column)(using reader)
      )
      .asInstanceOf[Tuples.ToColumn[F, mirror.MirroredElemTypes]]
    val statement = s"SELECT ${ table.*.toList.mkString(", ") } FROM ${ table._name }"
    new Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]](this, statement, columns, Seq.empty)

  /** A method to build a query model that specifies and retrieves columns defined in a table.
    *
    * @param func
    *   Function to retrieve columns from Table.
    * @tparam T
    *   Type of value to be obtained
    */
  def select[T](func: TableQuery[F, P] => T)(using Tuples.IsColumnQuery[F, T] =:= true): Select[F, P, T] =
    val columns = func(this)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM ${ table._name }"
    Select[F, P, T](this, statement, columns, Seq.empty)

  /** A method to join another table to itself.
    *
    * @param other
    *   Table model to be joined.
    * @tparam O
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    */
  def join[O <: Product](other: Table[O]): Join[F, P, O] =
    new Join(TableQuery(alias), TableQuery(other.as(s"${ other._name }_alias")))

  /** A method to join another table to itself.
    *
    * @param other
    *   A model for generating queries from Table information.
    * @tparam O
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    */
  def join[O <: Product](other: TableQuery[F, O]): Join[F, P, O] = new Join(TableQuery(alias), TableQuery(other.alias))

  // TODO: In the following implementation, Warning occurs at the time of Compile, so it is cast by asInstanceOf.
  // case (value: Any, parameter: Parameter[F, Any]) => ???
  /** A method to build a query model that inserts data into all columns defined in the table.
    *
    * @param mirror
    *   product isomorphism map
    * @param values
    *   A list of Tuples constructed with all the property types that Table has.
    */
  inline def insert(using mirror: Mirror.ProductOf[P])(values: mirror.MirroredElemTypes*): Insert[F, P] =
    val parameterBinders = values
      .flatMap(
        _.zip(Parameter.fold[F, mirror.MirroredElemTypes])
          .map[ParamBind](
            [t] =>
              (x: t) =>
                val (value, parameter) = x.asInstanceOf[(t, Parameter[F, t])]
                ParameterBinder[F, t](value)(using parameter)
          )
          .toList
      )
      .toList
      .asInstanceOf[List[ParameterBinder[F]]]
    new Insert.Multi[F, P, Tuple](this, values.toList, parameterBinders)

  /** A method to build a query model that inserts data into specified columns defined in a table.
    *
    * @param func
    *   Function to retrieve columns from Table.
    * @tparam T
    *   Type of value to be obtained
    */
  def selectInsert[T <: Tuple](func: TableQuery[F, P] => Tuple.Map[T, Column]): Insert.Select[F, P, T] =
    Insert.Select[F, P, T](this, func(this))

  /** A method to build a query model that inserts data from the model into all columns defined in the table.
    *
    * @param value
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    * @param mirror
    *   product isomorphism map
    */
  @targetName("insertProduct")
  inline def +=(value: P)(using mirror: Mirror.ProductOf[P]): Insert[F, P] =
    val tuples = Tuple.fromProductTyped(value)
    val parameterBinders = tuples
      .zip(Parameter.fold[F, mirror.MirroredElemTypes])
      .map[ParamBind](
        [t] =>
          (x: t) =>
            val (value, parameter) = x.asInstanceOf[(t, Parameter[F, t])]
            ParameterBinder[F, t](value)(using parameter)
      )
      .toList
      .asInstanceOf[List[ParameterBinder[F]]]
    new Insert.Single[F, P, Tuple](this, tuples, parameterBinders)

  /** A method to build a query model that inserts data from multiple models into all columns defined in a table.
    *
    * @param values
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    * @param mirror
    *   product isomorphism map
    */
  @targetName("insertProducts")
  inline def ++=(values: List[P])(using mirror: Mirror.ProductOf[P]): Insert[F, P] =
    val tuples = values.map(Tuple.fromProductTyped)
    val parameterBinders = tuples
      .flatMap(
        _.zip(Parameter.fold[F, mirror.MirroredElemTypes])
          .map[ParamBind](
            [t] =>
              (x: t) =>
                val (value, parameter) = x.asInstanceOf[(t, Parameter[F, t])]
                ParameterBinder[F, t](value)(using parameter)
          )
          .toList
      )
      .asInstanceOf[List[ParameterBinder[F]]]
    new Insert.Multi[F, P, Tuple](this, tuples, parameterBinders)

  /** A method to build a query model that updates specified columns defined in a table.
    *
    * @param tag
    *   A type with a single instance. Here, Column is passed.
    * @param value
    *   A value of type T to be inserted into the specified column.
    * @param mirror
    *   product isomorphism map
    * @param index
    *   Position of the specified type in tuple X
    * @param check
    *   A value to verify that the specified type matches the type of the specified column that the Table has.
    * @tparam Tag
    *   Type with a single instance
    * @tparam T
    *   Scala types that match SQL DataType
    */
  inline def update[Tag <: Singleton, T](tag: Tag, value: T)(using
    mirror:                                   Mirror.ProductOf[P],
    index:                                    ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check: T =:= Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[F, P] =
    type PARAM = Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val params = List(ParameterBinder[F, PARAM](check(value))(using Parameter.infer[F, PARAM]))
    new Update[F, P](
      tableQuery = this,
      columns    = List(table.selectDynamic[Tag](tag).label),
      params     = params
    )

  /** A method to build a query model that updates all columns defined in the table using the model.
    *
    * @param value
    *   A class that implements a [[Product]] that is one-to-one with the table definition.
    * @param mirror
    *   product isomorphism map
    */
  inline def update(value: P)(using mirror: Mirror.ProductOf[P]): Update[F, P] =
    val params = Tuple
      .fromProductTyped(value)
      .zip(Parameter.fold[F, mirror.MirroredElemTypes])
      .map[ParamBind](
        [t] =>
          (x: t) =>
            val (value, parameter) = x.asInstanceOf[(t, Parameter[F, t])]
            ParameterBinder[F, t](value)(using parameter)
      )
      .toList
      .asInstanceOf[List[ParameterBinder[F]]]
    new Update[F, P](
      tableQuery = this,
      columns    = table.all.map(_.label),
      params     = params
    )

  /** Method to construct a query to delete a table.
    */
  def delete: Delete[F, P] = Delete[F, P](this)
