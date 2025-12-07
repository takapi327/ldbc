/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Base exception for AWS SDK client-side errors.
 * 
 * This exception is thrown when errors occur on the client side, such as:
 * - Invalid configuration
 * - Network connectivity issues
 * - Authentication failures
 * - Missing required resources
 * 
 * @param message A descriptive error message explaining the cause of the exception
 */
class SdkClientException(message: String) extends RuntimeException:

  /**
   * Returns the error message for this exception.
   * 
   * @return The descriptive error message
   */
  override def getMessage: String = message
