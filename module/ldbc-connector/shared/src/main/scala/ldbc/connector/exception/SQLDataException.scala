package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>22</i>', or under vendor-specified conditions.  This indicates
 * various data errors, including but not limited to data conversion errors,
 * division by 0, and invalid arguments to functions.
 * <p>
 * Please consult your driver vendor documentation for the vendor-specified
 * conditions for which this <code>Exception</code> may be thrown.
 */
class SQLDataException(
                        sqlState: String,
                        vendorCode: Int,
                        message:          String,
                        sql:              Option[String] = None,
                        detail:           Option[String] = None,
                        hint:             Option[String] = None,
                        originatedPacket: Option[String] = None
                      ) extends SQLNonTransientException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
