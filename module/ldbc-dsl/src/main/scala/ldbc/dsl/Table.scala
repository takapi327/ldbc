/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl

import scala.language.dynamics
import scala.deriving.Mirror
import scala.compiletime.*
import scala.compiletime.ops.int.*
import scala.annotation.targetName

import ldbc.sql.*
import ldbc.dsl.interpreter.*
import ldbc.dsl.statement.*

import scala.Tuple.Elem

/**
 * Trait for generating SQL table information.
 *
 * @tparam P
 *   A class that implements a [[Product]] that is one-to-one with the table definition.
 */
trait Table[P <: Product] extends Dynamic:

  def _name: String

  type Columns <: Tuple
  @targetName("all")
  def * : Columns

  transparent inline def selectDynamic[Tag <: Singleton](
    tag: Tag
  )(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    *.productElement(index.value)
      .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

  def select[T](func: Table[P] => T): Select[P, T] =
    val columns = func(this)
    val str = columns match
      case v: Tuple => v.toArray.distinct.mkString(", ")
      case v        => v
    val statement = s"SELECT $str FROM $_name"
    Select(this, statement, columns, Nil)

  /**
   * A method to perform a simple Join.
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def join[O <: Product](other: Table[O])(
    on: Table[P] *: Tuple1[Table[O]] => Expression
  ): Join[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[Table[O]]] =
    val joins: Table[P] *: Tuple1[Table[O]] = this *: Tuple(other)
    Join.Impl[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[Table[O]]](
      this,
      joins,
      joins,
      List(s"${ Join.JoinType.JOIN.statement } ${ other._name } ON ${ on(joins).statement }")
    )

  /**
   * Method to perform Left Join.
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def leftJoin[O <: Product](other: Table[O])(
    on: Table[P] *: Tuple1[Table[O]] => Expression
  ): Join[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[Table.Opt[O]]] =
    val joins: Table[P] *: Tuple1[Table[O]] = this *: Tuple(other)
    Join.Impl[Table[P] *: Tuple1[Table[O]], Table[P] *: Tuple1[Table.Opt[O]]](
      this,
      joins,
      this *: Tuple(Table.Opt(other.*)),
      List(s"${ Join.JoinType.LEFT_JOIN.statement } ${ other._name } ON ${ on(joins).statement }")
    )

  /**
   * Method to perform Right Join.
   *
   * @param other
   *   [[Table]] to do a Join.
   * @param on
   *   Comparison function that performs a Join.
   * @tparam O
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def rightJoin[O <: Product](other: Table[O])(
    on: Table[P] *: Tuple1[Table[O]] => Expression
  ): Join[Table[P] *: Tuple1[Table[O]], Table.Opt[P] *: Tuple1[Table[O]]] =
    val joins: Table[P] *: Tuple1[Table[O]] = this *: Tuple(other)
    Join.Impl[Table[P] *: Tuple1[Table[O]], Table.Opt[P] *: Tuple1[Table[O]]](
      this,
      joins,
      Table.Opt(this.*) *: Tuple(other),
      List(s"${ Join.JoinType.RIGHT_JOIN.statement } ${ other._name } ON ${ on(joins).statement }")
    )

  /** Type alias for ParameterBinder. Mainly for use with Tuple.map. */
  private type ParamBind[A] = Parameter.Binder

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
  ): Insert[P] =
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
    new MultiInsert[P, Tuple](this, values.toList, parameterBinders)

  /**
   * A method to build a query model that inserts data into specified columns defined in a table.
   *
   * @param func
   *   Function to retrieve columns from Table.
   * @tparam T
   *   Type of value to be obtained
   */
  inline def insertInto[T](func: Table[P] => T)(using
    Tuples.IsColumn[T] =:= true
  ): SelectInsert[P, T] =
    val parameter: Parameter.MapToTuple[Column.Extract[T]] = Parameter.fold[Column.Extract[T]]
    SelectInsert[P, T](this, func(this), parameter)

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
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check:  T =:= Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[P] =
    type PARAM = Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val params = List(Parameter.DynamicBinder[PARAM](check(value))(using Parameter.infer[PARAM]))
    new Update[P](
      table   = this,
      columns = List(selectDynamic[Tag](tag).name),
      params  = params
    )

  /**
   * A method to build a query model that updates all columns defined in the table using the model.
   *
   * @param value
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   * @param mirror
   *   product isomorphism map
   */
  inline def update(value: P)(using mirror: Mirror.ProductOf[P]): Update[P] =
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
    new Update[P](
      table   = this,
      columns = *.toList.map(_.asInstanceOf[Column[?]].name),
      params  = params
    )

  /**
   * Method to construct a query to delete a table.
   */
  def delete: Delete[P, Columns] = Delete[P, Columns](this, *)

object Table:

  def apply[P <: Product](using t: Table[P]): Table[P] = t

  private[ldbc] case class Opt[P](columns: Tuple) extends Dynamic:

    transparent inline def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.ProductOf[P],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[
      Option[ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
    ] =
      columns
        .productElement(index.value)
        .asInstanceOf[Column[
          Option[ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
        ]]

  private inline def buildColumns[NT <: Tuple, T <: Tuple, I <: Int](
    inline nt: NT,
    inline xs: List[Column[?]]
  ): Tuple.Map[T, Column] =
    inline nt match
      case nt1: (e *: ts) =>
        inline nt1.head match
          case h: String =>
            val c = Column.Impl[Tuple.Elem[T, I]](h, None)
            buildColumns[ts, T, I + 1](nt1.tail, xs :+ c)
          case n: (name, _) =>
            error("stat " + constValue[name] + " should be a constant string")
      case _: EmptyTuple => Tuple.fromArray(xs.toArray).asInstanceOf[Tuple.Map[T, Column]]

  inline def derived[P <: Product](using m: Mirror.ProductOf[P]): Table[P] =
    val labels = constValueTuple[m.MirroredElemLabels]
    new:
      override def _name: String = constValue[m.MirroredLabel]
      override type Columns = Tuple.Map[m.MirroredElemTypes, Column]
      @targetName("all")
      override def * : Columns = buildColumns[m.MirroredElemLabels, m.MirroredElemTypes, 0](labels, Nil)
