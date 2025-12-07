/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.time.Instant

import cats.effect.{ IO, Ref }
import cats.effect.std.{ Env, SystemProperties }

import munit.CatsEffectSuite

import ldbc.amazon.auth.credentials.internal.WebIdentityCredentialsUtils
import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.auth.credentials.AwsSessionCredentials
import ldbc.amazon.identity.AwsCredentials

class WebIdentityTokenFileCredentialsProviderTest extends CatsEffectSuite:

  // Test fixtures
  private val testRoleArn         = "arn:aws:iam::123456789012:role/test-role"
  private val testTokenFile       = "/var/run/secrets/eks.amazonaws.com/serviceaccount/token"
  private val testSessionName     = "test-session"
  private val testAccessKeyId     = "ASIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken    = "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"
  private val futureExpiration    = Instant.now().plusSeconds(3600)

  // Mock environment
  private def mockEnv(envVars: Map[String, String]): Env[IO] =
    new Env[IO]:
      override def get(name: String): IO[Option[String]] =
        IO.pure(envVars.get(name))
      override def entries: IO[scala.collection.immutable.Iterable[(String, String)]] =
        IO.pure(envVars)

  // Mock system properties
  private def mockSystemProperties(sysProps: Map[String, String] = Map.empty): SystemProperties[IO] =
    new SystemProperties[IO]:
      override def get(name: String): IO[Option[String]] =
        IO.pure(sysProps.get(name))
      override def clear(key: String): IO[Option[String]] =
        IO.raiseError(new UnsupportedOperationException("clear not supported in mock"))
      override def set(key: String, value: String): IO[Option[String]] =
        IO.raiseError(new UnsupportedOperationException("set not supported in mock"))

  // Mock WebIdentityCredentialsUtils
  private def mockWebIdentityUtils(
    response:             Option[AwsCredentials] = None,
    shouldFail:           Boolean = false,
    errorMessage:         String = "Mock STS error",
    captureRequestsTo:    Option[Ref[IO, List[WebIdentityTokenCredentialProperties]]] = None
  ): WebIdentityCredentialsUtils[IO] =
    (config: WebIdentityTokenCredentialProperties) => for
      _ <- captureRequestsTo match
        case Some(ref) => ref.update(_ :+ config)
        case None => IO.unit
    yield
      if shouldFail then throw new SdkClientException(errorMessage)
      else
        response.getOrElse(
          AwsSessionCredentials(
            accessKeyId = testAccessKeyId,
            secretAccessKey = testSecretAccessKey,
            sessionToken = testSessionToken,
            validateCredentials = false,
            providerName = None,
            accountId = Some("123456789012"),
            expirationTime = Some(futureExpiration)
          )
        )

  test("resolveCredentials with successful Web Identity Token authentication") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile,
      "AWS_ROLE_ARN"                -> testRoleArn,
      "AWS_ROLE_SESSION_NAME"       -> testSessionName
    )

    given Env[IO]              = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val requestCapture   = Ref.unsafe[IO, List[WebIdentityTokenCredentialProperties]](List.empty)
    val webIdentityUtils = mockWebIdentityUtils(captureRequestsTo = Some(requestCapture))
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    for
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, testAccessKeyId)
          assertEquals(session.secretAccessKey, testSecretAccessKey)
          assertEquals(session.sessionToken, testSessionToken)
          assertEquals(session.accountId, Some("123456789012"))
          assertEquals(session.expirationTime, Some(futureExpiration))
        case _ => fail("Expected AwsSessionCredentials")

      // Verify the correct configuration was passed
      assertEquals(requests.length, 1)
      val request = requests.head
      assertEquals(request.webIdentityTokenFile.toString, testTokenFile)
      assertEquals(request.roleArn, testRoleArn)
      assertEquals(request.roleSessionName, Some(testSessionName))
  }

  test("resolveCredentials with system properties instead of environment variables") {
    val sysProps = Map(
      "aws.webIdentityTokenFile" -> testTokenFile,
      "aws.roleArn"              -> testRoleArn,
      "aws.roleSessionName"      -> testSessionName
    )

    given Env[IO]              = mockEnv(Map.empty)
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val webIdentityUtils = mockWebIdentityUtils()
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    provider.resolveCredentials().map {
        case session: AwsSessionCredentials =>
          assertEquals(session.accessKeyId, testAccessKeyId)
        case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("environment variables take precedence over system properties") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile,
      "AWS_ROLE_ARN"                -> testRoleArn
    )
    
    val sysProps = Map(
      "aws.webIdentityTokenFile" -> "/wrong/path",
      "aws.roleArn"              -> "arn:aws:iam::999999999999:role/wrong-role"
    )

    given Env[IO]              = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val requestCapture   = Ref.unsafe[IO, List[WebIdentityTokenCredentialProperties]](List.empty)
    val webIdentityUtils = mockWebIdentityUtils(captureRequestsTo = Some(requestCapture))
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    for
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => assert(true)
        case _                        => fail("Expected AwsSessionCredentials")

      // Verify environment variables were used
      val request = requests.head
      assertEquals(request.webIdentityTokenFile.toString, testTokenFile)
      assertEquals(request.roleArn, testRoleArn)
  }

  test("resolveCredentials without role session name uses None") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile,
      "AWS_ROLE_ARN"                -> testRoleArn
    )

    given Env[IO]            = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val requestCapture   = Ref.unsafe[IO, List[WebIdentityTokenCredentialProperties]](List.empty)
    val webIdentityUtils = mockWebIdentityUtils(captureRequestsTo = Some(requestCapture))
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    for
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => assert(true)
        case _                        => fail("Expected AwsSessionCredentials")

      // Verify no session name was provided
      val request = requests.head
      assertEquals(request.roleSessionName, None)
  }

  test("fail when token file is not specified") {
    val envVars = Map(
      "AWS_ROLE_ARN" -> testRoleArn
    )

    given Env[IO]            = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val webIdentityUtils = mockWebIdentityUtils()
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load Web Identity Token credentials"))
        assert(exception.getMessage.contains("AWS_WEB_IDENTITY_TOKEN_FILE"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when role ARN is not specified") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile
    )

    given Env[IO]            = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val webIdentityUtils = mockWebIdentityUtils()
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load Web Identity Token credentials"))
        assert(exception.getMessage.contains("AWS_ROLE_ARN"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when both token file and role ARN are missing") {
    given Env[IO]            = mockEnv(Map.empty)
    given SystemProperties[IO] = mockSystemProperties()

    val webIdentityUtils = mockWebIdentityUtils()
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    provider.resolveCredentials().attempt.map {
        case Left(exception: SdkClientException) =>
          assert(exception.getMessage.contains("Unable to load Web Identity Token credentials"))
        case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when WebIdentityCredentialsUtils throws an exception") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile,
      "AWS_ROLE_ARN"                -> testRoleArn
    )

    given Env[IO]            = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val webIdentityUtils = mockWebIdentityUtils(shouldFail = true, errorMessage = "STS service unavailable")
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assertEquals(exception.getMessage, "STS service unavailable")
      case _ => fail("Expected SdkClientException")
    }
  }

  test("implements AwsCredentialsProvider trait") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile,
      "AWS_ROLE_ARN"                -> testRoleArn
    )

    given Env[IO]            = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val webIdentityUtils = mockWebIdentityUtils()
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    // Type check passes - provider implements the trait correctly
    val providerTrait: ldbc.amazon.identity.AwsCredentialsProvider[IO] = provider

    assertIOBoolean(providerTrait.resolveCredentials().map(_.isInstanceOf[AwsCredentials]))
  }

  test("isAvailable returns true when required configuration is present") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile,
      "AWS_ROLE_ARN"                -> testRoleArn
    )

    given Env[IO]              = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    assertIOBoolean(
      WebIdentityTokenFileCredentialsProvider.isAvailable[IO](),
    )
  }

  test("isAvailable returns true with system properties") {
    val sysProps = Map(
      "aws.webIdentityTokenFile" -> testTokenFile,
      "aws.roleArn"              -> testRoleArn
    )

    given Env[IO]              = mockEnv(Map.empty)
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    assertIOBoolean(WebIdentityTokenFileCredentialsProvider.isAvailable[IO]())
  }

  test("isAvailable returns false when token file is missing") {
    val envVars = Map(
      "AWS_ROLE_ARN" -> testRoleArn
    )

    given Env[IO]              = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    assertIO(WebIdentityTokenFileCredentialsProvider.isAvailable[IO](), false)
  }

  test("isAvailable returns false when role ARN is missing") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> testTokenFile
    )

    given Env[IO]              = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    assertIO(WebIdentityTokenFileCredentialsProvider.isAvailable[IO](), false)
  }

  test("isAvailable returns false when both are missing") {
    given Env[IO]              = mockEnv(Map.empty)
    given SystemProperties[IO] = mockSystemProperties()

    assertIO(WebIdentityTokenFileCredentialsProvider.isAvailable[IO](), false)
  }

  test("isAvailable returns false when values are empty or whitespace") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> "   ",
      "AWS_ROLE_ARN"                -> ""
    )

    given Env[IO]              = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    assertIO(WebIdentityTokenFileCredentialsProvider.isAvailable[IO](), false)
  }

  test("handles whitespace trimming in configuration values") {
    val envVars = Map(
      "AWS_WEB_IDENTITY_TOKEN_FILE" -> s"  $testTokenFile  ",
      "AWS_ROLE_ARN"                -> s"  $testRoleArn  ",
      "AWS_ROLE_SESSION_NAME"       -> s"  $testSessionName  "
    )

    given Env[IO]            = mockEnv(envVars)
    given SystemProperties[IO] = mockSystemProperties()

    val requestCapture   = Ref.unsafe[IO, List[WebIdentityTokenCredentialProperties]](List.empty)
    val webIdentityUtils = mockWebIdentityUtils(captureRequestsTo = Some(requestCapture))
    val provider         = WebIdentityTokenFileCredentialsProvider.create[IO](webIdentityUtils)

    for
      credentials <- provider.resolveCredentials()
      requests    <- requestCapture.get
    yield
      credentials match
        case _: AwsSessionCredentials => assert(true)
        case _                        => fail("Expected AwsSessionCredentials")

      // Verify trimmed values were used
      val request = requests.head
      assertEquals(request.webIdentityTokenFile.toString, testTokenFile)
      assertEquals(request.roleArn, testRoleArn)
      assertEquals(request.roleSessionName, Some(testSessionName))
  }