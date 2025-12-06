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
 * @param httpClient HTTP client for STS requests
 * @param region AWS region for STS endpoint
 * @tparam F The effect type
 */
final class WebIdentityTokenFileCredentialsProvider[F[_]: Env: SystemProperties: Concurrent](
  webIdentityUtils: WebIdentityCredentialsUtils[F]
) extends AwsCredentialsProvider[F]:

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

  private def loadTokenFilePath(): F[Option[String]] =
    for
      envValue     <- Env[F].get("AWS_WEB_IDENTITY_TOKEN_FILE")
      sysPropValue <- SystemProperties[F].get(SdkSystemSetting.AWS_WEB_IDENTITY_TOKEN_FILE.systemProperty)
    yield envValue.orElse(sysPropValue).map(_.trim).filter(_.nonEmpty)

  private def loadRoleArn(): F[Option[String]] =
    for
      envValue     <- Env[F].get("AWS_ROLE_ARN")
      sysPropValue <- SystemProperties[F].get(SdkSystemSetting.AWS_ROLE_ARN.systemProperty)
    yield envValue.orElse(sysPropValue).map(_.trim).filter(_.nonEmpty)

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
