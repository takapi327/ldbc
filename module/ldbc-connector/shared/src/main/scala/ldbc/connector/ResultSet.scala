/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import java.time.*

import ldbc.connector.codec.Codec
import ldbc.connector.util.Version
import ldbc.connector.net.packet.response.*
import ldbc.connector.codec.all.*

/**
 * A table of data representing a database result set, which is usually generated by executing a statement that queries the database.
 */
trait ResultSet:

  /**
   * Moves the cursor forward one row from its current position.
   * A <code>ResultSet</code> cursor is initially positioned
   * before the first row; the first call to the method
   * <code>next</code> makes the first row the current row; the
   * second call makes the second row the current row, and so on.
   * <p>
   * When a call to the <code>next</code> method returns <code>false</code>,
   * the cursor is positioned after the last row. Any
   * invocation of a <code>ResultSet</code> method which requires a
   * current row will result in a <code>SQLException</code> being thrown.
   *  If the result set type is <code>TYPE_FORWARD_ONLY</code>, it is vendor specified
   * whether their JDBC driver implementation will return <code>false</code> or
   *  throw an <code>SQLException</code> on a
   * subsequent call to <code>next</code>.
   *
   * <P>If an input stream is open for the current row, a call
   * to the method <code>next</code> will
   * implicitly close it. A <code>ResultSet</code> object's
   * warning chain is cleared when a new row is read.
   *
   * @return <code>true</code> if the new current row is valid;
   * <code>false</code> if there are no more rows
   */
  def next(): Boolean

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getString(columnIndex: Int): Option[String]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Boolean</code> in the Scala programming language.
   *
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 1, a value of <code>true</code> is returned.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>false</code>
   */
  def getBoolean(columnIndex: Int): Boolean

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Byte</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getByte(columnIndex: Int): Byte

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Short</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getShort(columnIndex: Int): Short

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>Int</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getInt(columnIndex: Int): Int

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Long</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getLong(columnIndex: Int): Long

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Float</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getFloat(columnIndex: Int): Float

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Double</code> in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getDouble(columnIndex: Int): Double

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Byte</code> array in the Scala programming language.
   * The bytes represent the raw values returned by the driver.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getBytes(columnIndex: Int): Option[Array[Byte]]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.time.LocalDate</code> object in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getDate(columnIndex: Int): Option[LocalDate]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.time.LocalTime</code> object in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getTime(columnIndex: Int): Option[LocalTime]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.time.LocalDateTime</code> object in the Scala programming language.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getTimestamp(columnIndex: Int): Option[LocalDateTime]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>String</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getString(columnLabel: String): Option[String]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Boolean</code> in the Scala programming language.
   *
   * <P>If the designated column has a datatype of CHAR or VARCHAR
   * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 0, a value of <code>false</code> is returned.  If the designated column has a datatype
   * of CHAR or VARCHAR
   * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
   * and contains  a 1, a value of <code>true</code> is returned.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>false</code>
   */
  def getBoolean(columnLabel: String): Boolean

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Byte</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getByte(columnLabel: String): Byte

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Short</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getShort(columnLabel: String): Short

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * an <code>Int</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getInt(columnLabel: String): Int

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Long</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getLong(columnLabel: String): Long

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Float</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getFloat(columnLabel: String): Float

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>Double</code> in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>0</code>
   */
  def getDouble(columnLabel: String): Double

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>byte</code> array in the Scala programming language.
   * The bytes represent the raw values returned by the driver.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getBytes(columnLabel: String): Option[Array[Byte]]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.time.LocalDate</code> object in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getDate(columnLabel: String): Option[LocalDate]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.time.LocalTime</code> object in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value;
   * if the value is SQL <code>NULL</code>,
   * the value returned is <code>None</code>
   */
  def getTime(columnLabel: String): Option[LocalTime]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as
   * a <code>java.time.LocalDateTime</code> object in the Scala programming language.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value; if the value is SQL <code>NULL</code>, the
   * value returned is <code>None</code>
   */
  def getTimestamp(columnLabel: String): Option[LocalDateTime]

  /**
   * Retrieves the  number, types and properties of
   * this <code>ResultSet</code> object's columns.
   *
   * @return the description of this <code>ResultSet</code> object's columns
   */
  def getMetaData(): ResultSetMetaData

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>scala.math.BigDecimal</code> with full precision.
   *
   * @param columnIndex the first column is 1, the second is 2, ...
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>None</code> in the Scala programming language.
   */
  def getBigDecimal(columnIndex: Int): Option[BigDecimal]

  /**
   * Retrieves the value of the designated column in the current row
   * of this <code>ResultSet</code> object as a
   * <code>scala.math.BigDecimal</code> with full precision.
   *
   * @param columnLabel the label for the column specified with the SQL AS clause.  If the SQL AS clause was not specified, then the label is the name of the column
   * @return the column value (full precision);
   * if the value is SQL <code>NULL</code>, the value returned is
   * <code>None</code> in the Scala programming language.
   */
  def getBigDecimal(columnLabel: String): Option[BigDecimal]

  /**
   * Function to decode all lines with the specified type.
   *
   * @param codec
   *   The codec to decode the value
   * @tparam T
   *   The type of the value
   * @return
   *   A list of values decoded with the specified type.
   */
  def decode[T](codec: Codec[T]): List[T]

object ResultSet:

  case class Impl(
    columns: Vector[ColumnDefinitionPacket],
    rows:    Vector[ResultSetRowPacket],
    version: Version
  ) extends ResultSet:

    private var currentCursor: Int = 0
    private var currentRow: Option[ResultSetRowPacket] = rows.headOption

    def next(): Boolean =
      if currentCursor >= rows.size then
        currentRow = rows.lift(currentCursor)
        currentCursor += 1
        currentRow.isDefined
      else false

    override def getString(columnIndex: Int): Option[String] =
      currentRow.flatMap(row => text.decode(columnIndex, row.values).toOption)

    override def getBoolean(columnIndex: Int): Boolean =
      currentRow.flatMap(row => boolean.decode(columnIndex, row.values).toOption).getOrElse(false)

    override def getByte(columnIndex: Int): Byte =
      currentRow.flatMap(row => bit.decode(columnIndex, row.values).toOption).getOrElse(0)

    override def getShort(columnIndex: Int): Short =
      currentRow.flatMap(row => smallint.decode(columnIndex, row.values).toOption).getOrElse(0)

    override def getInt(columnIndex: Int): Int =
      currentRow.flatMap(row => int.decode(columnIndex, row.values).toOption).getOrElse(0)

    override def getLong(columnIndex: Int): Long =
      currentRow.flatMap(row => bigint.decode(columnIndex, row.values).toOption).getOrElse(0L)

    override def getFloat(columnIndex: Int): Float =
      currentRow.flatMap(row => float.decode(columnIndex, row.values).toOption).getOrElse(0f)

    override def getDouble(columnIndex: Int): Double =
      currentRow.flatMap(row => double.decode(columnIndex, row.values).toOption).getOrElse(0.toDouble)

    override def getBytes(columnIndex: Int): Option[Array[Byte]] =
      currentRow.flatMap(row => binary(255).decode(columnIndex, row.values).toOption)

    override def getDate(columnIndex: Int): Option[LocalDate] =
      currentRow.flatMap(row => date.decode(columnIndex, row.values).toOption)

    override def getTime(columnIndex: Int): Option[LocalTime] =
      currentRow.flatMap(row => time.decode(columnIndex, row.values).toOption)

    override def getTimestamp(columnIndex: Int): Option[LocalDateTime] =
      currentRow.flatMap(row => timestamp.decode(columnIndex, row.values).toOption)

    override def getString(columnLabel: String): Option[String] =
      columns.zipWithIndex.find(_._1.name == columnLabel).flatMap { case (_, index) =>
        getString(index + 1)
      }

    override def getBoolean(columnLabel: String): Boolean =
      columns.zipWithIndex.find(_._1.name == columnLabel).exists { case (_, index) =>
        getBoolean(index + 1)
      }

    override def getByte(columnLabel: String): Byte =
      columns.zipWithIndex.find(_._1.name == columnLabel).map { case (_, index) =>
        getByte(index + 1)
      }.getOrElse(0)

    override def getShort(columnLabel: String): Short =
      columns.zipWithIndex.find(_._1.name == columnLabel).map { case (_, index) =>
        getShort(index + 1)
      }.getOrElse(0)

    override def getInt(columnLabel: String): Int =
      columns.zipWithIndex.find(_._1.name == columnLabel).map { case (_, index) =>
        getInt(index + 1)
      }.getOrElse(0)

    override def getLong(columnLabel: String): Long =
      columns.zipWithIndex.find(_._1.name == columnLabel).map { case (_, index) =>
        getLong(index + 1)
      }.getOrElse(0L)

    override def getFloat(columnLabel: String): Float =
      columns.zipWithIndex.find(_._1.name == columnLabel).map { case (_, index) =>
        getFloat(index + 1)
      }.getOrElse(0f)

    override def getDouble(columnLabel: String): Double =
      columns.zipWithIndex.find(_._1.name == columnLabel).map { case (_, index) =>
        getDouble(index + 1)
      }.getOrElse(0.toDouble)

    override def getBytes(columnLabel: String): Option[Array[Byte]] =
      columns.zipWithIndex.find(_._1.name == columnLabel).flatMap { case (_, index) =>
        getBytes(index + 1)
      }

    override def getDate(columnLabel: String): Option[LocalDate] =
      columns.zipWithIndex.find(_._1.name == columnLabel).flatMap { case (_, index) =>
        getDate(index + 1)
      }

    override def getTime(columnLabel: String): Option[LocalTime] =
      columns.zipWithIndex.find(_._1.name == columnLabel).flatMap { case (_, index) =>
        getTime(index + 1)
      }

    override def getTimestamp(columnLabel: String): Option[LocalDateTime] =
      columns.zipWithIndex.find(_._1.name == columnLabel).flatMap { case (_, index) =>
        getTimestamp(index + 1)
      }

    override def getMetaData(): ResultSetMetaData = ResultSetMetaData(columns, version)

    override def getBigDecimal(columnIndex: Int): Option[BigDecimal] =
      currentRow.flatMap(row => decimal().decode(columnIndex, row.values).toOption)

    override def getBigDecimal(columnLabel: String): Option[BigDecimal] =
      columns.zipWithIndex.find(_._1.name == columnLabel).flatMap { case (_, index) =>
        getBigDecimal(index + 1)
      }

    override def decode[T](codec: Codec[T]): List[T] =
      rows.flatMap(row => codec.decode(0, row.values).toOption).toList

  def apply(columns: Vector[ColumnDefinitionPacket], rows: Vector[ResultSetRowPacket], version: Version): ResultSet =
    Impl(columns, rows, version)

  def empty(version: Version): ResultSet = this.apply(Vector.empty, Vector.empty, version)
