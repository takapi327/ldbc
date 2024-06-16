/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.query.builder.statement

import scala.deriving.Mirror
import scala.annotation.targetName

import ldbc.dsl.*
import ldbc.query.builder.*
import ldbc.query.builder.interpreter.Tuples

/**
 * A model for constructing UPDATE statements in MySQL.
 *
 * @param table
 *   Trait for generating SQL table information.
 * @param columns
 *   Column name list
 * @param params
 *   A list of Traits that generate values from Parameter, allowing PreparedStatement to be set to a value by index
 *   only.
 * @tparam P
 *   Base trait for all products
 */
case class Update[P <: Product](
  table:   Table[P],
  columns: List[String],
  params:  List[Parameter.DynamicBinder],
  other:   Option[String] = None
) extends Command:

  private val values = columns.map(column => s"$column = ?")

  override def statement: String =
    s"UPDATE ${ table._name } SET ${ values.mkString(", ") }" ++ other.fold("")(str => s" $str")

  @targetName("combine")
  override def ++(sql: SQL): SQL =
    Update(table, columns, params ++ sql.params, Some(sql.statement))

  /**
   * A method that sets additional values to be updated in the query model that updates specific columns defined in the
   * table.
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
  inline def set[Tag <: Singleton, T](tag: Tag, value: T)(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check:  T =:= Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[P] =
    type Param = Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val param = Parameter.DynamicBinder[Param](check(value))(using Parameter.infer[Param])
    this.copy(
      columns = columns :+ table.selectDynamic[Tag](tag).name,
      params  = params :+ param
    )

  /**
   * A method that sets additional values to be updated in the query model that updates specific columns defined in the
   * table.
   *
   * @param tag
   * A type with a single instance. Here, Column is passed.
   * @param value
   * A value of type T to be inserted into the specified column.
   * @param mirror
   * product isomorphism map
   * @param index
   * Position of the specified type in tuple X
   * @param check
   * A value to verify that the specified type matches the type of the specified column that the Table has.
   * @tparam Tag
   * Type with a single instance
   * @tparam T
   * Scala types that match SQL DataType
   */
  inline def set[Tag <: Singleton, T](tag: Tag, value: Option[T])(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check:  Option[T] =:= Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[P] =
    type Param = Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    val param = Parameter.DynamicBinder[Param](check(value))(using Parameter.infer[Param])
    this.copy(
      columns = columns :+ table.selectDynamic[Tag](tag).name,
      params  = params :+ param
    )

  /**
   * A method that sets additional values to be updated in the query model that updates specific columns defined in the
   * table.
   *
   * @param tag
   *   A type with a single instance. Here, Column is passed.
   * @param value
   *   A value of type T to be inserted into the specified column.
   * @param bool
   *   Conditional value of whether or not to update the value.
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
  inline def set[Tag <: Singleton, T](tag: Tag, value: T, bool: Boolean)(using
    mirror: Mirror.ProductOf[P],
    index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]],
    check:  T =:= Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
  ): Update[P] =
    if bool then
      type Param = Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
      val param = Parameter.DynamicBinder[Param](check(value))(using Parameter.infer[Param])
      this.copy(
        columns = columns :+ table.selectDynamic[Tag](tag).name,
        params  = params :+ param
      )
    else this

  /**
   * A method for setting the WHERE condition in a UPDATE statement.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def where(func: Table[P] => Expression): Where[P, Tuple.Map[table.Columns, Column]] =
    val expression = func(table)
    Where(
      table     = table,
      statement = statement ++ s" WHERE ${ expression.statement }",
      columns   = table.*,
      params    = params ++ expression.parameter
    )
