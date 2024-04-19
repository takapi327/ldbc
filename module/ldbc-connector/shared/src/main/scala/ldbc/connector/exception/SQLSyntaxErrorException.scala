package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>42</i>', or under vendor-specified conditions. This indicates that the
 * in-progress query has violated SQL syntax rules.
 */
class SQLSyntaxErrorException(
                               sqlState: String,
                               vendorCode: Int,
                               message:          String,
                               sql:              Option[String] = None,
                               detail:           Option[String] = None,
                               hint:             Option[String] = None,
                               originatedPacket: Option[String] = None
                             ) extends SQLNonTransientException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
