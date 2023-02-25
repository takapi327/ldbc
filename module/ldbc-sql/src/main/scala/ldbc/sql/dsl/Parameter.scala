/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql.dsl

import java.net.URL
import java.sql.{ Blob, Clob, Date, Time, Timestamp, Array as SqlArray }
import java.util.Date as UtilDate
import java.time.{ ZoneId, Instant, ZonedDateTime, LocalTime, LocalDate, LocalDateTime }

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

  def convert[F[_], A, B](f: B => A)(using parameter: Parameter[F, A]): Parameter[F, B] =
    new Parameter[F, B]:
      override def bind(statement: PreparedStatement[F], index: Int, value: B): F[Unit] =
        parameter.bind(statement, index, f(value))

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

  given [F[_]]: Parameter[F, UtilDate] = Parameter.convert(date => new Timestamp(date.getTime))

  given [F[_]]: Parameter[F, Time] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Time): F[Unit] =
      statement.setTime(index, value)

  given [F[_]]: Parameter[F, Timestamp] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Timestamp): F[Unit] =
      statement.setTimestamp(index, value)

  given [F[_]]: Parameter[F, Instant] = Parameter.convert(Timestamp.from)

  given [F[_]]: Parameter[F, ZonedDateTime] = Parameter.convert(_.toInstant)

  given [F[_]]: Parameter[F, LocalTime] = Parameter.convert(Time.valueOf)

  given [F[_]]: Parameter[F, LocalDate] = Parameter.convert(Date.valueOf)

  given [F[_]]: Parameter[F, LocalDateTime] = Parameter.convert(ZonedDateTime.of(_, ZoneId.systemDefault()))

  given [F[_]]: Parameter[F, Object] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Object): F[Unit] =
      statement.setObject(index, value)

  given [F[_]]: Parameter[F, URL] with
    override def bind(statement: PreparedStatement[F], index: Int, value: URL): F[Unit] =
      statement.setURL(index, value)

  given [F[_]]: Parameter[F, Null] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Null): F[Unit] =
      statement.setObject(index, value)

  given [F[_], T](using parameter: Parameter[F, T], nullParameter: Parameter[F, Null]): Parameter[F, Option[T]] with
    override def bind(statement: PreparedStatement[F], index: Int, value: Option[T]): F[Unit] =
      value match
        case Some(value) => parameter.bind(statement, index, value)
        case None        => nullParameter.bind(statement, index, null)
