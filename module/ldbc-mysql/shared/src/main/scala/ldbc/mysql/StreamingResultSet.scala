/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.sql.ResultSet

import ldbc.fx.{ Fx, Ref }
import ldbc.fx.syntax.*
import ldbc.mysql.data.ColumnValueDecoder
import ldbc.mysql.net.packet.request.*
import ldbc.mysql.net.packet.response.*
import ldbc.mysql.net.Protocol
import ldbc.mysql.util.Version

private[ldbc] case class StreamingResultSet(
  protocol:             Protocol,
  statementId:          Long,
  columns:              Vector[ColumnDefinitionPacket],
  records:              Vector[ResultSetRowPacket],
  serverVariables:      Map[String, String],
  version:              Version,
  isClosed:             Ref[Boolean],
  fetchSize:            Ref[Int],
  useCursorFetch:       Boolean,
  useServerPrepStmts:   Boolean,
  decoder:              ColumnValueDecoder,
  resultSetType:        Int            = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int            = ResultSet.CONCUR_READ_ONLY,
  statement:            Option[String] = None
) extends SharedResultSet:

  private var isCompleteAllFetch: Boolean                    = false
  private var rows:               Vector[ResultSetRowPacket] = Vector.empty

  /**
   * Fetches the specified number of rows from the database server.
   * Updates the internal rows collection and resets the cursor position.
   *
   * @param size the number of rows to fetch
   * @return an Fx[Unit] representing the fetch operation
   */
  private def fetchRow(size: Int): Fx[Unit] =
    protocol.resetSequenceId *> protocol.send(ComStmtFetchPacket(statementId, size)) *>
      protocol
        .readUntilEOF[ResultSetRowPacket](
          binaryResultSetRowDecoder(protocol.initialPacket.capabilityFlags)
        )
        .map { resultSetRow =>
          rows               = resultSetRow
          currentCursor      = 0
          currentRow         = None
          isCompleteAllFetch = resultSetRow.length < size
        }

  /**
   * Closes the prepared statement on the server side.
   * Sets the completion flag to indicate no more rows are available.
   *
   * @return an Fx[Boolean] indicating whether rows were available in the final batch
   */
  private def closeStmt(): Fx[Boolean] =
    protocol.send(ComStmtClosePacket(statementId)).as(false)

  override def next(): Fx[Boolean] =
    checkClosed() *> fetchSize.get.flatMap { size =>
      if isCompleteAllFetch && currentCursor >= rows.length then protocol.resetSequenceId *> closeStmt()
      else if rows.isEmpty then fetchRow(size) *> next()
      else if currentCursor >= rows.length then fetchRow(size) *> next()
      else
        currentCursor = currentCursor + 1
        currentRow    = rows.lift(currentCursor - 1)
        Fx.pure(true)
    }
