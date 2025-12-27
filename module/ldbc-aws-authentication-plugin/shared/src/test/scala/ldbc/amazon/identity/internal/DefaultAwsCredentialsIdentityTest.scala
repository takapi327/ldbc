/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.identity.internal

import java.time.Instant

import munit.CatsEffectSuite

import ldbc.amazon.identity.AwsCredentialsIdentity

class DefaultAwsCredentialsIdentityTest extends CatsEffectSuite:

  private val testAccessKeyId     = "AKIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testAccountId       = "123456789012"
  private val testProviderName    = "test-provider"
  private val testExpirationTime  = Instant.parse("2024-12-06T12:00:00Z")

  test("create minimal credentials identity") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None,
      expirationTime  = None,
      providerName    = None
    )

    assertEquals(credentials.accessKeyId, testAccessKeyId)
    assertEquals(credentials.secretAccessKey, testSecretAccessKey)
    assertEquals(credentials.accountId, None)
    assertEquals(credentials.expirationTime, None)
    assertEquals(credentials.providerName, None)
  }

  test("create full credentials identity with all fields") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    assertEquals(credentials.accessKeyId, testAccessKeyId)
    assertEquals(credentials.secretAccessKey, testSecretAccessKey)
    assertEquals(credentials.accountId, Some(testAccountId))
    assertEquals(credentials.expirationTime, Some(testExpirationTime))
    assertEquals(credentials.providerName, Some(testProviderName))
  }

  test("create credentials using factory method") {
    val credentials = AwsCredentialsIdentity.create(testAccessKeyId, testSecretAccessKey)

    assertEquals(credentials.accessKeyId, testAccessKeyId)
    assertEquals(credentials.secretAccessKey, testSecretAccessKey)
    assertEquals(credentials.accountId, None)
    assertEquals(credentials.expirationTime, None)
    assertEquals(credentials.providerName, None)
  }

  test("toString should not expose secret access key") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    val stringRepresentation = credentials.toString

    // Should contain access key and other safe fields
    assert(stringRepresentation.contains(testAccessKeyId))
    assert(stringRepresentation.contains(testAccountId))
    assert(stringRepresentation.contains(testProviderName))

    // Should NOT contain secret access key
    assert(!stringRepresentation.contains(testSecretAccessKey))

    // Should contain class name
    assert(stringRepresentation.contains("AwsCredentialsIdentity"))
  }

  test("toString with minimal fields") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None,
      expirationTime  = None,
      providerName    = None
    )

    val stringRepresentation = credentials.toString

    assertEquals(stringRepresentation, s"AwsCredentialsIdentity(accessKeyId=$testAccessKeyId)")
    assert(!stringRepresentation.contains(testSecretAccessKey))
  }

  test("toString with partial fields") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = None
    )

    val stringRepresentation = credentials.toString
    val expected             = s"AwsCredentialsIdentity(accessKeyId=$testAccessKeyId, accountId=$testAccountId)"

    assertEquals(stringRepresentation, expected)
    assert(!stringRepresentation.contains(testSecretAccessKey))
  }

  test("equals should work correctly for identical credentials") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    assertEquals(credentials1, credentials2)
    assertEquals(credentials1.hashCode(), credentials2.hashCode())
  }

  test("equals should work for credentials with different expiration and provider") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(Instant.parse("2025-01-01T00:00:00Z")), // Different expiration
      providerName    = Some("different-provider")                   // Different provider
    )

    // Should still be equal because equals only compares key fields
    assertEquals(credentials1, credentials2)
    assertEquals(credentials1.hashCode(), credentials2.hashCode())
  }

  test("equals should return false for different access key") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = "DIFFERENT_ACCESS_KEY",
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    assert(credentials1 != credentials2)
    assert(credentials1.hashCode() != credentials2.hashCode())
  }

  test("equals should return false for different secret access key") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = "DIFFERENT_SECRET_KEY",
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    assert(credentials1 != credentials2)
    assert(credentials1.hashCode() != credentials2.hashCode())
  }

  test("equals should return false for different account ID") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some("999999999999"),
      expirationTime  = None,
      providerName    = None
    )

    assert(credentials1 != credentials2)
    assert(credentials1.hashCode() != credentials2.hashCode())
  }

  test("equals should handle None vs Some account ID") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None,
      expirationTime  = None,
      providerName    = None
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    assert(credentials1 != credentials2)
    assert(credentials1.hashCode() != credentials2.hashCode())
  }

  test("equals should return false for null object") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None,
      expirationTime  = None,
      providerName    = None
    )

    assert(credentials != null)
    assert(!credentials.equals(null))
  }

  test("equals should return false for different class") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None,
      expirationTime  = None,
      providerName    = None
    )

    assert(credentials.toString != "not a credentials object")
    assert(!credentials.equals("not a credentials object"))
  }

  test("equals should work with same concrete type") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    val credentials2 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None, // Different account ID
      expirationTime  = None,
      providerName    = None
    )

    val credentials3 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId), // Same account ID
      expirationTime  = None,
      providerName    = None
    )

    // Different account IDs (Some vs None)
    assert(credentials1 != credentials2)

    // Same account IDs
    assertEquals(credentials1, credentials3)
  }

  test("equals should return false for different AwsCredentialsIdentity implementations") {
    val credentials1 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = None,
      providerName    = None
    )

    // Factory method creates same type, but let's test interface behavior
    val credentials2: AwsCredentialsIdentity = AwsCredentialsIdentity.create(testAccessKeyId, testSecretAccessKey)

    // These should be equal since factory creates DefaultAwsCredentialsIdentity
    // but with different accountId (None vs Some)
    assert(credentials1 != credentials2)

    // Test with exactly same fields through factory
    val credentials3 = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = None,
      expirationTime  = None,
      providerName    = None
    )

    assertEquals(credentials2, credentials3)
  }

  test("hashCode should be consistent") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    val hashCode1 = credentials.hashCode()
    val hashCode2 = credentials.hashCode()

    assertEquals(hashCode1, hashCode2)
  }

  test("implements Identity interface correctly") {
    val credentials: ldbc.amazon.identity.Identity = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    assertEquals(credentials.expirationTime, Some(testExpirationTime))
    assertEquals(credentials.providerName, Some(testProviderName))
  }

  test("implements AwsCredentialsIdentity interface correctly") {
    val credentials: AwsCredentialsIdentity = DefaultAwsCredentialsIdentity(
      accessKeyId     = testAccessKeyId,
      secretAccessKey = testSecretAccessKey,
      accountId       = Some(testAccountId),
      expirationTime  = Some(testExpirationTime),
      providerName    = Some(testProviderName)
    )

    assertEquals(credentials.accessKeyId, testAccessKeyId)
    assertEquals(credentials.secretAccessKey, testSecretAccessKey)
    assertEquals(credentials.accountId, Some(testAccountId))
    assertEquals(credentials.expirationTime, Some(testExpirationTime))
    assertEquals(credentials.providerName, Some(testProviderName))
  }

  test("handle empty strings") {
    val credentials = DefaultAwsCredentialsIdentity(
      accessKeyId     = "",
      secretAccessKey = "",
      accountId       = Some(""),
      expirationTime  = None,
      providerName    = Some("")
    )

    assertEquals(credentials.accessKeyId, "")
    assertEquals(credentials.secretAccessKey, "")
    assertEquals(credentials.accountId, Some(""))
    assertEquals(credentials.providerName, Some(""))
  }
