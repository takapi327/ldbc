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

trait PreparedStatement[F[_]] extends Statement[F]:

  def params: Ref[F, ListMap[Int, Parameter]]

  def setNull(index: Int): F[Unit] =
    params.update(_ + (index -> Parameter.none))

  def setBoolean(index: Int, value: Boolean): F[Unit] =
    params.update(_ + (index -> Parameter.boolean(value)))

  def setBoolean(index: Int, value: Option[Boolean]): F[Unit] =
    value match
      case Some(value) => setBoolean(index, value)
      case None        => setNull(index)

  def setByte(index: Int, value: Byte): F[Unit] =
    params.update(_ + (index -> Parameter.byte(value)))

  def setByte(index: Int, value: Option[Byte]): F[Unit] =
    value match
      case Some(value) => setByte(index, value)
      case None        => setNull(index)

  def setShort(index: Int, value: Short): F[Unit] =
    params.update(_ + (index -> Parameter.short(value)))

  def setShort(index: Int, value: Option[Short]): F[Unit] =
    value match
      case Some(value) => setShort(index, value)
      case None        => setNull(index)

  def setInt(index: Int, value: Int): F[Unit] =
    params.update(_ + (index -> Parameter.int(value)))

  def setInt(index: Int, value: Option[Int]): F[Unit] =
    value match
      case Some(value) => setInt(index, value)
      case None        => setNull(index)

  def setLong(index: Int, value: Long): F[Unit] =
    params.update(_ + (index -> Parameter.long(value)))

  def setLong(index: Int, value: Option[Long]): F[Unit] =
    value match
      case Some(value) => setLong(index, value)
      case None        => setNull(index)

  def setBigInt(index: Int, value: BigInt): F[Unit] =
    params.update(_ + (index -> Parameter.bigInt(value)))

  def setBigInt(index: Int, value: Option[BigInt]): F[Unit] =
    value match
      case Some(value) => setBigInt(index, value)
      case None        => setNull(index)

  def setFloat(index: Int, value: Float): F[Unit] =
    params.update(_ + (index -> Parameter.float(value)))

  def setFloat(index: Int, value: Option[Float]): F[Unit] =
    value match
      case Some(value) => setFloat(index, value)
      case None        => setNull(index)

  def setDouble(index: Int, value: Double): F[Unit] =
    params.update(_ + (index -> Parameter.double(value)))

  def setDouble(index: Int, value: Option[Double]): F[Unit] =
    value match
      case Some(value) => setDouble(index, value)
      case None        => setNull(index)

  def setBigDecimal(index: Int, value: BigDecimal): F[Unit] =
    params.update(_ + (index -> Parameter.bigDecimal(value)))

  def setBigDecimal(index: Int, value: Option[BigDecimal]): F[Unit] =
    value match
      case Some(value) => setBigDecimal(index, value)
      case None        => setNull(index)

  def setString(index: Int, value: String): F[Unit] =
    params.update(_ + (index -> Parameter.string(value)))

  def setString(index: Int, value: Option[String]): F[Unit] =
    value match
      case Some(value) => setString(index, value)
      case None        => setNull(index)

  def setBytes(index: Int, value: Array[Byte]): F[Unit] =
    params.update(_ + (index -> Parameter.bytes(value)))

  def setBytes(index: Int, value: Option[Array[Byte]]): F[Unit] =
    value match
      case Some(value) => setBytes(index, value)
      case None        => setNull(index)

  def setTime(index: Int, value: LocalTime): F[Unit] =
    params.update(_ + (index -> Parameter.time(value)))

  def setTime(index: Int, value: Option[LocalTime]): F[Unit] =
    value match
      case Some(value) => setTime(index, value)
      case None        => setNull(index)

  def setDate(index: Int, value: LocalDate): F[Unit] =
    params.update(_ + (index -> Parameter.date(value)))

  def setDate(index: Int, value: Option[LocalDate]): F[Unit] =
    value match
      case Some(value) => setDate(index, value)
      case None        => setNull(index)

  def setDateTime(index: Int, value: LocalDateTime): F[Unit] =
    params.update(_ + (index -> Parameter.datetime(value)))

  def setDateTime(index: Int, value: Option[LocalDateTime]): F[Unit] =
    value match
      case Some(value) => setDateTime(index, value)
      case None        => setNull(index)

  def setYear(index: Int, value: Year): F[Unit] =
    params.update(_ + (index -> Parameter.year(value)))

  def setYear(index: Int, value: Option[Year]): F[Unit] =
    value match
      case Some(value) => setYear(index, value)
      case None        => setNull(index)

object PreparedStatement:

  case class Client[F[_]: Exchange: Tracer](
    socket:          PacketSocket[F],
    initialPacket:   InitialPacket,
    sql:             String,
    params:          Ref[F, ListMap[Int, Parameter]],
    resetSequenceId: F[Unit]
  )(using ev: MonadError[F, Throwable])
    extends PreparedStatement[F]:

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
      exchange[F, ResultSet]("client.prepared.statement.execute.query") { (span: Span[F]) =>
        span.addAttribute(Attribute("sql", sql)) *>
          resetSequenceId *> (
            for
              params <- params.get
              columnCount <-
                socket.send(ComQueryPacket(buildQuery(params), initialPacket.capabilityFlags, ListMap.empty)) *>
                  socket.receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags))
              resultSet <- (
                             columnCount match
                               case _: OKPacket =>
                                 ev.pure(
                                   new ResultSet:
                                     override def columns: Vector[ColumnDefinitionPacket] = Vector.empty
                                     override def rows:    Vector[ResultSetRowPacket]     = Vector.empty
                                 )
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
                           )
            yield resultSet
          )
      }

    override def executeUpdate(): F[Int] =
      exchange[F, Int]("client.prepared.statement.execute.update") { (span: Span[F]) =>
        span.addAttribute(Attribute("sql", sql)) *> resetSequenceId *>
          params.get.flatMap(values =>
            socket.send(ComQueryPacket(buildQuery(values), initialPacket.capabilityFlags, ListMap.empty)) *>
              socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
                case result: OKPacket => ev.pure(result.affectedRows)
                case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                case _: EOFPacket     => ev.raiseError(new MySQLException("Unexpected EOF packet"))
              }
          )
      }

    override def returningAutoGeneratedKey(): F[Int] =
      exchange[F, Int]("client.prepared.statement.execute.returning.update") { (span: Span[F]) =>
        span.addAttribute(Attribute("sql", sql)) *> resetSequenceId *>
          params.get.flatMap(values =>
            socket.send(ComQueryPacket(buildQuery(values), initialPacket.capabilityFlags, ListMap.empty)) *>
              socket.receive(GenericResponsePackets.decoder(initialPacket.capabilityFlags)).flatMap {
                case result: OKPacket => ev.pure(result.lastInsertId)
                case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query", sql))
                case _: EOFPacket     => ev.raiseError(new MySQLException("Unexpected EOF packet"))
              }
          )
      }

    override def close(): F[Unit] = ev.unit
