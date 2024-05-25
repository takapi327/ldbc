/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import ldbc.sql.ResultSetMetaData

private[jdbc] case class ResultSetMetaDataImpl(metaData: java.sql.ResultSetMetaData) extends ResultSetMetaData:

  override def getColumnCount(): Int = metaData.getColumnCount

  override def isAutoIncrement(column: Int): Boolean = metaData.isAutoIncrement(column)

  override def isCaseSensitive(column: Int): Boolean = metaData.isCaseSensitive(column)

  override def isSearchable(column: Int): Boolean = metaData.isSearchable(column)

  override def isCurrency(column: Int): Boolean = metaData.isCurrency(column)

  override def isNullable(column: Int): Int = metaData.isNullable(column)

  override def isSigned(column: Int): Boolean = metaData.isSigned(column)

  override def getColumnDisplaySize(column: Int): Int = metaData.getColumnDisplaySize(column)

  override def getColumnLabel(column: Int): String = metaData.getColumnLabel(column)

  override def getColumnName(column: Int): String = metaData.getColumnName(column)

  override def getSchemaName(column: Int): String = metaData.getSchemaName(column)

  override def getPrecision(column: Int): Int = metaData.getPrecision(column)

  override def getScale(column: Int): Int = metaData.getScale(column)

  override def getTableName(column: Int): String = metaData.getTableName(column)

  override def getCatalogName(column: Int): String = metaData.getCatalogName(column)

  override def getColumnType(column: Int): Int = metaData.getColumnType(column)

  override def getColumnTypeName(column: Int): String = metaData.getColumnTypeName(column)

  override def isReadOnly(column: Int): Boolean = metaData.isReadOnly(column)

  override def isWritable(column: Int): Boolean = metaData.isWritable(column)

  override def isDefinitelyWritable(column: Int): Boolean = metaData.isDefinitelyWritable(column)
