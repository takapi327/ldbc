/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.deriving.Mirror
import scala.language.dynamics

import ldbc.dsl.codec.Codec

import ldbc.statement.{ AbstractTable, Column }

import ldbc.schema.attribute.Attribute
import ldbc.schema.interpreter.*

trait Table[T](val $name: String) extends AbstractTable[T]:

  export ldbc.statement.Column

  protected final def column[A](name: String)(using codec: Codec[A]): Column[A] =
    ColumnImpl[A](name, Some(s"${ $name }.$name"), codec.asDecoder, codec.asEncoder, None, List.empty)

  protected final def column[A](name: String, dataType: DataType[A])(using codec: Codec[A]): Column[A] =
    ColumnImpl[A](name, Some(s"${ $name }.$name"), codec.asDecoder, codec.asEncoder, Some(dataType), List.empty)

  protected final def column[A](name: String, dataType: DataType[A], attributes: Attribute[A]*)(using
    codec: Codec[A]
  ): Column[A] =
    ColumnImpl[A](name, Some(s"${ $name }.$name"), codec.asDecoder, codec.asEncoder, Some(dataType), attributes.toList)

  /**
   * Methods for setting key information for tables.
   */
  def keys: List[Key] = List.empty

  override final def statement: String = $name

  override def toString: String = s"Table($$name)"

object Table:

  case class Opt[T](
    $name:   String,
    columns: List[Column[?]],
    *      : Column[Option[T]]
  ) extends AbstractTable.Opt[T],
            Dynamic:

    override def statement: String = $name

    transparent inline def selectDynamic[Tag <: Singleton](
      tag: Tag
    )(using
      mirror: Mirror.Of[T],
      index:  ValueOf[Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]
    ): Column[
      Option[ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]]
    ] =
      columns
        .apply(index.value)
        .asInstanceOf[Column[
          ExtractOption[Tuple.Elem[mirror.MirroredElemTypes, Tuples.IndexOf[mirror.MirroredElemLabels, Tag]]]
        ]]
        .opt
