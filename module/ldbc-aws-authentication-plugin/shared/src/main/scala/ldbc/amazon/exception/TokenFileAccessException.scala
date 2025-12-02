/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Thrown when the Web Identity Token file cannot be accessed due to permissions or I/O issues.
 * 
 * This exception is typically thrown when:
 * - The file exists but cannot be read due to insufficient permissions
 * - I/O errors occur while reading the token file
 * - File system issues prevent access to the token file
 * 
 * Common solutions:
 * - Verify file permissions (typically 600 for token files)
 * - Check if the process has read access to the file
 * - Ensure the file system is mounted and accessible
 * 
 * @param message The detailed error message
 * @param cause The underlying cause of the exception (optional)
 */
class TokenFileAccessException(
  message: String, 
  cause: Option[Throwable] = None
) extends WebIdentityTokenException(message, cause):

  /**
   * Constructor with cause
   */
  def this(message: String, cause: Throwable) =
    this(message, Some(cause))