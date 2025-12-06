/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.net.URI
import java.time.Instant


import cats.syntax.all.*

import cats.effect.Concurrent
import cats.effect.std.Env

import fs2.io.file.{ Files, Path }

import ldbc.amazon.client.HttpClient
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*
import ldbc.amazon.useragent.BusinessMetricFeatureId
import ldbc.amazon.util.SimpleJsonParser

/**
 * [[AwsCredentialsProvider]] implementation that loads credentials from AWS Container Credential Provider.
 * 
 * This provider is used in containerized environments such as:
 * - Amazon ECS with IAM Task Roles
 * - Amazon EKS with IAM Roles for Service Accounts (IRSA) 
 * - Amazon EKS with Pod Identity
 * 
 * The provider reads configuration from environment variables:
 * - AWS_CONTAINER_CREDENTIALS_RELATIVE_URI: Relative URI for ECS metadata endpoint
 * - AWS_CONTAINER_CREDENTIALS_FULL_URI: Full URI for credential endpoint
 * - AWS_CONTAINER_AUTHORIZATION_TOKEN: Authorization token for requests
 * - AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE: Path to file containing authorization token
 * 
 * Authentication flow:
 * 1. Check environment variables for endpoint configuration
 * 2. Load authorization token from direct env var or file
 * 3. Make HTTP GET request to credential endpoint with Authorization header
 * 4. Parse JSON response to extract temporary credentials
 * 
 * ECS endpoint: http://169.254.170.2/<relative_uri>
 * EKS Pod Identity endpoint: http://169.254.170.23/v1/credentials
 * 
 * @param httpClient HTTP client for making credential requests
 * @tparam F The effect type
 */
final class ContainerCredentialsProvider[F[_]: Files: Env: Concurrent](
  httpClient: HttpClient[F]
) extends AwsCredentialsProvider[F]:

  override def resolveCredentials(): F[AwsCredentials] =
    for
      config      <- loadContainerCredentialsConfig()
      credentials <- config match {
                       case None =>
                         Concurrent[F].raiseError(
                           new SdkClientException(
                             "Unable to load container credentials. " +
                               "Environment variables AWS_CONTAINER_CREDENTIALS_RELATIVE_URI or " +
                               "AWS_CONTAINER_CREDENTIALS_FULL_URI are not set."
                           )
                         )
                       case Some(containerConfig) =>
                         fetchCredentialsFromEndpoint(containerConfig)
                     }
    yield credentials

  private def loadContainerCredentialsConfig(): F[Option[ContainerCredentialsConfig]] =
    for
      relativeUri <- Env[F].get("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI")
      fullUri     <- Env[F].get("AWS_CONTAINER_CREDENTIALS_FULL_URI")
      directToken <- Env[F].get("AWS_CONTAINER_AUTHORIZATION_TOKEN")
      tokenFile   <- Env[F].get("AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE")
      token       <- loadAuthorizationToken(directToken, tokenFile)
    yield (relativeUri, fullUri) match {
      case (Some(relative), _) =>
        Some(
          ContainerCredentialsConfig(
            endpointUri        = s"http://169.254.170.2$relative",
            authorizationToken = token
          )
        )
      case (_, Some(full)) =>
        Some(
          ContainerCredentialsConfig(
            endpointUri        = full,
            authorizationToken = token
          )
        )
      case _ => None
    }

  private def loadAuthorizationToken(
    directToken:   Option[String],
    tokenFilePath: Option[String]
  ): F[Option[String]] =
    (directToken, tokenFilePath) match {
      case (Some(token), _) =>
        Concurrent[F].pure(Some(token.trim).filter(_.nonEmpty))
      case (_, Some(filePath)) =>
        loadTokenFromFile(Path(filePath))
      case _ =>
        Concurrent[F].pure(None)
    }

  private def loadTokenFromFile(tokenFilePath: Path): F[Option[String]] =
    Files[F].exists(tokenFilePath).flatMap { exists =>
      if exists then {
        Files[F]
          .readUtf8(tokenFilePath)
          .compile
          .string
          .map(_.trim)
          .map(token => if token.nonEmpty then Some(token) else None)
          .handleErrorWith { _ =>
            Concurrent[F].pure(None)
          }
      } else {
        Concurrent[F].pure(None)
      }
    }

  private def fetchCredentialsFromEndpoint(config: ContainerCredentialsConfig): F[AwsCredentials] =
    val headers = buildRequestHeaders(config.authorizationToken)
    for
      response    <- httpClient.get(URI.create(config.endpointUri), headers)
      _           <- validateHttpResponse(response)
      credentials <- parseCredentialsResponse(response.body)
    yield credentials

  private def buildRequestHeaders(authToken: Option[String]): Map[String, String] =
    val baseHeaders = Map(
      "Accept"     -> "application/json",
      "User-Agent" -> "aws-sdk-scala/ldbc"
    )
    authToken match {
      case Some(token) => baseHeaders + ("Authorization" -> token)
      case None        => baseHeaders
    }

  private def validateHttpResponse(response: ldbc.amazon.client.HttpResponse): F[Unit] =
    if response.statusCode >= 200 && response.statusCode < 300 then {
      Concurrent[F].unit
    } else {
      Concurrent[F].raiseError(
        new SdkClientException(
          s"Container credentials request failed with status ${ response.statusCode }: ${ response.body }"
        )
      )
    }

  private def parseCredentialsResponse(jsonBody: String): F[AwsCredentials] =
    Concurrent[F]
      .fromEither(
        SimpleJsonParser
          .parse(jsonBody)
          .flatMap(ContainerCredentialsResponse.fromJson)
          .left
          .map(msg => new SdkClientException(s"Failed to parse JSON: $msg"))
      )
      .map { response =>
        AwsSessionCredentials(
          accessKeyId         = response.AccessKeyId,
          secretAccessKey     = response.SecretAccessKey,
          sessionToken        = response.Token,
          validateCredentials = false,
          providerName        = Some(BusinessMetricFeatureId.CREDENTIALS_CONTAINER.code),
          accountId           = extractAccountIdFromRoleArn(response.RoleArn),
          expirationTime      = Some(Instant.parse(response.Expiration))
        )
      }
      .adaptError {
        case ex =>
          new SdkClientException(s"Failed to parse container credentials response: ${ ex.getMessage }")
      }

  private def extractAccountIdFromRoleArn(roleArn: Option[String]): Option[String] =
    roleArn.flatMap { arn =>
      // ARN format: arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME
      val arnParts = arn.split(":")
      if arnParts.length >= 5 then {
        Some(arnParts(4))
      } else {
        None
      }
    }

/**
 * Configuration for container credentials endpoint.
 * 
 * @param endpointUri The full URI to the credential endpoint
 * @param authorizationToken Optional authorization token for requests
 */
private case class ContainerCredentialsConfig(
  endpointUri:        String,
  authorizationToken: Option[String]
)

/**
 * JSON response from container credentials endpoint.
 * 
 * @param AccessKeyId AWS access key ID
 * @param SecretAccessKey AWS secret access key  
 * @param Token Session token for temporary credentials
 * @param Expiration RFC3339 formatted expiration timestamp
 * @param RoleArn Optional ARN of the assumed role
 */
private case class ContainerCredentialsResponse(
  AccessKeyId:     String,
  SecretAccessKey: String,
  Token:           String,
  Expiration:      String,
  RoleArn:         Option[String] = None
)

private object ContainerCredentialsResponse:
  def fromJson(json: SimpleJsonParser.JsonObject): Either[String, ContainerCredentialsResponse] =
    for
      accessKeyId     <- json.require("AccessKeyId")
      secretAccessKey <- json.require("SecretAccessKey")
      token           <- json.require("Token")
      expiration      <- json.require("Expiration")
    yield ContainerCredentialsResponse(
      AccessKeyId     = accessKeyId,
      SecretAccessKey = secretAccessKey,
      Token           = token,
      Expiration      = expiration,
      RoleArn         = json.get("RoleArn")
    )

object ContainerCredentialsProvider:

  /**
   * Creates a new Container credentials provider with custom HTTP client.
   * 
   * @param httpClient The HTTP client for credential requests
   * @tparam F The effect type
   * @return A new ContainerCredentialsProvider instance
   */
  def create[F[_]: Files: Env: Concurrent](
    httpClient: HttpClient[F]
  ): ContainerCredentialsProvider[F] =
    new ContainerCredentialsProvider[F](httpClient)

  /**
   * Checks if Container credentials are available by verifying
   * the presence of required environment variables.
   * 
   * @tparam F The effect type  
   * @return true if Container credentials are properly configured
   */
  def isAvailable[F[_]: Env: Concurrent](): F[Boolean] =
    for
      relativeUri <- Env[F].get("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI")
      fullUri     <- Env[F].get("AWS_CONTAINER_CREDENTIALS_FULL_URI")
    yield relativeUri.exists(_.trim.nonEmpty) || fullUri.exists(_.trim.nonEmpty)
