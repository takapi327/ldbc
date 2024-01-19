/** Copyright (c) 2023-2024 by Takahiko Tominaga This software is licensed under the MIT License (MIT). For more
  * information see LICENSE or https://opensource.org/licenses/MIT
  */

package ldbc.dsl.internal

import java.util.Calendar

import java.io.{ InputStream, Reader }

import java.net.URL

import java.sql.{
  Blob,
  Clob,
  Date,
  NClob,
  Ref,
  ResultSetMetaData,
  RowId,
  SQLType,
  SQLWarning,
  SQLXML,
  Time,
  Timestamp,
  Array as JavaSqlArray
}

import scala.jdk.CollectionConverters.*

import cats.effect.Sync

import ldbc.sql.ResultSet
import ResultSet.*

trait ResultSetSyntax:

  implicit class ResultSetF(resultSetObject: ResultSet.type):

    def apply[F[_]: Sync](resultSet: java.sql.ResultSet): ResultSet[F] = new ResultSet[F]:
      override def next(): F[Boolean] = Sync[F].blocking(resultSet.next())

      override def close(): F[Unit] = Sync[F].blocking(resultSet.close())

      override def wasNull(): F[Boolean] = Sync[F].blocking(resultSet.wasNull())

      override def getString(columnIndex: Int): F[String] = Sync[F].blocking(resultSet.getString(columnIndex))

      override def getString(columnLabel: String): F[String] = Sync[F].blocking(resultSet.getString(columnLabel))

      override def getBoolean(columnIndex: Int): F[Boolean] = Sync[F].blocking(resultSet.getBoolean(columnIndex))

      override def getBoolean(columnLabel: String): F[Boolean] = Sync[F].blocking(resultSet.getBoolean(columnLabel))

      override def getByte(columnIndex: Int): F[Byte] = Sync[F].blocking(resultSet.getByte(columnIndex))

      override def getByte(columnLabel: String): F[Byte] = Sync[F].blocking(resultSet.getByte(columnLabel))

      override def getBytes(columnIndex: Int): F[Array[Byte]] = Sync[F].blocking(resultSet.getBytes(columnIndex))

      override def getBytes(columnLabel: String): F[Array[Byte]] = Sync[F].blocking(resultSet.getBytes(columnLabel))

      override def getShort(columnIndex: Int): F[Short] = Sync[F].blocking(resultSet.getShort(columnIndex))

      override def getShort(columnLabel: String): F[Short] = Sync[F].blocking(resultSet.getShort(columnLabel))

      override def getInt(columnIndex: Int): F[Int] = Sync[F].blocking(resultSet.getInt(columnIndex))

      override def getInt(columnLabel: String): F[Int] = Sync[F].blocking(resultSet.getInt(columnLabel))

      override def getLong(columnIndex: Int): F[Long] = Sync[F].blocking(resultSet.getLong(columnIndex))

      override def getLong(columnLabel: String): F[Long] = Sync[F].blocking(resultSet.getLong(columnLabel))

      override def getFloat(columnIndex: Int): F[Float] = Sync[F].blocking(resultSet.getFloat(columnIndex))

      override def getFloat(columnLabel: String): F[Float] = Sync[F].blocking(resultSet.getFloat(columnLabel))

      override def getDouble(columnIndex: Int): F[Double] = Sync[F].blocking(resultSet.getDouble(columnIndex))

      override def getDouble(columnLabel: String): F[Double] = Sync[F].blocking(resultSet.getDouble(columnLabel))

      override def getDate(columnIndex: Int): F[Date] = Sync[F].blocking(resultSet.getDate(columnIndex))

      override def getDate(columnLabel: String): F[Date] = Sync[F].blocking(resultSet.getDate(columnLabel))

      override def getDate(columnIndex: Int, cal: Calendar): F[Date] =
        Sync[F].blocking(resultSet.getDate(columnIndex, cal))

      override def getDate(columnLabel: String, cal: Calendar): F[Date] =
        Sync[F].blocking(resultSet.getDate(columnLabel, cal))

      override def getTime(columnIndex: Int): F[Time] = Sync[F].blocking(resultSet.getTime(columnIndex))

      override def getTime(columnLabel: String): F[Time] = Sync[F].blocking(resultSet.getTime(columnLabel))

      override def getTime(columnIndex: Int, cal: Calendar): F[Time] =
        Sync[F].blocking(resultSet.getTime(columnIndex, cal))

      override def getTime(columnLabel: String, cal: Calendar): F[Time] =
        Sync[F].blocking(resultSet.getTime(columnLabel, cal))

      override def getTimestamp(columnIndex: Int): F[Timestamp] = Sync[F].blocking(resultSet.getTimestamp(columnIndex))

      override def getTimestamp(columnLabel: String): F[Timestamp] =
        Sync[F].blocking(resultSet.getTimestamp(columnLabel))

      override def getTimestamp(columnIndex: Int, cal: Calendar): F[Timestamp] =
        Sync[F].blocking(resultSet.getTimestamp(columnIndex, cal))

      override def getTimestamp(columnLabel: String, cal: Calendar): F[Timestamp] =
        Sync[F].blocking(resultSet.getTimestamp(columnLabel, cal))

      override def getAsciiStream(columnIndex: Int): F[InputStream] =
        Sync[F].blocking(resultSet.getAsciiStream(columnIndex))

      override def getAsciiStream(columnLabel: String): F[InputStream] =
        Sync[F].blocking(resultSet.getAsciiStream(columnLabel))

      override def getBinaryStream(columnIndex: Int): F[InputStream] =
        Sync[F].blocking(resultSet.getBinaryStream(columnIndex))

      override def getBinaryStream(columnLabel: String): F[InputStream] =
        Sync[F].blocking(resultSet.getBinaryStream(columnLabel))

      override def getWarnings(): F[SQLWarning] = Sync[F].blocking(resultSet.getWarnings)

      override def clearWarnings(): F[Unit] = Sync[F].blocking(resultSet.clearWarnings())

      override def getCursorName(): F[String] = Sync[F].blocking(resultSet.getCursorName)

      override def getMetaData(): F[ResultSetMetaData] = Sync[F].blocking(resultSet.getMetaData)

      override def getObject(columnIndex: Int): F[Object] = Sync[F].blocking(resultSet.getObject(columnIndex))

      override def getObject(columnLabel: String): F[Object] = Sync[F].blocking(resultSet.getObject(columnLabel))

      override def getObject(columnIndex: Int, map: Map[String, Class[_]]): F[Object] =
        Sync[F].blocking(resultSet.getObject(columnIndex, map.asJava))

      override def getObject(columnLabel: String, map: Map[String, Class[_]]): F[Object] =
        Sync[F].blocking(resultSet.getObject(columnLabel, map.asJava))

      override def findColumn(columnLabel: String): F[Int] = Sync[F].blocking(resultSet.findColumn(columnLabel))

      override def getCharacterStream(columnIndex: Int): F[Reader] =
        Sync[F].blocking(resultSet.getCharacterStream(columnIndex))

      override def getCharacterStream(columnLabel: String): F[Reader] =
        Sync[F].blocking(resultSet.getCharacterStream(columnLabel))

      override def getBigDecimal(columnIndex: Int): F[BigDecimal] =
        Sync[F].blocking(resultSet.getBigDecimal(columnIndex))

      override def getBigDecimal(columnLabel: String): F[BigDecimal] =
        Sync[F].blocking(resultSet.getBigDecimal(columnLabel))

      override def isBeforeFirst(): F[Boolean] = Sync[F].blocking(resultSet.isBeforeFirst)

      override def isAfterLast(): F[Boolean] = Sync[F].blocking(resultSet.isAfterLast)

      override def isFirst(): F[Boolean] = Sync[F].blocking(resultSet.first())

      override def isLast(): F[Boolean] = Sync[F].blocking(resultSet.isLast)

      override def beforeFirst(): F[Unit] = Sync[F].blocking(resultSet.beforeFirst())

      override def afterLast(): F[Unit] = Sync[F].blocking(resultSet.afterLast())

      override def first(): F[Boolean] = Sync[F].blocking(resultSet.first())

      override def last(): F[Boolean] = Sync[F].blocking(resultSet.last())

      override def getRow(): F[Int] = Sync[F].blocking(resultSet.getRow)

      override def absolute(row: Int): F[Boolean] = Sync[F].blocking(resultSet.absolute(row))

      override def relative(rows: Int): F[Boolean] = Sync[F].blocking(resultSet.relative(rows))

      override def previous(): F[Boolean] = Sync[F].blocking(resultSet.previous())

      override def setFetchDirection(direction: FetchType): F[Unit] =
        Sync[F].blocking(resultSet.setFetchDirection(direction.code))

      override def getFetchDirection(): F[Int] = Sync[F].blocking(resultSet.getFetchDirection)

      override def setFetchSize(rows: Int): F[Unit] = Sync[F].blocking(resultSet.setFetchSize(rows))

      override def getFetchSize(): F[Int] = Sync[F].blocking(resultSet.getFetchSize)

      override def getType(): F[Option[Type]] = Sync[F].blocking(Type.values.find(_.code == resultSet.getType))

      override def getConcurrency(): F[Int] = Sync[F].blocking(resultSet.getConcurrency)

      override def rowUpdated(): F[Boolean] = Sync[F].blocking(resultSet.rowUpdated())

      override def rowInserted(): F[Boolean] = Sync[F].blocking(resultSet.rowInserted())

      override def rowDeleted(): F[Boolean] = Sync[F].blocking(resultSet.rowDeleted())

      override def updateNull(columnIndex: Int): F[Unit] = Sync[F].blocking(resultSet.updateNull(columnIndex))

      override def updateNull(columnLabel: String): F[Unit] = Sync[F].blocking(resultSet.updateNull(columnLabel))

      override def updateBoolean(columnIndex: Int, x: Boolean): F[Unit] =
        Sync[F].blocking(resultSet.updateBoolean(columnIndex, x))

      override def updateBoolean(columnLabel: String, x: Boolean): F[Unit] =
        Sync[F].blocking(resultSet.updateBoolean(columnLabel, x))

      override def updateByte(columnIndex: Int, x: Byte): F[Unit] =
        Sync[F].blocking(resultSet.updateByte(columnIndex, x))

      override def updateByte(columnLabel: String, x: Byte): F[Unit] =
        Sync[F].blocking(resultSet.updateByte(columnLabel, x))

      override def updateShort(columnIndex: Int, x: Short): F[Unit] =
        Sync[F].blocking(resultSet.updateShort(columnIndex, x))

      override def updateShort(columnLabel: String, x: Short): F[Unit] =
        Sync[F].blocking(resultSet.updateShort(columnLabel, x))

      override def updateInt(columnIndex: Int, x: Int): F[Unit] = Sync[F].blocking(resultSet.updateInt(columnIndex, x))

      override def updateInt(columnLabel: String, x: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateInt(columnLabel, x))

      override def updateLong(columnIndex: Int, x: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateLong(columnIndex, x))

      override def updateLong(columnLabel: String, x: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateLong(columnLabel, x))

      override def updateFloat(columnIndex: Int, x: Float): F[Unit] =
        Sync[F].blocking(resultSet.updateFloat(columnIndex, x))

      override def updateFloat(columnLabel: String, x: Float): F[Unit] =
        Sync[F].blocking(resultSet.updateFloat(columnLabel, x))

      override def updateDouble(columnIndex: Int, x: Double): F[Unit] =
        Sync[F].blocking(resultSet.updateDouble(columnIndex, x))

      override def updateDouble(columnLabel: String, x: Double): F[Unit] =
        Sync[F].blocking(resultSet.updateDouble(columnLabel, x))

      override def updateBigDecimal(columnIndex: Int, x: BigDecimal): F[Unit] =
        Sync[F].blocking(resultSet.updateBigDecimal(columnIndex, x.bigDecimal))

      override def updateBigDecimal(columnLabel: String, x: BigDecimal): F[Unit] =
        Sync[F].blocking(resultSet.updateBigDecimal(columnLabel, x.bigDecimal))

      override def updateString(columnIndex: Int, x: String): F[Unit] =
        Sync[F].blocking(resultSet.updateString(columnIndex, x))

      override def updateString(columnLabel: String, x: String): F[Unit] =
        Sync[F].blocking(resultSet.updateString(columnLabel, x))

      override def updateBytes(columnIndex: Int, x: Array[Byte]): F[Unit] =
        Sync[F].blocking(resultSet.updateBytes(columnIndex, x))

      override def updateBytes(columnLabel: String, x: Array[Byte]): F[Unit] =
        Sync[F].blocking(resultSet.updateBytes(columnLabel, x))

      override def updateDate(columnIndex: Int, x: Date): F[Unit] =
        Sync[F].blocking(resultSet.updateDate(columnIndex, x))

      override def updateDate(columnLabel: String, x: Date): F[Unit] =
        Sync[F].blocking(resultSet.updateDate(columnLabel, x))

      override def updateTime(columnIndex: Int, x: Time): F[Unit] =
        Sync[F].blocking(resultSet.updateTime(columnIndex, x))

      override def updateTime(columnLabel: String, x: Time): F[Unit] =
        Sync[F].blocking(resultSet.updateTime(columnLabel, x))

      override def updateTimestamp(columnIndex: Int, x: Timestamp): F[Unit] =
        Sync[F].blocking(resultSet.updateTimestamp(columnIndex, x))

      override def updateTimestamp(columnLabel: String, x: Timestamp): F[Unit] =
        Sync[F].blocking(resultSet.updateTimestamp(columnLabel, x))

      override def updateAsciiStream(columnIndex: Int, x: InputStream, length: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateAsciiStream(columnIndex, x, length))

      override def updateAsciiStream(columnLabel: String, x: InputStream, length: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateAsciiStream(columnLabel, x, length))

      override def updateAsciiStream(columnLabel: String, x: InputStream): F[Unit] =
        Sync[F].blocking(resultSet.updateAsciiStream(columnLabel, x))

      override def updateBinaryStream(columnIndex: Int, x: InputStream, length: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateBinaryStream(columnIndex, x, length))

      override def updateBinaryStream(columnLabel: String, x: InputStream, length: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateBinaryStream(columnLabel, x, length))

      override def updateBinaryStream(columnLabel: String, x: InputStream): F[Unit] =
        Sync[F].blocking(resultSet.updateBinaryStream(columnLabel, x))

      override def updateCharacterStream(columnIndex: Int, x: Reader, length: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateCharacterStream(columnIndex, x, length))

      override def updateCharacterStream(columnLabel: String, x: Reader, length: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateCharacterStream(columnLabel, x, length))

      override def updateCharacterStream(columnLabel: String, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateCharacterStream(columnLabel, reader))

      override def updateObject(columnIndex: Int, x: Object, scaleOrLength: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnIndex, x, scaleOrLength))

      override def updateObject(columnIndex: Int, x: Object): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnIndex, x))

      override def updateObject(columnLabel: String, x: Object, scaleOrLength: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnLabel, x, scaleOrLength))

      override def updateObject(columnLabel: String, x: Object): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnLabel, x))

      override def insertRow(): F[Unit] = Sync[F].blocking(resultSet.insertRow())

      override def updateRow(): F[Unit] = Sync[F].blocking(resultSet.updateRow())

      override def deleteRow(): F[Unit] = Sync[F].blocking(resultSet.deleteRow())

      override def refreshRow(): F[Unit] = Sync[F].blocking(resultSet.refreshRow())

      override def cancelRowUpdates(): F[Unit] = Sync[F].blocking(resultSet.cancelRowUpdates())

      override def moveToInsertRow(): F[Unit] = Sync[F].blocking(resultSet.moveToInsertRow())

      override def moveToCurrentRow(): F[Unit] = Sync[F].blocking(resultSet.moveToCurrentRow())

      override def getStatement(): F[Unit] = Sync[F].blocking(resultSet.getStatement)

      override def getRef(columnIndex: Int): F[Ref] = Sync[F].blocking(resultSet.getRef(columnIndex))

      override def getRef(columnLabel: String): F[Ref] = Sync[F].blocking(resultSet.getRef(columnLabel))

      override def getBlob(columnIndex: Int): F[Blob] = Sync[F].blocking(resultSet.getBlob(columnIndex))

      override def getBlob(columnLabel: String): F[Blob] = Sync[F].blocking(resultSet.getBlob(columnLabel))

      override def getClob(columnIndex: Int): F[Clob] = Sync[F].blocking(resultSet.getNClob(columnIndex))

      override def getClob(columnLabel: String): F[Clob] = Sync[F].blocking(resultSet.getNClob(columnLabel))

      override def getArray(columnIndex: Int): F[JavaSqlArray] = Sync[F].blocking(resultSet.getArray(columnIndex))

      override def getArray(columnLabel: String): F[JavaSqlArray] = Sync[F].blocking(resultSet.getArray(columnLabel))

      override def getURL(columnIndex: Int): F[URL] = Sync[F].blocking(resultSet.getURL(columnIndex))

      override def getURL(columnLabel: String): F[URL] = Sync[F].blocking(resultSet.getURL(columnLabel))

      override def updateRef(columnIndex: Int, ref: Ref): F[Unit] =
        Sync[F].blocking(resultSet.updateRef(columnIndex, ref))

      override def updateRef(columnLabel: String, ref: Ref): F[Unit] =
        Sync[F].blocking(resultSet.updateRef(columnLabel, ref))

      override def updateBlob(columnIndex: Int, ref: Blob): F[Unit] =
        Sync[F].blocking(resultSet.updateBlob(columnIndex, ref))

      override def updateBlob(columnLabel: String, ref: Blob): F[Unit] =
        Sync[F].blocking(resultSet.updateBlob(columnLabel, ref))

      override def updateClob(columnIndex: Int, ref: Clob): F[Unit] =
        Sync[F].blocking(resultSet.updateClob(columnIndex, ref))

      override def updateClob(columnLabel: String, ref: Clob): F[Unit] =
        Sync[F].blocking(resultSet.updateClob(columnLabel, ref))

      override def updateArray(columnIndex: Int, array: JavaSqlArray): F[Unit] =
        Sync[F].blocking(resultSet.updateArray(columnIndex, array))

      override def updateArray(columnLabel: String, array: JavaSqlArray): F[Unit] =
        Sync[F].blocking(resultSet.updateArray(columnLabel, array))

      override def getRowId(columnIndex: Int): F[RowId] = Sync[F].blocking(resultSet.getRowId(columnIndex))

      override def getRowId(columnLabel: String): F[RowId] = Sync[F].blocking(resultSet.getRowId(columnLabel))

      override def updateRowId(columnIndex: Int, rowId: RowId): F[Unit] =
        Sync[F].blocking(resultSet.updateRowId(columnIndex, rowId))

      override def updateRowId(columnLabel: String, rowId: RowId): F[Unit] =
        Sync[F].blocking(resultSet.updateRowId(columnLabel, rowId))

      override def getHoldability(): F[Int] = Sync[F].blocking(resultSet.getHoldability)

      override def isClosed(): F[Boolean] = Sync[F].blocking(resultSet.isClosed)

      override def updateNString(columnIndex: Int, nString: String): F[Unit] =
        Sync[F].blocking(resultSet.updateNString(columnIndex, nString))

      override def updateNString(columnLabel: String, nString: String): F[Unit] =
        Sync[F].blocking(resultSet.updateNString(columnLabel, nString))

      override def updateNClob(columnIndex: Int, nClob: NClob): F[Unit] =
        Sync[F].blocking(resultSet.updateNClob(columnIndex, nClob))

      override def updateNClob(columnLabel: String, nClob: NClob): F[Unit] =
        Sync[F].blocking(resultSet.updateNClob(columnLabel, nClob))

      override def getNClob(columnIndex: Int): F[NClob] = Sync[F].blocking(resultSet.getNClob(columnIndex))

      override def getNClob(columnLabel: String): F[NClob] = Sync[F].blocking(resultSet.getNClob(columnLabel))

      override def getSQLXML(columnIndex: Int): F[SQLXML] = Sync[F].blocking(resultSet.getSQLXML(columnIndex))

      override def getSQLXML(columnLabel: String): F[SQLXML] = Sync[F].blocking(resultSet.getSQLXML(columnLabel))

      override def updateSQLXML(columnIndex: Int, xmlObject: SQLXML): F[Unit] =
        Sync[F].blocking(resultSet.updateSQLXML(columnIndex, xmlObject))

      override def updateSQLXML(columnLabel: String, xmlObject: SQLXML): F[Unit] =
        Sync[F].blocking(resultSet.updateSQLXML(columnLabel, xmlObject))

      override def getNString(columnIndex: Int): F[String] = Sync[F].blocking(resultSet.getNString(columnIndex))

      override def getNString(columnLabel: String): F[String] = Sync[F].blocking(resultSet.getNString(columnLabel))

      override def getNCharacterStream(columnIndex: Int): F[Reader] =
        Sync[F].blocking(resultSet.getNCharacterStream(columnIndex))

      override def getNCharacterStream(columnLabel: String): F[Reader] =
        Sync[F].blocking(resultSet.getNCharacterStream(columnLabel))

      override def updateNCharacterStream(columnIndex: Int, reader: Reader, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateNCharacterStream(columnIndex, reader, length))

      override def updateNCharacterStream(columnLabel: String, reader: Reader, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateNCharacterStream(columnLabel, reader, length))

      override def updateAsciiStream(columnIndex: Int, inputStream: InputStream, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateAsciiStream(columnIndex, inputStream, length))

      override def updateAsciiStream(columnLabel: String, inputStream: InputStream, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateAsciiStream(columnLabel, inputStream, length))

      override def updateBinaryStream(columnIndex: Int, inputStream: InputStream, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateBinaryStream(columnIndex, inputStream, length))

      override def updateBinaryStream(columnLabel: String, inputStream: InputStream, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateBinaryStream(columnLabel, inputStream, length))

      override def updateBlob(columnIndex: Int, inputStream: InputStream, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateBlob(columnIndex, inputStream, length))

      override def updateBlob(columnLabel: String, inputStream: InputStream, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateBlob(columnLabel, inputStream, length))

      override def updateBlob(columnIndex: Int, inputStream: InputStream): F[Unit] =
        Sync[F].blocking(resultSet.updateBlob(columnIndex, inputStream))

      override def updateBlob(columnLabel: String, inputStream: InputStream): F[Unit] =
        Sync[F].blocking(resultSet.updateBlob(columnLabel, inputStream))

      override def updateClob(columnIndex: Int, reader: Reader, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateClob(columnIndex, reader, length))

      override def updateClob(columnLabel: String, reader: Reader, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateClob(columnLabel, reader, length))

      override def updateClob(columnIndex: Int, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateClob(columnIndex, reader))

      override def updateClob(columnLabel: String, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateClob(columnLabel, reader))

      override def updateNClob(columnIndex: Int, reader: Reader, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateNClob(columnIndex, reader, length))

      override def updateNClob(columnLabel: String, reader: Reader, length: Long): F[Unit] =
        Sync[F].blocking(resultSet.updateNClob(columnLabel, reader, length))

      override def updateNClob(columnIndex: Int, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateNClob(columnIndex, reader))

      override def updateNClob(columnLabel: String, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateNClob(columnLabel, reader))

      override def updateNCharacterStream(columnIndex: Int, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateNCharacterStream(columnIndex, reader))

      override def updateNCharacterStream(columnLabel: String, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateNCharacterStream(columnLabel, reader))

      override def updateAsciiStream(columnIndex: Int, inputStream: InputStream): F[Unit] =
        Sync[F].blocking(resultSet.updateAsciiStream(columnIndex, inputStream))

      override def updateBinaryStream(columnIndex: Int, inputStream: InputStream): F[Unit] =
        Sync[F].blocking(resultSet.updateBinaryStream(columnIndex, inputStream))

      override def updateCharacterStream(columnIndex: Int, reader: Reader): F[Unit] =
        Sync[F].blocking(resultSet.updateCharacterStream(columnIndex, reader))

      override def getObject[T](columnIndex: Int, clazz: T): F[T] =
        Sync[F].blocking(resultSet.getObject(columnIndex, clazz.getClass))

      override def getObject[T](columnLabel: String, clazz: T): F[T] =
        Sync[F].blocking(resultSet.getObject(columnLabel, clazz.getClass))

      override def updateObject(columnIndex: Int, x: Object, targetSqlType: SQLType, scaleOrLength: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnIndex, x, targetSqlType, scaleOrLength))

      override def updateObject(columnLabel: String, x: Object, targetSqlType: SQLType, scaleOrLength: Int): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnLabel, x, targetSqlType, scaleOrLength))

      override def updateObject(columnIndex: Int, x: Object, targetSqlType: SQLType): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnIndex, x, targetSqlType))

      override def updateObject(columnLabel: String, x: Object, targetSqlType: SQLType): F[Unit] =
        Sync[F].blocking(resultSet.updateObject(columnLabel, x, targetSqlType))
