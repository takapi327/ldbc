/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.time.Instant

import ldbc.amazon.identity.AwsCredentials

/**
 * Basic AWS credentials implementation that contains an access key ID and secret access key.
 * 
 * This implementation is used for standard AWS access credentials that consist of a public
 * access key ID and a private secret access key. Basic credentials do not include session
 * tokens and are typically used for long-term access.
 *
 * @param accessKeyId The AWS access key ID used for authentication
 * @param secretAccessKey The AWS secret access key used for signing requests
 * @param validateCredentials Whether these credentials should be validated before use
 * @param providerName Optional name of the credentials provider that created these credentials
 * @param accountId Optional AWS account ID associated with these credentials
 * @param expirationTime Optional expiration time for these credentials, if applicable
 */
final case class AwsBasicCredentials(
  accessKeyId:         String,
  secretAccessKey:     String,
  validateCredentials: Boolean,
  providerName:        Option[String],
  accountId:           Option[String],
  expirationTime:      Option[Instant]
) extends AwsCredentials
