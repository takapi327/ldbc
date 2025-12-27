/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.client

import java.net.{ URI, URLEncoder }
import java.time.{ Instant, ZoneOffset }
import java.time.format.DateTimeFormatter

import cats.syntax.all.*
import cats.MonadThrow

import cats.effect.std.UUIDGen
import cats.effect.Concurrent

import ldbc.amazon.exception.StsException
import ldbc.amazon.util.SimpleXmlParser

/**
 * Trait for AWS STS (Security Token Service) client operations.
 * 
 * This client implements the AWS STS AssumeRoleWithWebIdentity operation to exchange
 * a Web Identity Token (JWT) for temporary AWS credentials.
 */
trait StsClient[F[_]]:

  /**
   * Performs AssumeRoleWithWebIdentity operation.
   * 
   * @param request The STS request parameters
   * @return STS response with temporary credentials
   */
  def assumeRoleWithWebIdentity(
    request: StsClient.AssumeRoleWithWebIdentityRequest
  ): F[StsClient.AssumeRoleWithWebIdentityResponse]

object StsClient:

  /**
   * AssumeRoleWithWebIdentity request parameters.
   * 
   * @param roleArn The ARN of the IAM role to assume
   * @param webIdentityToken The Web Identity Token (JWT)
   * @param roleSessionName Optional session name for the assumed role session
   * @param durationSeconds Optional duration of the session (default 3600 seconds)
   */
  case class AssumeRoleWithWebIdentityRequest(
    roleArn:          String,
    webIdentityToken: String,
    roleSessionName:  Option[String] = None,
    durationSeconds:  Option[Int]    = None
  )

  /**
   * STS response containing temporary credentials.
   * 
   * @param accessKeyId The temporary access key ID
   * @param secretAccessKey The temporary secret access key
   * @param sessionToken The temporary session token
   * @param expiration When the credentials expire
   * @param assumedRoleArn The ARN of the assumed role
   */
  case class AssumeRoleWithWebIdentityResponse(
    accessKeyId:     String,
    secretAccessKey: String,
    sessionToken:    String,
    expiration:      Instant,
    assumedRoleArn:  String
  )

  /**
   * Private implementation of StsClient.
   * 
   * This implementation handles:
   * 1. Request validation and parameter setup
   * 2. HTTP request formatting and execution
   * 3. Response parsing and error handling
   * 
   * @param stsEndpoint The STS service endpoint URL
   * @param httpClient The HTTP client to use for making requests
   * @tparam F The effect type that supports UUID generation and concurrency
   */
  private case class Impl[F[_]: UUIDGen: Concurrent](
    stsEndpoint: String,
    httpClient:  HttpClient[F]
  ) extends StsClient[F]:

    override def assumeRoleWithWebIdentity(
      request: AssumeRoleWithWebIdentityRequest
    ): F[AssumeRoleWithWebIdentityResponse] =
      for
        _           <- validateRoleArn(request.roleArn)
        sessionName <- request.roleSessionName.fold(
                         UUIDGen[F].randomUUID.map(uuid => s"ldbc-session-$uuid")
                       )(Concurrent[F].pure)
        duration = request.durationSeconds.getOrElse(3600)

        // Build STS request
        requestBody = buildRequestBody(
                        request.copy(
                          roleSessionName = Some(sessionName),
                          durationSeconds = Some(duration)
                        )
                      )

        // Make HTTP request
        headers = Map(
                    "Content-Type" -> "application/x-www-form-urlencoded",
                    "X-Amz-Target" -> "AWSSecurityTokenServiceV20110615.AssumeRoleWithWebIdentity",
                    "X-Amz-Date"   -> getCurrentTimestamp
                  )

        response    <- httpClient.post(URI.create(stsEndpoint), headers, requestBody)
        _           <- validateHttpResponse(response)
        stsResponse <- parseAssumeRoleResponse(response.body)
      yield stsResponse

    /**
     * Validates that the provided IAM role ARN has the correct format.
     * 
     * Role ARNs must match the pattern: arn:aws:iam::ACCOUNT_ID:role/ROLE_NAME
     * where ACCOUNT_ID is a 12-digit number and ROLE_NAME contains valid characters.
     * 
     * @param roleArn The IAM role ARN to validate
     * @return Unit if valid, raises an error if invalid
     */
    private def validateRoleArn(roleArn: String): F[Unit] =
      val roleArnPattern = """^arn:aws:iam::\d{12}:role/[\w+=,.@-]+$""".r
      roleArnPattern.findFirstIn(roleArn) match {
        case Some(_) => Concurrent[F].unit
        case None    =>
          Concurrent[F].raiseError(
            new IllegalArgumentException(
              s"An error occurred (ValidationError) when calling the AssumeRole operation: $roleArn is invalid"
            )
          )
      }

  /**
   * Creates implementation of StsClient.
   *
   * @param endpoint STS Endpoint
   * @param httpClient HTTP client for making requests
   * @tparam F The effect type
   * @return A StsClient instance
   */
  def build[F[_]: UUIDGen: Concurrent](endpoint: String, httpClient: HttpClient[F]): StsClient[F] =
    Impl[F](endpoint, httpClient)

  /**
   * Builds the STS request body in AWS Query format.
   * 
   * Creates a URL-encoded form body with the required STS parameters for the
   * AssumeRoleWithWebIdentity operation. All parameter names and values are
   * properly URL-encoded to ensure safe transmission.
   * 
   * @param request The STS request containing the parameters
   * @return URL-encoded form data string for the request body
   */
  private def buildRequestBody(request: AssumeRoleWithWebIdentityRequest): String =
    val params = Map(
      "Action"           -> "AssumeRoleWithWebIdentity",
      "Version"          -> "2011-06-15",
      "RoleArn"          -> request.roleArn,
      "WebIdentityToken" -> request.webIdentityToken,
      "RoleSessionName"  -> request.roleSessionName.getOrElse("ldbc-session"),
      "DurationSeconds"  -> request.durationSeconds.getOrElse(3600).toString
    )

    params
      .map {
        case (key, value) =>
          s"${ URLEncoder.encode(key, "UTF-8") }=${ URLEncoder.encode(value, "UTF-8") }"
      }
      .mkString("&")

  /**
   * Gets current timestamp in AWS format.
   *
   * Generates a timestamp string in the ISO 8601 format required by AWS:
   * yyyyMMddTHHmmssZ (e.g., "20231201T120000Z"). The timestamp is always
   * generated in UTC timezone.
   *
   * @return formatted timestamp string
   */
  private def getCurrentTimestamp: String =
    DateTimeFormatter
      .ofPattern("yyyyMMdd'T'HHmmss'Z'")
      .withZone(ZoneOffset.UTC)
      .format(Instant.now())

  /**
   * Validates HTTP response status.
   * 
   * Checks if the HTTP response has a successful status code (2xx range).
   * If the status code indicates an error, raises an StsException with
   * the status code and response body for debugging.
   * 
   * @param response The HTTP response to validate
   * @return Unit if status is successful, raises StsException otherwise
   */
  private def validateHttpResponse[F[_]: MonadThrow](response: HttpResponse): F[Unit] =
    if response.statusCode >= 200 && response.statusCode < 300 then MonadThrow[F].unit
    else
      MonadThrow[F].raiseError(
        new StsException(
          s"STS request failed with status ${ response.statusCode }: ${ response.body }"
        )
      )

  /**
   * Parses STS XML response to extract credentials.
   * 
   * Expected XML structure:
   * ```xml
   * <AssumeRoleWithWebIdentityResponse>
   *   <AssumeRoleWithWebIdentityResult>
   *     <Credentials>
   *       <AccessKeyId>ASIA...</AccessKeyId>
   *       <SecretAccessKey>...</SecretAccessKey>
   *       <SessionToken>...</SessionToken>
   *       <Expiration>2023-12-01T12:00:00Z</Expiration>
   *     </Credentials>
   *     <AssumedRoleUser>
   *       <Arn>arn:aws:sts::123456789012:assumed-role/MyRole/MySession</Arn>
   *       <AssumedRoleId>AROA....:MySession</AssumedRoleId>
   *     </AssumedRoleUser>
   *   </AssumeRoleWithWebIdentityResult>
   * </AssumeRoleWithWebIdentityResponse>
   * ```
   */
  private def parseAssumeRoleResponse[F[_]: MonadThrow](xmlBody: String): F[AssumeRoleWithWebIdentityResponse] =
    MonadThrow[F]
      .catchNonFatal {

        val accessKeyId     = SimpleXmlParser.requireTag("AccessKeyId", xmlBody, "AccessKeyId not found")
        val secretAccessKey = SimpleXmlParser.requireTag("SecretAccessKey", xmlBody, "SecretAccessKey not found")
        val sessionToken    = SimpleXmlParser.requireTag("SessionToken", xmlBody, "SessionToken not found")
        val expirationStr   = SimpleXmlParser.requireTag("Expiration", xmlBody, "Expiration not found")

        val assumedRoleArn = SimpleXmlParser
          .extractSection("AssumedRoleUser", xmlBody)
          .flatMap(section => SimpleXmlParser.extractTagContent("Arn", section))
          .filter(_.nonEmpty)
          .getOrElse(throw new StsException("AssumedRoleArn not found"))

        AssumeRoleWithWebIdentityResponse(
          accessKeyId     = accessKeyId,
          secretAccessKey = secretAccessKey,
          sessionToken    = sessionToken,
          expiration      = Instant.parse(expirationStr),
          assumedRoleArn  = assumedRoleArn
        )
      }
      .adaptError {
        case ex: StsException => ex
        case ex               =>
          new StsException(s"Failed to parse STS response: ${ ex.getMessage }")
      }
