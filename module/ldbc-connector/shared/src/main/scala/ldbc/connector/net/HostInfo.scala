/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

case class HostInfo(
  host:     String,
  port:     Int,
  user:     String,
  password: Option[String],
  database: Option[String]
):

  val url: String = s"jdbc:mysql://$host:$port" + database.map("/" + _).getOrElse("")
