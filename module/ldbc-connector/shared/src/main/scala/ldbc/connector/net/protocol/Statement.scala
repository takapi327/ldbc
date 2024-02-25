/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.protocol

import scala.collection.immutable.ListMap

import cats.*
import cats.syntax.all.*

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.{ Tracer, Span }

import ldbc.connector.net.PacketSocket
import ldbc.connector.net.packet.ResponsePacket
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.packet.request.*

/**
 * A statement is an object that represents a precompiled SQL statement.
 * 
 * @tparam F
 *   The effect type
 */
trait Statement[F[_]]:

  def executeQuery: F[List[ResultSetRowPacket]]

  def close(): F[Unit]

object Statement:

  def apply[F[_]: Exchange: Tracer](
    socket:          PacketSocket[F],
    initialPacket:   InitialPacket,
    sql:             String,
    resetSequenceId: F[Unit]
  )(using ev: MonadError[F, Throwable]): Statement[F] =
    new Statement[F]:

      private def repeatProcess[P <: ResponsePacket](times: Int, decoder: scodec.Decoder[P]): F[List[P]] =
        def read(remaining: Int, acc: List[P]): F[List[P]] =
          if remaining <= 0 then ev.pure(acc)
          else socket.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

        read(times, List.empty[P])

      private def readUntilEOF[P <: ResponsePacket](
        decoder: scodec.Decoder[P | EOFPacket | ERRPacket],
        acc:     List[P]
      ): F[List[P]] =
        socket.receive(decoder).flatMap {
          case _: EOFPacket     => ev.pure(acc)
          case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query"))
          case row              => readUntilEOF(decoder, acc :+ row.asInstanceOf[P])
        }

      override def executeQuery: F[List[ResultSetRowPacket]] =
        exchange[F, List[ResultSetRowPacket]]("statement") { (span: Span[F]) =>
          span.addAttribute(Attribute("sql", sql)) *> resetSequenceId *> (
            for
              columnCount <- socket.send(ComQueryPacket(sql, initialPacket.capabilityFlags, ListMap.empty)) *>
                               socket.receive(ColumnsNumberPacket.decoder(initialPacket.capabilityFlags)).flatMap {
                                 case error: ERRPacket => ev.raiseError(error.toException("Failed to execute query"))
                                 case result: ColumnsNumberPacket => ev.pure(result)
                               }
              columns <- repeatProcess(columnCount.size, ColumnDefinitionPacket.decoder(initialPacket.capabilityFlags))
              resultSetRow <- readUntilEOF[ResultSetRowPacket](
                                ResultSetRowPacket.decoder(initialPacket.capabilityFlags, columns),
                                Nil
                              )
            yield resultSetRow
          )
        }

      override def close(): F[Unit] = ev.unit
