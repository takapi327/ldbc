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

import cats.effect.Ref

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.connector.ResultSet
import ldbc.connector.data.*
import ldbc.connector.exception.MySQLException
import ldbc.connector.net.PacketSocket
import ldbc.connector.net.packet.ResponsePacket
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

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

  /**
   * Retrieves the current parameter values of this PreparedStatement object.
   */
  def params: Ref[F, ListMap[Int, Parameter]]

  /**
   * Sets the designated parameter to SQL NULL.
   * 
   * @param index
   *   the first parameter is 1, the second is 2, ...
   */
  def setNull(index: Int): F[Unit] =
    params.update(_ + (index -> Parameter.none))

  /**
   * Sets the designated parameter to the given Scala boolean value. The driver converts this to an SQL BIT or BOOLEAN
   * value when it sends it to the database.
   * 
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBoolean(index: Int, value: Boolean): F[Unit] =
    params.update(_ + (index -> Parameter.boolean(value)))

  /**
   * Sets the designated parameter to the given Scala boolean value. The driver converts this to an SQL BIT or BOOLEAN
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBoolean(index: Int, value: Option[Boolean]): F[Unit] =
    value match
      case Some(value) => setBoolean(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala byte value. The driver converts this to an SQL TINYINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setByte(index: Int, value: Byte): F[Unit] =
    params.update(_ + (index -> Parameter.byte(value)))

  /**
   * Sets the designated parameter to the given Scala byte value. The driver converts this to an SQL TINYINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setByte(index: Int, value: Option[Byte]): F[Unit] =
    value match
      case Some(value) => setByte(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala short value. The driver converts this to an SQL SMALLINT value
   * when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setShort(index: Int, value: Short): F[Unit] =
    params.update(_ + (index -> Parameter.short(value)))

  /**
   * Sets the designated parameter to the given Scala short value. The driver converts this to an SQL SMALLINT value
   * when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setShort(index: Int, value: Option[Short]): F[Unit] =
    value match
      case Some(value) => setShort(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala int value. The driver converts this to an SQL INTEGER value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setInt(index: Int, value: Int): F[Unit] =
    params.update(_ + (index -> Parameter.int(value)))

  /**
   * Sets the designated parameter to the given Scala int value. The driver converts this to an SQL INTEGER value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setInt(index: Int, value: Option[Int]): F[Unit] =
    value match
      case Some(value) => setInt(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setLong(index: Int, value: Long): F[Unit] =
    params.update(_ + (index -> Parameter.long(value)))

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setLong(index: Int, value: Option[Long]): F[Unit] =
    value match
      case Some(value) => setLong(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigInt(index: Int, value: BigInt): F[Unit] =
    params.update(_ + (index -> Parameter.bigInt(value)))

  /**
   * Sets the designated parameter to the given Scala long value. The driver converts this to an SQL BIGINT value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigInt(index: Int, value: Option[BigInt]): F[Unit] =
    value match
      case Some(value) => setBigInt(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala float value. The driver converts this to an SQL REAL value when it
   * ends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setFloat(index: Int, value: Float): F[Unit] =
    params.update(_ + (index -> Parameter.float(value)))

  /**
   * Sets the designated parameter to the given Scala float value. The driver converts this to an SQL REAL value when it
   * ends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setFloat(index: Int, value: Option[Float]): F[Unit] =
    value match
      case Some(value) => setFloat(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala double value. The driver converts this to an SQL DOUBLE value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDouble(index: Int, value: Double): F[Unit] =
    params.update(_ + (index -> Parameter.double(value)))

  /**
   * Sets the designated parameter to the given Scala double value. The driver converts this to an SQL DOUBLE value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setDouble(index: Int, value: Option[Double]): F[Unit] =
    value match
      case Some(value) => setDouble(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given Scala.math.BigDecimal value. The driver converts this to an SQL NUMERIC
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigDecimal(index: Int, value: BigDecimal): F[Unit] =
    params.update(_ + (index -> Parameter.bigDecimal(value)))

  /**
   * Sets the designated parameter to the given Scala.math.BigDecimal value. The driver converts this to an SQL NUMERIC
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setBigDecimal(index: Int, value: Option[BigDecimal]): F[Unit] =
    value match
      case Some(value) => setBigDecimal(index, value)
      case None        => setNull(index)

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
  def setString(index: Int, value: String): F[Unit] =
    params.update(_ + (index -> Parameter.string(value)))

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
  def setString(index: Int, value: Option[String]): F[Unit] =
    value match
      case Some(value) => setString(index, value)
      case None        => setNull(index)

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
  def setBytes(index: Int, value: Array[Byte]): F[Unit] =
    params.update(_ + (index -> Parameter.bytes(value)))

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
  def setBytes(index: Int, value: Option[Array[Byte]]): F[Unit] =
    value match
      case Some(value) => setBytes(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Time value. The driver converts this to an SQL TIME value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTime(index: Int, value: LocalTime): F[Unit] =
    params.update(_ + (index -> Parameter.time(value)))

  /**
   * Sets the designated parameter to the given java.time.Time value. The driver converts this to an SQL TIME value when
   * it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTime(index: Int, value: Option[LocalTime]): F[Unit] =
    value match
      case Some(value) => setTime(index, value)
      case None        => setNull(index)

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
  def setDate(index: Int, value: LocalDate): F[Unit] =
    params.update(_ + (index -> Parameter.date(value)))

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
  def setDate(index: Int, value: Option[LocalDate]): F[Unit] =
    value match
      case Some(value) => setDate(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Timestamp value. The driver converts this to an SQL TIMESTAMP
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTimestamp(index: Int, value: LocalDateTime): F[Unit] =
    params.update(_ + (index -> Parameter.datetime(value)))

  /**
   * Sets the designated parameter to the given java.time.Timestamp value. The driver converts this to an SQL TIMESTAMP
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setTimestamp(index: Int, value: Option[LocalDateTime]): F[Unit] =
    value match
      case Some(value) => setTimestamp(index, value)
      case None        => setNull(index)

  /**
   * Sets the designated parameter to the given java.time.Year value. The driver converts this to an SQL YEAR
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setYear(index: Int, value: Year): F[Unit] =
    params.update(_ + (index -> Parameter.year(value)))

  /**
   * Sets the designated parameter to the given java.time.Year value. The driver converts this to an SQL YEAR
   * value when it sends it to the database.
   *
   * @param index
   *   the first parameter is 1, the second is 2, ...
   * @param value
   *   the parameter value
   */
  def setYear(index: Int, value: Option[Year]): F[Unit] =
    value match
      case Some(value) => setYear(index, value)
      case None        => setNull(index)

object PreparedStatement:

  /**
   * PreparedStatement for query construction at the client side.
   * 
   * @param socket
   *   the packet socket
   * @param initialPacket
   *   the initial packet
   * @param sql
   *   the SQL statement
   * @param params
   *   the parameters
   * @param resetSequenceId
   *   the reset sequence id
   * @param ev
   *   the effect type class
   * @tparam F
   *   the effect type
   */
  case class Client[F[_]: Exchange: Tracer](
    socket:          PacketSocket[F],
    initialPacket:   InitialPacket,
    sql:             String,
    params:          Ref[F, ListMap[Int, Parameter]],
    resetSequenceId: F[Unit]
  )(using ev: MonadError[F, Throwable])
    extends PreparedStatement[F]:

    private val attributes = List(
      Attribute("type", "Client PreparedStatement"),
      Attribute("sql", sql)
    )

    private def repeatProcess[P <: ResponsePacket](times: Int, decoder: scodec.Decoder[P]): F[Vector[P]] =
      def read(remaining: Int, acc: Vector[P]): F[Vector[P]] =
        if remaining <= 0 then ev.pure(acc)
        else socket.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

      read(times, Vector.empty[P])

    private def readUntilEOF[P <: ResponsePacket](
      decoder: scodec.Decoder[P | EOFPacket | ERRPacket],
      acc:     Vector[P]
    ): F[Vector[P]] =
      socket.receive(decoder).flatMap {
        case _: EOFPacket     => ev.pure(acc)
        case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query"))
        case row              => readUntilEOF(decoder, acc :+ row.asInstanceOf[P])
      }

    private def buildQuery(params: ListMap[Int, Parameter]): String =
      val query = sql.toCharArray
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

    override def executeQuery(): F[ResultSet] =
      exchange[F, ResultSet]("statement") { (span: Span[F]) =>
        params.get.flatMap { params =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
              Attribute("execute", "query")
            ))*
          ) *>
            resetSequenceId *>
            socket.send(ComQueryPacket(buildQuery(params), initialPacket.capabilityFlags, ListMap.empty)) *>
            socket.receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags)).flatMap {
              case _: OKPacket      => ev.pure(ResultSet.empty)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case result: ColumnsNumberPacket =>
                for
                  columnDefinitions <-
                    repeatProcess(
                      result.size,
                      ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags)
                    )
                  resultSetRow <-
                    readUntilEOF[ResultSetRowPacket](
                      ResultSetRowPacket.decoder(initialPacket.capabilityFlags, columnDefinitions),
                      Vector.empty
                    )
                yield new ResultSet:
                  override def columns: Vector[ColumnDefinitionPacket] = columnDefinitions
                  override def rows:    Vector[ResultSetRowPacket]     = resultSetRow
            }
        }
      }

    override def executeUpdate(): F[Int] =
      exchange[F, Int]("statement") { (span: Span[F]) =>
        params.get.flatMap { params =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
              Attribute("execute", "update")
            ))*
          ) *>
            resetSequenceId *>
            socket.send(ComQueryPacket(buildQuery(params), initialPacket.capabilityFlags, ListMap.empty)) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case result: OKPacket => ev.pure(result.affectedRows)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new MySQLException("Unexpected EOF packet"))
            }
        }
      }

    override def returningAutoGeneratedKey(): F[Int] =
      exchange[F, Int]("statement") { (span: Span[F]) =>
        params.get.flatMap { params =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
              Attribute("execute", "returning.update")
            ))*
          ) *>
            resetSequenceId *>
            socket.send(ComQueryPacket(buildQuery(params), initialPacket.capabilityFlags, ListMap.empty)) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case result: OKPacket => ev.pure(result.lastInsertId)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new MySQLException("Unexpected EOF packet"))
            }
        }
      }

    override def close(): F[Unit] = ev.unit

  /**
   * PreparedStatement for query construction at the server side.
   *
   * @param socket
   *   the packet socket
   * @param initialPacket
   *   the initial packet
   * @param statementId
   *   the statement id
   * @param sql
   *   the SQL statement
   * @param params
   *   the parameters
   * @param resetSequenceId
   *   the reset sequence id
   * @tparam F
   *   The effect type
   */
  case class Server[F[_]: Exchange: Tracer](
    socket:          PacketSocket[F],
    initialPacket:   InitialPacket,
    statementId:     Long,
    sql:             String,
    params:          Ref[F, ListMap[Int, Parameter]],
    resetSequenceId: F[Unit]
  )(using ev: MonadError[F, Throwable])
    extends PreparedStatement[F]:

    private val attributes = List(
      Attribute("type", "Server PreparedStatement"),
      Attribute("sql", sql)
    )

    private def repeatProcess[P <: ResponsePacket](times: Int, decoder: scodec.Decoder[P]): F[Vector[P]] =
      def read(remaining: Int, acc: Vector[P]): F[Vector[P]] =
        if remaining <= 0 then ev.pure(acc)
        else socket.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

      read(times, Vector.empty[P])

    private def readUntilEOF[P <: ResponsePacket](
      decoder: scodec.Decoder[P | EOFPacket | ERRPacket],
      acc:     Vector[P]
    ): F[Vector[P]] =
      socket.receive(decoder).flatMap {
        case _: EOFPacket     => ev.pure(acc)
        case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query"))
        case row              => readUntilEOF(decoder, acc :+ row.asInstanceOf[P])
      }

    override def executeQuery(): F[ResultSet] =
      exchange[F, ResultSet]("statement") { (span: Span[F]) =>
        for
          params <- params.get
          columnCount <- span.addAttributes(
                           (attributes ++ List(
                             Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
                             Attribute("execute", "query")
                           ))*
                         ) *>
                           resetSequenceId *>
                           socket.send(ComStmtExecutePacket(statementId, params)) *>
                           socket.receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags)).flatMap {
                             case _: OKPacket      => ev.pure(ColumnsNumberPacket(0))
                             case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                             case result: ColumnsNumberPacket => ev.pure(result)
                           }
          columnDefinitions <-
            repeatProcess(columnCount.size, ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags))
          resultSetRow <- readUntilEOF[BinaryProtocolResultSetRowPacket](
                            BinaryProtocolResultSetRowPacket.decoder(initialPacket.capabilityFlags, columnDefinitions),
                            Vector.empty
                          )
        yield new ResultSet:
          override def columns: Vector[ColumnDefinitionPacket] = columnDefinitions
          override def rows:    Vector[ResultSetRowPacket]     = resultSetRow
      }

    override def executeUpdate(): F[Int] =
      exchange[F, Int]("statement") { (span: Span[F]) =>
        params.get.flatMap { params =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
              Attribute("execute", "update")
            ))*
          ) *>
            resetSequenceId *>
            socket.send(ComStmtExecutePacket(statementId, params)) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case result: OKPacket => ev.pure(result.affectedRows)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new MySQLException("Unexpected EOF packet"))
            }
        }
      }

    override def returningAutoGeneratedKey(): F[Int] =
      exchange[F, Int]("statement") { (span: Span[F]) =>
        params.get.flatMap { params =>
          span.addAttributes(
            (attributes ++ List(
              Attribute("params", params.map((_, param) => param.toString).mkString(", ")),
              Attribute("execute", "returning.update")
            ))*
          ) *>
            resetSequenceId *>
            socket.send(ComStmtExecutePacket(statementId, params)) *>
            socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
              case result: OKPacket => ev.pure(result.lastInsertId)
              case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
              case _: EOFPacket     => ev.raiseError(new MySQLException("Unexpected EOF packet"))
            }
        }
      }

    override def close(): F[Unit] =
      exchange[F, Unit]("statement") { (span: Span[F]) =>
        span.addAttributes(
          (attributes ++ List(Attribute("execute", "close"), Attribute("statementId", statementId)))*
        ) *> resetSequenceId *> socket.send(ComStmtClosePacket(statementId))
      }
