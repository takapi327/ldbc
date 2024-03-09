/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

class MySQLException(
  message:          String,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  originatedPacket: Option[String] = None
) extends Exception(message):

  override def getMessage: String =
    s"message: $message${ sql.fold("")(s => s", sql: $s") }${ detail.fold("")(d => s", detail: $d") }${ hint
        .fold("")(h => s", hint: $h") }${ originatedPacket.fold("")(p => s", point of origin: $p") }"

  def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)

    sql.foreach(a => builder += Attribute("error.sql", a))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))
    originatedPacket.foreach(packet => builder += Attribute("error.originatedPacket", packet))

    builder.result()
