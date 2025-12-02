/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Thrown when the Web Identity Token file cannot be found.
 * 
 * This exception is typically thrown when:
 * - The file path specified in AWS_WEB_IDENTITY_TOKEN_FILE does not exist
 * - The token file has been moved or deleted
 * - The file path is incorrectly configured
 * 
 * Example usage in EKS/IRSA:
 * ```
 * AWS_WEB_IDENTITY_TOKEN_FILE=/var/run/secrets/eks.amazonaws.com/serviceaccount/token
 * ```
 * 
 * @param message The detailed error message
 * @param tokenFilePath The path to the missing token file (optional)
 * @param cause The underlying cause of the exception (optional)
 */
class TokenFileNotFoundException(
  message: String,
  tokenFilePath: Option[String] = None,
  cause: Option[Throwable] = None
) extends WebIdentityTokenException(message, cause):

  /**
   * Constructor with cause only
   */
  def this(message: String, cause: Throwable) =
    this(message, None, Some(cause))

  /**
   * Constructor with token file path only
   */
  def this(message: String, tokenFilePath: String) =
    this(message, Some(tokenFilePath), None)

  /**
   * Constructor with both token file path and cause
   */
  def this(message: String, tokenFilePath: String, cause: Throwable) =
    this(message, Some(tokenFilePath), Some(cause))

  override def getMessage: String =
    tokenFilePath match
      case Some(path) => s"$message (Token file path: $path)"
      case None => message