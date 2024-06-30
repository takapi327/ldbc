/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.codegen.builder

import ldbc.codegen.model.DataType
import ldbc.query.builder.formatter.Naming

/**
 * DataType model for constructing code strings.
 *
 * @param scalaType
 *   Scala types passed to the DataType type parameter
 * @param formatter
 *   A formatter that converts strings to an arbitrary format
 */
case class DataTypeCodeBuilder(scalaType: String, formatter: Naming):

  def build(dataType: DataType): String =
    dataType match
      case data: DataType.BIT => data.length.fold(s"${ data.name }[$scalaType]")(n => s"${ data.name }[$scalaType]($n)")
      case data: DataType.NumberDataType =>
        data.length.fold(s"${ data.name }[$scalaType]")(n => s"${ data.name }[$scalaType]($n)") + buildNumberDataType(
          data.unsigned,
          data.zerofill
        )
      case data: DataType.DECIMAL =>
        s"${ data.name }[$scalaType](${ data.accuracy }, ${ data.scale })" + buildNumberDataType(
          data.unsigned,
          data.zerofill
        )
      case data: DataType.FLOAT =>
        s"${ data.name }[$scalaType](${ data.accuracy })" + buildNumberDataType(data.unsigned, data.zerofill)
      case data: DataType.StringDataType =>
        s"${ data.name }[$scalaType](${ data.length })" + buildCharacterSet(data.character, data.collate)
      case data: DataType.BINARY    => s"${ data.name }[$scalaType](${ data.length })"
      case data: DataType.VARBINARY => s"${ data.name }[$scalaType](${ data.length })"
      case data: DataType.TINYBLOB  => s"${ data.name }[$scalaType]()"
      case data: DataType.TINYTEXT  => s"${ data.name }[$scalaType]()" + buildCharacterSet(data.character, data.collate)
      case data: DataType.ENUM =>
        s"${ data.name }[$scalaType](using ${ scalaType.replace("Option[", "").replace("]", "") })" + buildCharacterSet(
          data.character,
          data.collate
        )
      case data: DataType.BLOB =>
        data.length.fold(s"${ data.name }[$scalaType]()")(n => s"${ data.name }[$scalaType]($n)")
      case data: DataType.TEXT => s"${ data.name }[$scalaType]()" + buildCharacterSet(data.character, data.collate)
      case data: DataType.MEDIUMBLOB => s"${ data.name }[$scalaType]()"
      case data: DataType.MEDIUMTEXT =>
        s"${ data.name }[$scalaType]()" + buildCharacterSet(data.character, data.collate)
      case data: DataType.LONGBLOB => s"${ data.name }[$scalaType]()"
      case data: DataType.LONGTEXT => s"${ data.name }[$scalaType]()" + buildCharacterSet(data.character, data.collate)
      case data: DataType.DATE     => s"${ data.name }[$scalaType]"
      case data: DataType.DATETIME =>
        data.fsp.fold(s"${ data.name }[$scalaType]")(n => s"${ data.name }[$scalaType]($n)")
      case data: DataType.TIMESTAMP =>
        data.fsp.fold(s"${ data.name }[$scalaType]")(n => s"${ data.name }[$scalaType]($n)")
      case data: DataType.TIME => data.fsp.fold(s"${ data.name }[$scalaType]")(n => s"${ data.name }[$scalaType]($n)")
      case data: DataType.YEAR => data.digit.fold(s"${ data.name }[$scalaType]")(n => s"${ data.name }[$scalaType]($n)")
      case data: DataType.SERIAL  => s"${ data.name }[${ data.scalaType.code }]"
      case data: DataType.BOOLEAN => s"${ data.name }[$scalaType]"

  private def buildNumberDataType(unsigned: Boolean, zerofill: Boolean): String =
    (unsigned, zerofill) match
      case (true, true)   => ".UNSIGNED.ZEROFILL"
      case (true, false)  => ".UNSIGNED"
      case (false, true)  => ".ZEROFILL"
      case (false, false) => ""

  private def buildCharacterSet(character: Option[String], collate: Option[String]): String =
    (character, collate) match
      case (Some(ch), Some(co)) => s".CHARACTER_SET(Character.$ch).COLLATE(Collate.$co)"
      case (Some(ch), None)     => s".CHARACTER_SET(Character.$ch)"
      case (None, Some(co))     => s".COLLATE(Collate.$co)"
      case (None, None)         => ""
