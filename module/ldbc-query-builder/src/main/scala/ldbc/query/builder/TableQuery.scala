/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder

import scala.language.dynamics
import scala.deriving.Mirror
import scala.annotation.targetName

import ldbc.core.*
import ldbc.core.builder.TableQueryBuilder
import ldbc.core.interpreter.Tuples as CoreTuples
import ldbc.sql.*
import ldbc.query.builder.statement.*
import ldbc.query.builder.interpreter.Tuples

/**
 * A model for generating queries from Table information.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @tparam F
 *   The effect type
 * @tparam P
 *   A class that implements a [[Product]] that is one-to-one with the table definition.
 */
case class TableQuery[F[_], P <: Product](table: Table[P]) extends Dynamic, TableQueryBuilder:

  transparent inline def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): ColumnQuery[F, Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    ColumnQuery.fromColumn[F, Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]](
      table.selectDynamic[Tag](tag)
    )

  /** Type alias for ParameterBinder. Mainly for use with Tuple.map. */
  private type ParamBind[A] = Parameter.Binder

  /**
   * A method to build a query model to retrieve all columns defined in the Table.
   *
   * @param mirror
   *   product isomorphism map
   */
  inline def selectAll(using mirror: Mirror.ProductOf[P]): Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]] =
    val columns = Tuple
      .fromArray(table.all.map(ColumnQuery.fromColumn).toArray)
      .asInstanceOf[Tuples.ToColumn[F, mirror.MirroredElemTypes]]
    val statement = s"SELECT ${ table.all.mkString(", ") } FROM ${ table._name }"
    new Select[F, P, Tuples.ToColumn[F, mirror.MirroredElemTypes]](this, statement, columns, Seq.empty)

  /**
   * A method to build a query model that specifies and retrieves columns defined in a table.
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

  /**
   * A method to perform a simple Join.
   *
   * @param other
   *   [[TableQuery]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def join[O <: Product](other: TableQuery[F, O])(
    on: TableQuery[F, P] *: Tuple1[TableQuery[F, O]] => ExpressionSyntax[F]
  ): Join[F, TableQuery[F, P] *: Tuple1[TableQuery[F, O]], TableQuery[F, P] *: Tuple1[TableQuery[F, O]]] =
    val main:      TableQuery[F, P]                             = setNameForJoin(this)
    val joinTable: TableQuery[F, O]                             = setNameForJoin(other)
    val joins:     TableQuery[F, P] *: Tuple1[TableQuery[F, O]] = main *: Tuple(joinTable)
    Join[F, TableQuery[F, P] *: Tuple1[TableQuery[F, O]], TableQuery[F, P] *: Tuple1[TableQuery[F, O]]](
      this,
      joins,
      main *: Tuple(joinTable),
      Seq(s"${ Join.JoinType.JOIN.statement } ${ other.table._name } ON ${ on(joins).statement }")
    )

  /**
   * Method to perform Left Join.
   *
   * @param other
   *   [[TableQuery]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def leftJoin[O <: Product](other: TableQuery[F, O])(
    on: TableQuery[F, P] *: Tuple1[TableQuery[F, O]] => ExpressionSyntax[F]
  ): Join[F, TableQuery[F, P] *: Tuple1[TableQuery[F, O]], TableQuery[F, P] *: Tuple1[TableOpt[F, O]]] =
    val main:      TableQuery[F, P]                             = setNameForJoin(this)
    val joinTable: TableQuery[F, O]                             = setNameForJoin(other)
    val joins:     TableQuery[F, P] *: Tuple1[TableQuery[F, O]] = main *: Tuple(joinTable)
    Join[F, TableQuery[F, P] *: Tuple1[TableQuery[F, O]], TableQuery[F, P] *: Tuple1[TableOpt[F, O]]](
      this,
      joins,
      main *: Tuple(TableOpt(joinTable.table)),
      Seq(s"${ Join.JoinType.LEFT_JOIN.statement } ${ other.table._name } ON ${ on(joins).statement }")
    )

  /**
   * Method to perform Right Join.
   *
   * @param other
   *   [[TableQuery]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def rightJoin[O <: Product](other: TableQuery[F, O])(
    on: TableQuery[F, P] *: Tuple1[TableQuery[F, O]] => ExpressionSyntax[F]
  ): Join[F, TableQuery[F, P] *: Tuple1[TableQuery[F, O]], TableOpt[F, P] *: Tuple1[TableQuery[F, O]]] =
    val main:      TableQuery[F, P]                             = setNameForJoin(this)
    val joinTable: TableQuery[F, O]                             = setNameForJoin(other)
    val joins:     TableQuery[F, P] *: Tuple1[TableQuery[F, O]] = main *: Tuple(joinTable)
    Join[F, TableQuery[F, P] *: Tuple1[TableQuery[F, O]], TableOpt[F, P] *: Tuple1[TableQuery[F, O]]](
      this,
      joins,
      TableOpt(main.table) *: Tuple(joinTable),
      Seq(s"${ Join.JoinType.RIGHT_JOIN.statement } ${ other.table._name } ON ${ on(joins).statement }")
    )

  private def setNameForJoin[J <: Product](tableQuery: TableQuery[F, J]): TableQuery[F, J] =
    val table: Table[J] =
      tableQuery.table.alias.fold(tableQuery.table.as(tableQuery.table._name))(_ => tableQuery.table)
    TableQuery[F, J](table)

  // TODO: In the following implementation, Warning occurs at the time of Compile, so it is cast by asInstanceOf.
  // case (value: Any, parameter: Parameter[F, Any]) => ???
  /**
   * A method to build a query model that inserts data into all columns defined in the table.
   *
   * @param mirror
   *   product isomorphism map
   * @param values
   *   A list of Tuples constructed with all the property types that Table has.
   */
  inline def insert(using mirror: Mirror.ProductOf[P])(
    values: mirror.MirroredElemTypes*
  ): Insert[F, P] =
    val parameterBinders = values
      .flatMap(
        _.zip(Parameter.fold[mirror.MirroredElemTypes])
          .map[ParamBind](
            [t] =>
              (x: t) =>
                val (value, parameter) = x.asInstanceOf[(t, Parameter[t])]
                Parameter.DynamicBinder[t](value)(using parameter)
          )
          .toList
      )
      .toList
      .asInstanceOf[List[Parameter.DynamicBinder]]
    new MultiInsert[F, P, Tuple](this, values.toList, parameterBinders)

  /**
   * A method to build a query model that inserts data into specified columns defined in a table.
   *
   * @param func
   *   Function to retrieve columns from Table.
   * @tparam T
   *   Type of value to be obtained
   */
  inline def insertInto[T](func: TableQuery[F, P] => T)(using
    Tuples.IsColumnQuery[F, T] =:= true
  ): SelectInsert[F, P, T] =
    val parameter: Parameter.MapToTuple[Column.Extract[T]] = Parameter.fold[Column.Extract[T]]
    SelectInsert[F, P, T](this, func(this), parameter)

  /**
   * A method to build a query model that inserts data from the model into all columns defined in the table.
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
      .zip(Parameter.fold[mirror.MirroredElemTypes])
      .map[ParamBind](
        [t] =>
          (x: t) =>
            val (value, parameter) = x.asInstanceOf[(t, Parameter[t])]
            Parameter.DynamicBinder[t](value)(using parameter)
      )
      .toList
      .asInstanceOf[List[Parameter.DynamicBinder]]
    new SingleInsert[F, P, Tuple](this, tuples, parameterBinders)

  /**
   * A method to build a query model that inserts data from multiple models into all columns defined in a table.
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
        _.zip(Parameter.fold[mirror.MirroredElemTypes])
          .map[ParamBind](
            [t] =>
              (x: t) =>
                val (value, parameter) = x.asInstanceOf[(t, Parameter[t])]
                Parameter.DynamicBinder[t](value)(using parameter)
          )
          .toList
      )
      .asInstanceOf[List[Parameter.DynamicBinder]]
    new MultiInsert[F, P, Tuple](this, tuples, parameterBinders)

  /**
   * A method to build a query model that inserts data in all columns defined in the table or updates the data if there
   * are duplicate primary keys.
   *
   * @param mirror
   *   product isomorphism map
   * @param values
   *   A list of Tuples constructed with all the property types that Table has.
   */
  inline def insertOrUpdate(using mirror: Mirror.ProductOf[P])(
    values: mirror.MirroredElemTypes*
  ): DuplicateKeyUpdateInsert[F] =
    val parameterBinders = values
      .flatMap(
        _.zip(Parameter.fold[mirror.MirroredElemTypes])
          .map[ParamBind](
            [t] =>
              (x: t) =>
                val (value, parameter) = x.asInstanceOf[(t, Parameter[t])]
                Parameter.DynamicBinder[t](value)(using parameter)
          )
          .toList
      )
      .toList
      .asInstanceOf[List[Parameter.DynamicBinder]]
    new DuplicateKeyUpdate[F, P, Tuple](this, values.toList, parameterBinders)

  /**
   * A method to build a query model that inserts data in all columns defined in the table or updates the data if there
   * are duplicate primary keys.
   *
   * @param values
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   * @param mirror
   *   product isomorphism map
   */
  inline def insertOrUpdates(values: List[P])(using mirror: Mirror.ProductOf[P]): DuplicateKeyUpdateInsert[F] =
    val tuples = values.map(Tuple.fromProductTyped)
    val parameterBinders = tuples
      .flatMap(
        _.zip(Parameter.fold[mirror.MirroredElemTypes])
          .map[ParamBind](
            [t] =>
              (x: t) =>
                val (value, parameter) = x.asInstanceOf[(t, Parameter[t])]
                Parameter.DynamicBinder[t](value)(using parameter)
          )
          .toList
      )
      .asInstanceOf[List[Parameter.DynamicBinder]]
    new DuplicateKeyUpdate[F, P, Tuple](this, tuples, parameterBinders)

  /**
   * A method to build a query model that updates specified columns defined in a table.
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
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check:  T =:= Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[F, P] =
    type PARAM = Tuple.Elem[mirror.MirroredElemTypes, CoreTuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val params = List(Parameter.DynamicBinder[PARAM](check(value))(using Parameter.infer[PARAM]))
    new Update[F, P](
      tableQuery = this,
      columns    = List(table.selectDynamic[Tag](tag).label),
      params     = params
    )

  /**
   * A method to build a query model that updates all columns defined in the table using the model.
   *
   * @param value
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   * @param mirror
   *   product isomorphism map
   */
  inline def update(value: P)(using mirror: Mirror.ProductOf[P]): Update[F, P] =
    val params = Tuple
      .fromProductTyped(value)
      .zip(Parameter.fold[mirror.MirroredElemTypes])
      .map[ParamBind](
        [t] =>
          (x: t) =>
            val (value, parameter) = x.asInstanceOf[(t, Parameter[t])]
            Parameter.DynamicBinder[t](value)(using parameter)
      )
      .toList
      .asInstanceOf[List[Parameter.DynamicBinder]]
    new Update[F, P](
      tableQuery = this,
      columns    = table.all.map(_.label),
      params     = params
    )

  /**
   * Method to construct a query to delete a table.
   */
  val delete: Delete[F, P] = Delete[F, P](this)

  /**
   * Method to construct a query to create a table.
   */
  val createTable: Command = new Command:
    override def params:    Seq[Parameter.DynamicBinder] = Seq.empty
    override def statement: String                       = createStatement

  /**
   * Method to construct a query to drop a table.
   */
  val dropTable: Command = new Command:
    override def params:    Seq[Parameter.DynamicBinder] = Seq.empty
    override def statement: String                       = dropStatement

  /**
   * Method to construct a query to truncate a table.
   */
  val truncateTable: Command = new Command:
    override def params:    Seq[Parameter.DynamicBinder] = Seq.empty
    override def statement: String                       = truncateStatement
