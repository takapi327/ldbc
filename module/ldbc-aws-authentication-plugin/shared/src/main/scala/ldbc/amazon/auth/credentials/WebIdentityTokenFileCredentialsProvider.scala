/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.{ Env, SystemProperties, UUIDGen }

import fs2.io.file.{ Files, Path }

import ldbc.amazon.auth.credentials.internal.WebIdentityCredentialsUtils
import ldbc.amazon.client.HttpClient
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*
import ldbc.amazon.util.SdkSystemSetting

/**
 * [[AwsCredentialsProvider]] implementation that loads credentials from AWS Web Identity Token File.
 * 
 * This provider is primarily used in environments that support OIDC (OpenID Connect) authentication,
 * such as Kubernetes with service accounts, EKS with IAM Roles for Service Accounts (IRSA), 
 * or other containerized environments that provide JWT tokens for AWS authentication.
 * 
 * The provider reads configuration from:
 * - Environment variables: AWS_WEB_IDENTITY_TOKEN_FILE, AWS_ROLE_ARN, AWS_ROLE_SESSION_NAME
 * - System properties: aws.webIdentityTokenFile, aws.roleArn, aws.roleSessionName
 * - AWS config file: web_identity_token_file, role_arn, role_session_name (profile-specific)
 * 
 * Authentication flow:
 * 1. Read JWT token from the specified file path
 * 2. Use the token to assume the specified IAM role via STS AssumeRoleWithWebIdentity
 * 3. Return temporary AWS credentials (access key, secret key, session token)
 * 
 * The JWT token file is typically mounted by container orchestration systems and rotated automatically.
 * 
 * Example Kubernetes configuration with IRSA:
 * {{{
 * apiVersion: v1
 * kind: ServiceAccount
 * metadata:
 *   annotations:
 *     eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/my-role
 * }}}
 * 
 * This will automatically set:
 * - AWS_ROLE_ARN=arn:aws:iam::123456789012:role/my-role
 * - AWS_WEB_IDENTITY_TOKEN_FILE=/var/run/secrets/eks.amazonaws.com/serviceaccount/token
 * 
 * @param webIdentityUtils Web Identity credentials utility for STS operations
 * @tparam F The effect type
 */
final class WebIdentityTokenFileCredentialsProvider[F[_]: Env: SystemProperties: Concurrent](
  webIdentityUtils: WebIdentityCredentialsUtils[F]
) extends AwsCredentialsProvider[F]:

  /**
   * Resolves AWS credentials using Web Identity Token authentication.
   * 
   * This method implements the OIDC (OpenID Connect) authentication flow used in
   * containerized environments such as Kubernetes with IRSA (IAM Roles for Service Accounts)
   * or other OIDC-enabled platforms.
   * 
   * The resolution process:
   * 1. Load Web Identity configuration from environment variables/system properties
   * 2. Validate that required configuration (token file path and role ARN) is present
   * 3. Use STS AssumeRoleWithWebIdentity to exchange the JWT token for AWS credentials
   * 4. Return the temporary AWS credentials with session token
   * 
   * This provider is commonly used in:
   * - Amazon EKS with IAM Roles for Service Accounts (IRSA)
   * - Self-managed Kubernetes clusters with OIDC providers
   * - CI/CD environments with OIDC authentication
   * - Serverless platforms with Web Identity token support
   * 
   * @return F[AwsCredentials] The resolved AWS credentials with temporary access key, secret key, and session token
   * @throws SdkClientException if Web Identity configuration is missing or credential exchange fails
   */
  override def resolveCredentials(): F[AwsCredentials] =
    for
      config      <- loadWebIdentityConfig()
      credentials <- config match {
                       case None =>
                         Concurrent[F].raiseError(
                           new SdkClientException(
                             "Unable to load Web Identity Token credentials. " +
                               "Required environment variables (AWS_WEB_IDENTITY_TOKEN_FILE, AWS_ROLE_ARN) or " +
                               "system properties (aws.webIdentityTokenFile, aws.roleArn) are not set."
                           )
                         )
                       case Some(webIdentityConfig) =>
                         webIdentityUtils.assumeRoleWithWebIdentity(webIdentityConfig)
                     }
    yield credentials

  /**
   * Loads Web Identity Token credential configuration from environment variables and system properties.
   * 
   * This method attempts to load the required configuration for Web Identity Token authentication
   * by checking environment variables first, then falling back to system properties.
   * 
   * Required configuration:
   * - Token file path: Path to the JWT token file (typically mounted by Kubernetes)
   * - Role ARN: The ARN of the IAM role to assume using the Web Identity token
   * 
   * Optional configuration:
   * - Role session name: Custom session name for the assumed role session
   * 
   * Configuration sources (in priority order):
   * 1. Environment variables: AWS_WEB_IDENTITY_TOKEN_FILE, AWS_ROLE_ARN, AWS_ROLE_SESSION_NAME
   * 2. System properties: aws.webIdentityTokenFile, aws.roleArn, aws.roleSessionName
   * 
   * @return F[Option[WebIdentityTokenCredentialProperties]] Web Identity configuration if both token file and role ARN are available, None otherwise
   */
  private def loadWebIdentityConfig(): F[Option[WebIdentityTokenCredentialProperties]] =
    for
      tokenFilePath   <- loadTokenFilePath()
      roleArn         <- loadRoleArn()
      roleSessionName <- loadRoleSessionName()
    yield (tokenFilePath, roleArn) match {
      case (Some(tokenFile), Some(arn)) =>
        Some(
          WebIdentityTokenCredentialProperties(
            webIdentityTokenFile = Path(tokenFile),
            roleArn              = arn,
            roleSessionName      = roleSessionName
          )
        )
      case _ => None
    }

  /**
   * Loads the Web Identity Token file path from configuration sources.
   * 
   * This method retrieves the file system path to the JWT token file used for
   * Web Identity authentication. The token file is typically mounted by container
   * orchestration systems and contains a JWT token signed by an OIDC provider.
   * 
   * Configuration sources (in priority order):
   * 1. Environment variable: AWS_WEB_IDENTITY_TOKEN_FILE
   * 2. System property: aws.webIdentityTokenFile
   * 
   * Common token file paths in Kubernetes environments:
   * - /var/run/secrets/eks.amazonaws.com/serviceaccount/token (EKS IRSA)
   * - /var/run/secrets/kubernetes.io/serviceaccount/token (Standard Kubernetes SA token)
   * 
   * @return F[Option[String]] The token file path if configured, None otherwise
   */
  private def loadTokenFilePath(): F[Option[String]] =
    for
      envValue     <- Env[F].get("AWS_WEB_IDENTITY_TOKEN_FILE")
      sysPropValue <- SystemProperties[F].get(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.systemProperty)
    yield envValue.orElse(sysPropValue).map(_.trim).filter(_.nonEmpty)

  /**
   * Loads the IAM role ARN for Web Identity Token authentication.
   * 
   * This method retrieves the Amazon Resource Name (ARN) of the IAM role that should
   * be assumed using the Web Identity token. The role must be configured with a trust
   * policy that allows the OIDC provider to assume it.
   * 
   * Configuration sources (in priority order):
   * 1. Environment variable: AWS_ROLE_ARN
   * 2. System property: aws.roleArn
   * 
   * Example role ARN format:
   * arn:aws:iam::123456789012:role/EKS-Pod-Identity-Role
   * 
   * The IAM role must have a trust policy similar to:
   * ```json
   * {
   *   "Version": "2012-10-17",
   *   "Statement": [
   *     {
   *       "Effect": "Allow",
   *       "Principal": {
   *         "Federated": "arn:aws:iam::ACCOUNT_ID:oidc-provider/OIDC_PROVIDER_URL"
   *       },
   *       "Action": "sts:AssumeRoleWithWebIdentity"
   *     }
   *   ]
   * }
   * ```
   * 
   * @return F[Option[String]] The IAM role ARN if configured, None otherwise
   */
  private def loadRoleArn(): F[Option[String]] =
    for
      envValue     <- Env[F].get("AWS_ROLE_ARN")
      sysPropValue <- SystemProperties[F].get(SdkSystemSetting.AWS_ROLE_ARN.systemProperty)
    yield envValue.orElse(sysPropValue).map(_.trim).filter(_.nonEmpty)

  /**
   * Loads the optional role session name for Web Identity Token authentication.
   * 
   * This method retrieves an optional session name that will be used to identify
   * the assumed role session in AWS CloudTrail logs and other AWS services.
   * If not provided, a default session name will be generated automatically.
   * 
   * Configuration sources (in priority order):
   * 1. Environment variable: AWS_ROLE_SESSION_NAME
   * 2. System property: aws.roleSessionName
   * 
   * Session name requirements:
   * - Must be 2-64 characters long
   * - Can contain letters, numbers, and the characters +=,.@-
   * - Cannot contain spaces
   * 
   * The session name appears in:
   * - AWS CloudTrail logs as the "roleSessionName" field
   * - AWS STS GetCallerIdentity response
   * - IAM policy evaluation context for condition keys
   * 
   * Example session names:
   * - "kubernetes-pod-web-app"
   * - "ci-cd-pipeline-123"
   * - "user-workload-session"
   * 
   * @return F[Option[String]] The role session name if configured, None otherwise
   */
  private def loadRoleSessionName(): F[Option[String]] =
    for
      envValue     <- Env[F].get("AWS_ROLE_SESSION_NAME")
      sysPropValue <- SystemProperties[F].get(SdkSystemSetting.AWS_ROLE_SESSION_NAME.systemProperty)
    yield envValue.orElse(sysPropValue).map(_.trim).filter(_.nonEmpty)

/**
 * Configuration properties for Web Identity Token credentials.
 * 
 * @param webIdentityTokenFile Path to the JWT token file
 * @param roleArn The ARN of the IAM role to assume
 * @param roleSessionName Optional session name for the assumed role session
 */
case class WebIdentityTokenCredentialProperties(
  webIdentityTokenFile: Path,
  roleArn:              String,
  roleSessionName:      Option[String]
)

object WebIdentityTokenFileCredentialsProvider:

  /**
   * Creates a new Web Identity Token File credentials provider with custom HTTP client.
   * 
   * @param httpClient The HTTP client for STS requests
   * @param region The AWS region for STS endpoint
   * @tparam F The effect type
   * @return A new WebIdentityTokenFileCredentialsProvider instance
   */
  def default[F[_]: Env: SystemProperties: Files: UUIDGen: Concurrent](
    httpClient: HttpClient[F],
    region:     String = "us-east-1"
  ): WebIdentityTokenFileCredentialsProvider[F] =
    val webIdentityUtils = WebIdentityCredentialsUtils.default[F](region, httpClient)
    new WebIdentityTokenFileCredentialsProvider[F](webIdentityUtils)

  /**
   * Creates a new Web Identity Token File credentials provider with custom WebIdentityCredentialsUtils.
   * 
   * @param webIdentityUtils Custom Web Identity credentials utility
   * @tparam F The effect type
   * @return A new WebIdentityTokenFileCredentialsProvider instance
   */
  def create[F[_]: Env: SystemProperties: Concurrent](
    webIdentityUtils: WebIdentityCredentialsUtils[F]
  ): WebIdentityTokenFileCredentialsProvider[F] =
    new WebIdentityTokenFileCredentialsProvider[F](webIdentityUtils)

  /**
   * Checks if Web Identity Token authentication is available by verifying
   * the presence of required environment variables or system properties.
   * 
   * @tparam F The effect type  
   * @return true if Web Identity Token authentication is properly configured
   */
  def isAvailable[F[_]: Env: SystemProperties: Concurrent](): F[Boolean] =
    for
      tokenFile <- Env[F]
                     .get("AWS_WEB_IDENTITY_TOKEN_FILE")
                     .flatMap(envValue =>
                       SystemProperties[F]
                         .get(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.systemProperty)
                         .map(envValue.orElse(_))
                     )
      roleArn <- Env[F]
                   .get("AWS_ROLE_ARN")
                   .flatMap(envValue =>
                     SystemProperties[F]
                       .get(SdkSystemSetting.AWS_ROLE_ARN.systemProperty)
                       .map(envValue.orElse(_))
                   )
    yield tokenFile.exists(_.trim.nonEmpty) && roleArn.exists(_.trim.nonEmpty)
