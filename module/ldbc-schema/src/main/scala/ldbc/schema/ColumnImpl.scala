/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.dsl.codec.{ Decoder, Encoder }
import ldbc.statement.Column
import ldbc.schema.attribute.Attribute

private[ldbc] case class ColumnImpl[T](
  name:       String,
  alias:      Option[String],
  decoder:    Decoder[T],
  encoder:    Encoder[T],
  dataType:   Option[DataType[T]],
  attributes: List[Attribute[T]]
) extends Column[T]:

  override def as(name: String): Column[T] =
    this.copy(alias   = Some(name))

  override def statement: String =
    dataType.fold(s"`$name`")(dataType => s"`$name` ${ dataType.queryString }") + attributes
      .map(v => s" ${ v.queryString }")
      .mkString("")
  override def updateStatement:             String = s"$name = ?"
  override def duplicateKeyUpdateStatement: String = s"$name = VALUES(${ alias.getOrElse(name) })"
