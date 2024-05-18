/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

import java.time.*

/**
 * The interface used to execute SQL stored procedures.  The JDBC API
 * provides a stored procedure SQL escape syntax that allows stored procedures
 * to be called in a standard way for all RDBMSs. This escape syntax has one
 * form that includes a result parameter and one that does not. If used, the result
 * parameter must be registered as an OUT parameter. The other parameters
 * can be used for input, output or both. Parameters are referred to
 * sequentially, by number, with the first parameter being 1.
 * <PRE>
 *   {?= call &lt;procedure-name&gt;[(&lt;arg1&gt;,&lt;arg2&gt;, ...)]}
 *   {call &lt;procedure-name&gt;[(&lt;arg1&gt;,&lt;arg2&gt;, ...)]}
 * </PRE>
 * <P>
 * IN parameter values are set using the <code>set</code> methods inherited from
 * {@link PreparedStatement}.  The type of all OUT parameters must be
 * registered prior to executing the stored procedure; their values
 * are retrieved after execution via the <code>get</code> methods provided here.
 * <P>
 * A <code>CallableStatement</code> can return one {@link ResultSet} object or
 * multiple <code>ResultSet</code> objects.  Multiple
 * <code>ResultSet</code> objects are handled using operations
 * inherited from {@link Statement}.
 * <P>
 * For maximum portability, a call's <code>ResultSet</code> objects and
 * update counts should be processed prior to getting the values of output
 * parameters.
 *
 * @tparam F
 *   the effect type
 */
trait CallableStatement[F[_]] extends PreparedStatement[F]:

  /**
   * Registers the OUT parameter in ordinal position
   * <code>parameterIndex</code> to the JDBC type
   * <code>sqlType</code>.  All OUT parameters must be registered
   * before a stored procedure is executed.
   * <p>
   * The JDBC type specified by <code>sqlType</code> for an OUT
   * parameter determines the Scala type that must be used
   * in the <code>get</code> method to read the value of that parameter.
   * <p>
   * If the JDBC type expected to be returned to this output parameter
   * is specific to this particular database, <code>sqlType</code>
   * should be <code>java.sql.Types.OTHER</code>.  The method
   * {@link #getObject} retrieves the value.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @param sqlType the JDBC type code defined by <code>java.sql.Types</code>.
   *        If the parameter is of JDBC type <code>NUMERIC</code>
   *        or <code>DECIMAL</code>, the version of
   *        <code>registerOutParameter</code> that accepts a scale value
   *        should be used.
   */
  def registerOutParameter(parameterIndex: Int, sqlType: Int): F[Unit]

  /**
   * Retrieves the value of the designated JDBC <code>CHAR</code>,
   * <code>VARCHAR</code>, or <code>LONGVARCHAR</code> parameter as a
   * <code>String</code> in the Sava programming language
   * <p>
   * For the fixed-length type JDBC <code>CHAR</code>,
   * the <code>String</code> object
   * returned has exactly the same value the SQL
   * <code>CHAR</code> value had in the
   * database, including any padding added by the database.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result
   *         is <code>None</code>.
   */
  def getString(parameterIndex: Int): F[Option[String]]

  /**
   * Retrieves the value of the designated JDBC <code>BIT</code>
   * or <code>BOOLEAN</code> parameter as a
   * <code>Boolean</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>false</code>.
   */
  def getBoolean(parameterIndex: Int): F[Boolean]

  /**
   * Retrieves the value of the designated JDBC <code>TINYINT</code> parameter
   * as a <code>byte</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getByte(parameterIndex: Int): F[Byte]

  /**
   * Retrieves the value of the designated JDBC <code>SMALLINT</code> parameter
   * as a <code>short</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getShort(parameterIndex: Int): F[Short]

  /**
   * Retrieves the value of the designated JDBC <code>INTEGER</code> parameter
   * as an <code>int</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value.  If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getInt(parameterIndex: Int): F[Int]

  /**
   * Retrieves the value of the designated JDBC <code>BIGINT</code> parameter
   * as a <code>long</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getLong(parameterIndex: Int): F[Long]

  /**
   * Retrieves the value of the designated JDBC <code>FLOAT</code> parameter
   * as a <code>float</code> in the Sava programming language
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>0</code>.
   */
  def getFloat(parameterIndex: Int): F[Float]

  /**
   * Retrieves the value of the designated JDBC <code>DOUBLE</code> parameter as a <code>double</code>
   * in the Sava programming language
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>0</code>.
   */
  def getDouble(parameterIndex: Int): F[Double]

  /**
   * Retrieves the value of the designated JDBC <code>BINARY</code> or
   * <code>VARBINARY</code> parameter as an array of <code>byte</code>
   * values in the Sava programming language
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>None</code>.
   */
  def getBytes(parameterIndex: Int): F[Option[Array[Byte]]]

  /**
   * Retrieves the value of the designated JDBC <code>DATE</code> parameter as a
   * <code>java.time.LocalDate</code> object.
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>None</code>.
   */
  def getDate(parameterIndex: Int): F[Option[LocalDate]]

  /**
   * Retrieves the value of the designated JDBC <code>TIME</code> parameter as a
   * <code>java.time.LocalTime</code> object.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>null</code>.
   */
  def getTime(parameterIndex: Int): F[Option[LocalTime]]

  /**
   * Retrieves the value of the designated JDBC <code>TIMESTAMP</code> parameter as a
   * <code>java.time.LocalDateTime</code> object.
   *
   * @param parameterIndex the first parameter is 1, the second is 2,
   *        and so on
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   *         is <code>None</code>.
   */
  def getTimestamp(parameterIndex: Int): F[Option[LocalDateTime]]

  /**
   * Retrieves the value of the designated JDBC <code>NUMERIC</code> parameter as a
   * <code>java.math.BigDecimal</code> object with as many digits to the
   * right of the decimal point as the value contains.
   * @param parameterIndex the first parameter is 1, the second is 2,
   * and so on
   * @return the parameter value in full precision.  If the value is
   * SQL <code>NULL</code>, the result is <code>None</code>.
   */
  def getBigDecimal(parameterIndex: Int): F[Option[BigDecimal]]

  /**
   * Retrieves the value of a JDBC <code>CHAR</code>, <code>VARCHAR</code>,
   * or <code>LONGVARCHAR</code> parameter as a <code>String</code> in
   * the Sava programming language
   * <p>
   * For the fixed-length type JDBC <code>CHAR</code>,
   * the <code>String</code> object
   * returned has exactly the same value the SQL
   * <code>CHAR</code> value had in the
   * database, including any padding added by the database.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>None</code>.
   */
  def getString(parameterName: String): F[Option[String]]

  /**
   * Retrieves the value of a JDBC <code>BIT</code> or <code>BOOLEAN</code>
   * parameter as a
   * <code>Boolean</code> in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>false</code>.
   */
  def getBoolean(parameterName: String): F[Boolean]

  /**
   * Retrieves the value of a JDBC <code>TINYINT</code> parameter as a <code>byte</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getByte(parameterName: String): F[Byte]

  /**
   * Retrieves the value of a JDBC <code>SMALLINT</code> parameter as a <code>short</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>0</code>.
   */
  def getShort(parameterName: String): F[Short]

  /**
   * Retrieves the value of a JDBC <code>INTEGER</code> parameter as an <code>int</code>
   * in the Sava programming language
   *
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getInt(parameterName: String): F[Int]

  /**
   * Retrieves the value of a JDBC <code>BIGINT</code> parameter as a <code>long</code>
   * in the Sava programming language
   *
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getLong(parameterName: String): F[Long]

  /**
   * Retrieves the value of a JDBC <code>FLOAT</code> parameter as a <code>float</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getFloat(parameterName: String): F[Float]

  /**
   * Retrieves the value of a JDBC <code>DOUBLE</code> parameter as a <code>double</code>
   * in the Sava programming language
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>,
   *         the result is <code>0</code>.
   */
  def getDouble(parameterName: String): F[Double]

  /**
   * Retrieves the value of a JDBC <code>BINARY</code> or <code>VARBINARY</code>
   * parameter as an array of <code>byte</code> values in the Scala
   * programming language.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result is
   *  <code>None</code>.
   */
  def getBytes(parameterName: String): F[Option[Array[Byte]]]

  /**
   * Retrieves the value of a JDBC <code>DATE</code> parameter as a
   * <code>java.sql.Date</code> object.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>None</code>.
   */
  def getDate(parameterName: String): F[Option[LocalDate]]

  /**
   * Retrieves the value of a JDBC <code>TIME</code> parameter as a
   * <code>java.sql.Time</code> object.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>null</code>.
   */
  def getTime(parameterName: String): F[Option[LocalTime]]

  /**
   * Retrieves the value of a JDBC <code>TIMESTAMP</code> parameter as a
   * <code>java.sql.Timestamp</code> object.
   * @param parameterName the name of the parameter
   * @return the parameter value. If the value is SQL <code>NULL</code>, the result
   * is <code>None</code>.
   */
  def getTimestamp(parameterName: String): F[Option[LocalDateTime]]

  /**
   * Retrieves the value of a JDBC <code>NUMERIC</code> parameter as a
   * <code>java.math.BigDecimal</code> object with as many digits to the
   * right of the decimal point as the value contains.
   * @param parameterName the name of the parameter
   * @return the parameter value in full precision. If the value is
   * SQL <code>NULL</code>, the result is <code>None</code>.
   */
  def getBigDecimal(parameterName: String): F[Option[BigDecimal]]
