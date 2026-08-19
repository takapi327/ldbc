/**
 * Copyright (c) 2023-2026 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.mysql.exception

import ldbc.sql.SQLException

/** Thrown when the network socket reaches EOF before a full MySQL packet could be read. */
case class EofException(
  bytesRequested: Int,
  bytesRead:      Int
) extends SQLException(
    message = "EOF was reached on the network socket.",
    detail = Some(
      s"Attempt to read $bytesRequested byte(s) failed after $bytesRead bytes(s) were read, because the connection had closed."
    ),
    hint   = Some("Discard this session and retry with a new one."),
    vendor = "MySQL"
  )
