package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value
 * is '<i>28</i>', or under vendor-specified conditions. This indicates that
 * the authorization credentials presented during connection establishment
 * are not valid.
 */
class SQLInvalidAuthorizationSpecException(
                                            sqlState: String,
                                            vendorCode: Int,
                                            message:          String,
                                            sql:              Option[String] = None,
                                            detail:           Option[String] = None,
                                            hint:             Option[String] = None,
                                            originatedPacket: Option[String] = None
                                          ) extends SQLNonTransientException(sqlState, vendorCode, message, sql, detail, hint, originatedPacket)
