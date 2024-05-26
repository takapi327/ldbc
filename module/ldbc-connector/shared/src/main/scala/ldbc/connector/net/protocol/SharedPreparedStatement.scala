/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import java.time.*

import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import cats.effect.*

import ldbc.sql.PreparedStatement

import ldbc.connector.data.*
import ldbc.connector.exception.SQLException

private[ldbc] trait SharedPreparedStatement[F[_]: Temporal]
  extends PreparedStatement[F],
          StatementImpl.ShareStatement[F]:

  def params: Ref[F, ListMap[Int, Parameter]]

  override def setNull(index: Int, sqlType: Int): F[Unit] =
    params.update(_ + (index -> Parameter.none))

  override def setBoolean(index: Int, value: Boolean): F[Unit] =
    params.update(_ + (index -> Parameter.boolean(value)))

  override def setByte(index: Int, value: Byte): F[Unit] =
    params.update(_ + (index -> Parameter.byte(value)))

  override def setShort(index: Int, value: Short): F[Unit] =
    params.update(_ + (index -> Parameter.short(value)))

  override def setInt(index: Int, value: Int): F[Unit] =
    params.update(_ + (index -> Parameter.int(value)))

  override def setLong(index: Int, value: Long): F[Unit] =
    params.update(_ + (index -> Parameter.long(value)))

  override def setFloat(index: Int, value: Float): F[Unit] =
    params.update(_ + (index -> Parameter.float(value)))

  override def setDouble(index: Int, value: Double): F[Unit] =
    params.update(_ + (index -> Parameter.double(value)))

  override def setBigDecimal(index: Int, value: BigDecimal): F[Unit] =
    params.update(_ + (index -> Parameter.bigDecimal(value)))

  override def setString(index: Int, value: String): F[Unit] =
    params.update(_ + (index -> Parameter.string(value)))

  override def setBytes(index: Int, value: Array[Byte]): F[Unit] =
    params.update(_ + (index -> Parameter.bytes(value)))

  override def setTime(index: Int, value: LocalTime): F[Unit] =
    params.update(_ + (index -> Parameter.time(value)))

  override def setDate(index: Int, value: LocalDate): F[Unit] =
    params.update(_ + (index -> Parameter.date(value)))

  override def setTimestamp(index: Int, value: LocalDateTime): F[Unit] =
    params.update(_ + (index -> Parameter.datetime(value)))

  override def setObject(parameterIndex: Int, value: Object): F[Unit] =
    value match
      case null                                     => setNull(parameterIndex, MysqlType.NULL.jdbcType)
      case value if value.isInstanceOf[Boolean]     => setBoolean(parameterIndex, value.asInstanceOf[Boolean])
      case value if value.isInstanceOf[Byte]        => setByte(parameterIndex, value.asInstanceOf[Byte])
      case value if value.isInstanceOf[Short]       => setShort(parameterIndex, value.asInstanceOf[Short])
      case value if value.isInstanceOf[Int]         => setInt(parameterIndex, value.asInstanceOf[Int])
      case value if value.isInstanceOf[Long]        => setLong(parameterIndex, value.asInstanceOf[Long])
      case value if value.isInstanceOf[Float]       => setFloat(parameterIndex, value.asInstanceOf[Float])
      case value if value.isInstanceOf[Double]      => setDouble(parameterIndex, value.asInstanceOf[Double])
      case value if value.isInstanceOf[String]      => setString(parameterIndex, value.asInstanceOf[String])
      case value if value.isInstanceOf[Array[Byte]] => setBytes(parameterIndex, value.asInstanceOf[Array[Byte]])
      case value if value.isInstanceOf[LocalTime]   => setTime(parameterIndex, value.asInstanceOf[LocalTime])
      case value if value.isInstanceOf[LocalDate]   => setDate(parameterIndex, value.asInstanceOf[LocalDate])
      case value if value.isInstanceOf[LocalDateTime] =>
        setTimestamp(parameterIndex, value.asInstanceOf[LocalDateTime])
      case unknown => throw new SQLException(s"Unsupported object type ${ unknown.getClass.getName } for setObject")

  override def executeUpdate(): F[Int] = executeLargeUpdate().map(_.toInt)

  protected def buildQuery(original: String, params: ListMap[Int, Parameter]): String =
    val query = original.toCharArray
    params
      .foldLeft(query) {
        case (query, (offset, param)) =>
          val index = query.indexOf('?', offset - 1)
          if index < 0 then query
          else
            val (head, tail)         = query.splitAt(index)
            val (tailHead, tailTail) = tail.splitAt(1)
            head ++ param.sql ++ tailTail
      }
      .mkString

  protected def buildBatchQuery(original: String, params: ListMap[Int, Parameter]): String =
    val placeholderCount = original.split("\\?", -1).length - 1
    require(placeholderCount == params.size, "The number of parameters does not match the number of placeholders")
    original.trim.toLowerCase match
      case q if q.startsWith("insert") =>
        val bindQuery = buildQuery(original, params)
        bindQuery.split("VALUES").last
      case q if q.startsWith("update") || q.startsWith("delete") => buildQuery(original, params)
      case _ => throw new IllegalArgumentException("The batch query must be an INSERT, UPDATE, or DELETE statement.")
