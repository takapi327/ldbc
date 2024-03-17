/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

case class EofException(
  bytesRequested: Int,
  bytesRead:      Int
) extends MySQLException(
    sql     = None,
    message = "EOF was reached on the network socket.",
    detail = Some(
      s"Attempt to read $bytesRequested byte(s) failed after $bytesRead bytes(s) were read, because the connection had closed."
    ),
    hint = Some(s"Discard this session and retry with a new one.")
  )
