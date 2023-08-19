/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import java.io.{ InputStream, Reader }
import java.sql.{ Date, Time, Timestamp }
import java.util.Date as UtilDate
import java.time.{ ZoneId, Instant, ZonedDateTime, LocalTime, LocalDate, LocalDateTime }

import cats.{ Functor, Monad }
import cats.implicits.*

/** Trait to get the DataType that matches the Scala type information from the ResultSet.
  *
  * @tparam F
  *   The effect type
  * @tparam T
  *   Scala types that match SQL DataType
  */
trait ResultSetReader[F[_], T]:

  def read(resultSet: ResultSet[F], columnLabel: String): F[T]

object ResultSetReader:

  def apply[F[_], T](func: ResultSet[F] => String => F[T]): ResultSetReader[F, T] =
    (resultSet: ResultSet[F], columnLabel: String) => func(resultSet)(columnLabel)

  /** A method to convert the specified Scala type to an arbitrary type so that it can be handled by ResultSetReader.
    *
    * @param f
    *   Function to convert from type A to B.
    * @param reader
    *   ResultSetReader to retrieve the DataType matching the type A information from the ResultSet.
    * @tparam F
    *   The effect type
    * @tparam A
    *   The Scala type to be converted from.
    * @tparam B
    *   The Scala type to be converted to.
    */
  def mapping[F[_]: Functor, A, B](f: A => B)(using reader: ResultSetReader[F, A]): ResultSetReader[F, B] =
    reader.map(f(_))

  given [F[_]: Functor]: Functor[[T] =>> ResultSetReader[F, T]] with
    override def map[A, B](fa: ResultSetReader[F, A])(f: A => B): ResultSetReader[F, B] =
      ResultSetReader(resultSet => columnLabel => fa.read(resultSet, columnLabel).map(f))

  given [F[_]]: ResultSetReader[F, String]      = ResultSetReader(_.getString)
  given [F[_]]: ResultSetReader[F, Boolean]     = ResultSetReader(_.getBoolean)
  given [F[_]]: ResultSetReader[F, Byte]        = ResultSetReader(_.getByte)
  given [F[_]]: ResultSetReader[F, Array[Byte]] = ResultSetReader(_.getBytes)
  given [F[_]]: ResultSetReader[F, Short]       = ResultSetReader(_.getShort)
  given [F[_]]: ResultSetReader[F, Int]         = ResultSetReader(_.getInt)
  given [F[_]]: ResultSetReader[F, Long]        = ResultSetReader(_.getLong)
  given [F[_]]: ResultSetReader[F, Float]       = ResultSetReader(_.getFloat)
  given [F[_]]: ResultSetReader[F, Double]      = ResultSetReader(_.getDouble)
  given [F[_]]: ResultSetReader[F, Date]        = ResultSetReader(_.getDate)
  given [F[_]]: ResultSetReader[F, Time]        = ResultSetReader(_.getTime)
  given [F[_]]: ResultSetReader[F, Timestamp]   = ResultSetReader(_.getTimestamp)
  given [F[_]]: ResultSetReader[F, InputStream] = ResultSetReader(_.getAsciiStream)
  given [F[_]]: ResultSetReader[F, Object]      = ResultSetReader(_.getObject)
  given [F[_]]: ResultSetReader[F, Reader]      = ResultSetReader(_.getCharacterStream)
  given [F[_]]: ResultSetReader[F, BigDecimal]  = ResultSetReader(_.getBigDecimal)

  given [F[_]: Functor](using reader: ResultSetReader[F, String]): ResultSetReader[F, BigInt] =
    reader.map(BigInt(_))

  given [F[_]: Functor](using reader: ResultSetReader[F, Timestamp]): ResultSetReader[F, Instant] =
    reader.map(_.toInstant)

  given [F[_]: Functor](using reader: ResultSetReader[F, Timestamp]): ResultSetReader[F, UtilDate] =
    reader.map(timestamp => new UtilDate(timestamp.getTime))

  given [F[_]: Functor](using reader: ResultSetReader[F, Instant]): ResultSetReader[F, ZonedDateTime] =
    reader.map(instant => ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))

  given [F[_]: Functor](using reader: ResultSetReader[F, Time]): ResultSetReader[F, LocalTime] =
    reader.map(_.toLocalTime)

  given [F[_]: Functor](using reader: ResultSetReader[F, Date]): ResultSetReader[F, LocalDate] =
    reader.map(_.toLocalDate)

  given [F[_]: Functor](using reader: ResultSetReader[F, Timestamp]): ResultSetReader[F, LocalDateTime] =
    reader.map(_.toLocalDateTime)

  given [F[_]: Monad, A](using reader: ResultSetReader[F, A]): ResultSetReader[F, Option[A]] with

    override def read(resultSet: ResultSet[F], columnLabel: String): F[Option[A]] =
      for
        result <- reader.read(resultSet, columnLabel)
        bool   <- resultSet.wasNull()
      yield if bool then None else Some(result)
