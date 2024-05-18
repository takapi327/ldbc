/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.internal

import cats.effect.Sync

import ldbc.sql.ResultSetMetaData

trait ResultSetMetaDataSyntax:

  implicit class ResultSetMetaDataF[F[_]: Sync](resultSetMetaDataObject: ResultSetMetaData.type):

    def apply(resultSetMetaData: java.sql.ResultSetMetaData): ResultSetMetaData =
      new ResultSetMetaData:

        override def getColumnCount(): Int = resultSetMetaData.getColumnCount

        override def isAutoIncrement(column: Int): Boolean =
          resultSetMetaData.isAutoIncrement(column)

        override def isCaseSensitive(column: Int): Boolean =
          resultSetMetaData.isCaseSensitive(column)

        override def isSearchable(column: Int): Boolean = resultSetMetaData.isSearchable(column)

        override def isCurrency(column: Int): Boolean = resultSetMetaData.isCurrency(column)

        override def isNullable(column: Int): Int =
          resultSetMetaData.isNullable(column)

        override def isSigned(column: Int): Boolean = resultSetMetaData.isSigned(column)

        override def getColumnDisplaySize(column: Int): Int =
          resultSetMetaData.getColumnDisplaySize(column)

        override def getColumnLabel(column: Int): String = resultSetMetaData.getColumnLabel(column)

        override def getColumnName(column: Int): String = resultSetMetaData.getColumnName(column)

        override def getSchemaName(column: Int): String = resultSetMetaData.getSchemaName(column)

        override def getPrecision(column: Int): Int = resultSetMetaData.getPrecision(column)

        override def getScale(column: Int): Int = resultSetMetaData.getScale(column)

        override def getTableName(column: Int): String = resultSetMetaData.getTableName(column)

        override def getCatalogName(column: Int): String = resultSetMetaData.getCatalogName(column)

        override def getColumnType(column: Int): Int =
          resultSetMetaData.getColumnType(column)

        override def getColumnTypeName(column: Int): String =
          resultSetMetaData.getColumnTypeName(column)

        override def isReadOnly(column: Int): Boolean = resultSetMetaData.isReadOnly(column)

        override def isWritable(column: Int): Boolean = resultSetMetaData.isWritable(column)

        override def isDefinitelyWritable(column: Int): Boolean =
          resultSetMetaData.isDefinitelyWritable(column)
