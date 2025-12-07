/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Exception thrown when a Web Identity Token is invalid, malformed, or cannot be processed.
 * 
 * This exception is typically thrown during token validation when:
 * - The JWT token does not have the correct format (header.payload.signature)
 * - The token file is empty or contains only whitespace
 * - The token contains invalid characters or encoding issues
 * - The JWT structure is corrupted or missing required components
 * - Base64 decoding of JWT segments fails
 * - JSON parsing of JWT header or payload fails
 * 
 * Valid JWT token format (3 base64-encoded segments separated by dots):
 * ```
 * eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMyJ9.eyJpc3MiOiJodHRwczovL29pZGMuZWtzLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tL2lkLzEyMyIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0Om15LWFwcCJ9.signature
 * ```
 * 
 * Common scenarios that trigger this exception:
 * - Token file contains non-JWT content (e.g., HTML error page, plain text)
 * - Network issues resulted in partial token download
 * - Token rotation occurred mid-process leaving stale content
 * - File system corruption affecting the token file
 * 
 * This exception extends [[WebIdentityTokenException]] and inherits [[NoStackTrace]] behavior
 * for performance optimization during token validation workflows.
 * 
 * @param message The detailed error message describing the specific validation failure,
 *                including information about which part of the token validation failed
 */
class InvalidTokenException(
  message: String,
) extends WebIdentityTokenException(message)
