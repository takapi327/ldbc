/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials.internal

import cats.syntax.all.*
import cats.MonadThrow

import cats.effect.Concurrent
import cats.effect.std.UUIDGen

import fs2.io.file.{ Files, Path }

import ldbc.amazon.auth.credentials.*
import ldbc.amazon.client.{ HttpClient, StsClient }
import ldbc.amazon.exception.{ InvalidTokenException, TokenFileNotFoundException }
import ldbc.amazon.identity.AwsCredentials

/**
 * Trait for handling Web Identity Token credentials and STS AssumeRoleWithWebIdentity operations.
 * 
 * This trait provides functionality to:
 * - Read JWT tokens from file system
 * - Parse STS AssumeRoleWithWebIdentity responses
 * - Generate AWS credentials from temporary session tokens
 */
trait WebIdentityCredentialsUtils[F[_]]:

  /**
   * Assumes an IAM role using Web Identity Token and returns AWS credentials.
   * 
   * This method performs the STS AssumeRoleWithWebIdentity operation, exchanging
   * a Web Identity Token (JWT) for temporary AWS credentials.
   * 
   * @param config The Web Identity Token configuration
   * @param region The AWS region for STS endpoint (default: us-east-1)
   * @param httpClient HTTP client for making STS requests
   * @return AWS credentials with session token
   */
  def assumeRoleWithWebIdentity(
    config:     WebIdentityTokenCredentialProperties,
    region:     String,
    httpClient: HttpClient[F]
  ): F[AwsCredentials]

object WebIdentityCredentialsUtils:

  private case class Impl[F[_]: Files: Concurrent](
    stsClient: StsClient[F]
  ) extends WebIdentityCredentialsUtils[F]:

    def assumeRoleWithWebIdentity(
      config:     WebIdentityTokenCredentialProperties,
      region:     String,
      httpClient: HttpClient[F]
    ): F[AwsCredentials] =
      for
        token <- readTokenFromFile(config.webIdentityTokenFile)
        _     <- validateToken(token)
        stsRequest = StsClient.AssumeRoleWithWebIdentityRequest(
                       roleArn          = config.roleArn,
                       webIdentityToken = token,
                       roleSessionName  = config.roleSessionName
                     )
        stsResponse <- stsClient.assumeRoleWithWebIdentity(stsRequest, region, httpClient)
        credentials = convertStsResponseToCredentials(stsResponse, config)
      yield credentials

    /**
     * Reads JWT token from the specified file path.
     * 
     * @param tokenFilePath Path to the JWT token file
     * @return The JWT token content as a string
     */
    private def readTokenFromFile(tokenFilePath: Path): F[String] =
      for
        exists <- Files[F].exists(tokenFilePath)
        _      <- Concurrent[F].raiseUnless(exists)(
               new TokenFileNotFoundException(s"Web Identity Token file not found: $tokenFilePath")
             )
        token <- Files[F].readUtf8(tokenFilePath).compile.string.map(_.trim)
        _     <- Concurrent[F].raiseWhen(token.isEmpty)(
               new InvalidTokenException(s"Web Identity Token file is empty: $tokenFilePath")
             )
      yield token

    /**
     * Validates the JWT token format.
     * 
     * This is a basic validation to ensure the token has a JWT-like structure.
     * A full implementation would include signature verification and claims validation.
     * 
     * @param token The JWT token to validate
     * @return Unit if token is valid
     */
    private def validateToken(token: String): F[Unit] =
      MonadThrow[F].fromEither {
        // Basic JWT format validation (header.payload.signature)
        val parts = token.split("\\.")
        if parts.length != 3 then {
          Left(new InvalidTokenException(s"Invalid JWT token format. Expected 3 parts, got ${ parts.length }"))
        } else if parts.exists(_.isEmpty) then {
          Left(new InvalidTokenException("JWT token contains empty parts"))
        } else {
          Right(())
        }
      }

    /**
     * Converts STS response to AWS credentials.
     * 
     * @param stsResponse The STS AssumeRoleWithWebIdentity response
     * @param config The Web Identity Token configuration
     * @return AWS session credentials
     */
    private def convertStsResponseToCredentials(
      stsResponse: StsClient.AssumeRoleWithWebIdentityResponse,
      config:      WebIdentityTokenCredentialProperties
    ): AwsCredentials =
      AwsSessionCredentials(
        accessKeyId         = stsResponse.accessKeyId,
        secretAccessKey     = stsResponse.secretAccessKey,
        sessionToken        = stsResponse.sessionToken,
        validateCredentials = false,
        providerName        = Some(config.providerName),
        accountId           = extractAccountIdFromArn(stsResponse.assumedRoleArn),
        expirationTime      = Some(stsResponse.expiration)
      )

    /**
     * Extracts AWS account ID from ARN.
     * 
     * @param arn The AWS ARN (e.g., arn:aws:sts::123456789012:assumed-role/MyRole/MySession)
     * @return Optional account ID
     */
    private def extractAccountIdFromArn(arn: String): Option[String] =
      // ARN format: arn:aws:sts::ACCOUNT_ID:assumed-role/ROLE_NAME/SESSION_NAME
      val arnParts = arn.split(":")
      if arnParts.length >= 5 then {
        Some(arnParts(4))
      } else {
        None
      }

  /**
   * Creates a default implementation of WebIdentityCredentialsUtils.
   * 
   * @tparam F The effect type
   * @return A WebIdentityCredentialsUtils instance
   */
  def default[F[_]: Files: UUIDGen: Concurrent]: WebIdentityCredentialsUtils[F] =
    val stsClient = StsClient.default[F]
    Impl[F](stsClient)

  /**
   * Creates a WebIdentityCredentialsUtils with custom StsClient.
   * 
   * @param stsClient Custom STS client implementation
   * @tparam F The effect type
   * @return A WebIdentityCredentialsUtils instance
   */
  def create[F[_]: Files: Concurrent](stsClient: StsClient[F]): WebIdentityCredentialsUtils[F] =
    Impl[F](stsClient)
