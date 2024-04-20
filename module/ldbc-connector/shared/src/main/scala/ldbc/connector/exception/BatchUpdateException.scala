/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

/**
 * The subclass of {@link SQLException} thrown when an error
 * occurs during a batch update operation.  In addition to the
 * information provided by {@link SQLException}, a
 * <code>BatchUpdateException</code> provides the update
 * counts for all commands that were executed successfully during the
 * batch update, that is, all commands that were executed before the error
 * occurred.  The order of elements in an array of update counts
 * corresponds to the order in which commands were added to the batch.
 */
class BatchUpdateException(
  message:          String,
  updateCounts:     List[Int],
  sqlState:         Option[String] = None,
  vendorCode:       Option[Int]    = None,
  sql:              Option[String] = None,
  detail:           Option[String] = None,
  hint:             Option[String] = None,
  originatedPacket: Option[String] = None
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint, originatedPacket):

  override def getMessage: String =
    s"Message: $message, Update Counts: [${ updateCounts.mkString(",") }]${ sqlState
        .fold("")(s => s", SQLState: $s") }${ vendorCode.fold("")(c => s", Vendor Code: $c") }${ sql
        .fold("")(s => s", SQL: $s") }${ detail.fold("")(d => s", Detail: $d") }${ hint
        .fold("")(h => s", Hint: $h") }${ originatedPacket.fold("")(p => s", Point of Origin: $p") }"

  /**
   * Summarize error information into attributes.
   */
  override def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)
    builder += Attribute("error.updateCounts", s"[${ updateCounts.mkString(",") }]")

    sqlState.foreach(a => builder += Attribute("error.sqlstate", a))
    vendorCode.foreach(a => builder += Attribute("error.vendorCode", a.toLong))
    sql.foreach(a => builder += Attribute("error.sql", a))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))
    originatedPacket.foreach(packet => builder += Attribute("error.originatedPacket", packet))

    builder.result()
