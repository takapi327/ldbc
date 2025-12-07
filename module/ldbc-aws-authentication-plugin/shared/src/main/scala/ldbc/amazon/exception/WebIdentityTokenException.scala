/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Base exception for Web Identity Token operations.
 * 
 * @param message The error message
 */
abstract class WebIdentityTokenException(
  message: String,
) extends SdkClientException(message)
