/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.net.URI
import java.time.Instant

import scala.concurrent.duration.*

import cats.syntax.all.*

import cats.effect.{ Async, Concurrent, Ref }
import cats.effect.std.Env

import fs2.io.net.*

import ldbc.amazon.client.*
import ldbc.amazon.client.{ HttpClient, SimpleHttpClient }
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.*
import ldbc.amazon.useragent.BusinessMetricFeatureId
import ldbc.amazon.util.SimpleJsonParser

/**
 * [[AwsCredentialsProvider]] implementation that loads credentials from EC2 Instance Metadata Service (IMDS).
 * 
 * This provider is used on Amazon EC2 instances with attached IAM instance profiles.
 * It automatically retrieves temporary credentials from the EC2 metadata service at:
 * http://169.254.169.254/latest/meta-data/iam/security-credentials/
 * 
 * The provider supports both IMDSv1 (legacy) and IMDSv2 (recommended) protocols:
 * - IMDSv2: Uses session tokens for enhanced security against SSRF attacks
 * - IMDSv1: Fallback mode for backward compatibility
 * 
 * Environment variables:
 * - AWS_EC2_METADATA_DISABLED: Set to 'true' to disable IMDS credential provider
 * - AWS_EC2_METADATA_SERVICE_ENDPOINT: Override default IMDS endpoint (for testing)
 * 
 * Authentication flow:
 * 1. Optionally acquire IMDSv2 session token (PUT request with TTL header)
 * 2. List available IAM roles from metadata service
 * 3. Retrieve credentials for the first available role
 * 4. Cache credentials until 4 minutes before expiration
 * 5. Automatically refresh credentials in the background
 * 
 * @param httpClient HTTP client for metadata service requests
 * @param credentialsRef Mutable reference for credential caching
 * @tparam F The effect type
 */
final class InstanceProfileCredentialsProvider[F[_]: Env: Concurrent](
  httpClient:     HttpClient[F],
  credentialsRef: Ref[F, Option[CachedCredentials]]
) extends AwsCredentialsProvider[F]:

  private val DEFAULT_IMD_SEND_POINT     = "http://169.254.169.254"
  private val METADATA_TOKEN_TTL_SECONDS = 21600 // 6 hours
  private val CREDENTIAL_REFRESH_BUFFER  = 4.minutes

  /**
   * Resolves AWS credentials from EC2 instance metadata service.
   * 
   * This method performs the following steps:
   * 1. Checks if the metadata service is disabled via environment variable
   * 2. Retrieves cached credentials if they are still valid
   * 3. Refreshes credentials from IMDS if needed
   * 
   * @return AWS credentials from the instance profile
   * @throws SdkClientException if metadata service is disabled or unreachable
   */
  override def resolveCredentials(): F[AwsCredentials] =
    for
      disabled <- checkIfDisabled()
      _        <- Concurrent[F].raiseWhen(disabled)(
             new SdkClientException("EC2 metadata service is disabled via AWS_EC2_METADATA_DISABLED")
           )
      cached      <- credentialsRef.get
      credentials <- cached match {
                       case Some(creds) if !isExpiringSoon(creds) =>
                         Concurrent[F].pure(creds.credentials)
                       case _ =>
                         refreshCredentials()
                     }
    yield credentials

  /**
   * Checks if the EC2 metadata service is disabled via environment variable.
   * 
   * The AWS_EC2_METADATA_DISABLED environment variable can be set to "true"
   * to disable this credentials provider.
   * 
   * @return true if the metadata service is disabled, false otherwise
   */
  private def checkIfDisabled(): F[Boolean] =
    Env[F].get("AWS_EC2_METADATA_DISABLED").map {
      case Some(value) => value.toLowerCase == "true"
      case None        => false
    }

  /**
   * Refreshes credentials from the EC2 instance metadata service.
   * 
   * This method performs the complete IMDS authentication flow:
   * 1. Gets the IMDS endpoint (default or custom)
   * 2. Attempts to acquire an IMDSv2 token (falls back to IMDSv1 if it fails)
   * 3. Retrieves the available IAM role name
   * 4. Fetches credentials for the role
   * 5. Caches the credentials with timestamp
   * 
   * @return Fresh AWS credentials from IMDS
   */
  private def refreshCredentials(): F[AwsCredentials] =
    for
      endpoint    <- getImdsEndpoint()
      token       <- acquireMetadataToken(endpoint).attempt.map(_.toOption)
      roleName    <- getRoleName(endpoint, token)
      credentials <- getCredentialsForRole(endpoint, token, roleName)
      cached = CachedCredentials(credentials, Instant.now())
      _ <- credentialsRef.set(Some(cached))
    yield credentials

  /**
   * Gets the IMDS endpoint URL from environment variable or default.
   * 
   * The AWS_EC2_METADATA_SERVICE_ENDPOINT environment variable can be used
   * to override the default IMDS endpoint (useful for testing or proxies).
   * 
   * @return The IMDS endpoint URL without trailing slash
   */
  private def getImdsEndpoint(): F[String] =
    Env[F].get("AWS_EC2_METADATA_SERVICE_ENDPOINT").flatMap {
      case Some(endpoint) =>
        Concurrent[F]
          .raiseUnless(endpoint.matches("^https?://169\\.254\\.169\\.254.*"))(new SecurityException("Invalid IMDS endpoint"))
          .map(_ => endpoint.stripSuffix("/"))
      case None           => Concurrent[F].pure(DEFAULT_IMD_SEND_POINT)
    }

  /**
   * Acquires an IMDSv2 session token for enhanced security.
   * 
   * IMDSv2 requires a session token to access metadata, which helps prevent
   * Server-Side Request Forgery (SSRF) attacks. The token is obtained via
   * a PUT request with a TTL header.
   * 
   * @param endpoint The IMDS endpoint URL
   * @return A session token valid for the configured TTL period
   * @throws SdkClientException if token acquisition fails
   */
  private def acquireMetadataToken(endpoint: String): F[String] =
    val tokenUrl = s"$endpoint/latest/api/token"
    val headers  = Map(
      "X-aws-ec2-metadata-token-ttl-seconds" -> METADATA_TOKEN_TTL_SECONDS.toString
    )

    for
      response <- httpClient.put(URI.create(tokenUrl), headers, "")
      _        <- validateHttpResponse(response, "Failed to acquire metadata token")
    yield response.body.trim

  /**
   * Retrieves the IAM role name associated with the instance.
   * 
   * This method lists all available IAM roles from the metadata service
   * and selects the first one. In practice, EC2 instances typically have
   * only one attached IAM role.
   * 
   * @param endpoint The IMDS endpoint URL
   * @param token Optional IMDSv2 session token
   * @return The name of the IAM role attached to the instance
   * @throws SdkClientException if no roles are available or request fails
   */
  private def getRoleName(endpoint: String, token: Option[String]): F[String] =
    val roleUrl = s"$endpoint/latest/meta-data/iam/security-credentials/"
    val headers = buildRequestHeaders(token)

    for
      response <- httpClient.get(URI.create(roleUrl), headers)
      _        <- validateHttpResponse(response, "Failed to list IAM roles")
      roleName <- parseRoleListResponse(response.body)
    yield roleName

  /**
   * Retrieves AWS credentials for the specified IAM role.
   * 
   * This method fetches the temporary credentials (access key, secret key,
   * and session token) associated with the IAM role. The credentials are
   * returned in JSON format by the metadata service.
   * 
   * @param endpoint The IMDS endpoint URL
   * @param token Optional IMDSv2 session token
   * @param roleName The name of the IAM role
   * @return AWS credentials for the specified role
   * @throws SdkClientException if credentials cannot be retrieved or parsed
   */
  private def getCredentialsForRole(
    endpoint: String,
    token:    Option[String],
    roleName: String
  ): F[AwsCredentials] =
    val credentialsUrl = s"$endpoint/latest/meta-data/iam/security-credentials/$roleName"
    val headers        = buildRequestHeaders(token)

    for
      response    <- httpClient.get(URI.create(credentialsUrl), headers)
      _           <- validateHttpResponse(response, s"Failed to retrieve credentials for role $roleName")
      credentials <- parseCredentialsResponse(response.body, roleName)
    yield credentials

  /**
   * Builds HTTP request headers for IMDS requests.
   * 
   * Creates appropriate headers for both IMDSv1 and IMDSv2 requests.
   * When an IMDSv2 token is available, it's included in the headers.
   * 
   * @param token Optional IMDSv2 session token
   * @return Map of HTTP headers for the request
   */
  private def buildRequestHeaders(token: Option[String]): Map[String, String] =
    val baseHeaders = Map(
      "Accept"     -> "application/json",
      "User-Agent" -> "aws-sdk-scala/ldbc"
    )
    token match {
      case Some(t) => baseHeaders + ("X-aws-ec2-metadata-token" -> t)
      case None    => baseHeaders
    }

  /**
   * Validates HTTP response status codes and provides appropriate error messages.
   * 
   * This method interprets common HTTP status codes in the context of IMDS operations
   * and provides meaningful error messages for debugging authentication issues.
   * 
   * @param response The HTTP response to validate
   * @param context A descriptive context for error messages
   * @return Unit if the response is successful
   * @throws SdkClientException with context-specific error message for failed responses
   */
  private def validateHttpResponse(response: HttpResponse, context: String): F[Unit] =
    response.statusCode match {
      case code if code >= 200 && code < 300 => Concurrent[F].unit
      case 401                               =>
        Concurrent[F].raiseError(
          new SdkClientException(s"$context: Unauthorized (401) - Invalid or expired metadata token")
        )
      case 403 =>
        Concurrent[F].raiseError(
          new SdkClientException(s"$context: Forbidden (403) - No instance profile attached")
        )
      case 404 =>
        Concurrent[F].raiseError(
          new SdkClientException(s"$context: Not Found (404) - Instance metadata not available")
        )
      case code =>
        Concurrent[F].raiseError(
          new SdkClientException(s"$context: HTTP $code - ${ response.body }")
        )
    }

  private def parseRoleListResponse(body: String): F[String] =
    val roles = body.trim.split('\n').map(_.trim).filter(_.nonEmpty)
    roles.headOption match {
      case Some(roleName) => Concurrent[F].pure(roleName)
      case None           =>
        Concurrent[F].raiseError(
          new SdkClientException("No IAM roles found in instance metadata")
        )
    }

  private def parseCredentialsResponse(jsonBody: String, roleName: String): F[AwsCredentials] =
    Concurrent[F]
      .fromEither(
        SimpleJsonParser
          .parse(jsonBody)
          .flatMap(InstanceMetadataCredentialsResponse.fromJson)
          .left
          .map(msg => new SdkClientException(s"Failed to parse JSON: $msg"))
      )
      .flatMap { response =>
        if response.Code == "Success" then {
          Concurrent[F].pure(
            AwsSessionCredentials(
              accessKeyId         = response.AccessKeyId,
              secretAccessKey     = response.SecretAccessKey,
              sessionToken        = response.Token,
              validateCredentials = false,
              providerName        = Some(BusinessMetricFeatureId.CREDENTIALS_IMDS.code),
              accountId           = extractAccountIdFromArn(response.AccessKeyId),
              expirationTime      = Some(Instant.parse(response.Expiration))
            )
          )
        } else {
          Concurrent[F].raiseError(
            new SdkClientException(s"Failed to retrieve credentials for role $roleName: ${ response.Code }")
          )
        }
      }
      .adaptError {
        case ex =>
          new SdkClientException(s"Failed to parse instance metadata credentials response: ${ ex.getMessage }")
      }

  private def extractAccountIdFromArn(accessKeyId: String): Option[String] =
    // For instance profile credentials, we don't have the account ID directly
    // The access key ID pattern is AKIA... for long-term or ASIA... for temporary credentials
    None

  private def isExpiringSoon(cached: CachedCredentials): Boolean =
    cached.credentials.expirationTime match {
      case Some(expiration) =>
        val now        = Instant.now()
        val bufferTime = expiration.minusSeconds(CREDENTIAL_REFRESH_BUFFER.toSeconds)
        now.isAfter(bufferTime)
      case None => false
    }

/**
 * Cached credentials with retrieval timestamp.
 */
private case class CachedCredentials(
  credentials: AwsCredentials,
  retrievedAt: Instant
)

/**
 * JSON response from EC2 instance metadata service.
 */
private case class InstanceMetadataCredentialsResponse(
  Code:            String,
  LastUpdated:     String,
  Type:            String,
  AccessKeyId:     String,
  SecretAccessKey: String,
  Token:           String,
  Expiration:      String
)

private object InstanceMetadataCredentialsResponse:

  def fromJson(json: SimpleJsonParser.JsonObject): Either[String, InstanceMetadataCredentialsResponse] =
    for
      code            <- json.require("Code")
      accessKeyId     <- json.require("AccessKeyId")
      secretAccessKey <- json.require("SecretAccessKey")
      token           <- json.require("Token")
      expiration      <- json.require("Expiration")
    yield InstanceMetadataCredentialsResponse(
      Code            = code,
      LastUpdated     = json.getOrEmpty("LastUpdated"),
      Type            = json.getOrEmpty("Type"),
      AccessKeyId     = accessKeyId,
      SecretAccessKey = secretAccessKey,
      Token           = token,
      Expiration      = expiration
    )

object InstanceProfileCredentialsProvider:

  /**
   * Creates a new Instance Profile credentials provider with default settings.
   * 
   * @tparam F The effect type
   * @return A new InstanceProfileCredentialsProvider instance
   */
  def apply[F[_]: Env: Network: Async](): F[InstanceProfileCredentialsProvider[F]] =
    for
      httpClient     <- createDefaultHttpClient[F]()
      credentialsRef <- Ref.of[F, Option[CachedCredentials]](None)
    yield new InstanceProfileCredentialsProvider[F](httpClient, credentialsRef)

  /**
   * Creates a new Instance Profile credentials provider with custom HTTP client.
   * 
   * @param httpClient The HTTP client for metadata service requests
   * @tparam F The effect type
   * @return A new InstanceProfileCredentialsProvider instance
   */
  def create[F[_]: Env: Concurrent](
    httpClient: HttpClient[F]
  ): F[InstanceProfileCredentialsProvider[F]] =
    Ref.of[F, Option[CachedCredentials]](None).map { credentialsRef =>
      new InstanceProfileCredentialsProvider[F](httpClient, credentialsRef)
    }

  /**
   * Creates a default HTTP client optimized for EC2 metadata service.
   * 
   * @tparam F The effect type
   * @return A configured HTTP client
   */
  private def createDefaultHttpClient[F[_]: Network: Async](): F[HttpClient[F]] =
    Async[F].pure(
      SimpleHttpClient[F](
        connectTimeout = 2.seconds,
        readTimeout    = 5.seconds
      )
    )

  /**
   * Checks if Instance Profile credentials are available by attempting
   * to connect to the EC2 metadata service.
   * 
   * @tparam F The effect type  
   * @return true if the metadata service is reachable
   */
  def isAvailable[F[_]: Env: Network: Async](): F[Boolean] =
    for
      disabled <- Env[F].get("AWS_EC2_METADATA_DISABLED").map {
                    case Some(value) => value.toLowerCase == "true"
                    case None        => false
                  }
      available <- if disabled then {
                     Async[F].pure(false)
                   } else {
                     checkMetadataServiceAvailability[F]()
                   }
    yield available

  private def checkMetadataServiceAvailability[F[_]: Network: Async](): F[Boolean] =
    val httpClient = SimpleHttpClient[F](
      connectTimeout = 1.second,
      readTimeout    = 2.seconds
    )

    httpClient
      .get(
        URI.create("http://169.254.169.254/latest/meta-data/"),
        Map.empty
      )
      .map(_.statusCode == 200)
      .handleErrorWith(_ => Async[F].pure(false))
