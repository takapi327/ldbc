/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql.dsl

import java.net.URL
import java.sql.{ Blob, Clob, Date, Time, Timestamp, Array as SqlArray }

import ldbc.sql.PreparedStatement

/** Trait for setting Scala and Java values to PreparedStatement.
  *
  * @tparam F
  *   The effect type
  * @tparam T
  *   Scala and Java types available in PreparedStatement.
  */
trait Parameter[F[_], -T]:

  /** Methods for setting Scala and Java values to the specified position in PreparedStatement.
    *
    * @param statement
    *   An object that represents a precompiled SQL statement.
    * @param index
    *   the first parameter is 1, the second is 2, ...
    * @param value
    *   the parameter value
    */
  def bind(statement: PreparedStatement[F], index: Int, value: T): F[Unit]

object Parameter:

  given [F[_]]: Parameter[F, Boolean] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Boolean): F[Unit] =
      statement.setBoolean(index, value)

  given [F[_]]: Parameter[F, Byte] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Byte): F[Unit] =
      statement.setByte(index, value)

  given [F[_]]: Parameter[F, Int] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Int): F[Unit] =
      statement.setInt(index, value)

  given [F[_]]: Parameter[F, Short] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Short): F[Unit] =
      statement.setShort(index, value)

  given [F[_]]: Parameter[F, Long] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Long): F[Unit] =
      statement.setLong(index, value)

  given [F[_]]: Parameter[F, Float] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Float): F[Unit] =
      statement.setFloat(index, value)

  given [F[_]]: Parameter[F, Double] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Double): F[Unit] =
      statement.setDouble(index, value)

  given [F[_]]: Parameter[F, BigDecimal] with
    override def bind(statement: PreparedStatement[F], index: Int, value: BigDecimal): F[Unit] =
      statement.setBigDecimal(index, value)

  given [F[_]]: Parameter[F, String] with
    override def bind(statement: PreparedStatement[F], index: Int, value: String): F[Unit] =
      statement.setString(index, value)

  given [F[_]]: Parameter[F, Array[Byte]] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Array[Byte]): F[Unit] =
      statement.setBytes(index, value)

  given [F[_]]: Parameter[F, Date] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Date): F[Unit] =
      statement.setDate(index, value)

  given [F[_]]: Parameter[F, Time] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Time): F[Unit] =
      statement.setTime(index, value)

  given [F[_]]: Parameter[F, Timestamp] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Timestamp): F[Unit] =
      statement.setTimestamp(index, value)

  given [F[_]]: Parameter[F, Object] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Object): F[Unit] =
      statement.setObject(index, value)

  given [F[_]]: Parameter[F, URL] with
    override def bind(statement: PreparedStatement[F], index: Int, value: URL): F[Unit] =
      statement.setURL(index, value)
