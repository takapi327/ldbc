/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Exception thrown when the Web Identity Token file cannot be found at the specified location.
 * 
 * This exception is typically thrown in the following scenarios:
 * - The file path specified in `AWS_WEB_IDENTITY_TOKEN_FILE` environment variable does not exist
 * - The token file has been moved, deleted, or renamed after the application started
 * - The file path is incorrectly configured in the environment
 * - Directory structure changes that affect the token file location
 * - Container restart scenarios where mounted volumes are not yet available
 * 
 * Common environments where this occurs:
 * - **EKS with IRSA (IAM Roles for Service Accounts)**: Token files are mounted by the EKS service
 * - **Fargate**: Token files provided via task metadata endpoint
 * - **Local development**: When mimicking AWS environments with local token files
 * 
 * Example usage in EKS/IRSA:
 * ```
 * AWS_WEB_IDENTITY_TOKEN_FILE=/var/run/secrets/eks.amazonaws.com/serviceaccount/token
 * AWS_ROLE_ARN=arn:aws:iam::123456789012:role/my-service-role
 * ```
 * 
 * This exception extends [[WebIdentityTokenException]] and provides enhanced error messages
 * that include the attempted file path when available, making debugging easier.
 * 
 * @param message The detailed error message describing the file lookup failure
 * @param tokenFilePath The path to the missing token file (optional). When provided, this path
 *                      will be included in the error message returned by [[getMessage]]
 * @param cause The underlying cause of the exception (optional). Typically a file system
 *              related exception such as `FileNotFoundException` or access-related errors
 */
class TokenFileNotFoundException(
  message:       String,
  tokenFilePath: Option[String]    = None,
  cause:         Option[Throwable] = None
) extends WebIdentityTokenException(message, cause):

  /**
   * Constructor with cause only.
   * 
   * Use this constructor when the underlying file system exception is available but the
   * specific file path should be extracted from the exception or is not relevant.
   * 
   * @param message The detailed error message describing the file lookup failure
   * @param cause The underlying file system exception that caused this failure
   */
  def this(message: String, cause: Throwable) =
    this(message, None, Some(cause))

  /**
   * Constructor with token file path only.
   * 
   * Use this constructor when you have the specific file path that failed but no underlying
   * exception (e.g., when programmatically checking file existence).
   * 
   * @param message The detailed error message describing the file lookup failure
   * @param tokenFilePath The full path to the token file that was not found
   */
  def this(message: String, tokenFilePath: String) =
    this(message, Some(tokenFilePath), None)

  /**
   * Constructor with both token file path and cause.
   * 
   * This is the most comprehensive constructor, providing both the file path context
   * and the underlying exception for complete error traceability.
   * 
   * @param message The detailed error message describing the file lookup failure
   * @param tokenFilePath The full path to the token file that was not found
   * @param cause The underlying file system exception that caused this failure
   */
  def this(message: String, tokenFilePath: String, cause: Throwable) =
    this(message, Some(tokenFilePath), Some(cause))

  /**
   * Returns the error message for this exception, including the token file path when available.
   * 
   * This method enhances the base error message by appending the token file path when it
   * was provided during exception construction, making it easier to identify which specific
   * file could not be found.
   * 
   * @return The enhanced error message that includes the file path context when available
   */
  override def getMessage: String =
    tokenFilePath match
      case Some(path) => s"$message (Token file path: $path)"
      case None       => message
