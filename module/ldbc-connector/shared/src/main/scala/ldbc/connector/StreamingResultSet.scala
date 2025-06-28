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
        currentRow = resultSetRow.lift(currentCursor)
      }

  /**
   * Closes the prepared statement on the server side.
   * Sets the completion flag to indicate no more rows are available.
   *
   * @return an F[Boolean] that always returns true upon successful closure
   */
  private def closeStmt: F[Boolean] =
    protocol.send(ComStmtClosePacket(statementId)).map { _ =>
      isCompleteAllFetch = true
      true
    }

  /**
   * Determines if the cursor is at the end of the current batch.
   * Used to decide when a new batch fetch is required.
   *
   * @param size the fetch size configured for this result set
   * @return true if at the end of the current batch, false otherwise
   */
  private def isAtBatchEnd(size: Int): Boolean = 
    size == currentCursor + 1

  /**
   * Moves the cursor to the next row within the current batch.
   * Updates the current row reference and increments the cursor position.
   *
   * @return an F[Boolean] that always returns true
   */
  private def moveToNextRowInBatch(): F[Boolean] =
    currentRow = rows.lift(currentCursor)
    currentCursor = currentCursor + 1
    ev.pure(true)

  /**
   * Performs the initial fetch operation when no rows are currently loaded.
   * Used when the result set is first accessed.
   *
   * @param size the number of rows to fetch
   * @return an F[Boolean] that always returns true after successful fetch
   */
  private def performInitialFetch(size: Int): F[Boolean] =
    fetchRow(size) *> ev.pure(true)

  /**
   * Fetches the next batch of rows from the server.
   * If the returned batch is smaller than requested, closes the statement.
   *
   * @param size the number of rows to fetch
   * @return an F[Boolean] indicating whether more rows are available
   */
  private def fetchNextBatch(size: Int): F[Boolean] =
    fetchRow(size).flatMap { _ =>
      if rows.length == size then
        ev.pure(true)
      else 
        protocol.resetSequenceId *> closeStmt
    }

  override def next(): F[Boolean] =
    checkClosed() *> fetchSize.get.flatMap { size =>
      if isCompleteAllFetch then
        currentCursor = 0
        ev.pure(false)
      else if rows.isEmpty then
        performInitialFetch(size)
      else if isAtBatchEnd(size) then
        fetchNextBatch(size)
      else
        moveToNextRowInBatch()
    }
