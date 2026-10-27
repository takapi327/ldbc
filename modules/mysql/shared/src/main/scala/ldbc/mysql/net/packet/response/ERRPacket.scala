/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.net.packet
package response

import scodec.*
import scodec.codecs.*

import ldbc.sql.{
  BatchUpdateException,
  SQLDataException,
  SQLException,
  SQLFeatureNotSupportedException,
  SQLIntegrityConstraintViolationException,
  SQLInvalidAuthorizationSpecException,
  SQLNonTransientConnectionException,
  SQLSyntaxErrorException,
  SQLTransactionRollbackException
}
import ldbc.sql.Attribute

import ldbc.mysql.data.*

/**
 * This packet signals that an error occurred.
 *
 * It contains a SQL state value if CLIENT_PROTOCOL_41 is enabled.
 *
 * Error texts cannot exceed MYSQL_ERRMSG_SIZE
 *
 * @param status
 *   Type: int<1>
 *   Name: header
 *   Description: 0xFF ERR packet header
 * @param errorCode
 *   Type: int<2>
 *   Name: error_code
 *   Description: error-code
 * @param sqlStateMarker
 *   Type: string<1>
 *   Name: sql_state_marker
 *   Description: # marker of the SQL state
 * @param sqlState
 *   Type: string<5>
 *   Name: sql_state
 *   Description: SQL state
 * @param errorMessage
 *   Type: string<EOF>
 *   Name: error_message
 *   Description: human readable error message
 */
case class ERRPacket(
  status:         Int,
  errorCode:      Int,
  sqlStateMarker: Int,
  sqlState:       Option[String],
  errorMessage:   String
) extends GenericResponsePackets:

  override def toString: String = "ERR_Packet"

  def attributes: List[Attribute[?]] =
    val errorType = sqlState.map(SQLState.classOf) match
      case Some(SQLState.CONNECTION_EXCEPTION)           => "NonTransientConnectionException"
      case Some(SQLState.DATA_EXCEPTION)                 => "DataException"
      case Some(SQLState.INVALID_AUTHORIZATION_SPEC)     => "InvalidAuthorizationSpecException"
      case Some(SQLState.INTEGRITY_CONSTRAINT_VIOLATION) => "IntegrityConstraintViolationException"
      case Some(SQLState.TRANSACTION_ROLLBACK)           => "TransactionRollbackException"
      case Some(SQLState.SYNTAX_ERROR)                   => "SyntaxErrorException"
      case Some(SQLState.FEATURE_NOT_SUPPORTED)          => "FeatureNotSupportedException"
      case _                                             => "SQLException"
    List(
      Attribute[String]("error.type", errorType),
      Attribute[Long]("db.response.status_code", errorCode.toLong)
    )

  /**
   * Maps the server's SQLSTATE onto the matching [[ldbc.sql.SQLException]] subclass.
   *
   * The match is on the SQLSTATE *class* — its first two characters — not the whole string. JDBC
   * defines each subclass by class value, so `08000` and `08S01` are both connection errors; matching
   * the full five characters would quietly demote every state whose subclass is not `000` to a bare
   * `SQLException`, and MySQL does send such states (`08S01` for its network errors, for one).
   *
   * Class `08` is reported as non-transient. The state cannot say whether a retry would help, so the
   * caller has to decide, and nothing is known here about why the server gave up on the connection.
   * Failures ldbc raises itself are transient where that is actually known to be true.
   */
  def toException(
    sql:    Option[String],
    detail: Option[String]
  ): ldbc.sql.SQLException =
    sqlState.map(SQLState.classOf) match
      case Some(SQLState.CONNECTION_EXCEPTION) =>
        SQLNonTransientConnectionException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(SQLState.DATA_EXCEPTION) =>
        SQLDataException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(SQLState.INVALID_AUTHORIZATION_SPEC) =>
        SQLInvalidAuthorizationSpecException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(SQLState.INTEGRITY_CONSTRAINT_VIOLATION) =>
        SQLIntegrityConstraintViolationException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(SQLState.TRANSACTION_ROLLBACK) =>
        SQLTransactionRollbackException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(SQLState.SYNTAX_ERROR) =>
        SQLSyntaxErrorException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(SQLState.FEATURE_NOT_SUPPORTED) =>
        SQLFeatureNotSupportedException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case Some(_) =>
        SQLException(
          message    = errorMessage,
          sqlState   = sqlState,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )
      case None =>
        SQLException(
          message    = errorMessage,
          vendorCode = Some(errorCode),
          sql        = sql,
          detail     = detail,
          vendor     = "MySQL"
        )

  def toException: ldbc.sql.SQLException = toException(None, None)

  def toException(message: String): ldbc.sql.SQLException = toException(None, Some(message))

  def toException(message: String, sql: String): ldbc.sql.SQLException = toException(Some(sql), Some(message))

  def toException(message: String, updateCounts: Vector[Long]): ldbc.sql.SQLException = BatchUpdateException(
    message      = errorMessage,
    updateCounts = updateCounts.toList,
    sqlState     = sqlState,
    vendorCode   = Some(errorCode),
    detail       = Some(message),
    vendor       = "MySQL"
  )

object ERRPacket:

  val STATUS = 0xff

  def decoder(capabilityFlags: Set[CapabilitiesFlags]): Decoder[ERRPacket] =
    val hasClientProtocol41Flag = capabilityFlags.contains(CapabilitiesFlags.CLIENT_PROTOCOL_41)
    for
      errorCode      <- uint16L
      sqlStateMarker <- if hasClientProtocol41Flag then uint8 else provide(0)
      sqlState       <- if hasClientProtocol41Flag then bytes(5).map(v => Some(v.decodeUtf8Lenient)) else provide(None)
      errorMessage   <- bytes
    yield ERRPacket(
      STATUS,
      errorCode,
      sqlStateMarker,
      sqlState,
      errorMessage.decodeUtf8Lenient
    )
