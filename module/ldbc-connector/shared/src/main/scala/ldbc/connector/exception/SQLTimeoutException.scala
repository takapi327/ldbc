package ldbc.connector.exception

/**
 * <P>The subclass of {@link SQLException} thrown when the timeout specified by
 * {@code Statement.setQueryTimeout}, {@code DriverManager.setLoginTimeout},
 * {@code DataSource.setLoginTimeout},{@code XADataSource.setLoginTimeout}
 * has expired.
 * <P> This exception does not correspond to a standard SQLState.
 */
class SQLTimeoutException(
                           sqlState: String,
                           vendorCode: Int,
                           message:          String,
                           sql:              Option[String] = None,
                           detail:           Option[String] = None,
                           hint:             Option[String] = None,
                           originatedPacket: Option[String] = None
                         ) extends SQLTransientException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
