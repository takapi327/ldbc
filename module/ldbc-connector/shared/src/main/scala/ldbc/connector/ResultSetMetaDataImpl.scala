/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.sql.ResultSetMetaData

import ldbc.connector.data.*
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*
import ldbc.connector.util.Version

private[ldbc] class ResultSetMetaDataImpl(
  columns:         Vector[ColumnDefinitionPacket],
  serverVariables: Map[String, String],
  version:         Version
) extends ResultSetMetaData:
  override def getColumnCount(): Int = columns.size

  override def isAutoIncrement(column: Int): Boolean =
    unsafeFindByIndex(column).flags.contains(ColumnDefinitionFlags.AUTO_INCREMENT_FLAG)

  override def isCaseSensitive(column: Int): Boolean = unsafeFindByIndex(column).columnType match
    case ColumnDataType.MYSQL_TYPE_BIT | ColumnDataType.MYSQL_TYPE_TINY | ColumnDataType.MYSQL_TYPE_SHORT |
      ColumnDataType.MYSQL_TYPE_LONG | ColumnDataType.MYSQL_TYPE_INT24 | ColumnDataType.MYSQL_TYPE_LONGLONG |
      ColumnDataType.MYSQL_TYPE_FLOAT | ColumnDataType.MYSQL_TYPE_DOUBLE | ColumnDataType.MYSQL_TYPE_DATE |
      ColumnDataType.MYSQL_TYPE_YEAR | ColumnDataType.MYSQL_TYPE_TIME | ColumnDataType.MYSQL_TYPE_TIMESTAMP |
      ColumnDataType.MYSQL_TYPE_TIMESTAMP2 | ColumnDataType.MYSQL_TYPE_DATETIME =>
      false
    case ColumnDataType.MYSQL_TYPE_STRING | ColumnDataType.MYSQL_TYPE_VARCHAR | ColumnDataType.MYSQL_TYPE_VAR_STRING |
      ColumnDataType.MYSQL_TYPE_JSON | ColumnDataType.MYSQL_TYPE_ENUM | ColumnDataType.MYSQL_TYPE_SET =>
      CharsetMapping
        .getStaticCollationNameForCollationIndex(
          getMysqlCharsetForJavaEncoding(serverVariables.getOrElse("character_set_client", "utf8mb4"))
        )
        .fold(false)(_.endsWith("_ci"))
    case _ => true

  override def isSearchable(column: Int): Boolean = true

  override def isCurrency(column: Int): Boolean = false

  override def isNullable(column: Int): Int =
    if unsafeFindByIndex(column).flags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG) then
      ResultSetMetaData.columnNoNulls
    else ResultSetMetaData.columnNullable

  override def isSigned(column: Int): Boolean =
    unsafeFindByIndex(column).flags.contains(ColumnDefinitionFlags.UNSIGNED_FLAG)

  override def getColumnDisplaySize(column: Int): Int = clampedGetLength(unsafeFindByIndex(column))

  override def getColumnLabel(column: Int): String = unsafeFindByIndex(column) match
    case definition: ColumnDefinition41Packet  => definition.name
    case definition: ColumnDefinition320Packet => definition.name

  override def getColumnName(column: Int): String = unsafeFindByIndex(column) match
    case definition: ColumnDefinition41Packet  => definition.orgName
    case definition: ColumnDefinition320Packet => definition.name

  override def getSchemaName(column: Int): String = unsafeFindByIndex(column) match
    case definition: ColumnDefinition41Packet => definition.schema
    case _: ColumnDefinition320Packet         => ""

  override def getPrecision(column: Int): Int =
    val definition = unsafeFindByIndex(column)
    definition.columnType match
      case ColumnDataType.MYSQL_TYPE_TINY_BLOB | ColumnDataType.MYSQL_TYPE_BLOB |
        ColumnDataType.MYSQL_TYPE_MEDIUM_BLOB | ColumnDataType.MYSQL_TYPE_LONG_BLOB |
        ColumnDataType.MYSQL_TYPE_DECIMAL =>
        clampedGetLength(definition)
      case _ =>
        val charset = serverVariables.getOrElse("character_set_client", "utf8mb4")
        clampedGetLength(definition) / getMaxBytesPerChar(
          getMysqlCharsetForJavaEncoding(charset),
          charset
        )

  override def getScale(column: Int): Int = unsafeFindByIndex(column) match
    case definition: ColumnDefinition41Packet => definition.decimals
    case _                                    => 0

  override def getTableName(column: Int): String = unsafeFindByIndex(column) match
    case definition: ColumnDefinition41Packet  => definition.orgTable
    case definition: ColumnDefinition320Packet => definition.table

  override def getCatalogName(column: Int): String = unsafeFindByIndex(column) match
    case definition: ColumnDefinition41Packet => definition.catalog
    case _: ColumnDefinition320Packet         => ""

  override def getColumnType(column: Int): Int =
    val dataType = unsafeFindByIndex(column).columnType
    dataType match
      case ColumnDataType.MYSQL_TYPE_YEAR => ColumnDataType.MYSQL_TYPE_SHORT.code.toInt
      case _                              => dataType.code.toInt

  override def getColumnTypeName(column: Int): String = unsafeFindByIndex(column).columnType.name

  override def isReadOnly(column: Int): Boolean =
    val definition = unsafeFindByIndex(column)
    definition.name.isEmpty && definition.table.isEmpty

  override def isWritable(column: Int): Boolean = !isReadOnly(column)

  override def isDefinitelyWritable(column: Int): Boolean = isWritable(column)

  private def clampedGetLength(definition: ColumnDefinitionPacket): Int =
    definition match
      case definition: ColumnDefinition41Packet =>
        val length = definition.length
        if length > Int.MaxValue then Int.MaxValue else length
      case _: ColumnDefinition320Packet => Int.MaxValue

  private def getMysqlCharsetForJavaEncoding(javaEncoding: String): Int =
    CharsetMapping.getStaticCollationIndexForMysqlCharsetName(
      CharsetMapping.getStaticMysqlCharsetForJavaEncoding(javaEncoding, Some(version))
    )

  private def getMaxBytesPerChar(charsetIndex: Int, javaCharsetName: String): Int =
    CharsetMapping.getStaticMysqlCharsetNameForCollationIndex(charsetIndex) match
      case Some(charsetName) => CharsetMapping.getStaticMblen(charsetName)
      case None =>
        CharsetMapping.getStaticMysqlCharsetForJavaEncoding(javaCharsetName, Some(version)) match
          case Some(charsetName) => CharsetMapping.getStaticMblen(charsetName)
          case None              => 1

  private def unsafeFindByIndex(index: Int): ColumnDefinitionPacket =
    columns.lift(index - 1) match
      case Some(column) => column
      case None         => throw new SQLException("Column index out of range.")

private[ldbc] object ResultSetMetaDataImpl:

  def apply(
    columns:         Vector[ColumnDefinitionPacket],
    serverVariables: Map[String, String],
    version:         Version
  ): ResultSetMetaData =
    new ResultSetMetaDataImpl(columns, serverVariables, version)
