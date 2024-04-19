package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown in situations where a
 * previously failed operation might be able to succeed if the application performs
 *  some recovery steps and retries the entire transaction or in the case of a
 * distributed transaction, the transaction branch.  At a minimum,
 * the recovery operation must include closing the current connection and getting
 * a new connection.
 */
class SQLRecoverableException(
                               sqlState: String,
                               vendorCode: Int,
                               message:          String,
                               sql:              Option[String] = None,
                               detail:           Option[String] = None,
                               hint:             Option[String] = None,
                               originatedPacket: Option[String] = None
                             ) extends SQLException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
