/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import cats.effect.Sync

/** An object that can be used to get information about the types and properties of the columns in a ResultSet object.
  * The following code fragment creates the ResultSet object rs, creates the ResultSetMetaData object rsmd, and uses
  * rsmd to find out how many columns rs has and whether the first column in rs can be used in a WHERE clause.
  *
  * @tparam F
  *   The effect type
  */
trait ResultSetMetaData[F[_]]:

  /** Returns the number of columns in this ResultSet object.
    *
    * @return
    *   the number of columns
    */
  def getColumnCount(): F[Int]

  /** Indicates whether the designated column is automatically numbered.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isAutoIncrement(column: Int): F[Boolean]

  /** Indicates whether a column's case matters.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isCaseSensitive(column: Int): F[Boolean]

  /** Indicates whether the designated column can be used in a where clause.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isSearchable(column: Int): F[Boolean]

  /** Indicates whether the designated column is a cash value.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isCurrency(column: Int): F[Boolean]

  /** Indicates the nullability of values in the designated column.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   the nullability status of the given column; one of columnNoNulls, columnNullable or columnNullableUnknown
    */
  def isNullable(column: Int): F[Option[ResultSetMetaData.ColumnNull]]

  /** Indicates whether values in the designated column are signed numbers.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isSigned(column: Int): F[Boolean]

  /** Indicates the designated column's normal maximum width in characters.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   the normal maximum number of characters allowed as the width of the designated column
    */
  def getColumnDisplaySize(column: Int): F[Int]

  /** Gets the designated column's suggested title for use in printouts and displays. The suggested title is usually
    * specified by the SQL AS clause. If a SQL AS is not specified, the value returned from getColumnLabel will be the
    * same as the value returned by the getColumnName method.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   the suggested column title
    */
  def getColumnLabel(column: Int): F[String]

  /** Get the designated column's name.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   column name
    */
  def getColumnName(column: Int): F[String]

  /** Get the designated column's table's schema.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   schema name or "" if not applicable
    */
  def getSchemaName(column: Int): F[String]

  /** Get the designated column's specified column size. For numeric data, this is the maximum precision. For character
    * data, this is the length in characters. For datetime datatypes, this is the length in characters of the String
    * representation (assuming the maximum allowed precision of the fractional seconds component). For binary data, this
    * is the length in bytes. For the ROWID datatype, this is the length in bytes. 0 is returned for data types where
    * the column size is not applicable.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   precision
    */
  def getPrecision(column: Int): F[Int]

  /** Gets the designated column's number of digits to right of the decimal point. 0 is returned for data types where
    * the scale is not applicable.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   scale
    */
  def getScale(column: Int): F[Int]

  /** Gets the designated column's table name.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    */
  def getTableName(column: Int): F[String]

  /** Gets the designated column's table's catalog name.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    */
  def getCatalogName(column: Int): F[String]

  /** Retrieves the designated column's SQL type.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   SQL type from [[ldbc.sql.JdbcType]]
    */
  def getColumnType(column: Int): F[JdbcType]

  /** Retrieves the designated column's database-specific type name.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *
    * type name used by the database. If the column type is a user-defined type, then a fully-qualified type name is
    * returned.
    */
  def getColumnTypeName(column: Int): F[String]

  /** Indicates whether the designated column is definitely not writable.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isReadOnly(column: Int): F[Boolean]

  /** Indicates whether it is possible for a write on the designated column to succeed.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isWritable(column: Int): F[Boolean]

  /** Indicates whether a write on the designated column will definitely succeed.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   true if so; false otherwise
    */
  def isDefinitelyWritable(column: Int): F[Boolean]

  /** Returns the fully-qualified name of the Java class whose instances are manufactured if the method
    * ResultSet.getObject is called to retrieve a value from the column. ResultSet.getObject may return a subclass of
    * the class returned by this method.
    *
    * @param column
    *   the first column is 1, the second is 2, ...
    * @return
    *   the fully-qualified name of the class in the Java programming language that would be used by the method
    *   ResultSet.getObject to retrieve the value in the specified column. This is the class name used for custom
    *   mapping.
    */
  def getColumnClassName(column: Int): F[String]

object ResultSetMetaData:

  enum ColumnNull(val code: Int):
    case COLUMN_NO_NULLS         extends ColumnNull(java.sql.ResultSetMetaData.columnNoNulls)
    case COLUMN_NULLABLE         extends ColumnNull(java.sql.ResultSetMetaData.columnNullable)
    case COLUMN_NULLABLE_UNKNOWN extends ColumnNull(java.sql.ResultSetMetaData.columnNullableUnknown)

  def apply[F[_]: Sync](resultSetMetaData: java.sql.ResultSetMetaData): ResultSetMetaData[F] = new ResultSetMetaData[F]:

    override def getColumnCount(): F[Int] = Sync[F].blocking(resultSetMetaData.getColumnCount)

    override def isAutoIncrement(column: Int): F[Boolean] = Sync[F].blocking(resultSetMetaData.isAutoIncrement(column))
    override def isCaseSensitive(column: Int): F[Boolean] = Sync[F].blocking(resultSetMetaData.isCaseSensitive(column))
    override def isSearchable(column: Int):    F[Boolean] = Sync[F].blocking(resultSetMetaData.isSearchable(column))
    override def isCurrency(column: Int):      F[Boolean] = Sync[F].blocking(resultSetMetaData.isCurrency(column))
    override def isNullable(column: Int): F[Option[ColumnNull]] =
      Sync[F].blocking(ColumnNull.values.find(_.code == resultSetMetaData.isNullable(column)))
    override def isSigned(column: Int): F[Boolean] = Sync[F].blocking(resultSetMetaData.isSigned(column))

    override def getColumnDisplaySize(column: Int): F[Int] =
      Sync[F].blocking(resultSetMetaData.getColumnDisplaySize(column))
    override def getColumnLabel(column: Int): F[String] = Sync[F].blocking(resultSetMetaData.getColumnLabel(column))
    override def getColumnName(column: Int):  F[String] = Sync[F].blocking(resultSetMetaData.getColumnName(column))
    override def getSchemaName(column: Int):  F[String] = Sync[F].blocking(resultSetMetaData.getSchemaName(column))
    override def getPrecision(column: Int):   F[Int]    = Sync[F].blocking(resultSetMetaData.getPrecision(column))
    override def getScale(column: Int):       F[Int]    = Sync[F].blocking(resultSetMetaData.getScale(column))
    override def getTableName(column: Int):   F[String] = Sync[F].blocking(resultSetMetaData.getTableName(column))
    override def getCatalogName(column: Int): F[String] = Sync[F].blocking(resultSetMetaData.getCatalogName(column))
    override def getColumnType(column: Int): F[JdbcType] =
      Sync[F].blocking(JdbcType.fromCode(resultSetMetaData.getColumnType(column)))
    override def getColumnTypeName(column: Int): F[String] =
      Sync[F].blocking(resultSetMetaData.getColumnTypeName(column))
    override def isReadOnly(column: Int): F[Boolean] = Sync[F].blocking(resultSetMetaData.isReadOnly(column))
    override def isWritable(column: Int): F[Boolean] = Sync[F].blocking(resultSetMetaData.isWritable(column))
    override def isDefinitelyWritable(column: Int): F[Boolean] =
      Sync[F].blocking(resultSetMetaData.isDefinitelyWritable(column))
    override def getColumnClassName(column: Int): F[String] =
      Sync[F].blocking(resultSetMetaData.getColumnClassName(column))
