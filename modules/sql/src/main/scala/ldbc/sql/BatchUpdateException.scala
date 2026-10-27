/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.sql

/**
 * The subclass of [[SQLException]] thrown when an error occurs during a batch update. In addition to
 * the standard diagnostics it carries the update counts for the commands that completed successfully
 * before the failure, in the order they were added to the batch.
 */
class BatchUpdateException(
  message:      String,
  updateCounts: List[Long],
  sqlState:     Option[String]    = None,
  vendorCode:   Option[Int]       = None,
  sql:          Option[String]    = None,
  detail:       Option[String]    = None,
  hint:         Option[String]    = None,
  vendor:       String            = "SQL",
  cause:        Option[Throwable] = None
) extends SQLException(message, sqlState, vendorCode, sql, detail, hint, vendor, cause):

  /** The per-command update counts, adding `error.updateCounts` to the standard telemetry attributes. */
  override def fields: List[Attribute[?]] =
    super.fields :+ Attribute("error.updateCounts", s"[${ updateCounts.mkString(",") }]")
