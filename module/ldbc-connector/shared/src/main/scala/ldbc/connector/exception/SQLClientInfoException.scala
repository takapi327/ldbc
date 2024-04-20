/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

/**
 * The subclass of {@link SQLException} is thrown when one or more client info properties
 * could not be set on a <code>Connection</code>.  In addition to the information provided
 * by <code>SQLException</code>, a <code>SQLClientInfoException</code> provides a list of client info
 * properties that were not set.
 */
class SQLClientInfoException(
  message: String
) extends SQLException(message)
