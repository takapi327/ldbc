/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import ldbc.connector.data.*
import ldbc.connector.util.Version
import ldbc.connector.exception.SQLException
import ldbc.connector.net.packet.response.*

/**
 * An object that can be used to get information about the types
 * and properties of the columns in a <code>ResultSet</code> object.
 * The following code fragment creates the <code>ResultSet</code> object rs,
 * creates the <code>ResultSetMetaData</code> object rsmd, and uses rsmd
 * to find out how many columns rs has and whether the first column in rs
 * can be used in a <code>WHERE</code> clause.
 * <PRE>
 *
 *   for
 *     rs <- stmt.executeQuery("SELECT a, b, c FROM TABLE2")
 *     rsmd <- rs.getMetaData()
 *   yield
 *     val numberOfColumns = rsmd.getColumnCount()
 *     val b = rsmd.isSearchable(1)
 *
 * </PRE>
 */
trait ResultSetMetaData:

  /**
   * Returns the number of columns in this <code>ResultSet</code> object.
   *
   * @return the number of columns
   */
  def getColumnCount(): Int

  /**
   * Indicates whether the designated column is automatically numbered.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isAutoIncrement(column: Int): Boolean

  /**
   * Indicates whether the designated column can be used in a where clause.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isCaseSensitive(column: Int): Boolean

  /**
   * Indicates whether the designated column can be used in a where clause.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isSearchable(column: Int): Boolean

  /**
   * Indicates whether the designated column is a cash value.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isCurrency(column: Int): Boolean

  /**
   * Indicates the nullability of values in the designated column.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the nullability status of the given column; one of <code>columnNoNulls</code>,
   *          <code>columnNullable</code> or <code>columnNullableUnknown</code>
   */
  def isNullable(column: Int): Int

  /**
   * Indicates whether values in the designated column are signed numbers.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isSigned(column: Int): Boolean

  /**
   * Indicates the designated column's normal maximum width in characters.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the normal maximum number of characters allowed as the width
   *          of the designated column
   */
  def getColumnDisplaySize(column: Int): Int

  /**
   * Gets the designated column's suggested title for use in printouts and
   * displays. The suggested title is usually specified by the SQL <code>AS</code>
   * clause.  If a SQL <code>AS</code> is not specified, the value returned from
   * <code>getColumnLabel</code> will be the same as the value returned by the
   * <code>getColumnName</code> method.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the suggested column title
   */
  def getColumnLabel(column: Int): String

  /**
   * Get the designated column's name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return column name
   */
  def getColumnName(column: Int): String

  /**
   * Get the designated column's table's schema.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return schema name or "" if not applicable
   */
  def getSchemaName(column: Int): String

  /**
   * Get the designated column's specified column size.
   * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
   * For datetime datatypes, this is the length in characters of the String representation (assuming the
   * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
   * this is the length in bytes. 0 is returned for data types where the
   * column size is not applicable.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return precision
   */
  def getPrecision(column: Int): Int

  /**
   * Gets the designated column's number of digits to right of the decimal point.
   * 0 is returned for data types where the scale is not applicable.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return scale
   */
  def getScale(column: Int): Int

  /**
   * Gets the designated column's table name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return table name or "" if not applicable
   */
  def getTableName(column: Int): String

  /**
   * Gets the designated column's table's catalog name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return the name of the catalog for the table in which the given column
   *          appears or "" if not applicable
   */
  def getCatalogName(column: Int): String

  /**
   * Retrieves the designated column's SQL type.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return SQL type from java.sql.Types
   */
  def getColumnType(column: Int): Int

  /**
   * Retrieves the designated column's database-specific type name.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return type name used by the database. If the column type is
   * a user-defined type, then a fully-qualified type name is returned.
   */
  def getColumnTypeName(column: Int): String

  /**
   * Indicates whether the designated column is definitely not writable.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isReadOnly(column: Int): Boolean

  /**
   * Indicates whether it is possible for a write on the designated column to succeed.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isWritable(column: Int): Boolean

  /**
   * Indicates whether a write on the designated column will definitely succeed.
   *
   * @param column the first column is 1, the second is 2, ...
   * @return <code>true</code> if so; <code>false</code> otherwise
   */
  def isDefinitelyWritable(column: Int): Boolean

object ResultSetMetaData:

  /**
     * The constant indicating that a
     * column does not allow <code>NULL</code> values.
     */
  val columnNoNulls = 0

  /**
     * The constant indicating that a
     * column allows <code>NULL</code> values.
    */
  val columnNullable = 1

  /**
     * The constant indicating that the
     * nullability of a column's values is unknown.
    */
  val columnNullableUnknown = 2

  def apply(columns: Vector[ColumnDefinitionPacket], version: Version): ResultSetMetaData =
    new ResultSetMetaData:
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
        case ColumnDataType.MYSQL_TYPE_STRING | ColumnDataType.MYSQL_TYPE_VARCHAR |
          ColumnDataType.MYSQL_TYPE_VAR_STRING | ColumnDataType.MYSQL_TYPE_JSON | ColumnDataType.MYSQL_TYPE_ENUM |
          ColumnDataType.MYSQL_TYPE_SET =>
          CharsetMapping.getStaticCollationNameForCollationIndex(getMysqlCharsetForJavaEncoding("UTF-8")).fold(false)(_.endsWith("_ci")) // TODO: Get the actual character code from the server and use it.
        case _ => true

      override def isSearchable(column: Int): Boolean = true

      override def isCurrency(column: Int): Boolean = false

      override def isNullable(column: Int): Int =
        if unsafeFindByIndex(column).flags.contains(ColumnDefinitionFlags.NOT_NULL_FLAG) then columnNoNulls
        else columnNullable

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
          case _ => clampedGetLength(definition) / getMaxBytesPerChar(getMysqlCharsetForJavaEncoding("UTF-8"), "UTF-8") // TODO: Get the actual character code from the server and use it.

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
        CharsetMapping.getStaticCollationIndexForMysqlCharsetName(CharsetMapping.getStaticMysqlCharsetForJavaEncoding(javaEncoding, Some(version)))

      private def getMaxBytesPerChar(charsetIndex: Int, javaCharsetName: String): Int =
        CharsetMapping.getStaticMysqlCharsetNameForCollationIndex(charsetIndex) match
          case Some(charsetName) => CharsetMapping.getStaticMblen(charsetName)
          case None =>
            CharsetMapping.getStaticMysqlCharsetForJavaEncoding(javaCharsetName, Some(version)) match
              case Some(charsetName) => CharsetMapping.getStaticMblen(charsetName)
              case None => 1

      private def unsafeFindByIndex(index: Int): ColumnDefinitionPacket =
        columns.lift(index) match
          case Some(column) => column
          case None         => throw new SQLException("Column index out of range.")
