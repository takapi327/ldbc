/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.amazon.auth.credentials

import java.time.Instant

import ldbc.amazon.identity.AwsCredentials

/**
 * AWS session credentials implementation that includes a session token.
 * 
 * Session credentials are temporary credentials that include an access key ID,
 * secret access key, and a session token. These are typically obtained from
 * AWS Security Token Service (STS) and have a limited lifetime. They are commonly
 * used for temporary access, role-based access, or federated access scenarios.
 *
 * @param accessKeyId The AWS access key ID used for authentication
 * @param secretAccessKey The AWS secret access key used for signing requests
 * @param sessionToken The session token that must be included with requests
 * @param validateCredentials Whether these credentials should be validated before use
 * @param providerName Optional name of the credentials provider that created these credentials
 * @param accountId Optional AWS account ID associated with these credentials
 * @param expirationTime Optional expiration time for these credentials
 */
final case class AwsSessionCredentials(
  accessKeyId:         String,
  secretAccessKey:     String,
  sessionToken:        String,
  validateCredentials: Boolean,
  providerName:        Option[String],
  accountId:           Option[String],
  expirationTime:      Option[Instant]
) extends AwsCredentials
