/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.annotation.targetName
import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.sql.ResultSet
import ldbc.dsl.codec.Decoder
import ldbc.query.builder.{ Table, Column }
import ldbc.schema.interpreter.*

private[ldbc] case class TableImpl[P <: Product, ElemLabels0 <: Tuple, ElemTypes0 <: Tuple](
  _name:          String,
  _alias:         Option[String],
  columns:        Tuple.Map[ElemTypes0, Column],
  columnNames:    List[String],
  keyDefinitions: List[Key],
  options:        List[TableOption | Character | Collate[String]],
  decoder:        Decoder[P]
) extends Table[P]:

  override type ElemLabels = ElemLabels0
  override type ElemTypes  = ElemTypes0

  @targetName("all")
  override def * : Tuple.Map[ElemTypes, Column] = columns

  override def as(name: String): Table[P] = this.copy(_alias = Some(name))

  override def setName(name: String): Table[P] = this.copy(_name = name)

  /**
   * Methods for setting key information for tables.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def keySet(func: TableImpl[P, ElemLabels0, ElemTypes0] => Key): TableImpl[P, ElemLabels0, ElemTypes0] =
    this.copy(keyDefinitions = List(func(this)))

  /**
   * Methods for setting multiple key information for a table.
   *
   * @param func
   *   Function to construct an expression using the columns that Table has.
   */
  def keySets(func: TableImpl[P, ElemLabels0, ElemTypes0] => List[Key]): TableImpl[P, ElemLabels0, ElemTypes0] =
    this.copy(keyDefinitions = func(this))

  /**
   * Methods for setting additional information for the table.
   *
   * @param option
   *   Additional information to be given to the table.
   */
  def setOption(option: TableOption | Character | Collate[String]): TableImpl[P, ElemLabels0, ElemTypes0] =
    this.copy(options = options :+ option)

  /**
   * Methods for setting multiple additional information for a table.
   *
   * @param options
   *   Additional information to be given to the table.
   */
  def setOptions(options: List[TableOption]): TableImpl[P, ElemLabels0, ElemTypes0] =
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
  ): TableImpl[P, mirror.MirroredElemLabels, mirror.MirroredElemTypes] =
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
  ): TableImpl[P, mirror.MirroredElemLabels, mirror.MirroredElemTypes] =
    val decoder: Decoder[P] = (resultSet: ResultSet, prefix: Option[String]) =>
      mirror.fromTuple(
        Tuple
          .fromArray(columns.toArray.map {
            case column: Column[?] => column.decoder.decode(resultSet, prefix.orElse(Some(name)))
          })
          .asInstanceOf[mirror.MirroredElemTypes]
      )
    TableImpl[P, mirror.MirroredElemLabels, mirror.MirroredElemTypes](
      _name   = name,
      _alias  = None,
      columns = columns,
      columnNames = columns.toList.map {
        case column: Column[?] => column.name
      },
      keyDefinitions = List.empty,
      options        = List.empty,
      decoder        = decoder
    )
