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
import ldbc.schema.interpreter.*
import ldbc.schema.attribute.Attribute

trait Table[T](val $name: String) extends AbstractTable[T], Alias:

  protected final def column[A](name: String)(using elem: Decoder.Elem[A]): Column[A] =
    val decoder = new Decoder[A]((resultSet, prefix) => elem.decode(resultSet, prefix.getOrElse(s"${ $name }.$name")))
    ColumnImpl[A](name, Some(s"${ $name }.$name"), decoder, None, List.empty)

  protected final def column[A](name: String, dataType: DataType[A])(using elem: Decoder.Elem[A]): Column[A] =
    val decoder = new Decoder[A]((resultSet, prefix) => elem.decode(resultSet, prefix.getOrElse(s"${ $name }.$name")))
    ColumnImpl[A](name, Some(s"${ $name }.$name"), decoder, Some(dataType), List.empty)

  protected final def column[A](name: String, dataType: DataType[A], attributes: Attribute[A]*)(using
    elem: Decoder.Elem[A]
  ): Column[A] =
    val decoder = new Decoder[A]((resultSet, prefix) => elem.decode(resultSet, prefix.getOrElse(s"${ $name }.$name")))
    ColumnImpl[A](name, Some(s"${ $name }.$name"), decoder, Some(dataType), attributes.toList)

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
