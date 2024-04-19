package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>23</i>', or under vendor-specified conditions.
 * This indicates that an integrity
 * constraint (foreign key, primary key or unique key) has been violated.
 * <p>
 * Please consult your driver vendor documentation for the vendor-specified
 * conditions for which this <code>Exception</code> may be thrown.
 */
class SQLIntegrityConstraintViolationException(
                                                sqlState: String,
                                                vendorCode: Int,
                                                message:          String,
                                                sql:              Option[String] = None,
                                                detail:           Option[String] = None,
                                                hint:             Option[String] = None,
                                                originatedPacket: Option[String] = None
                                              ) extends SQLNonTransientException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
