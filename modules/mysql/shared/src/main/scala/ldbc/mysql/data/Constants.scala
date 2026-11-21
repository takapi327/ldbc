/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.data

import ldbc.mysql.util.Version

/**
 * Represents various constants used in the driver.
 */
object Constants:

  val DRIVER_NAME:    String  = "MySQL Connector/L"
  val DRIVER_VERSION: Version = Version(0, 8, 0)
