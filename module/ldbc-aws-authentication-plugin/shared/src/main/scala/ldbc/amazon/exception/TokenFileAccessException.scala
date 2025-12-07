/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Exception thrown when the Web Identity Token file exists but cannot be accessed due to 
 * permissions, I/O errors, or other file system issues.
 * 
 * This exception is typically thrown in the following scenarios:
 * - The file exists but cannot be read due to insufficient file system permissions
 * - I/O errors occur while reading the token file (disk errors, network mount issues)
 * - File system issues prevent access (read-only file systems, corrupted file systems)
 * - File is locked by another process or application
 * - Security policy restrictions prevent file access (SELinux, AppArmor, etc.)
 * - File descriptor limits are exceeded
 * 
 * Common environments and solutions:
 * 
 * **Container environments (Docker, Kubernetes):**
 * - Ensure the container user has read access to mounted token files
 * - Verify that volume mounts are configured correctly
 * - Check that security contexts allow file access
 * 
 * **File permissions:**
 * - Token files typically need 600 permissions (read for owner only)
 * - Verify the process owner matches the file owner or has appropriate group access
 * - Check parent directory permissions (must be executable to access files within)
 * 
 * **File system issues:**
 * - Ensure file systems are mounted correctly (especially network mounts)
 * - Verify disk space and inode availability
 * - Check for file system corruption or read-only states
 * 
 * Example debugging steps:
 * ```bash
 * # Check file permissions
 * ls -la /var/run/secrets/eks.amazonaws.com/serviceaccount/token
 * 
 * # Test file access as the application user
 * sudo -u app-user cat /var/run/secrets/eks.amazonaws.com/serviceaccount/token
 * 
 * # Check file system mount status
 * mount | grep /var/run/secrets
 * ```
 * 
 * This exception extends [[WebIdentityTokenException]] and inherits [[NoStackTrace]] behavior
 * to avoid performance overhead during frequent access attempts.
 * 
 * @param message The detailed error message describing the specific access failure,
 *                including file path and permission details when available
 * @param cause The underlying cause of the exception (optional). Common causes include
 *              `AccessDeniedException`, `IOException`, `SecurityException`, or other
 *              file system related exceptions
 */
class TokenFileAccessException(
  message: String,
  cause:   Option[Throwable] = None
) extends WebIdentityTokenException(message, cause):

  /**
   * Alternative constructor that accepts a required cause parameter.
   * 
   * This constructor is preferred when the underlying file system exception provides
   * valuable diagnostic information that should be preserved for troubleshooting
   * access issues.
   * 
   * @param message The detailed error message describing the access failure
   * @param cause The underlying file system exception that caused this access failure
   *              (e.g., `AccessDeniedException`, `IOException`, `SecurityException`)
   */
  def this(message: String, cause: Throwable) =
    this(message, Some(cause))
