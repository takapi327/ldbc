/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import munit.CatsEffectSuite

/**
 * Verification test for the security finding: the default case-class `toString`
 * of [[AwsBasicCredentials]] / [[AwsSessionCredentials]] exposes the secret access
 * key (and session token) in plaintext.
 *
 * These assertions mirror the secure-toString contract already enforced for
 * `DefaultAwsCredentialsIdentity` (see DefaultAwsCredentialsIdentityTest, test
 * "toString should not expose secret access key"). If the finding is real, the
 * two tests below FAIL because the secret leaks into the string representation.
 */
class AwsCredentialsToStringTest extends CatsEffectSuite:

  private val testAccessKeyId     = "AKIAIOSFODNN7EXAMPLE"
  private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYzEXAMPLEKEY"
  private val testSessionToken    = "FQoGZXIvYXdzEXAMPLESESSIONTOKEN1234567890"

  test("AwsBasicCredentials.toString should not expose secret access key") {
    val credentials = AwsBasicCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      validateCredentials = true,
      providerName        = Some("test-provider"),
      accountId           = Some("123456789012"),
      expirationTime      = None
    )

    val rendered = credentials.toString

    // The access key ID is public and may appear.
    assert(rendered.contains(testAccessKeyId))

    // The secret access key MUST NOT appear in any string representation.
    assert(
      !rendered.contains(testSecretAccessKey),
      s"secretAccessKey leaked in toString: $rendered"
    )
  }

  test("AwsSessionCredentials.toString should not expose secret access key or session token") {
    val credentials = AwsSessionCredentials(
      accessKeyId         = testAccessKeyId,
      secretAccessKey     = testSecretAccessKey,
      sessionToken        = testSessionToken,
      validateCredentials = true,
      providerName        = Some("test-provider"),
      accountId           = Some("123456789012"),
      expirationTime      = None
    )

    val rendered = credentials.toString

    assert(rendered.contains(testAccessKeyId))

    assert(
      !rendered.contains(testSecretAccessKey),
      s"secretAccessKey leaked in toString: $rendered"
    )
    assert(
      !rendered.contains(testSessionToken),
      s"sessionToken leaked in toString: $rendered"
    )
  }
