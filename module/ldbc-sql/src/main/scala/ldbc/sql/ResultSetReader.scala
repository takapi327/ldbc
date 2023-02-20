/** This file is part of the ldbc. For the full copyright and license information, please view the LICENSE file that was
  * distributed with this source code.
  */

package ldbc.sql

import java.io.{ InputStream, Reader }
import java.sql.{ Date, SQLWarning, Time, Timestamp }
import java.time.LocalDateTime

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
    new ResultSetReader[F, T]:
      override def read(resultSet: ResultSet[F], columnLabel: String): F[T] =
        func(resultSet)(columnLabel)

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

  given [F[_]: Functor](using loader: ResultSetReader[F, Timestamp]): ResultSetReader[F, LocalDateTime] with
    override def read(resultSet: ResultSet[F], columnLabel: String): F[LocalDateTime] =
      for result <- loader.read(resultSet, columnLabel)
      yield result.toLocalDateTime

  given [F[_]: Monad, A](using loader: ResultSetReader[F, A]): ResultSetReader[F, Option[A]] with
    override def read(resultSet: ResultSet[F], columnLabel: String): F[Option[A]] =
      for
        result <- loader.read(resultSet, columnLabel)
        bool   <- resultSet.wasNull()
      yield if bool then None else Some(result)
