/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.net.URI
import java.time.Instant

import cats.syntax.all.*

import cats.effect.std.Env
import cats.effect.Concurrent

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

  /**
   * Resolves AWS credentials from the container credential provider endpoint.
   * 
   * This method implements the core logic for retrieving temporary AWS credentials
   * from container-based credential providers such as ECS Task Roles or EKS Pod Identity.
   * 
   * The resolution process:
   * 1. Load container credentials configuration from environment variables
   * 2. Validate that required environment variables are present
   * 3. Make HTTP request to the credential endpoint
   * 4. Parse and return the temporary credentials
   * 
   * @return F[AwsCredentials] The resolved AWS credentials with access key, secret key, and session token
   * @throws SdkClientException if container credentials configuration is missing or credential retrieval fails
   */
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

  /**
   * Loads container credentials configuration from environment variables.
   * 
   * This method reads configuration from the standard AWS environment variables used
   * by container credential providers and constructs the appropriate endpoint configuration.
   * 
   * Environment variables checked (in priority order):
   * - AWS_CONTAINER_CREDENTIALS_RELATIVE_URI: Relative URI path (used with ECS metadata endpoint)
   * - AWS_CONTAINER_CREDENTIALS_FULL_URI: Complete URI (used with custom endpoints like EKS Pod Identity)
   * - AWS_CONTAINER_AUTHORIZATION_TOKEN: Direct authorization token for requests
   * - AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE: Path to file containing authorization token
   * 
   * When AWS_CONTAINER_CREDENTIALS_RELATIVE_URI is present, the full endpoint URI is constructed
   * as: http://169.254.170.2 + relative_uri (ECS metadata endpoint)
   * 
   * When AWS_CONTAINER_CREDENTIALS_FULL_URI is present, it is used as-is (typically for EKS Pod Identity)
   * 
   * @return F[Option[ContainerCredentialsConfig]] Container configuration if environment variables are properly set, None otherwise
   */
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

  /**
   * Loads the authorization token from environment variables or file.
   * 
   * This method resolves the authorization token needed for container credential requests
   * by checking both direct token environment variable and token file path.
   * 
   * Token resolution priority:
   * 1. AWS_CONTAINER_AUTHORIZATION_TOKEN (direct token value)
   * 2. AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE (path to file containing token)
   * 
   * The authorization token is typically used in EKS environments where the token
   * is provided by the Kubernetes service account system.
   * 
   * @param directToken Optional token value from AWS_CONTAINER_AUTHORIZATION_TOKEN
   * @param tokenFilePath Optional file path from AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE
   * @return F[Option[String]] The authorization token if available, None otherwise
   */
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

  /**
   * Loads authorization token from a file path.
   * 
   * This method safely reads the authorization token from the specified file path,
   * handling potential file system errors gracefully. This is commonly used in
   * Kubernetes environments where tokens are mounted as files in the pod.
   * 
   * The method performs the following operations:
   * 1. Check if the file exists
   * 2. Read the file content as UTF-8 text
   * 3. Trim whitespace and validate the token is non-empty
   * 4. Handle any file I/O errors by returning None
   * 
   * Common token file locations:
   * - /var/run/secrets/eks.amazonaws.com/serviceaccount/token (EKS IRSA)
   * - /var/run/secrets/kubernetes.io/serviceaccount/token (Standard Kubernetes SA token)
   * 
   * @param tokenFilePath Path to the token file
   * @return F[Option[String]] The token content if file exists and is readable, None otherwise
   */
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

  /**
   * Fetches AWS credentials from the configured container endpoint.
   * 
   * This method performs the actual HTTP request to retrieve temporary AWS credentials
   * from the container credential provider endpoint. The request includes appropriate
   * headers and authorization tokens when required.
   * 
   * Request flow:
   * 1. Build HTTP headers with authorization token (if present)
   * 2. Make HTTP GET request to the credential endpoint
   * 3. Validate the HTTP response status
   * 4. Parse the JSON response to extract credentials
   * 
   * The endpoint returns a JSON response containing:
   * - AccessKeyId: Temporary access key
   * - SecretAccessKey: Temporary secret key
   * - Token: Session token
   * - Expiration: RFC3339 formatted expiration timestamp
   * - RoleArn: ARN of the assumed role (optional)
   * 
   * @param config Container credentials configuration with endpoint URI and auth token
   * @return F[AwsCredentials] The temporary AWS credentials from the endpoint
   * @throws SdkClientException if the HTTP request fails or response parsing fails
   */
  private def fetchCredentialsFromEndpoint(config: ContainerCredentialsConfig): F[AwsCredentials] =
    val headers = buildRequestHeaders(config.authorizationToken)
    for
      response    <- httpClient.get(URI.create(config.endpointUri), headers)
      _           <- validateHttpResponse(response)
      credentials <- parseCredentialsResponse(response.body)
    yield credentials

  /**
   * Builds HTTP request headers for container credential requests.
   * 
   * This method constructs the necessary HTTP headers for credential endpoint requests,
   * including content type specification and authorization token when available.
   * 
   * Standard headers included:
   * - Accept: application/json (indicates we expect JSON response)
   * - User-Agent: aws-sdk-scala/ldbc (identifies the client SDK)
   * - Authorization: token value (when authorization token is available)
   * 
   * The Authorization header is only included when an authorization token is provided,
   * which is typically required for EKS Pod Identity but not for ECS Task Roles.
   * 
   * @param authToken Optional authorization token for the request
   * @return Map[String, String] HTTP headers for the credential request
   */
  private def buildRequestHeaders(authToken: Option[String]): Map[String, String] =
    val baseHeaders = Map(
      "Accept"     -> "application/json",
      "User-Agent" -> "aws-sdk-scala/ldbc"
    )
    authToken match {
      case Some(token) => baseHeaders + ("Authorization" -> token)
      case None        => baseHeaders
    }

  /**
   * Validates the HTTP response from the container credential endpoint.
   * 
   * This method checks that the credential endpoint returned a successful HTTP status code
   * (2xx range) and raises an exception for any error responses.
   * 
   * Common error scenarios:
   * - 401 Unauthorized: Invalid or missing authorization token
   * - 403 Forbidden: Insufficient permissions for the requested role
   * - 404 Not Found: Invalid endpoint URI or credential path
   * - 500 Internal Server Error: Credential service internal error
   * 
   * The error message includes both the HTTP status code and response body
   * to provide detailed information for debugging credential issues.
   * 
   * @param response HTTP response from the credential endpoint
   * @return F[Unit] Success if status code is 2xx, failure otherwise
   * @throws SdkClientException if the HTTP status code indicates an error
   */
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

  /**
   * Parses the JSON credentials response from the container endpoint.
   * 
   * This method deserializes the JSON response containing temporary AWS credentials
   * and converts it into an AwsSessionCredentials instance. The response follows
   * the standard AWS credential response format.
   * 
   * Expected JSON structure:
   * ```json
   * {
   *   "AccessKeyId": "ASIA...",
   *   "SecretAccessKey": "...",
   *   "Token": "...",
   *   "Expiration": "2024-01-15T12:00:00Z",
   *   "RoleArn": "arn:aws:iam::123456789012:role/MyRole" // optional
   * }
   * ```
   * 
   * The method performs the following operations:
   * 1. Parse the JSON response using SimpleJsonParser
   * 2. Extract required fields (AccessKeyId, SecretAccessKey, Token, Expiration)
   * 3. Parse the expiration timestamp in RFC3339 format
   * 4. Extract the AWS account ID from the role ARN (if present)
   * 5. Create AwsSessionCredentials with provider metadata
   * 
   * @param jsonBody JSON response body from the credential endpoint
   * @return F[AwsCredentials] Parsed AWS session credentials
   * @throws SdkClientException if JSON parsing fails or required fields are missing
   */
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

  /**
   * Extracts the AWS account ID from an IAM role ARN.
   * 
   * This method parses the role ARN returned by container credential endpoints
   * to extract the AWS account ID, which is useful for credential metadata.
   * 
   * ARN format: arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME
   * Example: arn:aws:iam::123456789012:role/EKS-Pod-Identity-Role
   * 
   * The method splits the ARN by colons and extracts the account ID from the 5th position
   * (0-indexed position 4). If the ARN format is invalid or the account ID position
   * is not available, None is returned.
   * 
   * This information is particularly useful for:
   * - Auditing and logging credential usage
   * - Validating that credentials are from the expected AWS account
   * - Supporting multi-account AWS environments
   * 
   * @param roleArn Optional IAM role ARN from the credential response
   * @return Option[String] The AWS account ID if extractable from the ARN, None otherwise
   */
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

/**
 * Companion object for parsing JSON responses from container credential endpoints.
 */
private object ContainerCredentialsResponse:

  /**
   * Parses a JSON object into a ContainerCredentialsResponse.
   * 
   * This method deserializes the standard AWS container credentials JSON response format,
   * extracting all required fields and optional metadata.
   * 
   * Required JSON fields:
   * - AccessKeyId: The temporary AWS access key ID
   * - SecretAccessKey: The temporary AWS secret access key
   * - Token: The session token for temporary credentials
   * - Expiration: RFC3339 formatted timestamp indicating when credentials expire
   * 
   * Optional JSON fields:
   * - RoleArn: The ARN of the IAM role that was assumed to generate these credentials
   * 
   * @param json The parsed JSON object from the credential endpoint response
   * @return Either[String, ContainerCredentialsResponse] Success with parsed response or error message
   */
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
