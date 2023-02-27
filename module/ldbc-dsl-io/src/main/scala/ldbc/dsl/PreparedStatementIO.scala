/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.dsl

import cats.implicits.*

import cats.effect.Sync

import ldbc.sql.{ PreparedStatement, ResultSet, ParameterMetaData, ResultSetMetaData }

case class PreparedStatementIO[F[_]: Sync](statement: java.sql.PreparedStatement) extends PreparedStatement[F]:

  override def executeQuery(): F[ResultSet[F]] = Sync[F].blocking(statement.executeQuery()).map(ResultSetIO[F])

  override def executeUpdate(): F[Int] = Sync[F].blocking(statement.executeUpdate())

  override def close(): F[Unit] =
    if statement != null
    then Sync[F].blocking(statement.close())
    else Sync[F].unit

  override def setNull(parameterIndex: Int, sqlType: Int): F[Unit] =
    Sync[F].blocking(statement.setNull(parameterIndex, sqlType))

  override def setNull(parameterIndex: Int, sqlType: Int, typeName: String): F[Unit] =
    Sync[F].blocking(statement.setNull(parameterIndex, sqlType, typeName))

  override def setBoolean(parameterIndex: Int, x: Boolean): F[Unit] =
    Sync[F].blocking(statement.setBoolean(parameterIndex, x))

  override def setByte(parameterIndex: Int, x: Byte): F[Unit] = Sync[F].blocking(statement.setByte(parameterIndex, x))

  override def setShort(parameterIndex: Int, x: Short): F[Unit] =
    Sync[F].blocking(statement.setShort(parameterIndex, x))

  override def setInt(parameterIndex: Int, x: Int): F[Unit] = Sync[F].blocking(statement.setInt(parameterIndex, x))

  override def setLong(parameterIndex: Int, x: Long): F[Unit] = Sync[F].blocking(statement.setLong(parameterIndex, x))

  override def setFloat(parameterIndex: Int, x: Float): F[Unit] =
    Sync[F].blocking(statement.setFloat(parameterIndex, x))

  override def setDouble(parameterIndex: Int, x: Double): F[Unit] =
    Sync[F].blocking(statement.setDouble(parameterIndex, x))

  override def setBigDecimal(parameterIndex: Int, x: BigDecimal): F[Unit] =
    Sync[F].blocking(statement.setBigDecimal(parameterIndex, x.bigDecimal))

  override def setString(parameterIndex: Int, x: String): F[Unit] =
    Sync[F].blocking(statement.setString(parameterIndex, x))

  override def setBytes(parameterIndex: Int, x: Array[Byte]): F[Unit] =
    Sync[F].blocking(statement.setBytes(parameterIndex, x))

  override def setDate(parameterIndex: Int, x: java.sql.Date): F[Unit] =
    Sync[F].blocking(statement.setDate(parameterIndex, x))

  override def setDate(parameterIndex: Int, x: java.sql.Date, cal: java.util.Calendar): F[Unit] =
    Sync[F].blocking(statement.setDate(parameterIndex, x, cal))

  override def setTime(parameterIndex: Int, x: java.sql.Time): F[Unit] =
    Sync[F].blocking(statement.setTime(parameterIndex, x))

  override def setTime(parameterIndex: Int, x: java.sql.Time, cal: java.util.Calendar): F[Unit] =
    Sync[F].blocking(statement.setTime(parameterIndex, x, cal))

  override def setTimestamp(parameterIndex: Int, x: java.sql.Timestamp): F[Unit] =
    Sync[F].blocking(statement.setTimestamp(parameterIndex, x))

  override def setTimestamp(parameterIndex: Int, x: java.sql.Timestamp, cal: java.util.Calendar): F[Unit] =
    Sync[F].blocking(statement.setTimestamp(parameterIndex, x, cal))

  override def setAsciiStream(parameterIndex: Int, x: java.io.InputStream, length: Int): F[Unit] =
    Sync[F].blocking(statement.setAsciiStream(parameterIndex, x, length))

  override def setAsciiStream(parameterIndex: Int, x: java.io.InputStream): F[Unit] =
    Sync[F].blocking(statement.setAsciiStream(parameterIndex, x))

  override def setBinaryStream(parameterIndex: Int, x: java.io.InputStream, length: Int): F[Unit] =
    Sync[F].blocking(statement.setBinaryStream(parameterIndex, x, length))

  override def setBinaryStream(parameterIndex: Int, x: java.io.InputStream): F[Unit] =
    Sync[F].blocking(statement.setBinaryStream(parameterIndex, x))

  override def clearParameters(): F[Unit] = Sync[F].blocking(statement.clearParameters())

  override def setObject(parameterIndex: Int, x: Object, targetSqlType: Int): F[Unit] =
    Sync[F].blocking(statement.setObject(parameterIndex, x, targetSqlType))

  override def setObject(parameterIndex: Int, x: Object): F[Unit] =
    Sync[F].blocking(statement.setObject(parameterIndex, x))

  override def execute(): F[Boolean] = Sync[F].blocking(statement.execute())

  override def addBatch(): F[Unit] = Sync[F].blocking(statement.addBatch())

  override def setCharacterStream(parameterIndex: Int, reader: java.io.Reader, length: Int): F[Unit] =
    Sync[F].blocking(statement.setCharacterStream(parameterIndex, reader, length))

  override def setCharacterStream(parameterIndex: Int, reader: java.io.Reader): F[Unit] =
    Sync[F].blocking(statement.setCharacterStream(parameterIndex, reader))

  override def setRef(parameterIndex: Int, x: java.sql.Ref): F[Unit] =
    Sync[F].blocking(statement.setRef(parameterIndex, x))

  override def setBlob(parameterIndex: Int, x: java.sql.Blob): F[Unit] =
    Sync[F].blocking(statement.setBlob(parameterIndex, x))

  override def setBlob(parameterIndex: Int, inputStream: java.io.InputStream, length: Int): F[Unit] =
    Sync[F].blocking(statement.setBlob(parameterIndex, inputStream, length))

  override def setBlob(parameterIndex: Int, inputStream: java.io.InputStream): F[Unit] =
    Sync[F].blocking(statement.setBlob(parameterIndex, inputStream))

  override def setClob(parameterIndex: Int, x: java.sql.Clob): F[Unit] =
    Sync[F].blocking(statement.setClob(parameterIndex, x))

  override def setClob(parameterIndex: Int, reader: java.io.Reader, length: Int): F[Unit] =
    Sync[F].blocking(statement.setClob(parameterIndex, reader, length))

  override def setClob(parameterIndex: Int, reader: java.io.Reader): F[Unit] =
    Sync[F].blocking(statement.setClob(parameterIndex, reader))

  override def setArray(parameterIndex: Int, x: java.sql.Array): F[Unit] =
    Sync[F].blocking(statement.setArray(parameterIndex, x))

  override def getMetaData(): F[ResultSetMetaData[F]] =
    Sync[F].blocking(statement.getMetaData).map(ResultSetMetaDataIO(_))

  override def setURL(parameterIndex: Int, x: java.net.URL): F[Unit] =
    Sync[F].blocking(statement.setURL(parameterIndex, x))

  override def getParameterMetaData(): F[ParameterMetaData[F]] =
    Sync[F].blocking(statement.getParameterMetaData).map(ParameterMetaDataIO(_))

  override def setRowId(parameterIndex: Int, x: java.sql.RowId): F[Unit] =
    Sync[F].blocking(statement.setRowId(parameterIndex, x))

  override def setNString(parameterIndex: Int, x: String): F[Unit] =
    Sync[F].blocking(statement.setNString(parameterIndex, x))

  override def setNCharacterStream(parameterIndex: Int, value: java.io.Reader, length: Int): F[Unit] =
    Sync[F].blocking(statement.setNCharacterStream(parameterIndex, value, length))

  override def setNCharacterStream(parameterIndex: Int, value: java.io.Reader): F[Unit] =
    Sync[F].blocking(statement.setNCharacterStream(parameterIndex, value))

  override def setNClob(parameterIndex: Int, value: java.sql.NClob): F[Unit] =
    Sync[F].blocking(statement.setNClob(parameterIndex, value))

  override def setNClob(parameterIndex: Int, reader: java.io.Reader, length: Int): F[Unit] =
    Sync[F].blocking(statement.setNClob(parameterIndex, reader, length))

  override def setNClob(parameterIndex: Int, reader: java.io.Reader): F[Unit] =
    Sync[F].blocking(statement.setNClob(parameterIndex, reader))

  override def setSQLXML(parameterIndex: Int, xmlObject: java.sql.SQLXML): F[Unit] =
    Sync[F].blocking(statement.setSQLXML(parameterIndex, xmlObject))

  override def setObject(
    parameterIndex: Int,
    x:              Object,
    targetSqlType:  java.sql.SQLType,
    scaleOrLength:  Int
  ): F[Unit] =
    Sync[F].blocking(statement.setObject(parameterIndex, x, targetSqlType, scaleOrLength))

  override def setObject(parameterIndex: Int, x: Object, targetSqlType: Int, scaleOrLength: Int): F[Unit] =
    Sync[F].blocking(statement.setObject(parameterIndex, x, targetSqlType, scaleOrLength))

  override def executeLargeUpdate(): F[Long] = Sync[F].blocking(statement.executeLargeUpdate())

  override def getGeneratedKeys(): F[ResultSet[F]] = Sync[F].blocking(statement.getGeneratedKeys).map(ResultSetIO[F])
