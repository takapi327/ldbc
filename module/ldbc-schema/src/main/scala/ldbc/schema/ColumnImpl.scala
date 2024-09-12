/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.schema

import ldbc.sql.ResultSet
import ldbc.dsl.codec.Decoder
import ldbc.query.builder.Column
import ldbc.schema.attribute.Attribute

/**
 * Case class for representing SQL Column
 *
 * @param name
 *   Column Field Name
 * @param alias
 *   Column alias name
 * @param dataType
 *   Column data type
 * @param attributes
 *   Extra attribute of column
 * @param decoder
 *   Decoder for converting SQL data to Scala data
 * @tparam T
 *   Scala types that match SQL DataType
 */
case class ColumnImpl[T](
  name:       String,
  alias:      Option[String],
  dataType:   DataType[T],
  attributes: List[Attribute[T]],
  decoder:    Decoder[T]
) extends Column[T]:

  override def as(name: String): Column[T] = ColumnImpl[T](this.name, Some(name), dataType, attributes, decoder)

  /**
   * Define SQL query string for each Column
   *
   * @return
   *   SQL query string
   */
  def queryString: String =
    val str = s"`$name` ${ dataType.queryString }" + attributes.map(v => s" ${ v.queryString }").mkString("")
    alias.fold(str)(name => s"$name.$str")

  override def toString: String = alias.fold(s"`$name`")(name => s"$name.`${ this.name }`")

object Column:

  def apply[T](
    name:     String,
    dataType: DataType[T]
  )(using Decoder.Elem[T]): ColumnImpl[T] =
    val attributes: List[Attribute[T]] = dataType match
      case data: DataType.Alias[T] => data.attributes
      case _                       => List.empty
    this.apply(name, dataType, attributes*)

  def apply[T](
    name:       String,
    dataType:   DataType[T],
    attributes: Attribute[T]*
  )(using Decoder.Elem[T]): ColumnImpl[T] =
    this.apply(name, dataType, attributes, None)

  private[ldbc] def apply[T](
    name:       String,
    dataType:   DataType[T],
    attributes: Seq[Attribute[T]],
    alias:      Option[String]
  )(using elem: Decoder.Elem[T]): ColumnImpl[T] =
    val decoder: Decoder[T] = (resultSet: ResultSet, prefix: Option[String]) =>
      elem.decode(resultSet, prefix.map(_ + ".").getOrElse("") + name)
    ColumnImpl[T](name, alias, dataType, attributes.toList, decoder)
