/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.dsl.codec.Decoder
import ldbc.statement.{ AbstractTable, Column }
import ldbc.query.builder.interpreter.Tuples
import ldbc.schema.interpreter.*

private[ldbc] case class Table[P <: Product](
  $name:          String,
  columns: List[Column[?]],
  keyDefinitions: List[Key],
  options:        List[TableOption | Character | Collate[String]],
)(using mirror: Mirror.ProductOf[P]) extends AbstractTable[P], Dynamic:

  override def statement: String = $name

  override def * : Column[P] =
    val decoder: Decoder[P] = new Decoder[P]((resultSet, prefix) =>
      mirror.fromTuple(
        Tuple
          .fromArray(columns.map(_.decoder.decode(resultSet, prefix)).toArray)
          .asInstanceOf[mirror.MirroredElemTypes]
      )
    )

    val alias = columns.flatMap(_.alias).mkString(", ")
    Column.Impl[P](
      columns.map(_.name).mkString(", "),
      if alias.isEmpty then None else Some(alias),
      decoder,
      Some(columns.length),
      Some(columns.map(column => s"${column.name} = ?").mkString(", "))
    )

  /**
   * A method to get a specific column defined in the table.
   *
   * @param tag
   * A type with a single instance. Here, Column is passed.
   * @param mirror
   * product isomorphism map
   * @param index
   * Position of the specified type in tuple X
   * @tparam Tag
   * Type with a single instance
   */
  transparent inline def selectDynamic[Tag <: Singleton](
                                                          tag: Tag
                                                        )(using
                                                          mirror: Mirror.Of[P],
                                                          index: ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
                                                        ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
    columns.apply(index.value)
      .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]

  def setName(name: String): Table[P] =
    this.copy(
      $name = name,
      columns = columns.map(column => column.as(s"$name.${column.name}"))
    )

  /**
   * Methods for setting key information for tables.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def keySet(func: Table[P] => Key): Table[P] =
    this.copy(keyDefinitions = List(func(this)))

  /**
   * Methods for setting multiple key information for a table.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def keySets(func: Table[P] => List[Key]): Table[P] =
    this.copy(keyDefinitions = func(this))

  /**
   * Methods for setting additional information for the table.
   *
   * @param option
   *   Additional information to be given to the table.
   */
  def setOption(option: TableOption | Character | Collate[String]): Table[P] =
    this.copy(options = options :+ option)

  /**
   * Methods for setting multiple additional information for a table.
   *
   * @param options
   *   Additional information to be given to the table.
   */
  def setOptions(options: List[TableOption]): Table[P] =
    this.copy(options = this.options ++ options)

object Table:

  /**
   * Methods for static Table construction using Dynamic.
   *
   * @param nameApply
   *   The apply method
   * @param mirror
   *   product isomorphism map
   * @param converter
   *   An object that converts a Column's Tuple to a Tuple Map
   * @param name
   *   Table name
   * @param columns
   *   Tuple of columns matching the Product's Elem type
   * @tparam P
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  def apply[P <: Product](using
    mirror:    Mirror.ProductOf[P],
    converter: ColumnTupleConverter[mirror.MirroredElemTypes, Column]
  )(name: String)(
    columns: ColumnTuples[mirror.MirroredElemTypes, Column]
  ): Table[P] =
    fromTupleMap[P](name, ColumnTupleConverter.convert(columns))

  /**
   * Methods for generating a Table from a Column's Tuple Map.
   *
   * @param mirror
   *   product isomorphism map
   * @param name
   *   Table name
   * @param columns
   *   Tuple of columns matching the Product's Elem type
   * @tparam P
   *   A class that implements a [[Product]] that is one-to-one with the table definition.
   */
  private def fromTupleMap[P <: Product](using
    mirror: Mirror.ProductOf[P]
  )(
    name:    String,
    columns: Tuple.Map[mirror.MirroredElemTypes, Column]
  ): Table[P] =
    Table[P](
      $name   = name,
      columns = columns.toList.asInstanceOf[List[Column[?]]],
      keyDefinitions = List.empty,
      options        = List.empty,
    )
