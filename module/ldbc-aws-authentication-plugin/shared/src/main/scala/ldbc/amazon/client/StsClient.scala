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

import cats.effect.Concurrent
import cats.effect.std.UUIDGen

import ldbc.amazon.exception.{ SdkClientException, StsException }
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
   * @param region The AWS region for STS endpoint
   * @param httpClient HTTP client for making requests
   * @return STS response with temporary credentials
   */
  def assumeRoleWithWebIdentity(
    request:    StsClient.AssumeRoleWithWebIdentityRequest,
    region:     String,
    httpClient: HttpClient[F]
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

  private case class Impl[F[_]: UUIDGen: Concurrent]() extends StsClient[F]:

    def assumeRoleWithWebIdentity(
      request:    AssumeRoleWithWebIdentityRequest,
      region:     String,
      httpClient: HttpClient[F]
    ): F[AssumeRoleWithWebIdentityResponse] =
      for
        timestamp <- Concurrent[F].fromEither(getCurrentTimestamp())
        sessionName = request.roleSessionName.getOrElse(s"ldbc-session-${ UUIDGen[F].randomUUID }")
        duration    = request.durationSeconds.getOrElse(3600)

        // Build STS request
        stsEndpoint = s"https://sts.$region.amazonaws.com/"
        requestBody = buildRequestBody(
                        request.copy(
                          roleSessionName = Some(sessionName),
                          durationSeconds = Some(duration)
                        )
                      )

        // Make HTTP request
        headers = Map(
                    "Content-Type" -> "application/x-amz-json-1.0",
                    "X-Amz-Target" -> "AWSSecurityTokenServiceV20110615.AssumeRoleWithWebIdentity",
                    "X-Amz-Date"   -> timestamp
                  )

        response    <- httpClient.get(URI.create(stsEndpoint), headers)
        _           <- validateHttpResponse(response)
        stsResponse <- parseAssumeRoleResponse(response.body)
      yield stsResponse

  /**
   * Creates a default implementation of StsClient.
   * 
   * @tparam F The effect type
   * @return A StsClient instance
   */
  def default[F[_]: UUIDGen: Concurrent]: StsClient[F] = Impl[F]()

  /**
   * Builds the STS request body in AWS Query format.
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
   */
  private def getCurrentTimestamp(): Either[Throwable, String] =
    try {
      Right(
        DateTimeFormatter
          .ofPattern("yyyyMMdd'T'HHmmss'Z'")
          .withZone(ZoneOffset.UTC)
          .format(Instant.now())
      )
    } catch {
      case ex: Exception => Left(new SdkClientException("Failed to generate timestamp"))
    }

  /**
   * Validates HTTP response status.
   */
  private def validateHttpResponse[F[_]: MonadThrow](response: HttpResponse): F[Unit] =
    if response.statusCode >= 200 && response.statusCode < 300 then {
      MonadThrow[F].unit
    } else {
      MonadThrow[F].raiseError(
        new StsException(
          s"STS request failed with status ${ response.statusCode }: ${ response.body }"
        )
      )
    }

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

        val accessKeyId = SimpleXmlParser.requireTag("AccessKeyId", xmlBody, "AccessKeyId not found")
        val secretAccessKey = SimpleXmlParser.requireTag("SecretAccessKey", xmlBody, "SecretAccessKey not found")
        val sessionToken = SimpleXmlParser.requireTag("SessionToken", xmlBody, "SessionToken not found")
        val expirationStr = SimpleXmlParser.requireTag("Expiration", xmlBody, "Expiration not found")

        val assumedRoleArn = SimpleXmlParser.extractSection("AssumedRoleUser", xmlBody)
          .flatMap(section => SimpleXmlParser.extractTagContent("Arn", section))
          .filter(_.nonEmpty)
          .getOrElse(throw new StsException("AssumedRoleArn not found"))

        AssumeRoleWithWebIdentityResponse(
          accessKeyId = accessKeyId,
          secretAccessKey = secretAccessKey,
          sessionToken = sessionToken,
          expiration = Instant.parse(expirationStr),
          assumedRoleArn = assumedRoleArn
        )
      }
      .adaptError {
        case ex: StsException => ex
        case ex =>
          new StsException(s"Failed to parse STS response: ${ ex.getMessage }")
      }
