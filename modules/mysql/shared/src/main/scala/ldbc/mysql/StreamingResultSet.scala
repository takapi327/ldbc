/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql

import ldbc.sql.ResultSet
import ldbc.sql.SQLTransientConnectionException

import ldbc.effect.{ Concurrent, Ref }
import ldbc.effect.syntax.*
import ldbc.mysql.data.ColumnValueDecoder
import ldbc.mysql.net.packet.request.*
import ldbc.mysql.net.packet.response.*
import ldbc.mysql.net.Protocol
import ldbc.mysql.util.Version

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
  decoder:              ColumnValueDecoder,
  resultSetType:        Int            = ResultSet.TYPE_FORWARD_ONLY,
  resultSetConcurrency: Int            = ResultSet.CONCUR_READ_ONLY,
  statement:            Option[String] = None
)(using concurrentF: Concurrent[F])
  extends SharedResultSet[F]:

  override protected def F: Concurrent[F] = concurrentF

  private var isCompleteAllFetch: Boolean                    = false
  private var rows:               Vector[ResultSetRowPacket] = Vector.empty

  /**
   * Fetches the specified number of rows from the database server.
   * Updates the internal rows collection and resets the cursor position.
   *
   * @param size the number of rows to fetch
   * @return an F[Unit] representing the fetch operation
   */
  private def fetchRow(size: Int): F[Unit] =
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
   * @return an F[Boolean] indicating whether rows were available in the final batch
   */
  private def closeStmt(): F[Boolean] =
    protocol.send(ComStmtClosePacket(statementId)).as(false)

  /**
   * Adds a transport check on top of the shared close check.
   *
   * This is the only result set that keeps talking to the server — [[next]] fetches further rows
   * with `COM_STMT_FETCH` — so it is the only one that must refuse to continue once the byte stream
   * position is unknown. The check lives here rather than in [[SharedResultSet]] because a plain
   * buffered result set needs no protocol at all, and requiring one there would make the shared
   * path depend on state it does not use.
   */
  override protected def checkClosed(): F[Unit] =
    protocol.transportFailed.flatMap {
      case true =>
        F.raiseError(
          new SQLTransientConnectionException(
            "No operations allowed: the connection's transport has failed.",
            sql    = statement,
            detail = Some("The byte stream position is unknown, so this session cannot be reused."),
            hint   = Some("Discard this connection and obtain a new one.")
          )
        )
      case false => super.checkClosed()
    }

  override def next(): F[Boolean] =
    checkClosed() *> fetchSize.get.flatMap { size =>
      if isCompleteAllFetch && currentCursor >= rows.length then protocol.resetSequenceId *> closeStmt()
      else if rows.isEmpty then fetchRow(size) *> next()
      else if currentCursor >= rows.length then fetchRow(size) *> next()
      else
        currentCursor = currentCursor + 1
        currentRow    = rows.lift(currentCursor - 1)
        F.pure(true)
    }
