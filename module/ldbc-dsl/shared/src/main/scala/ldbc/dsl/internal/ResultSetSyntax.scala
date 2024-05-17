/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.internal

import java.time.*

import cats.effect.Sync
import cats.syntax.all.*

import ldbc.sql.*

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

      override def getDate(columnIndex: Int): F[LocalDate] =
        Sync[F].blocking(resultSet.getDate(columnIndex)).map {
          case null => null
          case date => date.toLocalDate
        }

      override def getDate(columnLabel: String): F[LocalDate] =
        Sync[F].blocking(resultSet.getDate(columnLabel)).map {
          case null => null
          case date => date.toLocalDate
        }

      override def getTime(columnIndex: Int): F[LocalTime] =
        Sync[F].blocking(resultSet.getTime(columnIndex)).map {
          case null => null
          case time => time.toLocalTime
        }

      override def getTime(columnLabel: String): F[LocalTime] =
        Sync[F].blocking(resultSet.getTime(columnLabel)).map {
          case null => null
          case time => time.toLocalTime
        }

      override def getTimestamp(columnIndex: Int): F[LocalDateTime] =
        Sync[F].blocking(resultSet.getTimestamp(columnIndex)).map {
          case null => null
          case timestamp => timestamp.toLocalDateTime
        }

      override def getTimestamp(columnLabel: String): F[LocalDateTime] =
        Sync[F].blocking(resultSet.getTimestamp(columnLabel)).map {
          case null => null
          case timestamp => timestamp.toLocalDateTime
        }

      override def getMetaData(): F[ResultSetMetaData] =
        Sync[F].blocking(resultSet.getMetaData).map(ResultSetMetaData(_))

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

      override def getType(): F[Int] = Sync[F].blocking(resultSet.getType)

      override def getConcurrency(): F[Int] = Sync[F].blocking(resultSet.getConcurrency)
