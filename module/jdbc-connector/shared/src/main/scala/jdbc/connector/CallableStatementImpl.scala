/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package jdbc.connector

import java.time.*

import cats.syntax.all.*

import cats.effect.Sync

import ldbc.sql.CallableStatement

private[jdbc] case class CallableStatementImpl[F[_] : Sync](callableStatement: java.sql.CallableStatement) extends PreparedStatementImpl[F](callableStatement), CallableStatement[F]:

  override def registerOutParameter(parameterIndex: Int, sqlType: Int): F[Unit] =
    Sync[F].blocking(callableStatement.registerOutParameter(parameterIndex, sqlType))

  override def getString(parameterIndex: Int): F[Option[String]] =
    Sync[F].blocking(Option(callableStatement.getString(parameterIndex)))

  override def getBoolean(parameterIndex: Int): F[Boolean] =
    Sync[F].blocking(callableStatement.getBoolean(parameterIndex))

  override def getByte(parameterIndex: Int): F[Byte] = Sync[F].blocking(callableStatement.getByte(parameterIndex))

  override def getShort(parameterIndex: Int): F[Short] =
    Sync[F].blocking(callableStatement.getShort(parameterIndex))

  override def getInt(parameterIndex: Int): F[Int] = Sync[F].blocking(callableStatement.getInt(parameterIndex))

  override def getLong(parameterIndex: Int): F[Long] = Sync[F].blocking(callableStatement.getLong(parameterIndex))

  override def getFloat(parameterIndex: Int): F[Float] =
    Sync[F].blocking(callableStatement.getFloat(parameterIndex))

  override def getDouble(parameterIndex: Int): F[Double] =
    Sync[F].blocking(callableStatement.getDouble(parameterIndex))

  override def getBigDecimal(parameterIndex: Int): F[Option[BigDecimal]] =
    Sync[F].blocking(Option(callableStatement.getBigDecimal(parameterIndex)))

  override def getBytes(parameterIndex: Int): F[Option[Array[Byte]]] =
    Sync[F].blocking(Option(callableStatement.getBytes(parameterIndex)))

  override def getDate(parameterIndex: Int): F[Option[LocalDate]] =
    Sync[F].blocking(Option(callableStatement.getDate(parameterIndex)).map(_.toLocalDate))

  override def getTime(parameterIndex: Int): F[Option[LocalTime]] =
    Sync[F].blocking(Option(callableStatement.getTime(parameterIndex)).map(_.toLocalTime))

  override def getTimestamp(parameterIndex: Int): F[Option[LocalDateTime]] =
    Sync[F].blocking(Option(callableStatement.getTimestamp(parameterIndex)).map(_.toLocalDateTime))

  override def getString(parameterName: String): F[Option[String]] =
    Sync[F].blocking(Option(callableStatement.getString(parameterName)))

  override def getBoolean(parameterName: String): F[Boolean] =
    Sync[F].blocking(callableStatement.getBoolean(parameterName))

  override def getByte(parameterName: String): F[Byte] =
    Sync[F].blocking(callableStatement.getByte(parameterName))

  override def getShort(parameterName: String): F[Short] =
    Sync[F].blocking(callableStatement.getShort(parameterName))

  override def getInt(parameterName: String): F[Int] = Sync[F].blocking(callableStatement.getInt(parameterName))

  override def getLong(parameterName: String): F[Long] =
    Sync[F].blocking(callableStatement.getLong(parameterName))

  override def getFloat(parameterName: String): F[Float] =
    Sync[F].blocking(callableStatement.getFloat(parameterName))

  override def getDouble(parameterName: String): F[Double] =
    Sync[F].blocking(callableStatement.getDouble(parameterName))

  override def getBigDecimal(parameterName: String): F[Option[BigDecimal]] =
    Sync[F].blocking(Option(callableStatement.getBigDecimal(parameterName)))

  override def getBytes(parameterName: String): F[Option[Array[Byte]]] =
    Sync[F].blocking(Option(callableStatement.getBytes(parameterName)))

  override def getDate(parameterName: String): F[Option[LocalDate]] =
    Sync[F].blocking(Option(callableStatement.getDate(parameterName)).map(_.toLocalDate))

  override def getTime(parameterName: String): F[Option[LocalTime]] =
    Sync[F].blocking(Option(callableStatement.getTime(parameterName)).map(_.toLocalTime))

  override def getTimestamp(parameterName: String): F[Option[LocalDateTime]] =
    Sync[F].blocking(Option(callableStatement.getTimestamp(parameterName)).map(_.toLocalDateTime))
