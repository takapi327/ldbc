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
  statementId:        Long,
  columns:              Vector[ColumnDefinitionPacket],
  records:              Vector[ResultSetRowPacket],
  serverVariables:      Map[String, String],
  version:              Version,
  isClosed:             Ref[F, Boolean],
  fetchSize:            Long,
  useCursorFetch:       Boolean,
  useServerPrepStmts:   Boolean,
  resultSetType:        Int            = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int            = ResultSet.CONCUR_READ_ONLY,
  statement:            Option[String] = None
)(using ev: MonadThrow[F]) extends SharedResultSet[F]:

  override def next(): F[Boolean] =
    checkClosed() *> protocol.resetSequenceId *> protocol.send(ComStmtFetchPacket(statementId, fetchSize)) *>           
      protocol.readUntilEOF[BinaryProtocolResultSetRowPacket](
        BinaryProtocolResultSetRowPacket.decoder(protocol.initialPacket.capabilityFlags, columns)
      ).map { resultSetRow =>
        currentRow = resultSetRow.headOption
        currentCursor = currentCursor + 1
        resultSetRow.nonEmpty
      }
