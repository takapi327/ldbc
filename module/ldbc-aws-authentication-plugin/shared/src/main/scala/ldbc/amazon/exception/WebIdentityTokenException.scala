/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

import scala.util.control.NoStackTrace

/**
 * Base exception for Web Identity Token operations.
 * 
 * @param message The error message
 * @param cause The underlying cause (optional)
 */
abstract class WebIdentityTokenException(
  message: String,
  cause:   Option[Throwable] = None
) extends SdkClientException(message)
     with NoStackTrace:

  // Set the cause if provided
  cause.foreach(initCause)

  /**
   * Constructor with cause
   */
  def this(message: String, cause: Throwable) =
    this(message, Some(cause))
