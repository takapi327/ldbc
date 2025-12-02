/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Thrown when the Web Identity Token is invalid or malformed.
 * 
 * This exception is typically thrown when:
 * - The JWT token does not have the correct format (header.payload.signature)
 * - The token file is empty or contains only whitespace
 * - The token contains invalid characters or encoding
 * - The JWT structure is corrupted
 * 
 * Valid JWT token format:
 * ```
 * eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMyJ9.eyJpc3MiOiJodHRwczovL29pZGMuZWtzLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tL2lkLzEyMyIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0Om15LWFwcCJ9.signature
 * ```
 * 
 * @param message The detailed error message
 * @param cause The underlying cause of the exception (optional)
 */
class InvalidTokenException(
  message: String, 
  cause: Option[Throwable] = None
) extends WebIdentityTokenException(message, cause):

  /**
   * Constructor with cause
   */
  def this(message: String, cause: Throwable) =
    this(message, Some(cause))