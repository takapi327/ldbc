/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import cats.effect.std.Env
import cats.effect.IO

import munit.CatsEffectSuite

import ldbc.amazon.exception.SdkClientException
import ldbc.amazon.identity.AwsCredentials
import ldbc.amazon.util.SdkSystemSetting

class EnvironmentVariableCredentialsProviderTest extends CatsEffectSuite:

  // Test fixtures
  private val testAccessKeyId = "AKIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken = "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"

  // Mock environment
  private def mockEnv(envVars: Map[String, String]): Env[IO] =
    new Env[IO]:
      override def get(name: String): IO[Option[String]] =
        IO.pure(envVars.get(name))
      override def entries: IO[scala.collection.immutable.Iterable[(String, String)]] =
        IO.pure(envVars)

  test("resolveCredentials with basic credentials") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, testAccessKeyId)
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
        assertEquals(basic.validateCredentials, false)
        assertEquals(basic.providerName, Some("g"))
        assertEquals(basic.accountId, None)
        assertEquals(basic.expirationTime, None)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("resolveCredentials with session credentials") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey,
      "AWS_SESSION_TOKEN" -> testSessionToken
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, testSessionToken)
        assertEquals(session.validateCredentials, false)
        assertEquals(session.providerName, Some("g"))
        assertEquals(session.accountId, None)
        assertEquals(session.expirationTime, None)
      case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("fail when AWS_ACCESS_KEY_ID is missing") {
    val envVars = Map(
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Access key"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when AWS_SECRET_ACCESS_KEY is missing") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Secret key"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("fail when no environment variables are set") {
    given Env[IO] = mockEnv(Map.empty)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load credentials from system settings"))
        assert(exception.getMessage.contains("Access key"))
      case _ => fail("Expected SdkClientException")
    }
  }

  test("handle empty environment variable values") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> "",
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, "") // Empty string is accepted
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("handle whitespace-only environment variable values") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> "   ",
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.accessKeyId, "") // Trimmed to empty string
        assertEquals(basic.secretAccessKey, testSecretAccessKey)
      case _ => fail("Expected AwsBasicCredentials")
    }
  }

  test("handle empty session token") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey,
      "AWS_SESSION_TOKEN" -> ""
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, "") // Empty string, not None
      case _ => fail("Expected AwsSessionCredentials even with empty session token")
    }
  }

  test("handle whitespace-only session token") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey,
      "AWS_SESSION_TOKEN" -> "   \n  "
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, "") // Trimmed to empty string
      case _ => fail("Expected AwsSessionCredentials even with whitespace session token")
    }
  }

  test("trim whitespace from credentials") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> s"  $testAccessKeyId  ",
      "AWS_SECRET_ACCESS_KEY" -> s"\n$testSecretAccessKey\t",
      "AWS_SESSION_TOKEN" -> s"  $testSessionToken  \n"
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case session: AwsSessionCredentials =>
        assertEquals(session.accessKeyId, testAccessKeyId)
        assertEquals(session.secretAccessKey, testSecretAccessKey)
        assertEquals(session.sessionToken, testSessionToken)
      case _ => fail("Expected AwsSessionCredentials")
    }
  }

  test("provider name is correct") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map { credentials =>
      assertEquals(credentials.providerName, Some("g")) // BusinessMetricFeatureId.CREDENTIALS_ENV_VARS.code
    }
  }

  test("loadSetting method reads from environment") {
    val testValue = "test-setting-value"
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testValue
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map { result =>
      assertEquals(result, Some(testValue))
    }
  }

  test("loadSetting returns None for missing environment variable") {
    given Env[IO] = mockEnv(Map.empty)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.loadSetting(SdkSystemSetting.AWS_ACCESS_KEY_ID).map { result =>
      assertEquals(result, None)
    }
  }

  test("implements AwsCredentialsProvider trait") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider: ldbc.amazon.identity.AwsCredentialsProvider[IO] = 
      new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map { credentials =>
      assert(credentials.isInstanceOf[AwsCredentials])
    }
  }

  test("consistent behavior across multiple calls") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey,
      "AWS_SESSION_TOKEN" -> testSessionToken
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

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
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map { credentials =>
      // Verify access key format (starts with AKIA for IAM users)
      assert(credentials.accessKeyId.startsWith("AKIA"))
      assert(credentials.accessKeyId.length >= 16)
      
      // Verify secret key is not empty and has reasonable length
      assert(credentials.secretAccessKey.nonEmpty)
      assert(credentials.secretAccessKey.length >= 20)
    }
  }

  test("case sensitivity of environment variable names") {
    val envVars = Map(
      "aws_access_key_id" -> testAccessKeyId,  // lowercase
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    // Should fail because AWS expects exact case
    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load"))
      case Left(other) => fail(s"Expected SdkClientException, got ${other.getClass.getSimpleName}")
      case Right(_) => fail("Should fail with incorrect case environment variable")
    }
  }

  test("alternative environment variable names are not supported") {
    val envVars = Map(
      "AWS_ACCESS_KEY" -> testAccessKeyId,  // Not AWS_ACCESS_KEY_ID
      "AWS_SECRET_KEY" -> testSecretAccessKey  // Not AWS_SECRET_ACCESS_KEY
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().attempt.map {
      case Left(exception: SdkClientException) =>
        assert(exception.getMessage.contains("Unable to load"))
      case Left(other) => fail(s"Expected SdkClientException, got ${other.getClass.getSimpleName}")
      case Right(_) => fail("Should fail with non-standard environment variable names")
    }
  }

  test("credentials validation is disabled by default") {
    val envVars = Map(
      "AWS_ACCESS_KEY_ID" -> testAccessKeyId,
      "AWS_SECRET_ACCESS_KEY" -> testSecretAccessKey
    )
    
    given Env[IO] = mockEnv(envVars)

    val provider = new EnvironmentVariableCredentialsProvider[IO]

    provider.resolveCredentials().map {
      case basic: AwsBasicCredentials =>
        assertEquals(basic.validateCredentials, false)
      case session: AwsSessionCredentials =>
        assertEquals(session.validateCredentials, false)
    }
  }