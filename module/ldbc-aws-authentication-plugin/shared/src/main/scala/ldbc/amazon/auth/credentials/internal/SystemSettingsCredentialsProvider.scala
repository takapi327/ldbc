/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials.internal

import cats.syntax.all.*
import cats.MonadThrow

import ldbc.amazon.auth.credentials.*
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*
import ldbc.amazon.util.SdkSystemSetting

/**
 * Base trait for AWS credential providers that load credentials from system settings.
 * 
 * This trait provides a common foundation for credential providers that read AWS credentials
 * from system-level configuration sources such as environment variables and Java system properties.
 * It implements the standard AWS credential loading pattern with support for both basic
 * credentials (access key + secret key) and session credentials (+ session token).
 * 
 * The trait uses the AWS SDK system setting definitions to ensure consistency with
 * AWS SDK naming conventions and behavior. It supports the following credential types:
 * 
 * Basic credentials:
 * - Access Key ID (AWS_ACCESS_KEY_ID / aws.accessKeyId)
 * - Secret Access Key (AWS_SECRET_ACCESS_KEY / aws.secretAccessKey)
 * 
 * Session credentials (temporary credentials):
 * - Access Key ID (AWS_ACCESS_KEY_ID / aws.accessKeyId)
 * - Secret Access Key (AWS_SECRET_ACCESS_KEY / aws.secretAccessKey)
 * - Session Token (AWS_SESSION_TOKEN / aws.sessionToken)
 * 
 * Optional metadata:
 * - Account ID (AWS_ACCOUNT_ID / aws.accountId)
 * 
 * Implementing classes must provide:
 * 1. loadSetting method to read from their specific configuration source
 * 2. provider property to identify the credential source for metrics
 * 
 * Error handling:
 * - Missing access key or secret key results in SdkClientException
 * - Missing session token is acceptable (falls back to basic credentials)
 * - Account ID is optional for all credential types
 * 
 * @tparam F The effect type that supports error handling via MonadThrow
 */
trait SystemSettingsCredentialsProvider[F[_]](using ev: MonadThrow[F]) extends AwsCredentialsProvider[F]:

  /**
   * Resolves AWS credentials from system settings (environment variables or system properties).
   * 
   * This method implements the standard AWS credential loading logic used by both
   * environment variable and system property credential providers. It attempts to
   * load all required and optional credential components from the configured source.
   * 
   * Credential loading process:
   * 1. Load access key ID (required)
   * 2. Load secret access key (required) 
   * 3. Load session token (optional)
   * 4. Load account ID (optional)
   * 5. Validate that required credentials are present
   * 6. Determine credential type based on session token presence
   * 7. Create appropriate credential instance (Basic or Session)
   * 
   * Credential validation:
   * - Access key ID and secret access key are required
   * - Values are trimmed and must be non-empty after trimming
   * - Missing required credentials result in SdkClientException
   * - Session token and account ID are optional
   * 
   * Credential types returned:
   * - AwsBasicCredentials: When only access key and secret key are provided
   * - AwsSessionCredentials: When session token is also provided
   * 
   * Both credential types include provider metadata for AWS usage metrics and debugging:
   * - providerName: Identifies the specific credential source
   * - accountId: AWS account ID if available
   * - validateCredentials: Set to false to skip validation
   * - expirationTime: None for system setting credentials (no expiration)
   * 
   * @return F[AwsCredentials] The resolved AWS credentials (Basic or Session type)
   * @throws SdkClientException if required credentials (access key or secret key) are missing
   */
  override def resolveCredentials(): F[AwsCredentials] =
    for
      accessKeyOpt    <- loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map(_.map(_.trim))
      secretKeyOpt    <- loadSetting(SdkSystemSetting.AWS_SECRET_ACCESS_KEY).map(_.map(_.trim))
      sessionTokenOpt <- loadSetting(SdkSystemSetting.AWS_SESSION_TOKEN).map(_.map(_.trim))
      accountId       <- loadSetting(SdkSystemSetting.AWS_ACCOUNT_ID).map(_.map(_.trim))
      accessKey       <- ev.fromOption(
                     accessKeyOpt,
                     new SdkClientException(
                       s"Unable to load credentials from system settings. Access key must be specified either via environment variable (${ SdkSystemSetting.AWS_ACCESS_KEY_ID }) or system property (${ SdkSystemSetting.AWS_ACCESS_KEY_ID.systemProperty })."
                     )
                   )
      secretKey <- ev.fromOption(
                     secretKeyOpt,
                     new SdkClientException(
                       s"Unable to load credentials from system settings. Secret key must be specified either via environment variable (${ SdkSystemSetting.AWS_SECRET_ACCESS_KEY }) or system property (${ SdkSystemSetting.AWS_SECRET_ACCESS_KEY.systemProperty })."
                     )
                   )
    yield sessionTokenOpt match {
      case None =>
        AwsBasicCredentials(
          accessKeyId         = accessKey,
          secretAccessKey     = secretKey,
          validateCredentials = false,
          providerName        = Some(provider),
          accountId           = accountId,
          expirationTime      = None
        )
      case Some(sessionToken) =>
        AwsSessionCredentials(
          accessKeyId         = accessKey,
          secretAccessKey     = secretKey,
          sessionToken        = sessionToken,
          validateCredentials = false,
          providerName        = Some(provider),
          accountId           = accountId,
          expirationTime      = None
        )
    }

  /**
   * Loads a system setting value from the concrete configuration source.
   * 
   * This abstract method must be implemented by concrete credential providers
   * to specify how to read configuration values from their specific source
   * (environment variables, system properties, etc.).
   * 
   * The implementation should:
   * 1. Read the value from the appropriate configuration source
   * 2. Return Some(value) if the setting exists and has a value
   * 3. Return None if the setting does not exist or has no value
   * 4. Handle any source-specific errors appropriately
   * 
   * Example implementations:
   * - Environment variables: Read from System.getenv() or effect-based environment access
   * - System properties: Read from System.getProperty() or effect-based property access
   * - Configuration files: Parse and read from file-based configuration
   * 
   * The SdkSystemSetting parameter provides both the environment variable name
   * and system property name for the setting, allowing implementations to choose
   * the appropriate source or implement fallback logic.
   * 
   * @param setting The AWS SDK system setting definition containing environment variable and system property names
   * @return F[Option[String]] The setting value if available, None otherwise
   */
  def loadSetting(setting: SdkSystemSetting): F[Option[String]]

  /**
   * Identifier string for this credential provider used in AWS usage metrics and logging.
   * 
   * This string is included in AWS service requests to track credential provider usage
   * and appears in AWS CloudTrail logs for debugging purposes. It should be a short,
   * descriptive identifier that uniquely identifies the credential source.
   * 
   * Common provider identifiers:
   * - "Environment": For environment variable credentials
   * - "SystemProperty": For Java system property credentials  
   * - "Profile": For AWS credentials file profiles
   * - "Container": For ECS/EKS container credentials
   * - "InstanceProfile": For EC2 instance profile credentials
   * 
   * This value is used for AWS business metrics and helps AWS understand
   * how different credential providers are being used across AWS SDKs.
   * 
   * @return String identifier for this credential provider
   */
  def provider: String
