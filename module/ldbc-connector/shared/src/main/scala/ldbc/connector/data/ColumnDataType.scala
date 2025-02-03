/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

/**
 * Enumeration of MySQL column data types.
 *
 * @see https://dev.mysql.com/doc/dev/mysql-server/latest/field__types_8h.html
 *
 * @param code
 *   the MySQL column data type code
 * @param name
 *   the MySQL column data type name
 */
enum ColumnDataType(val code: Long, val name: String):
  case MYSQL_TYPE_DECIMAL     extends ColumnDataType(0x00, "DECIMAL")
  case MYSQL_TYPE_TINY        extends ColumnDataType(0x01, "TINYINT")
  case MYSQL_TYPE_SHORT       extends ColumnDataType(0x02, "SMALLINT")
  case MYSQL_TYPE_LONG        extends ColumnDataType(0x03, "INT")
  case MYSQL_TYPE_FLOAT       extends ColumnDataType(0x04, "FLOAT")
  case MYSQL_TYPE_DOUBLE      extends ColumnDataType(0x05, "DOUBLE")
  case MYSQL_TYPE_NULL        extends ColumnDataType(0x06, "NULL")
  case MYSQL_TYPE_TIMESTAMP   extends ColumnDataType(0x07, "TIMESTAMP")
  case MYSQL_TYPE_LONGLONG    extends ColumnDataType(0x08, "BIGINT")
  case MYSQL_TYPE_INT24       extends ColumnDataType(0x09, "MEDIUMINT")
  case MYSQL_TYPE_DATE        extends ColumnDataType(0x0a, "DATE")
  case MYSQL_TYPE_TIME        extends ColumnDataType(0x0b, "TIME")
  case MYSQL_TYPE_DATETIME    extends ColumnDataType(0x0c, "DATETIME")
  case MYSQL_TYPE_YEAR        extends ColumnDataType(0x0d, "YEAR")
  case MYSQL_TYPE_NEWDATE     extends ColumnDataType(0x0e, "DATE")     // Internal to MySQL. Not used in protocol
  case MYSQL_TYPE_VARCHAR     extends ColumnDataType(0x0f, "VARCHAR")
  case MYSQL_TYPE_BIT         extends ColumnDataType(0x10, "BIT")
  case MYSQL_TYPE_TIMESTAMP2  extends ColumnDataType(0x11, "TIMESTAMP")
  case MYSQL_TYPE_DATETIME2   extends ColumnDataType(0x12, "DATETIME") // Internal to MySQL. Not used in protocol
  case MYSQL_TYPE_TIME2       extends ColumnDataType(0x13, "TIME")     // Internal to MySQL. Not used in protocol
  case MYSQL_TYPE_JSON        extends ColumnDataType(0xf5, "JSON")
  case MYSQL_TYPE_NEWDECIMAL  extends ColumnDataType(0xf6, "DECIMAL")
  case MYSQL_TYPE_ENUM        extends ColumnDataType(0xf7, "ENUM")
  case MYSQL_TYPE_SET         extends ColumnDataType(0xf8, "SET")
  case MYSQL_TYPE_TINY_BLOB   extends ColumnDataType(0xf9, "TINYBLOB")
  case MYSQL_TYPE_MEDIUM_BLOB extends ColumnDataType(0xfa, "MEDIUMBLOB")
  case MYSQL_TYPE_LONG_BLOB   extends ColumnDataType(0xfb, "LONGBLOB")
  case MYSQL_TYPE_BLOB        extends ColumnDataType(0xfc, "BLOB")
  case MYSQL_TYPE_VAR_STRING  extends ColumnDataType(0xfd, "VARCHAR")
  case MYSQL_TYPE_STRING      extends ColumnDataType(0xfe, "CHAR")
  case MYSQL_TYPE_GEOMETRY    extends ColumnDataType(0xff, "GEOMETRY")

  override def toString: String = name

object ColumnDataType:

  private val codeToTypeMap: Map[Long, ColumnDataType] =
    ColumnDataType.values.map(t => t.code -> t).toMap

  def apply(code: Long): ColumnDataType =
    codeToTypeMap.getOrElse(code, throw new IllegalArgumentException(s"Unknown column data type code: $code"))
