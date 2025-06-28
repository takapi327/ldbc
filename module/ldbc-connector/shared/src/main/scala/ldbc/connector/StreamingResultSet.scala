/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.syntax.all.*
import cats.MonadThrow

import cats.effect.Ref

import ldbc.sql.ResultSet

import ldbc.connector.net.packet.request.*
import ldbc.connector.net.packet.response.*
import ldbc.connector.net.Protocol
import ldbc.connector.util.Version

private[ldbc] case class StreamingResultSet[F[_]](
  protocol:             Protocol[F],
  statementId:          Long,
  columns:              Vector[ColumnDefinitionPacket],
  records:              Vector[ResultSetRowPacket],
  serverVariables:      Map[String, String],
  version:              Version,
  isClosed:             Ref[F, Boolean],
  fetchSize:            Ref[F, Int],
  useCursorFetch:       Boolean,
  useServerPrepStmts:   Boolean,
  resultSetType:        Int            = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int            = ResultSet.CONCUR_READ_ONLY,
  statement:            Option[String] = None
)(using ev: MonadThrow[F])
  extends SharedResultSet[F]:

  private var isCompleteAllFetch: Boolean = false
  private var rows: Vector[ResultSetRowPacket] = Vector.empty

  private def fetchRow(size: Int): F[Unit] =
    protocol.resetSequenceId *> protocol.send(ComStmtFetchPacket(statementId, size)) *>
      protocol.readUntilEOF[BinaryProtocolResultSetRowPacket](
        BinaryProtocolResultSetRowPacket.decoder(protocol.initialPacket.capabilityFlags, columns)
      ).map { resultSetRow =>
        rows = resultSetRow
        currentCursor = 0
        currentRow = resultSetRow.lift(currentCursor)
      }

  private def closeStmt: F[Boolean] =
    protocol.send(ComStmtClosePacket(statementId)).map { _ =>
      isCompleteAllFetch = true
      true
    }

  override def next(): F[Boolean] =
    checkClosed() *> fetchSize.get.flatMap { size =>
      if isCompleteAllFetch then
        currentCursor = 0
        ev.pure(false)
      else
        if rows.isEmpty then
          fetchRow(size) *> ev.pure(true)
        else
          if size == currentCursor + 1 then
            fetchRow(size).flatMap { _ =>
              if rows.length == size then
                ev.pure(true)
              else protocol.resetSequenceId *> closeStmt
            }
          else
            currentRow = rows.lift(currentCursor)
            currentCursor = currentCursor + 1
            ev.pure(true)
    }
