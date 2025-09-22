/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.dsl.exception

class UnexpectedContinuation(
  message: String
) extends LdbcException(
    message,
    None,
    None
  ):

  override def title: String = "Unexpected Continuation Exception"
  override protected def width = 180
