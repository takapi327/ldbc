/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import cats.effect.std.SystemProperties
import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.AwsCredentials
import ldbc.amazon.util.SdkSystemSetting

class SystemPropertyCredentialsProviderTest extends CatsEffectSuite:

  // Test fixtures
  private val testAccessKeyId = "AKIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken = "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"

  // Mock system properties
  private def mockSystemProperties(sysProps: Map[String, String]): SystemProperties[IO] =
    new SystemProperties[IO]:
      override def get(name: String): IO[Option[String]] =
        IO.pure(sysProps.get(name))
      override def clear(key: String): IO[Option[String]] =
        IO.raiseError(new UnsupportedOperationException("clear not supported in mock"))
      override def set(key: String, value: String): IO[Option[String]] =
        IO.raiseError(new UnsupportedOperationException("set not supported in mock"))

  test("resolveCredentials with basic credentials") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, testAccessKeyId)
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
        assertEquals(basic.validateCredentials, false)
        assertEquals(basic.providerName, Some("f"))
        assertEquals(basic.accountId, None)
        assertEquals(basic.expirationTime, None)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("resolveCredentials with session credentials") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.sessionToken" -> testSessionToken
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, testSessionToken)
        assertEquals(session.validateCredentials, false)
        assertEquals(session.providerName, Some("f"))
        assertEquals(session.accountId, None)
        assertEquals(session.expirationTime, None)
      case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("resolveCredentials with account ID") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.accountId" -> "123456789012"
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, testAccessKeyId)
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
        assertEquals(basic.accountId, Some("123456789012"))
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("fail when aws.accessKeyId is missing") {
    val sysProps = Map(
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Access key"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when aws.secretAccessKey is missing") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Secret key"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when no system properties are set") {
    given SystemProperties[IO] = mockSystemProperties(Map.empty)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Access key"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("handle empty system property values") {
    val sysProps = Map(
      "aws.accessKeyId" -> "",
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, "") // Empty string is accepted
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("handle whitespace-only system property values") {
    val sysProps = Map(
      "aws.accessKeyId" -> "   ",
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, "") // Trimmed to empty string
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("handle empty session token") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.sessionToken" -> ""
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, "") // Empty string, not None
      case _ => fail("Expected AwsSessionCredentials even with empty session token")
    }
  }

  test("handle whitespace-only session token") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.sessionToken" -> "   \n  "
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, "") // Trimmed to empty string
      case _ => fail("Expected AwsSessionCredentials even with whitespace session token")
    }
  }

  test("trim whitespace from credentials") {
    val sysProps = Map(
      "aws.accessKeyId" -> s"  $testAccessKeyId  ",
      "aws.secretAccessKey" -> s"\n$testSecretAccessKey\t",
      "aws.sessionToken" -> s"  $testSessionToken  \n"
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, testSessionToken)
      case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("provider name is correct") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map { credentials =>
      assertEquals(credentials.providerName, Some("f")) // BusinessMetricFeatureId.CREDENTIALS_JVM_SYSTEM_PROPERTIES.code
    }
  }

  test("loadSetting method reads from system properties") {
    val testValue = "test-setting-value"
    val sysProps = Map(
      "aws.accessKeyId" -> testValue
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map { result =>
      assertEquals(result, Some(testValue))
    }
  }

  test("loadSetting returns None for missing system property") {
    given SystemProperties[IO] = mockSystemProperties(Map.empty)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map { result =>
      assertEquals(result, None)
    }
  }

  test("implements AwsCredentialsProvider trait") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider: ldbc.amazon.identity.AwsCredentialsProvider[IO] = 
      new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map { credentials =>
      assert(credentials.isInstanceOf[AwsCredentials])
    }
  }

  test("consistent behavior across multiple calls") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.sessionToken" -> testSessionToken
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    for
      credentials1 <- provider.resolveCredentials()
      credentials2 <- provider.resolveCredentials()
    yield
      assertEquals(credentials1.accessKeyId, credentials2.accessKeyId)
      assertEquals(credentials1.secretAccessKey, credentials2.secretAccessKey)
      credentials1 match
        case session1: AwsSessionCredentials =>
          credentials2 match
            case session2: AwsSessionCredentials =>
              assertEquals(session1.sessionToken, session2.sessionToken)
            case _ => fail("Both credentials should be AwsSessionCredentials")
        case _ => fail("Expected AwsSessionCredentials")
  }

  test("validates credentials format") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map { credentials =>
      // Verify access key format (starts with AKIA for IAM users)
      assert(credentials.accessKeyId.startsWith("AKIA"))
      assert(credentials.accessKeyId.length >= 16)
      
      // Verify secret key is not empty and has reasonable length
      assert(credentials.secretAccessKey.nonEmpty)
      assert(credentials.secretAccessKey.length >= 20)
    }
  }

  test("system property keys are correct") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.sessionToken" -> testSessionToken,
      "aws.accountId" -> "123456789012"
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    for
      accessKey <- provider.loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID)
      secretKey <- provider.loadSetting(SdkSystemSetting.AWS_SECRET_ACCESS_KEY)
      sessionToken <- provider.loadSetting(SdkSystemSetting.AWS_SESSION_TOKEN)
      accountId <- provider.loadSetting(SdkSystemSetting.AWS_ACCOUNT_ID)
    yield
      assertEquals(accessKey, Some(testAccessKeyId))
      assertEquals(secretKey, Some(testSecretAccessKey))
      assertEquals(sessionToken, Some(testSessionToken))
      assertEquals(accountId, Some("123456789012"))
  }

  test("different from environment variables - uses system properties") {
    // This test ensures that SystemPropertyCredentialsProvider only reads from system properties,
    // not environment variables, even if the system property names are similar
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId, // System property format
      "aws.secretAccessKey" -> testSecretAccessKey,
      // Note: NO AWS_ACCESS_KEY_ID (environment variable format)
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, testAccessKeyId)
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("case sensitivity of system property names") {
    // System properties are case-sensitive in Java
    val sysProps = Map(
      "AWS.ACCESSKEYID" -> testAccessKeyId,  // Wrong case
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Access key"))
      case Left(other) => fail(s"Expected SdkClientException, got ${other.getClass.getSimpleName}")
      case Right(_) => fail("Should fail with incorrect case system property")
    }
  }

  test("alternative system property names are not supported") {
    val sysProps = Map(
      "aws.access.key.id" -> testAccessKeyId,  // Not aws.accessKeyId
      "aws.secret.access.key" -> testSecretAccessKey  // Not aws.secretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
      case Left(other) => fail(s"Expected SdkClientException, got ${other.getClass.getSimpleName}")
      case Right(_) => fail("Should fail with non-standard system property names")
    }
  }

  test("credentials validation is disabled by default") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.validateCredentials, false)
      case session: AwsSessionCredentials =>
        assertEquals(session.validateCredentials, false)
    }
  }

  test("handles all supported AWS system properties") {
    val sysProps = Map(
      "aws.accessKeyId" -> testAccessKeyId,
      "aws.secretAccessKey" -> testSecretAccessKey,
      "aws.sessionToken" -> testSessionToken,
      "aws.accountId" -> "123456789012"
    )
    
    given SystemProperties[IO] = mockSystemProperties(sysProps)

    val provider = new SystemPropertyCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, testSessionToken)
        assertEquals(session.accountId, Some("123456789012"))
        assertEquals(session.validateCredentials, false)
        assertEquals(session.providerName, Some("f"))
        assert(session.expirationTime.isEmpty)
      case _ => fail("Expected AwsSessionCredentials")
    }
  }