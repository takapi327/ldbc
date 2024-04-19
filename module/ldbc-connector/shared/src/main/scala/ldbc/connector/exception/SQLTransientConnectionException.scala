package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} for the SQLState class
 * value '<i>08</i>', or under vendor-specified conditions.  This indicates
 * that the connection operation that failed might be able to succeed if
 * the operation is retried without any application-level changes.
 */
class SQLTransientConnectionException(
                                       sqlState: String,
                                       vendorCode: Int,
                                       message:          String,
                                       sql:              Option[String] = None,
                                       detail:           Option[String] = None,
                                       hint:             Option[String] = None,
                                       originatedPacket: Option[String] = None
                                     ) extends SQLTransientException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
