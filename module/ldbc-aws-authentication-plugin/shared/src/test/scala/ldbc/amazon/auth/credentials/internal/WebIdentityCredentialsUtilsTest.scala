/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials.internal

import java.net.URI
import java.time.Instant

import cats.effect.{ IO, Ref }
import cats.effect.std.UUIDGen

import fs2.io.file.Files

import munit.CatsEffectSuite

import ldbc.amazon.auth.credentials.{ AwsSessionCredentials, WebIdentityTokenCredentialProperties }
import ldbc.amazon.client.{ HttpClient, HttpResponse, StsClient }
import ldbc.amazon.exception.{ InvalidTokenException, StsException }

class WebIdentityCredentialsUtilsTest extends CatsEffectSuite:

  // Test fixtures
  private val testRoleArn         = "arn:aws:iam::123456789012:role/test-role"
  private val testSessionName     = "test-session"
  private val testAccessKeyId     = "ASIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken    = "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"
  private val futureExpiration    = Instant.now().plusSeconds(3600)
  private val testAssumedRoleArn  = "arn:aws:sts::123456789012:assumed-role/test-role/test-session"

  // Valid JWT token (simplified for testing)
  private val validJwtToken =
    "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMyJ9.eyJpc3MiOiJodHRwczovL29pZGMuZWtzLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tL2lkLzEyMyIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDpkZWZhdWx0Om15LWFwcCJ9.signature"

  // For these tests, we'll focus on mocking the StsClient behavior since Files[IO] is sealed
  // We'll create temporary files for file-based tests when needed

  // Mock StsClient
  private def mockStsClient(
    response:          Option[StsClient.AssumeRoleWithWebIdentityResponse] = None,
    shouldFail:        Boolean = false,
    errorMessage:      String = "Mock STS error",
    captureRequestsTo: Option[Ref[IO, List[StsClient.AssumeRoleWithWebIdentityRequest]]] = None
  ): StsClient[IO] =
    (request: StsClient.AssumeRoleWithWebIdentityRequest) =>
      for _ <- captureRequestsTo match
                 case Some(ref) => ref.update(_ :+ request)
                 case None      => IO.unit
      yield
        if shouldFail then throw new StsException(errorMessage)
        else
          response.getOrElse(
            StsClient.AssumeRoleWithWebIdentityResponse(
              accessKeyId     = testAccessKeyId,
              secretAccessKey = testSecretAccessKey,
              sessionToken    = testSessionToken,
              expiration      = futureExpiration,
              assumedRoleArn  = testAssumedRoleArn
            )
          )

  test("assumeRoleWithWebIdentity with successful flow") {
    // Create a temporary file with the JWT token for testing
    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(validJwtToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val requestCapture   = Ref.unsafe[IO, List[StsClient.AssumeRoleWithWebIdentityRequest]](List.empty)
      val stsClient        = mockStsClient(captureRequestsTo = Some(requestCapture))
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        credentials <- webIdentityUtils.assumeRoleWithWebIdentity(config)
        requests    <- requestCapture.get
        _           <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield
        credentials match
          case session: AwsSessionCredentials =>
            assertEquals(session.accessKeyId, testAccessKeyId)
            assertEquals(session.secretAccessKey, testSecretAccessKey)
            assertEquals(session.sessionToken, testSessionToken)
            assertEquals(session.validateCredentials, false)
            assertEquals(session.providerName, None)
            assertEquals(session.accountId, Some("123456789012"))
            assertEquals(session.expirationTime, Some(futureExpiration))
          case _ => fail("Expected AwsSessionCredentials")

        // Verify STS request was made correctly
        assertEquals(requests.length, 1)
        val request = requests.head
        assertEquals(request.roleArn, testRoleArn)
        assertEquals(request.webIdentityToken, validJwtToken)
        assertEquals(request.roleSessionName, Some(testSessionName))
    }
  }

  // Focus on tests that can be validated without complex file mocking
  // JWT validation tests use temporary files for realistic testing

  test("fail when JWT token has invalid format - too few parts") {
    val invalidToken = "header.payload" // Missing signature part

    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(invalidToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val stsClient        = mockStsClient()
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        result <- webIdentityUtils.assumeRoleWithWebIdentity(config).attempt
        _      <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield result match
        case Left(exception: InvalidTokenException) =>
          assert(exception.getMessage.contains("Invalid JWT token format"))
          assert(exception.getMessage.contains("Expected 3 parts, got 2"))
        case _ => fail("Expected InvalidTokenException")
    }
  }

  test("fail when JWT token has invalid format - too many parts") {
    val invalidToken = "header.payload.signature.extra" // Too many parts

    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(invalidToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val stsClient        = mockStsClient()
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        result <- webIdentityUtils.assumeRoleWithWebIdentity(config).attempt
        _      <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield result match
        case Left(exception: InvalidTokenException) =>
          assert(exception.getMessage.contains("Invalid JWT token format"))
          assert(exception.getMessage.contains("Expected 3 parts, got 4"))
        case _ => fail("Expected InvalidTokenException")
    }
  }

  test("fail when JWT token has empty parts") {
    val invalidToken = "header..signature" // Empty payload

    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(invalidToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val stsClient        = mockStsClient()
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        result <- webIdentityUtils.assumeRoleWithWebIdentity(config).attempt
        _      <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield result match
        case Left(exception: InvalidTokenException) =>
          assert(exception.getMessage.contains("JWT token contains empty parts"))
        case _ => fail("Expected InvalidTokenException")
    }
  }

  test("fail when STS client throws exception") {
    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(validJwtToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val stsClient        = mockStsClient(shouldFail = true, errorMessage = "STS service unavailable")
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        result <- webIdentityUtils.assumeRoleWithWebIdentity(config).attempt
        _      <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield result match
        case Left(exception: StsException) =>
          assertEquals(exception.getMessage, "STS service unavailable")
        case _ => fail("Expected StsException")
    }
  }

  test("extract account ID from ARN correctly") {
    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(validJwtToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val customAssumedRoleArn = "arn:aws:sts::999888777666:assumed-role/my-role/my-session"
      val customResponse       = StsClient.AssumeRoleWithWebIdentityResponse(
        accessKeyId     = testAccessKeyId,
        secretAccessKey = testSecretAccessKey,
        sessionToken    = testSessionToken,
        expiration      = futureExpiration,
        assumedRoleArn  = customAssumedRoleArn
      )

      val stsClient        = mockStsClient(response = Some(customResponse))
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        credentials <- webIdentityUtils.assumeRoleWithWebIdentity(config)
        _           <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accountId, Some("999888777666"))
        case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("handle malformed ARN gracefully") {
    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(validJwtToken)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val malformedArn   = "invalid:arn:format"
      val customResponse = StsClient.AssumeRoleWithWebIdentityResponse(
        accessKeyId     = testAccessKeyId,
        secretAccessKey = testSecretAccessKey,
        sessionToken    = testSessionToken,
        expiration      = futureExpiration,
        assumedRoleArn  = malformedArn
      )

      val stsClient        = mockStsClient(response = Some(customResponse))
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        credentials <- webIdentityUtils.assumeRoleWithWebIdentity(config)
        _           <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accountId, None) // Should be None for malformed ARN
        case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("default factory creates WebIdentityCredentialsUtils with proper STS client") {
    val mockHttpClient: HttpClient[IO] = new HttpClient[IO]:
      override def get(uri: URI, headers: Map[String, String]): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("HTTP requests not supported in test"))
      override def post(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("HTTP requests not supported in test"))
      override def put(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("HTTP requests not supported in test"))

    val webIdentityUtils = WebIdentityCredentialsUtils.default[IO]("us-east-1", mockHttpClient)
    assert(webIdentityUtils != null)
  }

  test("reads token from file with correct trimming") {
    val tokenWithWhitespace = s"  $validJwtToken  \n\t"

    val tempFile = for
      tempDir   <- Files[IO].createTempDirectory(None, "webidentity-test", None)
      tokenFile <- Files[IO].createTempFile(Some(tempDir), "jwt-token", ".txt", None)
      _         <- Files[IO].writeUtf8(tokenFile).apply(fs2.Stream.emit(tokenWithWhitespace)).compile.drain
    yield tokenFile

    tempFile.flatMap { tokenFilePath =>
      val config = WebIdentityTokenCredentialProperties(
        webIdentityTokenFile = tokenFilePath,
        roleArn              = testRoleArn,
        roleSessionName      = Some(testSessionName)
      )

      val requestCapture   = Ref.unsafe[IO, List[StsClient.AssumeRoleWithWebIdentityRequest]](List.empty)
      val stsClient        = mockStsClient(captureRequestsTo = Some(requestCapture))
      val webIdentityUtils = WebIdentityCredentialsUtils.create[IO](stsClient)

      for
        credentials <- webIdentityUtils.assumeRoleWithWebIdentity(config)
        requests    <- requestCapture.get
        _           <- Files[IO].deleteIfExists(tokenFilePath) // Cleanup
      yield
        credentials match
          case session: AwsSessionCredentials =>
            assertEquals(session.accessKeyId, testAccessKeyId)
          case _ => fail("Expected AwsSessionCredentials")

        // Verify the token was trimmed correctly
        assertEquals(requests.length, 1)
        val request = requests.head
        assertEquals(request.webIdentityToken, validJwtToken) // Should be trimmed
    }
  }
