/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.token

import java.net.URLDecoder
import java.time.Instant

import cats.effect.IO

import fs2.hashing.Hashing

import munit.CatsEffectSuite

import ldbc.amazon.auth.credentials.{ AwsBasicCredentials, AwsSessionCredentials }

class RdsIamAuthTokenGeneratorTest extends CatsEffectSuite:

  // Test fixtures
  private val testHostname = "my-db-instance.ap-northeast-1.rds.amazonaws.com"
  private val testPort     = 3306
  private val testUsername = "db_user"
  private val testRegion   = "ap-northeast-1"

  private val testAccessKeyId     = "AKIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken    = "IQoJb3JpZ2luX2VjECoaCXVzLWVhc3QtMSJHMEUCIQDtqstfDEaRfZKFK5Z2n2CnP3"

  private def createGenerator(): RdsIamAuthTokenGenerator[IO] =
    new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = testPort,
      username = testUsername,
      region   = testRegion
    )

  test("generateToken with basic credentials") {
    val generator   = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = Some("test-provider"),
      accountId           = Some("123456789012"),
      expirationTime      = Some(Instant.now().plusSeconds(3600))
    )

    generator.generateToken(credentials).map { token =>
      // Verify token structure
      assert(token.startsWith(s"$testHostname:$testPort/?"))
      assert(token.contains("Action=connect"))
      assert(token.contains(s"DBUser=$testUsername"))
      assert(token.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"))
      assert(token.contains(s"X-Amz-Credential=$testAccessKeyId"))
      assert(token.contains("X-Amz-Date="))
      assert(token.contains("X-Amz-Expires=900"))
      assert(token.contains("X-Amz-SignedHeaders=host"))
      assert(token.contains("X-Amz-Signature="))

      // Should NOT contain session token for basic credentials
      assert(!token.contains("X-Amz-Security-Token"))
    }
  }

  test("generateToken with session credentials") {
    val generator   = createGenerator()
    val credentials = AwsSessionCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      sessionToken        = testSessionToken,
      validateCredentials = true,
      providerName        = Some("test-provider"),
      accountId           = Some("123456789012"),
      expirationTime      = Some(Instant.now().plusSeconds(3600))
    )

    generator.generateToken(credentials).map { token =>
      // Verify token structure
      assert(token.startsWith(s"$testHostname:$testPort/?"))
      assert(token.contains("Action=connect"))
      assert(token.contains(s"DBUser=$testUsername"))
      assert(token.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256"))
      assert(token.contains(s"X-Amz-Credential=$testAccessKeyId"))
      assert(token.contains("X-Amz-Date="))
      assert(token.contains("X-Amz-Expires=900"))
      assert(token.contains("X-Amz-SignedHeaders=host"))
      assert(token.contains("X-Amz-Signature="))

      // Should contain session token for session credentials
      assert(token.contains("X-Amz-Security-Token="))
      assert(token.contains(java.net.URLEncoder.encode(testSessionToken, "UTF-8")))
    }
  }

  test("generateToken produces consistent output structure") {
    val generator   = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    for {
      token1 <- generator.generateToken(credentials)
      token2 <- generator.generateToken(credentials)
    } yield {
      // Tokens may differ due to timestamp, but structure should be consistent
      assert(token1.startsWith(s"$testHostname:$testPort/?"))
      assert(token2.startsWith(s"$testHostname:$testPort/?"))

      // Both should contain the same parameter names
      val params1 = token1.split("\\?", 2)(1).split("&").map(_.split("=")(0)).toSet
      val params2 = token2.split("\\?", 2)(1).split("&").map(_.split("=")(0)).toSet
      assertEquals(params1, params2)
    }
  }

  test("token format validation") {
    val generator   = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      // Parse the token URL
      val parts = token.split("\\?", 2)
      assertEquals(parts.length, 2)

      val hostPart  = parts(0)
      val queryPart = parts(1)

      assertEquals(hostPart, s"$testHostname:$testPort/")

      // Parse query parameters
      val queryParams = queryPart
        .split("&")
        .map { param =>
          val kv = param.split("=", 2)
          kv(0) -> URLDecoder.decode(kv(1), "UTF-8")
        }
        .toMap

      // Verify required parameters
      assertEquals(queryParams("Action"), "connect")
      assertEquals(queryParams("DBUser"), testUsername)
      assertEquals(queryParams("X-Amz-Algorithm"), "AWS4-HMAC-SHA256")
      assertEquals(queryParams("X-Amz-Expires"), "900")
      assertEquals(queryParams("X-Amz-SignedHeaders"), "host")

      // Verify credential format
      val credential = queryParams("X-Amz-Credential")
      assert(credential.startsWith(testAccessKeyId))
      assert(credential.contains(testRegion))
      assert(credential.contains("rds-db"))
      assert(credential.contains("aws4_request"))

      // Verify date format (should be ISO 8601 basic format)
      val date = queryParams("X-Amz-Date")
      assert(date.matches("""\d{8}T\d{6}Z"""))

      // Verify signature is present and non-empty
      val signature = queryParams("X-Amz-Signature")
      assert(signature.nonEmpty)
      assert(signature.matches("[0-9a-f]+")) // Should be lowercase hex
    }
  }

  test("token parameter ordering") {
    val generator   = createGenerator()
    val credentials = AwsSessionCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      sessionToken        = testSessionToken,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      // Extract query string
      val queryString  = token.split("\\?", 2)(1)
      val paramsBefore = queryString.split("&").filterNot(_.startsWith("X-Amz-Signature=")).toList
      val paramsSorted = paramsBefore.sortBy(_.split("=")(0))

      // Parameters should be in alphabetical order (required for AWS SigV4)
      assertEquals(paramsBefore, paramsSorted)
    }
  }

  test("different credentials produce different signatures") {
    val generator = createGenerator()

    val credentials1 = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    val credentials2 = AwsBasicCredentials(
      accessKeyId         = "AKIADIFFERENTKEY",
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    for {
      token1 <- generator.generateToken(credentials1)
      token2 <- generator.generateToken(credentials2)
    } yield {
      assert(token1 != token2)

      // Extract signatures
      val sig1 = token1.split("X-Amz-Signature=")(1)
      val sig2 = token2.split("X-Amz-Signature=")(1)
      assert(sig1 != sig2)
    }
  }

  test("different regions produce different tokens") {
    val generator1 = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = testPort,
      username = testUsername,
      region   = "us-east-1"
    )

    val generator2 = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = testPort,
      username = testUsername,
      region   = "us-west-2"
    )

    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    for {
      token1 <- generator1.generateToken(credentials)
      token2 <- generator2.generateToken(credentials)
    } yield {
      assert(token1 != token2)

      // Verify region is included in credential scope
      assert(token1.contains("us-east-1"))
      assert(token2.contains("us-west-2"))
    }
  }

  test("different hostnames produce different tokens") {
    val generator1 = new RdsIamAuthTokenGenerator[IO](
      hostname = "host1.region.rds.amazonaws.com",
      port     = testPort,
      username = testUsername,
      region   = testRegion
    )

    val generator2 = new RdsIamAuthTokenGenerator[IO](
      hostname = "host2.region.rds.amazonaws.com",
      port     = testPort,
      username = testUsername,
      region   = testRegion
    )

    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    for {
      token1 <- generator1.generateToken(credentials)
      token2 <- generator2.generateToken(credentials)
    } yield {
      assert(token1 != token2)
      assert(token1.startsWith("host1.region.rds.amazonaws.com"))
      assert(token2.startsWith("host2.region.rds.amazonaws.com"))
    }
  }

  test("different ports produce different tokens") {
    val generator1 = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = 3306,
      username = testUsername,
      region   = testRegion
    )

    val generator2 = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = 5432,
      username = testUsername,
      region   = testRegion
    )

    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    for {
      token1 <- generator1.generateToken(credentials)
      token2 <- generator2.generateToken(credentials)
    } yield {
      assert(token1 != token2)
      assert(token1.contains(":3306/"))
      assert(token2.contains(":5432/"))
    }
  }

  test("different usernames produce different tokens") {
    val generator1 = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = testPort,
      username = "user1",
      region   = testRegion
    )

    val generator2 = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = testPort,
      username = "user2",
      region   = testRegion
    )

    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    for {
      token1 <- generator1.generateToken(credentials)
      token2 <- generator2.generateToken(credentials)
    } yield {
      assert(token1 != token2)
      assert(token1.contains("DBUser=user1"))
      assert(token2.contains("DBUser=user2"))
    }
  }

  test("URL encoding is applied correctly") {
    val generator = new RdsIamAuthTokenGenerator[IO](
      hostname = testHostname,
      port     = testPort,
      username = "user with spaces@domain.com",
      region   = testRegion
    )

    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      // Username should be URL encoded
      assert(token.contains("DBUser=user%20with%20spaces%40domain.com"))

      // Verify AWS-specific encoding rules
      assert(!token.contains("+")) // Spaces should be %20, not +

      // Extract all parameters to verify proper encoding
      val queryString = token.split("\\?", 2)(1)
      val params      = queryString.split("&")

      params.foreach { param =>
        val parts = param.split("=", 2)
        if parts.length == 2 then {
          val value = parts(1)

          // Verify no invalid characters remain unencoded
          assert(!value.contains(" "))
          assert(!value.contains("@"))
        }
      }
    }
  }

  test("signature calculation consistency") {
    val generator   = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      val signature = token.split("X-Amz-Signature=")(1)

      // Signature should be 64 characters (SHA-256 hex)
      assertEquals(signature.length, 64)

      // Signature should be lowercase hex
      assert(signature.forall(c => c.isDigit || ('a' <= c && c <= 'f')))
    }
  }

  test("implements AuthTokenGenerator trait") {
    val generator: AuthTokenGenerator[IO] = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      assert(token.nonEmpty)
      assert(token.contains(testHostname))
    }
  }

  test("token contains correct service and terminator") {
    val generator   = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      // Extract credential scope from the credential parameter
      val queryString     = token.split("\\?", 2)(1)
      val credentialParam = queryString
        .split("&")
        .find(_.startsWith("X-Amz-Credential="))
        .getOrElse(fail("X-Amz-Credential parameter not found"))

      val credentialValue = URLDecoder.decode(credentialParam.split("=")(1), "UTF-8")

      // Credential format: accessKeyId/date/region/service/terminator
      val parts = credentialValue.split("/")
      assertEquals(parts.length, 5)
      assertEquals(parts(0), testAccessKeyId)
      assertEquals(parts(2), testRegion)
      assertEquals(parts(3), "rds-db")       // SERVICE constant
      assertEquals(parts(4), "aws4_request") // TERMINATOR constant
    }
  }

  test("token expiration is set to 900 seconds") {
    val generator   = createGenerator()
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = None,
      accountId           = None,
      expirationTime      = None
    )

    generator.generateToken(credentials).map { token =>
      assert(token.contains("X-Amz-Expires=900"))
    }
  }
