/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown when one or more client info properties could not be set on
 * a connection.
 */
class SQLClientInfoException(
  message: String,
  vendor:  String = "SQL"
) extends SQLException(message, vendor = vendor)
