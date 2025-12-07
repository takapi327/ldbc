/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.net.URI
import java.time.Instant

import cats.effect.{ IO, Ref }
import cats.effect.std.Env

import munit.CatsEffectSuite

import ldbc.amazon.client.{ HttpClient, HttpResponse }
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.AwsCredentials

class InstanceProfileCredentialsProviderTest extends CatsEffectSuite:

  // Test fixtures
  private val testRoleName        = "test-role"
  private val testAccessKeyId     = "ASIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken    = "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"
  private val testMetadataToken   = "AQAEAFUsWMxSzWnKOL7wJKWJHFGDL+gUSCMd6FKPYa8wOYwR="
  private val futureExpiration    = Instant.now().plusSeconds(3600)

  // Sample IMDS JSON response
  private val validCredentialsResponse = s"""{
    "Code": "Success",
    "LastUpdated": "2024-01-01T12:00:00Z",
    "Type": "AWS-HMAC",
    "AccessKeyId": "$testAccessKeyId",
    "SecretAccessKey": "$testSecretAccessKey",
    "Token": "$testSessionToken",
    "Expiration": "${ futureExpiration.toString }"
  }"""

  private val failedCredentialsResponse = s"""{
    "Code": "Failed",
    "LastUpdated": "2024-01-01T12:00:00Z",
    "Type": "AWS-HMAC",
    "AccessKeyId": "",
    "SecretAccessKey": "",
    "Token": "",
    "Expiration": ""
  }"""

  // Mock HTTP client
  case class MockRequest(uri: URI, headers: Map[String, String], method: String, body: Option[String])

  private def mockHttpClient(
    responses:      Map[String, HttpResponse],
    captureRequest: Option[Ref[IO, List[MockRequest]]] = None
  ): HttpClient[IO] =
    new HttpClient[IO]:
      override def get(uri: URI, headers: Map[String, String]): IO[HttpResponse] =
        for _ <- captureRequest match
                   case Some(ref) => ref.update(_ :+ MockRequest(uri, headers, "GET", None))
                   case None      => IO.unit
        yield responses.getOrElse(
          uri.toString,
          HttpResponse(404, Map.empty, "Not Found")
        )

      override def post(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        for _ <- captureRequest match
                   case Some(ref) => ref.update(_ :+ MockRequest(uri, headers, "POST", Some(body)))
                   case None      => IO.unit
        yield responses.getOrElse(
          uri.toString,
          HttpResponse(404, Map.empty, "Not Found")
        )

      override def put(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        for _ <- captureRequest match
                   case Some(ref) => ref.update(_ :+ MockRequest(uri, headers, "PUT", Some(body)))
                   case None      => IO.unit
        yield responses.getOrElse(
          uri.toString,
          HttpResponse(200, Map.empty, testMetadataToken)
        )

  // Mock environment
  private def mockEnv(envVars: Map[String, String]): Env[IO] =
    new Env[IO]:
      override def get(name: String): IO[Option[String]] =
        IO.pure(envVars.get(name))
      override def entries: IO[scala.collection.immutable.Iterable[(String, String)]] =
        IO.pure(envVars)

  test("resolveCredentials with successful IMDSv2 flow") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val requestCapture = Ref.unsafe[IO, List[MockRequest]](List.empty)
    val httpClient     = mockHttpClient(responses, Some(requestCapture))

    for
      provider    <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, testAccessKeyId)
          assertEquals(session.secretAccessKey, testSecretAccessKey)
          assertEquals(session.sessionToken, testSessionToken)
          assertEquals(session.validateCredentials, false)
          assertEquals(session.providerName, Some("0")) // BusinessMetricFeatureId.CREDENTIALS_IMDS.code
          assertEquals(session.accountId, None)
          assertEquals(session.expirationTime, Some(futureExpiration))
        case _ => fail("Expected AwsSessionCredentials")

      // Verify the request flow
      val tokenRequest = requests.find(_.method == "PUT")
      assert(tokenRequest.isDefined)
      assert(tokenRequest.get.headers.contains("X-aws-ec2-metadata-token-ttl-seconds"))

      val roleRequest = requests.find(_.uri.toString.endsWith("security-credentials/"))
      assert(roleRequest.isDefined)
      assert(roleRequest.get.headers.contains("X-aws-ec2-metadata-token"))

      val credRequest = requests.find(_.uri.toString.endsWith(s"security-credentials/$testRoleName"))
      assert(credRequest.isDefined)
      assert(credRequest.get.headers.contains("X-aws-ec2-metadata-token"))
  }

  test("resolveCredentials with IMDSv1 fallback when token acquisition fails") {
    val responses = Map(
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = new HttpClient[IO]:
      override def get(uri: URI, headers: Map[String, String]): IO[HttpResponse] =
        IO.pure(responses.getOrElse(uri.toString, HttpResponse(404, Map.empty, "Not Found")))
      override def post(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("POST not supported"))
      override def put(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        // Simulate token acquisition failure
        IO.raiseError(new Exception("Token acquisition failed"))

    for
      provider    <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials <- provider.resolveCredentials()
    yield credentials match
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, testSessionToken)
      case _ => fail("Expected AwsSessionCredentials")
  }

  test("fail when EC2 metadata is disabled") {
    given Env[IO] = mockEnv(Map("AWS_EC2_METADATA_DISABLED" -> "true"))

    val httpClient = mockHttpClient(Map.empty)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("EC2 metadata service is disabled"))
      case _ => fail("Expected SdkClientException")
  }

  test("fail when no IAM roles are available") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(
        200,
        Map.empty,
        ""
      ) // Empty response
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("No IAM roles found"))
      case _ => fail("Expected SdkClientException")
  }

  test("fail when instance profile is not attached (403)") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(403, Map.empty, "Forbidden")
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Forbidden (403)"))
        assert(exception.getMessage.contains("No instance profile attached"))
      case _ => fail("Expected SdkClientException")
  }

  test("fail when metadata service is not available (404)") {
    val responses = Map(
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(404, Map.empty, "Not Found")
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Not Found (404)"))
        assert(exception.getMessage.contains("Instance metadata not available"))
      case _ => fail("Expected SdkClientException")
  }

  test("fail when metadata token is invalid (401)") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(
        401,
        Map.empty,
        "Unauthorized"
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unauthorized (401)"))
        assert(exception.getMessage.contains("Invalid or expired metadata token"))
      case _ => fail("Expected SdkClientException")
  }

  test("fail when credentials response has failed status") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        failedCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Failed to retrieve credentials"))
        assert(exception.getMessage.contains("Failed"))
      case _ => fail("Expected SdkClientException")
  }

  test("fail when credentials response is malformed JSON") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        "invalid json"
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      result   <- provider.resolveCredentials().attempt
    yield result match
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Failed to parse"))
      case _ => fail("Expected SdkClientException")
  }

  test("use custom IMDS endpoint when environment variable is set") {
    val customEndpoint = "http://169.254.169.254:8080"
    val responses      = Map(
      s"$customEndpoint/latest/api/token"                           -> HttpResponse(200, Map.empty, testMetadataToken),
      s"$customEndpoint/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"$customEndpoint/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map("AWS_EC2_METADATA_SERVICE_ENDPOINT" -> customEndpoint))

    val requestCapture = Ref.unsafe[IO, List[MockRequest]](List.empty)
    val httpClient     = mockHttpClient(responses, Some(requestCapture))

    for
      provider    <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, testAccessKeyId)
        case _ => fail("Expected AwsSessionCredentials")

      // Verify custom endpoint was used
      assert(requests.exists(_.uri.toString.startsWith(customEndpoint)))
  }

  test("strip trailing slash from custom endpoint") {
    val customEndpoint   = "http://169.254.169.254:8080/"
    val expectedEndpoint = "http://169.254.169.254:8080"
    val responses        = Map(
      s"$expectedEndpoint/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      s"$expectedEndpoint/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"$expectedEndpoint/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map("AWS_EC2_METADATA_SERVICE_ENDPOINT" -> customEndpoint))

    val requestCapture = Ref.unsafe[IO, List[MockRequest]](List.empty)
    val httpClient     = mockHttpClient(responses, Some(requestCapture))

    for
      provider    <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, testAccessKeyId)
        case _ => fail("Expected AwsSessionCredentials")

      // Verify trailing slash was stripped
      assert(requests.exists(_.uri.toString.startsWith(expectedEndpoint)))
      assert(!requests.exists(_.uri.toString.contains("//latest")))
  }

  test("caching works correctly - multiple calls return cached credentials") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val requestCapture = Ref.unsafe[IO, List[MockRequest]](List.empty)
    val httpClient     = mockHttpClient(responses, Some(requestCapture))

    for
      provider     <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials1 <- provider.resolveCredentials()
      credentials2 <- provider.resolveCredentials()
      requests     <- requestCapture.get
    yield
      // Both calls should return the same credentials
      assertEquals(credentials1.accessKeyId, credentials2.accessKeyId)
      assertEquals(credentials1.secretAccessKey, credentials2.secretAccessKey)

      // Only one set of requests should have been made (first call)
      val tokenRequests = requests.filter(_.method == "PUT")
      assertEquals(tokenRequests.length, 1)
  }

  test("credentials are refreshed when close to expiration") {
    val shortExpirationTime     = Instant.now().plusSeconds(180) // 3 minutes from now (less than 4 minute buffer)
    val shortExpirationResponse = s"""{
      "Code": "Success",
      "LastUpdated": "2024-01-01T12:00:00Z",
      "Type": "AWS-HMAC",
      "AccessKeyId": "$testAccessKeyId",
      "SecretAccessKey": "$testSecretAccessKey",
      "Token": "$testSessionToken",
      "Expiration": "${ shortExpirationTime.toString }"
    }"""

    val longExpirationTime = Instant.now().plusSeconds(3600) // 1 hour from now
    val newAccessKeyId     = "ASIANEWACCESSKEY123"
    val refreshedResponse  = s"""{
      "Code": "Success",
      "LastUpdated": "2024-01-01T12:30:00Z",
      "Type": "AWS-HMAC",
      "AccessKeyId": "$newAccessKeyId",
      "SecretAccessKey": "$testSecretAccessKey",
      "Token": "$testSessionToken",
      "Expiration": "${ longExpirationTime.toString }"
    }"""

    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName)
    )

    // First response with short expiration, second with long expiration
    val callCount  = Ref.unsafe[IO, Int](0)
    val httpClient = new HttpClient[IO]:
      override def get(uri: URI, headers: Map[String, String]): IO[HttpResponse] =
        if uri.toString.endsWith(s"security-credentials/$testRoleName") then
          callCount.updateAndGet(_ + 1).map { count =>
            if count == 1 then HttpResponse(200, Map.empty, shortExpirationResponse)
            else HttpResponse(200, Map.empty, refreshedResponse)
          }
        else IO.pure(responses.getOrElse(uri.toString, HttpResponse(404, Map.empty, "Not Found")))

      override def post(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.raiseError(new UnsupportedOperationException("POST not supported"))

      override def put(uri: URI, headers: Map[String, String], body: String): IO[HttpResponse] =
        IO.pure(HttpResponse(200, Map.empty, testMetadataToken))

    given Env[IO] = mockEnv(Map.empty)

    for
      provider     <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials1 <- provider.resolveCredentials() // Gets short expiration credentials
      credentials2 <- provider.resolveCredentials() // Should refresh due to short expiration
      count        <- callCount.get
    yield
      // First credentials should have short expiration
      assertEquals(credentials1.expirationTime, Some(shortExpirationTime))

      // Second credentials should be refreshed with new access key
      assertEquals(credentials2.accessKeyId, newAccessKeyId)
      assertEquals(credentials2.expirationTime, Some(longExpirationTime))

      // Should have made 2 credential requests due to refresh
      assertEquals(count, 2)
  }

  test("implements AwsCredentialsProvider trait") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      providerTrait: ldbc.amazon.identity.AwsCredentialsProvider[IO] = provider
      credentials <- providerTrait.resolveCredentials()
    yield assert(credentials.isInstanceOf[AwsCredentials])
  }

  test("handles multiple IAM roles by selecting the first one") {
    val multipleRoles = s"$testRoleName\nrole2\nrole3"
    val responses     = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(
        200,
        Map.empty,
        multipleRoles
      ),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val httpClient = mockHttpClient(responses)

    for
      provider    <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials <- provider.resolveCredentials()
    yield credentials match
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
      case _ => fail("Expected AwsSessionCredentials")
  }

  test("request headers are set correctly") {
    val responses = Map(
      "http://169.254.169.254/latest/api/token" -> HttpResponse(200, Map.empty, testMetadataToken),
      "http://169.254.169.254/latest/meta-data/iam/security-credentials/" -> HttpResponse(200, Map.empty, testRoleName),
      s"http://169.254.169.254/latest/meta-data/iam/security-credentials/$testRoleName" -> HttpResponse(
        200,
        Map.empty,
        validCredentialsResponse
      )
    )

    given Env[IO] = mockEnv(Map.empty)

    val requestCapture = Ref.unsafe[IO, List[MockRequest]](List.empty)
    val httpClient     = mockHttpClient(responses, Some(requestCapture))

    for
      provider    <- InstanceProfileCredentialsProvider.create[IO](httpClient)
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, testAccessKeyId)
        case _ => fail("Expected AwsSessionCredentials")

      // Verify PUT request headers for token acquisition
      val tokenRequest = requests.find(_.method == "PUT")
      assert(tokenRequest.isDefined)
      assertEquals(tokenRequest.get.headers("X-aws-ec2-metadata-token-ttl-seconds"), "21600")

      // Verify GET request headers include token and user agent
      val getRequests = requests.filter(_.method == "GET")
      getRequests.foreach { request =>
        assertEquals(request.headers("Accept"), "application/json")
        assertEquals(request.headers("User-Agent"), "aws-sdk-scala/ldbc")
        assertEquals(request.headers("X-aws-ec2-metadata-token"), testMetadataToken)
      }
  }
