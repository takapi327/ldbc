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

  /**
   * Fetches the specified number of rows from the database server.
   * Updates the internal rows collection and resets the cursor position.
   *
   * @param size the number of rows to fetch
   * @return an F[Unit] representing the fetch operation
   */
  private def fetchRow(size: Int): F[Unit] =
    protocol.resetSequenceId *> protocol.send(ComStmtFetchPacket(statementId, size)) *>
      protocol.readUntilEOF[BinaryProtocolResultSetRowPacket](
        BinaryProtocolResultSetRowPacket.decoder(protocol.initialPacket.capabilityFlags, columns)
      ).map { resultSetRow =>
        rows = resultSetRow
        currentCursor = 0
        currentRow = None
        isCompleteAllFetch = resultSetRow.length < size
      }

  /**
   * Closes the prepared statement on the server side.
   * Sets the completion flag to indicate no more rows are available.
   *
   * @return an F[Boolean] indicating whether rows were available in the final batch
   */
  private def closeStmt(): F[Boolean] =
    protocol.send(ComStmtClosePacket(statementId)).as(false)

  override def next(): F[Boolean] =
    checkClosed() *> fetchSize.get.flatMap { size =>
      if isCompleteAllFetch && currentCursor >= rows.length then
        protocol.resetSequenceId *> closeStmt()
      else if rows.isEmpty then
        fetchRow(size) *> next()
      else if currentCursor >= rows.length then
        fetchRow(size) *> next()
      else
        currentCursor = currentCursor + 1
        currentRow = rows.lift(currentCursor - 1)
        ev.pure(true)
    }
