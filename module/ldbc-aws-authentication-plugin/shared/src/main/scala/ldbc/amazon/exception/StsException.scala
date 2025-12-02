/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

import scala.util.control.NoStackTrace

/**
 * Exception thrown when AWS STS (Security Token Service) operations fail.
 * 
 * This exception is typically thrown when:
 * - STS AssumeRoleWithWebIdentity request fails (HTTP 400/403/500 errors)
 * - Invalid or expired Web Identity Token
 * - IAM role doesn't exist or lacks required trust relationships
 * - Network connectivity issues to STS endpoints
 * - STS response parsing failures
 * 
 * Common STS error scenarios:
 * - HTTP 403: Token expired or invalid trust policy
 * - HTTP 400: Malformed request or invalid parameters
 * - HTTP 500: STS service temporary issues
 * 
 * Example STS endpoint:
 * ```
 * https://sts.us-east-1.amazonaws.com/
 * ```
 * 
 * @param message The detailed error message including STS response details
 * @param cause The underlying cause of the exception (optional)
 */
class StsException(
  message: String, 
  cause: Option[Throwable] = None
) extends SdkClientException(message) with NoStackTrace:

  // Set the cause if provided
  cause.foreach(initCause)

  /**
   * Constructor with cause
   */
  def this(message: String, cause: Throwable) =
    this(message, Some(cause))