/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet
package request

import scala.collection.immutable.SortedMap

import scodec.*
import scodec.bits.*
import scodec.codecs.*
import scodec.interop.cats.*

import cats.syntax.all.*

import ldbc.sql.ResultSet
import ldbc.connector.data.*

/**
 * COM_STMT_EXECUTE asks the server to execute a prepared statement as identified by statement_id.
 *
 * It sends the values for the placeholders of the prepared statement (if it contained any) in Binary Protocol Value form.
 * The type of each parameter is made up of two bytes
 *
 * @param statementId
 *   The ID of the prepared statement to execute
 * @param params
 *   The parameters to bind to the prepared statement
 */
case class ComStmtExecutePacket(
  statementId: Long,
  params:      SortedMap[Int, Parameter],
  enumCursorType: ComStmtExecutePacket.EnumCursorType
) extends RequestPacket:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtExecutePacket.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_EXECUTE Request"

object ComStmtExecutePacket:

  def apply(
    statementId: Long,
    params:      SortedMap[Int, Parameter],
    resultSetType: Int,
    resultSetConcurrency: Int,
    useCursorFetch: Boolean
  ): ComStmtExecutePacket =
    val enumCursorType = (resultSetType, resultSetConcurrency, useCursorFetch) match {
      case (ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, false) => EnumCursorType.CURSOR_TYPE_NO_CURSOR
      case (ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, true) => EnumCursorType.CURSOR_TYPE_READ_ONLY
      case (ResultSet.TYPE_SCROLL_INSENSITIVE | ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY, _) => EnumCursorType.CURSOR_TYPE_NO_CURSOR
      case (ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE, _) => EnumCursorType.CURSOR_TYPE_NO_CURSOR
      case (ResultSet.TYPE_SCROLL_INSENSITIVE | ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, _) => EnumCursorType.CURSOR_TYPE_NO_CURSOR
      case _      => EnumCursorType.PARAMETER_COUNT_AVAILABLE
    }
    ComStmtExecutePacket(statementId, params, enumCursorType)

  val encoder: Encoder[ComStmtExecutePacket] = Encoder { comStmtExecute =>

    val types = comStmtExecute.params.values.foldLeft(BitVector.empty) { (acc, param) =>
      acc |+| uint24L.encode(param.columnDataType.code.toInt).require
    }

    val values = comStmtExecute.params.values.foldLeft(BitVector.empty) { (acc, param) =>
      acc |+| param.encode
    }

    val paramCount = comStmtExecute.params.size

    // Flag if parameters must be re-bound
    val newParamsBindFlag =
      if paramCount == 1 && comStmtExecute.params.values
          .map(_.columnDataType)
          .toSeq
          .contains(ColumnDataType.MYSQL_TYPE_NULL)
      then BitVector(0)
      else BitVector(1)

    Attempt.successful(
      BitVector(CommandId.COM_STMT_EXECUTE) |+|
        uint32L.encode(comStmtExecute.statementId).require |+|
        BitVector(comStmtExecute.enumCursorType.code) |+|
        uint32L.encode(1L).require |+|
        BitVector(paramCount) |+|
        nullBitmap(comStmtExecute.params.values.map(_.columnDataType).toList) |+|
        newParamsBindFlag |+|
        types |+|
        values
    )
  }

  // @see https://dev.mysql.com/doc/dev/mysql-server/latest/mysql__com_8h.html
  enum EnumCursorType(val code: Short):
    case CURSOR_TYPE_NO_CURSOR     extends EnumCursorType(0)
    case CURSOR_TYPE_READ_ONLY     extends EnumCursorType(1)
    case CURSOR_TYPE_FOR_UPDATE    extends EnumCursorType(2)
    case CURSOR_TYPE_SCROLLABLE    extends EnumCursorType(4)
    case PARAMETER_COUNT_AVAILABLE extends EnumCursorType(8)
