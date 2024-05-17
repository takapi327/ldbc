/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import java.time.*

/**
 * An object that represents a precompiled SQL statement.
 *
 * A SQL statement is precompiled and stored in a PreparedStatement object. This object can then be used to efficiently
 * execute this statement multiple times.
 *
 * Note: The setter methods (setShort, setString, and so on) for setting IN parameter values must specify types that are
 * compatible with the defined SQL type of the input parameter. For instance, if the IN parameter has SQL type INTEGER,
 * then the method setInt should be used.
 *
 * @tparam F
 *   The effect type
 */
trait PreparedStatement[F[_]] extends Statement[F]:

  @deprecated("This method cannot be called on a PreparedStatement.", "0.3.0")
  override def executeQuery(sql: String): F[ResultSet[F]] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  @deprecated("This method cannot be called on a PreparedStatement.", "0.3.0")
  override def executeUpdate(sql: String): F[Int] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  @deprecated("This method cannot be called on a PreparedStatement.", "0.3.0")
  override def execute(sql: String): F[Boolean] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  override def addBatch(sql: String): F[Unit] = throw new UnsupportedOperationException(
    "This method cannot be called on a PreparedStatement."
  )

  /**
   * Sets the designated parameter to SQL NULL.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   */
  def setNull(index: Int, sqlType: Int): F[Unit]

  /**
   * Sets the designated parameter to the given Scala boolean value. The driver converts this to an SQL BIT or BOOLEAN
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBoolean(index: Int, value: Boolean): F[Unit]

  /**
   * Sets the designated parameter to the given Scala byte value. The driver converts this to an SQL TINYINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setByte(index: Int, value: Byte): F[Unit]

  /**
   * Sets the designated parameter to the given Scala short value. The driver converts this to an SQL SMALLINT value
   * when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setShort(index: Int, value: Short): F[Unit]

  /**
   * Sets the designated parameter to the given Scala int value. The driver converts this to an SQL INTEGER value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setInt(index: Int, value: Int): F[Unit]

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setLong(index: Int, value: Long): F[Unit]

  /**
   * Sets the designated parameter to the given Scala float value. The driver converts this to an SQL REAL value when it
   * ends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setFloat(index: Int, value: Float): F[Unit]

  /**
   * Sets the designated parameter to the given Scala double value. The driver converts this to an SQL DOUBLE value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDouble(index: Int, value: Double): F[Unit]

  /**
   * Sets the designated parameter to the given Scala.math.BigDecimal value. The driver converts this to an SQL NUMERIC
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigDecimal(index: Int, value: BigDecimal): F[Unit]

  /**
   * Sets the designated parameter to the given Scala String value. The driver converts this to an SQL VARCHAR or
   * LONGVARCHAR value (depending on the argument's size relative to the driver's limits on VARCHAR values) when it
   * sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setString(index: Int, value: String): F[Unit]

  /**
   * Sets the designated parameter to the given Scala array of bytes. The driver converts this to an SQL VARBINARY or
   * LONGVARBINARY (depending on the argument's size relative to the driver's limits on VARBINARY values) when it sends
   * it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBytes(index: Int, value: Array[Byte]): F[Unit]

  /**
   * Sets the designated parameter to the given java.time.Time value. The driver converts this to an SQL TIME value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTime(index: Int, value: LocalTime): F[Unit]

  /**
   * Sets the designated parameter to the given java.time.Date value, using the given Calendar object. The driver uses
   * the Calendar object to construct an SQL DATE value, which the driver then sends to the database. With a Calendar
   * object, the driver can calculate the date taking into account a custom timezone. If no Calendar object is
   * specified, the driver uses the default timezone, which is that of the virtual machine running the application.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDate(index: Int, value: LocalDate): F[Unit]

  /**
   * Sets the designated parameter to the given java.time.Timestamp value. The driver converts this to an SQL TIMESTAMP
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTimestamp(index: Int, value: LocalDateTime): F[Unit]

  /**
   * Executes the specified SQL statement and returns one or more ResultSet objects.
   */
  def executeQuery(): F[ResultSet[F]]

  /**
   * Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE statement or an SQL statement that
   * returns nothing, such as an SQL DDL statement.
   */
  def executeUpdate(): F[Int]

  /**
   * Executes the SQL statement in this <code>PreparedStatement</code> object,
   * which may be any kind of SQL statement.
   * Some prepared statements return multiple results; the <code>execute</code>
   * method handles these complex statements as well as the simpler
   * form of statements handled by the methods <code>executeQuery</code>
   * and <code>executeUpdate</code>.
   * <P>
   * The <code>execute</code> method returns a <code>boolean</code> to
   * indicate the form of the first result.  You must call either the method
   * <code>getResultSet</code> or <code>getUpdateCount</code>
   * to retrieve the result; you must call <code>getMoreResults</code> to
   * move to any subsequent result(s).
   *
   * @return <code>true</code> if the first result is a <code>ResultSet</code>
   *         object; <code>false</code> if the first result is an update
   *         count or there is no result
   */
  def execute(): F[Boolean]

  /**
   * Adds a set of parameters to this PreparedStatement object's batch of commands.
   */
  def addBatch(): F[Unit]
