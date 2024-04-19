package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} thrown when the SQLState class value is '<i>0A</i>'
 * ( the value is 'zero' A).
 *
 *<UL>
 *<LI>no support for an optional feature
 *<LI>no support for an optional overloaded method
 *<LI>no support for an optional mode for a method.  The mode for a method is
 *determined based on constants passed as parameter values to a method
 *</UL>
 */
class SQLFeatureNotSupportedException(
                                       message:          String,
                                       sqlState:         Option[String] = None,
                                       vendorCode:       Option[Int]    = None,
                                       sql:              Option[String] = None,
                                       detail:           Option[String] = None,
                                       hint:             Option[String] = None,
                                       originatedPacket: Option[String] = None
                                     ) extends SQLNonTransientException(message, sqlState, vendorCode, sql, detail, hint, originatedPacket)

