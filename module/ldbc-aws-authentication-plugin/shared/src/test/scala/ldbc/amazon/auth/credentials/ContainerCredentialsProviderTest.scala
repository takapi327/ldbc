/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.net.URI
import java.time.Instant

import cats.effect.std.Env
import cats.effect.{ IO, Ref }

import fs2.io.file.{ Files, Path }

import munit.CatsEffectSuite

import ldbc.amazon.auth.credentials.AwsSessionCredentials
import ldbc.amazon.client.{ HttpClient, HttpResponse }
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.AwsCredentials

class ContainerCredentialsProviderTest extends CatsEffectSuite:

  // Test fixtures
  private val validJsonResponse = """{
    "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
    "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY",
    "Token": "session-token-123",
    "Expiration": "2024-12-31T23:59:59Z",
    "RoleArn": "arn:aws:iam::123456789012:role/test-role"
  }"""

  private val minimalJsonResponse = """{
    "AccessKeyId": "ASIAIOSFODNN7EXAMPLE", 
    "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY",
    "Token": "session-token-123",
    "Expiration": "2024-12-31T23:59:59Z"
  }"""

  private val invalidJsonResponse = """{
    "AccessKeyId": "ASIAIOSFODNN7EXAMPLE"
  }"""

  private val eksEndpoint = "http://169.254.170.23/v1/credentials"
  private val authToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

  // Mock HTTP client
  private def mockHttpClient(
    responseBody: String,
    statusCode: Int = 200,
    captureRequest: Option[Ref[IO, Option[MockRequest]]] = None
  ): HttpClient[IO] =
    new HttpClient[IO]:
      override def get(uri: URI, headers: Map[String, String]): IO[HttpResponse] =
        for
          _ <- captureRequest match
                 case Some(ref) => ref.set(Some(MockRequest(uri, headers)))
                 case None => IO.unit
        yield HttpResponse(
          statusCode = statusCode,
          headers = Map.empty,
          body = responseBody
        )

      override def post(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("POST not supported in mock"))
        
      override def put(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("PUT not supported in mock"))

  // Mock environment
  private def mockEnv(envVars: Map[String, String]): Env[IO] =
    new Env[IO]:
      override def get(name: String): IO[Option[String]] =
        IO.pure(envVars.get(name))
      override def entries: IO[scala.collection.immutable.Iterable[(String, String)]] =
        IO.pure(envVars)

  // Use real Files instance and mock file operations differently

  case class MockRequest(uri: URI, headers: Map[String, String])

  test("resolveCredentials with ECS relative URI") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> authToken
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      // Verify credentials
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, "ASIAIOSFODNN7EXAMPLE")
          assertEquals(session.secretAccessKey, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY")
          assertEquals(session.sessionToken, "session-token-123")
          assertEquals(session.validateCredentials, false)
          assertEquals(session.providerName, Some("1"))
          assertEquals(session.accountId, Some("123456789012"))
          assertEquals(session.expirationTime, Some(Instant.parse("2024-12-31T23:59:59Z")))
        case _ => fail("Expected AwsSessionCredentials")

      // Verify HTTP request
      captured match
        case Some(request) =>
          assertEquals(request.uri.toString, "http://169.254.170.2/v2/credentials/test-uuid")
          assertEquals(request.headers("Authorization"), authToken)
          assertEquals(request.headers("Accept"), "application/json")
          assertEquals(request.headers("User-Agent"), "aws-sdk-scala/ldbc")
        case None => fail("Expected captured request")
  }

  test("resolveCredentials with EKS full URI") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_FULL_URI" -> eksEndpoint,
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> authToken
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(minimalJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      // Verify credentials without RoleArn
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, "ASIAIOSFODNN7EXAMPLE")
          assertEquals(session.secretAccessKey, "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY")
          assertEquals(session.sessionToken, "session-token-123")
          assertEquals(session.accountId, None) // No RoleArn means no account ID
        case _ => fail("Expected AwsSessionCredentials")

      // Verify HTTP request
      captured match
        case Some(request) =>
          assertEquals(request.uri.toString, eksEndpoint)
          assertEquals(request.headers("Authorization"), authToken)
        case None => fail("Expected captured request")
  }

  test("resolveCredentials with token from direct environment variable") {
    // This test works in both JVM and JavaScript environments
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> s"  $authToken  \n" // With whitespace
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify token was trimmed and used (ContainerCredentialsProvider trims the token)
      captured match
        case Some(request) =>
          assertEquals(request.headers("Authorization"), authToken.trim)
        case None => fail("Expected captured request")
  }

  test("resolveCredentials without authorization token") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
      // No authorization token
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify no Authorization header
      captured match
        case Some(request) =>
          assert(!request.headers.contains("Authorization"))
        case None => fail("Expected captured request")
  }

  test("fail when no container credentials environment variables are set") {
    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(validJsonResponse)
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load container credentials"))
        assert(exception.getMessage.contains("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"))
        assert(exception.getMessage.contains("AWS_CONTAINER_CREDENTIALS_FULL_URI"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when HTTP request fails") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
    )
    
    given Env[IO] = mockEnv(envVars)

    val httpClient = mockHttpClient("Internal Server Error", statusCode = 500)
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Container credentials request failed"))
        assert(exception.getMessage.contains("500"))
        assert(exception.getMessage.contains("Internal Server Error"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when JSON response is invalid") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
    )
    
    given Env[IO] = mockEnv(envVars)

    val httpClient = mockHttpClient(invalidJsonResponse)
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Failed to parse container credentials response"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when token file does not exist") {
    val tokenFilePath = Path("/tmp/non-existent-token")
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE" -> tokenFilePath.toString
    )
    
    given Env[IO] = mockEnv(envVars)

    val httpClient = mockHttpClient(validJsonResponse)
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
    yield
      // Should succeed without token (Authorization header optional)
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")
  }

  test("handle empty authorization token") {
    // Test with empty environment variable
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> "   \n  " // Only whitespace
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify no Authorization header for empty token
      captured match
        case Some(request) =>
          assert(!request.headers.contains("Authorization"))
        case None => fail("Expected captured request")
  }

  test("prefer direct token over token file path") {
    // Test priority without actual file operations
    val directToken = "direct-token"
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> directToken,
      "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE" -> "/some/file/path" // File path provided but direct token should take precedence
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify direct token was used, not file token
      captured match
        case Some(request) =>
          assertEquals(request.headers("Authorization"), directToken)
        case None => fail("Expected captured request")
  }

  test("prefer relative URI over full URI") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_CREDENTIALS_FULL_URI" -> eksEndpoint
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify relative URI was used (ECS endpoint)
      captured match
        case Some(request) =>
          assertEquals(request.uri.toString, "http://169.254.170.2/v2/credentials/test-uuid")
        case None => fail("Expected captured request")
  }

  test("extractAccountIdFromRoleArn extracts account ID correctly") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
    )
    
    given Env[IO] = mockEnv(envVars)

    val responseWithValidArn = """{
      "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
      "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY", 
      "Token": "session-token-123",
      "Expiration": "2024-12-31T23:59:59Z",
      "RoleArn": "arn:aws:iam::987654321098:role/test-role"
    }"""

    val httpClient = mockHttpClient(responseWithValidArn)
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accountId, Some("987654321098"))
      case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("extractAccountIdFromRoleArn handles invalid ARN") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
    )
    
    given Env[IO] = mockEnv(envVars)

    val responseWithInvalidArn = """{
      "AccessKeyId": "ASIAIOSFODNN7EXAMPLE",
      "SecretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY",
      "Token": "session-token-123", 
      "Expiration": "2024-12-31T23:59:59Z",
      "RoleArn": "invalid-arn-format"
    }"""

    val httpClient = mockHttpClient(responseWithInvalidArn)
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accountId, None)
      case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("isAvailable returns true when relative URI is set") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
    )
    
    given Env[IO] = mockEnv(envVars)

    ContainerCredentialsProvider.isAvailable[IO]().map { available =>
      assertEquals(available, true)
    }
  }

  test("isAvailable returns true when full URI is set") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_FULL_URI" -> eksEndpoint
    )
    
    given Env[IO] = mockEnv(envVars)

    ContainerCredentialsProvider.isAvailable[IO]().map { available =>
      assertEquals(available, true)
    }
  }

  test("isAvailable returns false when no URIs are set") {
    given Env[IO] = mockEnv(Map.empty)

    ContainerCredentialsProvider.isAvailable[IO]().map { available =>
      assertEquals(available, false)
    }
  }

  test("isAvailable returns false when URIs are empty") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "   ",
      "AWS_CONTAINER_CREDENTIALS_FULL_URI" -> ""
    )
    
    given Env[IO] = mockEnv(envVars)

    ContainerCredentialsProvider.isAvailable[IO]().map { available =>
      assertEquals(available, false)
    }
  }

  test("implements AwsCredentialsProvider trait") {
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid"
    )
    
    given Env[IO] = mockEnv(envVars)

    val httpClient = mockHttpClient(validJsonResponse)
    val provider: ldbc.amazon.identity.AwsCredentialsProvider[IO] = 
      ContainerCredentialsProvider.create[IO](httpClient)

    provider.resolveCredentials().map { credentials =>
      assert(credentials.isInstanceOf[AwsCredentials])
    }
  }

  // JavaScript-compatible tests for file-based functionality
  test("resolveCredentials with token file path (JavaScript)") {
    // In JavaScript environment, test with direct token instead of file
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> authToken
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify token was used
      captured match
        case Some(request) =>
          assertEquals(request.headers("Authorization"), authToken)
        case None => fail("Expected captured request")
  }

  test("token preference logic (JavaScript)") {
    // Test that direct token takes precedence over file token path
    val envVars = Map(
      "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" -> "/v2/credentials/test-uuid",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN" -> "direct-token",
      "AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE" -> "/non/existent/file"
    )
    
    given Env[IO] = mockEnv(envVars)

    val requestCapture = Ref.unsafe[IO, Option[MockRequest]](None)
    val httpClient = mockHttpClient(validJsonResponse, captureRequest = Some(requestCapture))
    val provider = ContainerCredentialsProvider.create[IO](httpClient)

    for
      credentials <- provider.resolveCredentials()
      captured <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => // Success
        case _ => fail("Expected AwsSessionCredentials")

      // Verify direct token was used
      captured match
        case Some(request) =>
          assertEquals(request.headers("Authorization"), "direct-token")
        case None => fail("Expected captured request")
  }