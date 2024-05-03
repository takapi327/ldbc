/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import ldbc.connector.util.Version

/**
 * Represents various constants used in the driver.
 */
object Constants:

  val DRIVER_NAME:    String  = "MySQL Connector/L"
  val DRIVER_VERSION: Version = Version(0, 3, 0)
