/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.core

import scala.deriving.Mirror
import scala.language.dynamics

import ldbc.core.interpreter.*

/**
 * Trait for generating SQL table information.
 *
 * @tparam P
 *   A class that implements a [[Product]] that is one-to-one with the table definition.
 */
private[ldbc] trait Table[P <: Product] extends Dynamic:

  /** Table name */
  private[ldbc] def _name: String

  /** Tuple of columns the table has. */
  private[ldbc] def columns: Tuple

  /** Table Key definitions */
  private[ldbc] def keyDefinitions: Seq[Key]

  /** Table alias name */
  private[ldbc] def alias: Option[String]

  /** Additional table information */
  private[ldbc] def options: Seq[TableOption | Character | Collate[String]]

  /**
   * Method to retrieve an array of column information that a table has.
   */
  private[ldbc] def all: List[Column[[A] => A => A]]

  /**
   * Methods for setting key information for tables.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def keySet(func: Table[P] => Key): Table[P]

  /**
   * Methods for setting multiple key information for a table.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def keySets(func: Table[P] => Seq[Key]): Table[P]

  /**
   * Methods for setting additional information for the table.
   *
   * @param option
   *   Additional information to be given to the table.
   */
  def setOption(option: TableOption | Character | Collate[String]): Table[P]

  /**
   * Methods for setting multiple additional information for a table.
   *
   * @param options
   *   Additional information to be given to the table.
   */
  def setOptions(options: Seq[TableOption]): Table[P]

  /**
   * Methods for setting alias names for tables.
   *
   * @param name
   *   Alias name to be set for the table
   */
  def as(name: String): Table[P]

object Table extends Dynamic:

  extension [P <: Product](table: Table[P])
    /**
     * Methods for statically accessing column information held by a Table.
     */
    transparent inline def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.ProductOf[P],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]] =
      val column = table.columns
        .productElement(index.value)
        .asInstanceOf[Column[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
      table.alias.fold(column)(name => column.as(name))

  private case class Impl[P <: Product, T <: Tuple](
    _name:          String,
    columns:        Tuple.Map[T, Column],
    keyDefinitions: Seq[Key],
    options:        Seq[TableOption | Character | Collate[String]],
    alias:          Option[String] = None
  ) extends Table[P]:

    override private[ldbc] def all: List[Column[[A] => A => A]] =
      columns.toList.asInstanceOf[List[Column[[A] => A => A]]]

    override def keySet(func: Table[P] => Key): Table[P] = this.copy(keyDefinitions = this.keyDefinitions :+ func(this))

    override def keySets(func: Table[P] => Seq[Key]): Table[P] =
      this.copy(keyDefinitions = this.keyDefinitions ++ func(this))

    override def setOption(option: TableOption | Character | Collate[String]): Table[P] =
      this.copy(options = options :+ option)

    override def setOptions(options: Seq[TableOption]): Table[P] = this.copy(options = options)

    override def as(name: String): Table[P] = this.copy(alias = Some(name))

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
  def applyDynamic[P <: Product](using
    mirror:    Mirror.ProductOf[P],
    converter: ColumnTupleConverter[mirror.MirroredElemTypes, Column]
  )(nameApply: "apply")(name: String)(columns: ColumnTuples[mirror.MirroredElemTypes, Column]): Table[P] =
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
  ): Table[P] = Impl[P, mirror.MirroredElemTypes](name, columns, Seq.empty, Seq.empty, None)
