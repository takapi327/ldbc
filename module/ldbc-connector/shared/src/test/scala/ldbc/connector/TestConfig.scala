/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

object TestConfig:
  val host:     String = sys.env.getOrElse("MYSQL_HOST", "127.0.0.1")
  val port:     Int    = sys.env.getOrElse("MYSQL_PORT", "13306").toInt
  val user:     String = "ldbc"
  val password: String = "password"

  val version:         String  = sys.env.getOrElse("MYSQL_VERSION", "9.6.0")
  val majorVersion:    Int     = version.split('.').headOption.flatMap(_.toIntOption).getOrElse(9)
  val isMySql9OrLater: Boolean = majorVersion >= 9
