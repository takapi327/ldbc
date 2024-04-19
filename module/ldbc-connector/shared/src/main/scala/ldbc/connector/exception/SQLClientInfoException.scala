package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} is thrown when one or more client info properties
 * could not be set on a <code>Connection</code>.  In addition to the information provided
 * by <code>SQLException</code>, a <code>SQLClientInfoException</code> provides a list of client info
 * properties that were not set.
 */
class SQLClientInfoException(
  sqlState: String,
  vendorCode: Int,
  message:          String,
) extends SQLException(sqlState, vendorCode, message)
