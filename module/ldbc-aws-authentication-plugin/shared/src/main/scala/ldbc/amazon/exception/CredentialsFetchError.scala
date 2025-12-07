/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.exception

/**
 * Exception thrown when AWS credentials cannot be fetched from any available credential provider.
 * 
 * This exception represents a high-level failure in the AWS credentials resolution process and
 * is typically thrown when:
 * - All configured credential providers have failed to provide valid credentials
 * - The credential provider chain has been exhausted without success
 * - Network connectivity issues prevent access to credential sources
 * - Authentication tokens have expired and cannot be refreshed
 * - Required environment variables or configuration files are missing
 * 
 * Common credential sources that may fail:
 * - **Environment variables**: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`
 * - **AWS credentials file**: `~/.aws/credentials`
 * - **IAM instance profiles**: EC2 metadata service
 * - **Web Identity Tokens**: EKS service account tokens, OIDC providers
 * - **AWS STS**: AssumeRole, AssumeRoleWithWebIdentity operations
 * - **Container credentials**: ECS task metadata endpoints
 * 
 * This exception typically indicates that the application cannot authenticate with AWS services
 * and will likely be unable to access any AWS resources until the credential issue is resolved.
 * 
 * Troubleshooting steps:
 * 1. Verify AWS configuration and credentials
 * 2. Check network connectivity to AWS endpoints
 * 3. Validate IAM permissions and trust relationships
 * 4. Ensure credential files and environment variables are correctly set
 * 5. Check for expired tokens or certificates
 * 
 * Example scenarios:
 * - EKS pod without proper IRSA configuration
 * - EC2 instance without IAM instance profile
 * - Local development environment with missing AWS configuration
 * - Network policies blocking access to AWS credential endpoints
 * 
 * @param message The detailed error message describing why credential fetching failed,
 *                including information about which credential sources were attempted
 *                and what specific failures occurred
 */
class CredentialsFetchError(message: String) extends Exception:

  /**
   * Returns the error message for this exception.
   * 
   * The message typically includes details about:
   * - Which credential providers were attempted
   * - The specific failure reason for each provider
   * - Suggested remediation steps
   * - Environment or configuration context
   * 
   * @return The detailed error message describing the credential fetch failure
   */
  override def getMessage: String = message
