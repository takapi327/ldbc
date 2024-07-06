/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * <P>The subclass of {@link SQLException} thrown when the timeout specified by
 * {@code Statement.setQueryTimeout}, {@code DriverManager.setLoginTimeout},
 * {@code DataSource.setLoginTimeout},{@code XADataSource.setLoginTimeout}
 * has expired.
 * <P> This exception does not correspond to a standard SQLState.
 */
class SQLTimeoutException(
  message:    String,
  sqlState:   Option[String] = None,
  vendorCode: Option[Int]    = None,
  sql:        Option[String] = None,
  detail:     Option[String] = None,
  hint:       Option[String] = None
) extends SQLTransientException(message, sqlState, vendorCode, sql, detail, hint)
