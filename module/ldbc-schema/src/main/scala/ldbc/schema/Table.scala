/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import scala.language.dynamics
import scala.deriving.Mirror

import ldbc.dsl.codec.{ Decoder, Encoder }
import ldbc.statement.{ AbstractTable, Column }
import ldbc.schema.interpreter.*
import ldbc.schema.attribute.Attribute

trait Table[T](val $name: String) extends AbstractTable[T]:

  type Column[A] = ldbc.statement.Column[A]

  protected final def column[A](name: String)(using decoder: Decoder[A], encoder: Encoder[A]): Column[A] =
    ColumnImpl[A](name, Some(s"${ $name }.$name"), decoder, encoder, None, List.empty)

  protected final def column[A](name: String, dataType: DataType[A])(using
                                                                     decoder:    Decoder[A],
    encoder: Encoder[A]
  ): Column[A] =
    ColumnImpl[A](name, Some(s"${ $name }.$name"), decoder, encoder, Some(dataType), List.empty)

  protected final def column[A](name: String, dataType: DataType[A], attributes: Attribute[A]*)(using
                                                                                                decoder:    Decoder[A],
    encoder: Encoder[A]
  ): Column[A] =
    ColumnImpl[A](name, Some(s"${ $name }.$name"), decoder, encoder, Some(dataType), attributes.toList)

  /**
   * Methods for setting key information for tables.
   */
  def keys: List[Key] = List.empty

  override final def statement: String = $name

  override def toString: String = s"Table($$name)"

object Table:

  private[ldbc] case class Opt[T](
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
